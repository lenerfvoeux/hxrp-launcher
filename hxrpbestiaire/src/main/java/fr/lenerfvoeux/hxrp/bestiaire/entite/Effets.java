package fr.lenerfvoeux.hxrp.bestiaire.entite;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Method;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Random;

/**
 * Les effets des attaques, lus dans la colonne « effet » du classeur (onglet Attaques) avec leur durée :
 * poison, lenteur, cécité, feu, recul, projette, renverse, attire, immobilise, enracine, saignement, nausée,
 * faiblesse, faim, maladie (Hunter Virus). Plusieurs effets se cumulent (« recul + lenteur »).
 */
public final class Effets {
    private static Method maladie;
    private static boolean cherche;

    private Effets() {}

    /** Texte d'effet normalisé : minuscules, sans accents. */
    public static String cle(String effet) {
        String n = Normalizer.normalize(effet == null ? "" : effet, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    public static boolean a(String effet, String mot) {
        return cle(effet).contains(mot);
    }

    /**
     * Applique au joueur les effets d'une attaque. {@code source} : d'où vient le coup (recul, attire).
     * Les effets de déplacement (recul, projette, attire) ne sont pas appliqués ici quand {@code mouvement} est faux.
     */
    public static void appliquer(EntityPlayer p, Entity source, String effet, double dureeS, boolean mouvement) {
        String e = cle(effet);
        if (e.isEmpty()) return;
        int t = (int) Math.max(20, Math.round(dureeS * 20));
        if (e.contains("poison")) effet(p, MobEffects.POISON, t, 1);
        if (e.contains("lenteur") || e.contains("sol gele")) effet(p, MobEffects.SLOWNESS, t, 2);
        if (e.contains("cecite") || e.contains("vue reduite")) effet(p, MobEffects.BLINDNESS, t, 0);
        if (e.contains("feu")) p.setFire(Math.max(1, (int) Math.round(dureeS)));
        if (e.contains("immobilise") || e.contains("enracine")) {
            effet(p, MobEffects.SLOWNESS, t, 7);
            effet(p, MobEffects.JUMP_BOOST, t, 250);          // amplificateur négatif : plus de saut
        }
        if (e.contains("saignement")) effet(p, MobEffects.WITHER, t, 0);
        if (e.contains("nausee")) effet(p, MobEffects.NAUSEA, t, 0);
        if (e.contains("faiblesse")) effet(p, MobEffects.WEAKNESS, t, 1);
        if (e.contains("faim")) effet(p, MobEffects.HUNGER, t, 2);
        if (!mouvement || source == null) return;
        double dx = p.posX - source.posX, dz = p.posZ - source.posZ;
        if (e.contains("recul") || e.contains("renverse")) repousser(p, dx, dz, e.contains("renverse") ? 1.6 : 1.1, 0.45);
        if (e.contains("projette")) {
            p.motionY = Math.max(p.motionY, 1.1);
            repousser(p, dx, dz, 0.6, 1.1);
        }
        if (e.contains("attire")) repousser(p, -dx, -dz, 0.7, 0.2);
    }

    public static void effet(EntityPlayer p, Potion potion, int ticks, int niveau) {
        p.addPotionEffect(new PotionEffect(potion, ticks, niveau));
    }

    public static void repousser(Entity p, double dx, double dz, double force, double haut) {
        double h = Math.max(0.1, MathHelper.sqrt(dx * dx + dz * dz));
        p.motionX += dx / h * force;
        p.motionZ += dz / h * force;
        p.motionY = Math.max(p.motionY, haut);
        p.velocityChanged = true;
    }

    /** Une maladie du Hunter Virus au hasard (hxrpmetiers, par réflexion), avec la probabilité donnée. */
    public static boolean maladie(EntityPlayer p, double chance, Random r) {
        if (r.nextDouble() >= chance) return false;
        chercher();
        if (maladie == null) return false;
        try {
            return Boolean.TRUE.equals(maladie.invoke(null, p));
        } catch (Throwable t) {
            HxrpBestiaire.LOG.warn("Bestiaire : appel à VirusAPI.maladieAleatoire impossible, abandonné", t);
            maladie = null;
            return false;
        }
    }

    public static boolean virusPresent() {
        chercher();
        return maladie != null;
    }

    private static void chercher() {
        if (cherche) return;
        cherche = true;
        if (!Loader.isModLoaded("hxrpmetiers")) return;
        try {
            maladie = Class.forName("fr.lenerfvoeux.hxrp.metiers.virus.VirusAPI").getMethod("maladieAleatoire", EntityPlayer.class);
        } catch (Throwable t) {
            HxrpBestiaire.LOG.info("Bestiaire : hxrpmetiers sans VirusAPI.maladieAleatoire ({}), pas de maladie transmise", t.toString());
        }
    }
}
