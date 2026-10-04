package fr.lenerfvoeux.hxrp.metiers.virus.donnees;

import com.google.gson.Gson;
import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Les données du Hunter Virus : ingrédients, préparations, maladies, blessures.
 * <p>
 * Les valeurs par défaut sont dans le jar (assets/hxrpmetiers/data/virus) ; au premier démarrage elles sont copiées
 * dans config/hxrpmetiers/virus/ où les admins peuvent les modifier sans recompiler. Une copie jamais touchée est
 * remplacée quand le mod apporte une nouvelle version ; une copie modifiée est gardée (la nouvelle version est
 * écrite à côté, en « .nouveau »). Un fichier illisible n'empêche pas le serveur de démarrer : on repart des valeurs du jar.
 * <p>
 * Les objets sont enregistrés d'après les valeurs du jar (le client et le serveur doivent avoir les mêmes) ;
 * le serveur envoie ensuite ses fichiers au client à la connexion, pour que le carnet et le Grimoire affichent ses recettes.
 */
public final class DonneesVirus {
    public static final String[] FICHIERS = {"ingredients.json", "preparations.json", "maladies.json", "blessures.json"};
    private static final Gson GSON = new Gson();

    public static final Map<String, Defs.Ingredient> INGREDIENTS = new LinkedHashMap<>();
    public static final Map<String, Defs.Preparation> PREPARATIONS = new LinkedHashMap<>();
    public static final Map<String, Defs.Maladie> MALADIES = new LinkedHashMap<>();
    public static final Map<String, Defs.Blessure> BLESSURES = new LinkedHashMap<>();
    public static final List<Defs.Symptome> SYMPTOMES = new ArrayList<>();
    public static Map<String, String> reprisDuGourmet = Collections.emptyMap();

    /** Textes JSON en vigueur (envoyés au client). */
    private static final String[] textes = new String[FICHIERS.length];
    /** Préparations du jar (fixent la liste des objets enregistrés). */
    private static final Map<String, Defs.Preparation> PREPARATIONS_JAR = new LinkedHashMap<>();
    private static final Map<String, Defs.Ingredient> INGREDIENTS_JAR = new LinkedHashMap<>();
    private static boolean chargeJar;

    private DonneesVirus() {}

    // ================================================================== jar
    /** Valeurs du jar : appelé tôt (enregistrement des objets), sur les deux côtés. */
    public static synchronized void chargerJar() {
        if (chargeJar) return;
        String[] t = new String[FICHIERS.length];
        for (int i = 0; i < FICHIERS.length; i++) t[i] = lireJar(FICHIERS[i]);
        appliquer(t);
        PREPARATIONS_JAR.putAll(PREPARATIONS);
        INGREDIENTS_JAR.putAll(INGREDIENTS);
        chargeJar = true;
    }

    public static Map<String, Defs.Preparation> preparationsDuJar() {
        chargerJar();
        return PREPARATIONS_JAR;
    }

    public static Map<String, Defs.Ingredient> ingredientsDuJar() {
        chargerJar();
        return INGREDIENTS_JAR;
    }

    private static String lireJar(String nom) {
        try (InputStream in = DonneesVirus.class.getResourceAsStream("/assets/" + HxrpMetiers.MODID + "/data/virus/" + nom)) {
            if (in == null) throw new IllegalStateException("data/virus/" + nom + " introuvable dans le jar");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Lecture impossible de data/virus/" + nom, e);
        }
    }

