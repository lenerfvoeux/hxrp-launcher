package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import fr.lenerfvoeux.hxrp.metiers.minijeu.BancMiniJeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.BancMiniJeuxVirus;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ContexteVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ScenesVirus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * Rend de vraies images des mini-jeux du Virus hors Minecraft (même code de scène que le jeu),
 * à différents moments d'une partie jouée par le bot parfait.
 *
 *   java -cp build/banc fr.lenerfvoeux.hxrp.metiers.client.minijeu.ApercuMiniJeuxVirus [geste] [rang]
 */
public final class ApercuMiniJeuxVirus {
    static final String RES = "src/main/resources/assets/hxrpmetiers/";

    public static void main(String[] a) throws Exception {
        // les textes de présentation doivent tenir sur la plaque (sinon ils débordent de l'écran)
        int trop = 0;
        for (String k : new String[]{"yagen", "hachoir", "chaudron", "alambic", "jarres", "balance", "pilon", "pilulier", "table",
                "gorgees", "injection", "etaler_soin", "microscope"})
            for (int p = 0; p < 7; p++) for (int r = 0; r < 4; r++) {
                MiniJeu j = Jeux.creer(k, r, 99 + p, p);
                for (String s : new String[]{j.consigne(), j.commandes(), j.titre()})
                    if (Police.largeur(s, 1) > 250) {
                        System.out.println("  !! texte trop large (" + Police.largeur(s, 1) + " px) : " + k + "/" + p + " « " + s + " »");
                        trop++;
                    }
            }
        if (trop > 0) System.exit(1);
        Object[][] cas = {
                {"yagen", 0, "Poudre de gentiane", new String[]{"gentiane", "thym"}},
                {"hachoir", 0, "Racine de bardane hachée", new String[]{"bardane_racine", "thym"}},
                {"chaudron", 1, "Décoction de sureau", new String[]{"sureau_fleurs", "miel"}},
                {"chaudron", 3, "Torréfaction à sec", new String[]{"pissenlit"}},
                {"alambic", 0, "Eau florale de lavande", new String[]{"lavande"}},
                {"jarres", 3, "Teinture d'échinacée", new String[]{"echinacee"}},
                {"balance", 1, "Dose d'amanite", new String[]{"amanite_rouge"}},
                {"pilon", 0, "Pâte d'arnica", new String[]{"arnica"}},
                {"pilulier", 0, "Pilules de valériane", new String[]{"valeriane_racine"}},
                {"table", 3, "Onguent de souci", new String[]{"souci", "cire_d_abeille", "saindoux", "miel", "thym", "romarin"}},
                {"gorgees", 0, "Eau florale de lavande · Kurapika", new String[]{"lavande"}},
                {"gorgees", 1, "Fumigation de sauge · Kurapika", new String[]{"sauge"}},
                {"injection", 0, "Injection · Gon", new String[]{"echinacee"}},
                {"injection", 1, "Prise de sang · Gon", new String[]{}},
                {"etaler_soin", 0, "Onguent d'arnica · Leorio", new String[]{"arnica"}},
                {"etaler_soin", 1, "Bandage · Leorio", new String[]{"laine"}},
                {"microscope", 0, "Analyse · Gon", new String[]{}}};
        new File("tools/preview/out").mkdirs();
        int rang = a.length > 1 ? Integer.parseInt(a[1]) : 1;
        for (Object[] k : cas) {
            if (a.length > 0 && !a[0].equals(k[0])) continue;
            apercu((String) k[0], (Integer) k[1], (String) k[2], (String[]) k[3], rang);
        }
    }

    static void apercu(String cle, int param, String titre, String[] ingredients, int rang) throws Exception {
        ContexteVirus c = new ContexteVirus();
        c.titre = titre;
        c.sujet = titre.contains(" · ") ? titre.substring(titre.indexOf(" · ") + 3) : "";
        c.rang = rang;
        c.etape = 1;
        c.etapes = 3;
        for (String id : ingredients) {
            int[] ic = icone(id);
            if (ic != null) c.icones.add(ic);
        }
        if (!c.icones.isEmpty()) c.couleur = ContexteVirus.moyenne(c.icones.get(0));
        Contexte cg = new Contexte();
        cg.recette = titre;
        cg.etape = 1;
        cg.etapes = 3;
        ScenesVirus scene = new ScenesVirus(c);
        BancMiniJeux.Bot bot = BancMiniJeuxVirus.parfait(cle);
        Random rnd = new Random(1);
        int duree;
        {
            MiniJeu j2 = Jeux.creer(cle, rang, 4242, param);
            while (!j2.fini) { bot.jouer(j2, new Journal(), rnd); if (!j2.fini) j2.avancer(); }
            duree = j2.t;
        }
        MiniJeu j = Jeux.creer(cle, rang, 4242, param);
        Journal jl = new Journal();
        rnd = new Random(1);
        int[] instants = {-1, (int) (duree * 0.18), (int) (duree * 0.42), (int) (duree * 0.66), (int) (duree * 0.9), duree + 400};
        int S = 2, W = Jeux.W * S, H = Jeux.H * S;
        BufferedImage out = new BufferedImage(W * 3, H * 2, BufferedImage.TYPE_INT_ARGB);
        Toile t = new Toile(Jeux.W, Jeux.H);
        int idx = 0;
        for (int ins : instants) {
            while (!j.fini && j.t < ins) { bot.jouer(j, jl, rnd); if (!j.fini) j.avancer(); }
            int etat = ins < 0 ? Rendu.INTRO : j.fini ? Rendu.FIN : Rendu.JEU;
            Rendu.image(t, j, cg, j.t + 40, etat, 200, 100, j.noteFinale(), scene);
            BufferedImage fond = ApercuMiniJeux.decor(W, H, idx);
            for (int y = 0; y < H; y++)
                for (int x = 0; x < W; x++) fond.setRGB(x, y, ApercuMiniJeux.blend(fond.getRGB(x, y), t.px[(y / S) * Jeux.W + x / S]));
            out.getGraphics().drawImage(fond, (idx % 3) * W, (idx / 3) * H, null);
            idx++;
        }
        String nom = "tools/preview/out/virus_mj_" + cle + "_" + param + ".png";
        ImageIO.write(out, "png", new File(nom));
        System.out.printf("%-12s %d  note %.1f  -> %s%n", cle, param, j.noteFinale(), nom);
    }

    static int[] icone(String id) {
        for (String p : new String[]{"textures/items/virus/", "textures/items/"}) {
            try {
                File f = new File(RES + p + id + ".png");
                if (!f.exists()) continue;
                BufferedImage im = ImageIO.read(f);
                int[] px = new int[32 * 32];
                im.getRGB(0, 0, 32, 32, px, 0, 32);
                return px;
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
