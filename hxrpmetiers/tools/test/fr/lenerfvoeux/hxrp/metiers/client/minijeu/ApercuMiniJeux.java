package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.minijeu.BancMiniJeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Rend de vraies images des mini-jeux hors Minecraft (même code de scène que le jeu),
 * posées sur un faux décor, à différents moments d'une partie jouée par un bot.
 *
 *   java -cp build/banc fr.lenerfvoeux.hxrp.metiers.client.minijeu.ApercuMiniJeux [geste] [recette] [rang]
 */
public final class ApercuMiniJeux {
    static final String RES = "src/main/resources/assets/hxrpmetiers/";
    static final Map<String, FoodEntry> DB = new LinkedHashMap<>();

    public static void main(String[] a) throws Exception {
        charger();
        String[][] cas = {
                {"Couper", "salade_de_tomates"}, {"Couper", "carottes_rapees"}, {"Couper", "steak_frites"}, {"Couper", "poisson_pane"},
                {"Couper", "crevettes_sautees_a_l_ail"}, {"Couper", "tzatziki"}, {"Couper", "salade_de_fruits"}, {"Couper", "sandwich_jambon_beurre"},
                {"Étaler", "pizza_margherita"}, {"Pétrir", "pate_a_pain"}, {"Façonner", "pain"}, {"Fouetter", "pate_a_crepe"},
                {"Mélanger", "salade_verte"}, {"Piler", "farine"}, {"Tamiser", "farine"}, {"Saisir", "steak_frites"}, {"Bouillir", "riz_cuit"},
                {"Four", "pizza_margherita"}, {"Griller", "brochettes_de_poulet"}, {"Frire", "frites"}, {"Mijoter", "boeuf_bourguignon"},
                {"Presser", "jus_d_orange"}, {"Secouer", "milkshake_vanille"}, {"Assaisonner", "salade_verte"}};
        new File("tools/preview/out").mkdirs();
        int rang = a.length > 2 ? Integer.parseInt(a[2]) : 1;
        for (String[] k : cas) {
            if (a.length > 0 && !Jeux.cle(k[0]).equals(Jeux.cle(a[0]))) continue;
            if (a.length > 1 && !k[1].equals(a[1])) continue;
            apercu(k[0], k[1], rang);
        }
    }