    // ================================================================== config
    /** Charge config/hxrpmetiers/virus/ (côté serveur, ou solo) en créant/mettant à jour les copies. */
    public static synchronized void chargerConfig(File configDir) {
        chargerJar();
        File dir = new File(configDir, HxrpMetiers.MODID + File.separator + "virus");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            HxrpMetiers.LOG.error("Virus : impossible de créer {}, valeurs du jar utilisées", dir);
            return;
        }
        File registre = new File(dir, ".empreintes");
        Properties empreintes = new Properties();
        if (registre.isFile()) try (InputStream in = Files.newInputStream(registre.toPath())) {
            empreintes.load(in);
        } catch (IOException ignore) {
        }
        String[] t = new String[FICHIERS.length];
        for (int i = 0; i < FICHIERS.length; i++) {
            String nom = FICHIERS[i];
            String jar = lireJar(nom);
            String hJar = empreinte(jar);
            File f = new File(dir, nom);
            try {
                if (!f.isFile()) {
                    ecrire(f, jar);
                    empreintes.setProperty(nom, hJar);
                    t[i] = jar;
                    continue;
                }
                String local = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                String hLocal = empreinte(local);
                String connue = empreintes.getProperty(nom, "");
                if (!hLocal.equals(hJar) && hLocal.equals(connue)) {
                    // copie jamais modifiée par un admin : on suit la nouvelle version du mod
                    ecrire(f, jar);
                    empreintes.setProperty(nom, hJar);
                    HxrpMetiers.LOG.info("Virus : {} mis à jour avec la nouvelle version du mod", nom);
                    t[i] = jar;
                    continue;
                }
                if (!hLocal.equals(hJar)) {
                    ecrire(new File(dir, nom + ".nouveau"), jar);
                    HxrpMetiers.LOG.info("Virus : {} a été modifié par un admin, il est conservé (version du mod dans {}.nouveau)", nom, nom);
                }
                t[i] = local;
            } catch (IOException e) {
                HxrpMetiers.LOG.error("Virus : lecture de {} impossible ({}), valeurs du jar utilisées", f, e.toString());
                t[i] = jar;
            }
        }
        try (java.io.OutputStream out = Files.newOutputStream(registre.toPath())) {
            empreintes.store(out, "Empreintes des fichiers écrits par le mod (ne pas modifier)");
        } catch (IOException ignore) {
        }
        // un fichier illisible : on repart du jar pour celui-là
        for (int i = 0; i < FICHIERS.length; i++) {
            try {
                verifier(i, t[i]);
            } catch (RuntimeException e) {
                HxrpMetiers.LOG.error("Virus : {} illisible ({}), valeurs du jar utilisées", FICHIERS[i], e.getMessage());
                t[i] = lireJar(FICHIERS[i]);
            }
        }
        appliquer(t);
        HxrpMetiers.LOG.info("Virus : {} ingrédients, {} préparations, {} maladies, {} blessures chargés depuis {}",
                INGREDIENTS.size(), PREPARATIONS.size(), MALADIES.size(), BLESSURES.size(), dir);
    }

    private static void verifier(int i, String json) {
        Object o;
        switch (i) {
            case 0: o = GSON.fromJson(json, Defs.FichierIngredients.class); break;
            case 1: o = GSON.fromJson(json, Defs.FichierPreparations.class); break;
            case 2: o = GSON.fromJson(json, Defs.FichierMaladies.class); break;
            default: o = GSON.fromJson(json, Defs.FichierBlessures.class); break;
        }
        if (o == null) throw new IllegalStateException("fichier vide");
    }

    private static void ecrire(File f, String s) throws IOException {
        Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8));
    }

    private static String empreinte(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-1").digest(s.replace("\r\n", "\n").getBytes(StandardCharsets.UTF_8));
            StringBuilder b = new StringBuilder();
            for (byte x : h) b.append(String.format("%02x", x));
            return b.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    // ================================================================== application
    /** Remplace les données en vigueur (chargement, rechargement admin, ou réception du serveur côté client). */
    public static synchronized void appliquer(String[] t) {
        Defs.FichierIngredients fi = GSON.fromJson(t[0], Defs.FichierIngredients.class);
        Defs.FichierPreparations fp = GSON.fromJson(t[1], Defs.FichierPreparations.class);
        Defs.FichierMaladies fm = GSON.fromJson(t[2], Defs.FichierMaladies.class);
        Defs.FichierBlessures fb = GSON.fromJson(t[3], Defs.FichierBlessures.class);
        INGREDIENTS.clear();
        PREPARATIONS.clear();
        MALADIES.clear();
        BLESSURES.clear();
        SYMPTOMES.clear();
        if (fi != null) {
            for (Defs.Ingredient i : fi.ingredients) if (i != null && i.id != null) INGREDIENTS.put(i.id, i);
            if (fi.reprisDuGourmet != null) reprisDuGourmet = fi.reprisDuGourmet;
        }
        if (fp != null) for (Defs.Preparation p : fp.preparations) {
            if (p == null || p.id == null || p.etapes == null || p.etapes.isEmpty()) continue;
            if (p.ingredients == null) p.ingredients = new ArrayList<>();
            boolean ok = true;
            for (Defs.Etape e : p.etapes) if (Machine.de(e.machine) == null) ok = false;
            if (!ok) {
                HxrpMetiers.LOG.warn("Virus : préparation {} ignorée (machine inconnue)", p.id);
                continue;
            }
            PREPARATIONS.put(p.id, p);
        }
        if (fm != null) {
            for (Defs.Maladie m : fm.maladies) if (m != null && m.id != null && m.stades != null && !m.stades.isEmpty() && m.prises != null && !m.prises.isEmpty())
                MALADIES.put(m.id, m);
            SYMPTOMES.addAll(fm.symptomes);
        }
        if (fb != null) for (Defs.Blessure b : fb.blessures) if (b != null && b.id != null && b.stades != null && b.stades.size() == 4) BLESSURES.put(b.id, b);
        System.arraycopy(t, 0, textes, 0, Math.min(t.length, textes.length));
    }

    public static String[] textes() {
        return textes.clone();
    }

    public static Defs.Preparation preparation(String id) { return id == null ? null : PREPARATIONS.get(id); }
    public static Defs.Maladie maladie(String id) { return id == null ? null : MALADIES.get(id); }
    public static Defs.Blessure blessure(String id) { return id == null ? null : BLESSURES.get(id); }
    public static Defs.Ingredient ingredient(String id) { return id == null ? null : INGREDIENTS.get(id); }

    public static boolean estSymptome(String id) {
        for (Defs.Symptome s : SYMPTOMES) if (s.id.equals(id)) return true;
        return false;
    }

    public static String nomSymptome(String id) {
        for (Defs.Symptome s : SYMPTOMES) if (s.id.equals(id)) return s.nom;
        return id;
    }

    /** La préparation (remède ou soin) qui produit cet objet, ou null. */
    public static Defs.Preparation recettePour(String produit) {
        for (Defs.Preparation p : PREPARATIONS.values()) if (p.produit().equals(produit)) return p;
        return null;
    }

    /** Le soin de cette blessure, ou null. */
    public static Defs.Blessure blessureSoigneePar(String preparation) {
        for (Defs.Blessure b : BLESSURES.values()) if (b.soin.equals(preparation)) return b;
        return null;
    }
}
