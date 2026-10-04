package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import java.lang.reflect.Method;

/**
 * Lecture du mod d'identité HxRP (modid hunterxhunter) sans en dépendre : par réflexion sur
 * com.hunterxhunter.capability.IdentityCapability.get(entity) → PlayerIdentity (getGender(), getDisplayName(),
 * getCreatedAt(), isCreated()). Si le mod est absent ou l'identité pas encore créée : sexe inconnu.
 */
public final class Identite {
    public static final String HOMME = "homme", FEMME = "femme";
    private static boolean essaye;
    private static Method get, genre, nom, creeLe, cree;

    private Identite() {}

    private static synchronized void init() {
        if (essaye) return;
        essaye = true;
        try {
            Class<?> cap = Class.forName("com.hunterxhunter.capability.IdentityCapability");
            get = cap.getMethod("get", Entity.class);
            Class<?> id = Class.forName("com.hunterxhunter.identity.PlayerIdentity");
            genre = id.getMethod("getGender");
            nom = id.getMethod("getDisplayName");
            creeLe = id.getMethod("getCreatedAt");
            cree = id.getMethod("isCreated");
            HxrpMetiers.LOG.info("Virus : mod d'identité HxRP trouvé, le sexe des personnages sera lu");
        } catch (Throwable t) {
            get = null;
            HxrpMetiers.LOG.info("Virus : mod d'identité HxRP absent, sexe des personnages inconnu");
        }
    }

    private static Object identite(EntityPlayer p) {
        init();
        if (get == null || p == null) return null;
        try {
            Object o = get.invoke(null, p);
            if (o == null) return null;
            Object c = cree.invoke(o);
            return Boolean.TRUE.equals(c) ? o : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** « homme », « femme », ou null si inconnu. */
    public static String sexe(EntityPlayer p) {
        Object o = identite(p);
        if (o == null) return null;
        try {
            Object g = genre.invoke(o);
            if (g == null) return null;
            String n = ((Enum<?>) g).name();
            return "MALE".equals(n) ? HOMME : "FEMALE".equals(n) ? FEMME : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Nom RP du personnage, ou le pseudo du joueur. */
    public static String nom(EntityPlayer p) {
        Object o = identite(p);
        if (o != null) try {
            Object n = nom.invoke(o);
            if (n instanceof String && !((String) n).trim().isEmpty()) return ((String) n).trim();
        } catch (Throwable ignore) {
        }
        return p == null ? "?" : p.getName();
    }

    /** Date de création du personnage (0 si inconnue) : un nouveau personnage a une autre date. */
    public static long creation(EntityPlayer p) {
        Object o = identite(p);
        if (o == null) return 0;
        try {
            Object c = creeLe.invoke(o);
            return c instanceof Long ? (Long) c : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public static String libelleSexe(String s) {
        return HOMME.equals(s) ? "Homme" : FEMME.equals(s) ? "Femme" : "Inconnu";
    }
}