    static void apercu(String geste, String recette, int rang) throws Exception {
        FoodEntry r = DB.get(recette);
        Contexte c = Contexte.construire(r, geste, Math.max(1, r.steps.indexOf(geste) + 1), rang, DB::get, ApercuMiniJeux::icone);
        MiniJeu j = Jeux.creer(geste, rang, 4242, "Four".equals(geste) ? 6 : 4);
        BancMiniJeux.Bot bot = BancMiniJeux.parfait(geste);
        Journal jl = new Journal();
        Random rnd = new Random(1);
        int duree = 0;
        { // durée d'une partie parfaite, pour placer les images
            MiniJeu j2 = Jeux.creer(geste, rang, 4242, "Four".equals(geste) ? 6 : 4);
            while (!j2.fini) { bot.jouer(j2, new Journal(), rnd); if (!j2.fini) j2.avancer(); }
            duree = j2.t;
        }
        int[] instants = {-1, (int) (duree * 0.22), (int) (duree * 0.5), (int) (duree * 0.78), duree + 400};
        int S = 2, W = Jeux.W * S, H = Jeux.H * S;
        BufferedImage out = new BufferedImage(W * 3, H * 2, BufferedImage.TYPE_INT_ARGB);
        Toile t = new Toile(Jeux.W, Jeux.H);
        int idx = 0;
        for (int ins : instants) {
            while (!j.fini && j.t < ins) { bot.jouer(j, jl, rnd); if (!j.fini) j.avancer(); }
            int etat = ins < 0 ? Rendu.INTRO : j.fini ? Rendu.FIN : Rendu.JEU;
            Rendu.image(t, j, c, j.t + 40, etat, 200, 100, j.noteFinale());
            BufferedImage fond = decor(W, H, idx);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) {
                    int p = t.px[(y / S) * Jeux.W + x / S];
                    fond.setRGB(x, y, blend(fond.getRGB(x, y), p));
                }
            java.awt.Graphics g = out.getGraphics();
            g.drawImage(fond, (idx % 3) * W, (idx / 3) * H, null);
            idx++;
        }
        String nom = "tools/preview/out/mj_" + Jeux.cle(geste) + (recette.isEmpty() ? "" : "_" + recette) + ".png";
        ImageIO.write(out, "png", new File(nom));
        System.out.printf("%-12s %-28s note %.1f  -> %s%n", geste, recette, j.noteFinale(), nom);
    }

    static int blend(int dst, int src) {
        int a = src >>> 24;
        if (a == 0) return dst;
        double k = a / 255.0;
        int r = (int) (((src >> 16) & 255) * k + ((dst >> 16) & 255) * (1 - k));
        int g = (int) (((src >> 8) & 255) * k + ((dst >> 8) & 255) * (1 - k));
        int b = (int) ((src & 255) * k + (dst & 255) * (1 - k));
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** Faux décor de monde Minecraft (ciel, mur de pierre, sol), assombri comme par le menu. */
    static BufferedImage decor(int W, int H, int v) {
        BufferedImage im = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Random r = new Random(7 + v);
        for (int y = 0; y < H; y++)
            for (int x = 0; x < W; x++) {
                int c;
                if (y < H * 0.35) c = mix(0xFF8FB8F0, 0xFFC8E0FF, y / (H * 0.35));
                else if (y < H * 0.7) { int bx = (x / 32) % 2, by = (y / 32) % 2; c = (bx ^ by) == 0 ? 0xFF8A8A8A : 0xFF7E7E7E; if (r.nextInt(9) == 0) c = 0xFF6E6E6E; }
                else { c = ((x / 32 + y / 32) % 2 == 0) ? 0xFF9C7A4A : 0xFF8E6E40; if (r.nextInt(7) == 0) c = 0xFF7A5E36; }
                im.setRGB(x, y, blend(c, 0x70000000));
            }
        return im;
    }

    static int mix(int a, int b, double t) { return Toile.melange(a, b, t); }

    static int[] icone(String id) {
        try {
            File f = new File(RES + "textures/items/" + id + ".png");
            if (!f.exists()) return null;
            BufferedImage im = ImageIO.read(f);
            int[] px = new int[32 * 32];
            im.getRGB(0, 0, 32, 32, px, 0, 32);
            return px;
        } catch (Exception e) { return null; }
    }

    // ------------------------------------------------------------ lecture minimale de food.json
    static void charger() throws Exception {
        String s = new String(Files.readAllBytes(new File(RES + "data/food.json").toPath()), StandardCharsets.UTF_8);
        Object root = new Json(s).valeur();
        for (Object groupe : ((Map<?, ?>) root).values())
            for (Object o : (List<?>) groupe) {
                Map<?, ?> m = (Map<?, ?>) o;
                FoodEntry e = new FoodEntry();
                e.id = (String) m.get("id");
                e.name = (String) m.get("name");
                e.cat = (String) m.get("cat");
                e.kind = (String) m.get("kind");
                e.famille = (String) m.get("famille");
                e.motif = m.get("motif") == null ? 0 : ((Number) m.get("motif")).intValue();
                if (m.get("pal") != null) e.pal = ((List<?>) m.get("pal")).toArray(new String[0]);
                if (m.get("ingredients") != null) e.ingredients = new ArrayList<>((List<String>) m.get("ingredients"));
                if (m.get("steps") != null) e.steps = new ArrayList<>((List<String>) m.get("steps"));
                DB.put(e.id, e);
            }
    }

    static final class Json {
        final String s; int i;
        Json(String s) { this.s = s; }
        void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        Object valeur() {
            ws();
            char c = s.charAt(i);
            if (c == '{') { i++; Map<String, Object> m = new LinkedHashMap<>(); ws(); if (s.charAt(i) == '}') { i++; return m; }
                while (true) { ws(); String k = (String) valeur(); ws(); i++; m.put(k, valeur()); ws(); if (s.charAt(i++) == '}') return m; } }
            if (c == '[') { i++; List<Object> l = new ArrayList<>(); ws(); if (s.charAt(i) == ']') { i++; return l; }
                while (true) { l.add(valeur()); ws(); if (s.charAt(i++) == ']') return l; } }
            if (c == '"') { StringBuilder b = new StringBuilder(); i++; while (s.charAt(i) != '"') { char d = s.charAt(i++); if (d == '\\') { char e = s.charAt(i++); if (e == 'u') { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; } else b.append(e == 'n' ? '\n' : e); } else b.append(d); } i++; return b.toString(); }
            int d = i; while (i < s.length() && "-+.eE0123456789truefalsn".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(d, i);
            if (n.equals("true")) return true; if (n.equals("false")) return false; if (n.equals("null")) return null;
            return Double.parseDouble(n);
        }
    }
}
