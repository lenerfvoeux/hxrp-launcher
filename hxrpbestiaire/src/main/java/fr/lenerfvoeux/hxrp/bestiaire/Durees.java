package fr.lenerfvoeux.hxrp.bestiaire;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Durée (ticks) de chaque animation, écrite par tools/bestiaire/gen.py dans assets/hxrpbestiaire/durees.properties :
 * le serveur ne lit pas les fichiers d'animation de GeckoLib mais doit caler ses attaques sur elles.
 */
public final class Durees {
    private static final Properties P = new Properties();

    static {
        try (InputStream in = Durees.class.getResourceAsStream("/assets/" + HxrpBestiaire.MODID + "/durees.properties")) {
            if (in != null) P.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            else HxrpBestiaire.LOG.error("Bestiaire : durees.properties absent du jar");
        } catch (IOException e) {
            HxrpBestiaire.LOG.error("Bestiaire : durees.properties illisible", e);
        }
    }

    private Durees() {}

    /** Durée de l'animation « court » du modèle, ou la valeur par défaut si elle n'existe pas. */
    public static int ticks(String modele, String court, int defaut) {
        String v = P.getProperty(modele + "." + court);
        if (v == null) return defaut;
        try {
            return Math.max(1, Integer.parseInt(v.trim()));
        } catch (NumberFormatException e) {
            return defaut;
        }
    }

    public static boolean existe(String modele, String court) {
        return P.containsKey(modele + "." + court);
    }

    public static int nombre() {
        return P.size();
    }
}
