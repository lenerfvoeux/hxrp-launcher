package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Les traitements : chaque prise doit être donnée dans sa fenêtre (délai depuis la prise précédente ± tolérance),
 * toutes ses préparations dans la même demi-heure, en respectant la consigne. Prise oubliée, hors fenêtre ou consigne
 * non respectée : le traitement échoue et tout est à recommencer. Après la dernière prise, convalescence selon le rang
 * du Virus, puis guérison et immunité.
 */
public final class Traitements {
    private Traitements() {}

    // ================================================================== consignes
    public static List<JsonObject> consignes(Defs.Maladie m, String type) {
        List<JsonObject> l = new ArrayList<>();
        for (JsonObject c : m.consignes) if (c.has("type") && type.equals(c.get("type").getAsString())) l.add(c);
        return l;
    }

    public static boolean a(Defs.Maladie m, String type) {
        return !consignes(m, type).isEmpty();
    }

    private static int entier(JsonObject c, String k, int def) {
        return c.has(k) ? c.get(k).getAsInt() : def;
    }

    private static double reel(JsonObject c, String k, double def) {
        return c.has(k) ? c.get(k).getAsDouble() : def;
    }

    private static boolean contient(JsonObject c, String k, int v) {
        if (!c.has(k)) return false;
        JsonArray a = c.getAsJsonArray(k);
        for (JsonElement e : a) if (e.getAsInt() == v) return true;
        return false;
    }

    // ================================================================== fenêtres
    /** Début et fin (ms) de la fenêtre de la prise attendue ; {0, MAX} pour la première. */
    public static long[] fenetre(Defs.Maladie m, Traitement t) {
        if (t == null || t.prise <= 0 || t.prise >= m.prises.size()) return new long[]{0, Long.MAX_VALUE};
        Defs.Prise p = m.prises.get(t.prise);
        long tol = VirusConfig.toleranceMinutes * Maladies.MINUTE;
        if (p.moment != null && !p.moment.isEmpty() && p.delai <= 0)
            return new long[]{t.dernierePrise + Maladies.HEURE / 2, t.dernierePrise + VirusConfig.delaiMaxMomentHeures * Maladies.HEURE};
        long cible = t.dernierePrise + (long) (p.delai * Maladies.HEURE);
        return new long[]{cible - tol, cible + tol};
    }

    public static boolean estNuit(net.minecraft.world.World w) {
        long h = w.getWorldTime() % 24000;
        return h >= 13000 && h < 23000;
    }

    private static boolean estSoir(net.minecraft.world.World w) {
        long h = w.getWorldTime() % 24000;
        return h >= 11000 && h < 15000;
    }

    /** Libellé d'une prise : « Décoction de saule + Sirop de thym ». */
    public static String libellePrise(Defs.Prise p) {
        StringBuilder b = new StringBuilder();
        for (String id : p.items) {
            if (b.length() > 0) b.append(" + ");
            Defs.Preparation x = DonneesVirus.preparation(id);
            b.append(x == null ? id : x.nom);
        }
        return b.toString();
    }

    // ================================================================== avant d'administrer
    /**
     * Vérifie, avant de lancer le mini-jeu d'administration, que la préparation peut être donnée maintenant.
     * Renvoie un message d'erreur pour le Virus, ou null. Un remède qui ne correspond pas à la maladie n'est pas
     * refusé ici (le Virus le découvre en le donnant : il n'a aucun effet).
     */
    public static String verifierAvant(EntityPlayerMP patient, SanteData d, Defs.Preparation prep) {
        if (prep.soin != null && !prep.soin.isEmpty()) {
            SanteData.Blessure b = d.blessures.get(prep.soin);
            Defs.Blessure def = DonneesVirus.blessure(prep.soin);
            if (b == null || b.stade <= 0) return "Ce patient n'a pas de " + (def == null ? prep.soin : def.nom.toLowerCase(Locale.ROOT)) + " à soigner.";
            if (b.reposRequis > 0) return "Un soin est déjà posé : le patient doit se reposer.";
            return null;
        }
        if ("remede_du_second_souffle".equals(prep.id)) return d.aVie.isEmpty() ? "Ce patient ne porte aucune séquelle à effacer." : null;
        if (d.convalescenceFin > 0) return "Le patient est en convalescence : il n'a plus besoin de remède.";
        Traitement t = d.traitement;
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m == null) return null;
        if (t == null || !t.maladie.equals(m.id)) {
            Defs.Prise p0 = m.prises.get(0);
            if (p0.items.contains(prep.id)) {
                if ("soir".equals(p0.moment) && !estSoir(patient.world)) return "Cette prise se donne le soir.";
                if ("nuit".equals(p0.moment) && !estNuit(patient.world)) return "Cette prise se donne la nuit.";
            }
            return null;
        }
        if (t.attenteSommeil) return "Le traitement est terminé : le patient doit maintenant dormir dans un lit.";
        if (t.prise >= m.prises.size()) return null;
        Defs.Prise p = m.prises.get(t.prise);
        if (!p.items.contains(prep.id)) return null;
        if (t.faits.contains(prep.id)) return "Cette préparation a déjà été donnée pour la prise " + (t.prise + 1) + ".";
        long now = System.currentTimeMillis();
        long[] f = fenetre(m, t);
        if (t.debutPrise == 0 && now < f[0])
            return "Trop tôt : la prise " + (t.prise + 1) + " se donne dans " + fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(f[0] - now) + ".";
        if (t.debutPrise == 0 && t.prise > 0) {
            if ("soir".equals(p.moment) && !estSoir(patient.world)) return "Cette prise se donne le soir.";
            if ("nuit".equals(p.moment) && !estNuit(patient.world)) return "Cette prise se donne la nuit.";
            if ("reveil".equals(p.moment) && (d.dernierSommeil <= t.dernierePrise || now - d.dernierSommeil > Maladies.HEURE))
                return "Cette prise se donne au réveil : le patient doit d'abord dormir dans un lit.";
            Defs.Prise avant = m.prises.get(t.prise - 1);
            if ((avant.dormirApres || a(m, "dormir_apres_chaque_prise") || dormirEntre(m, t.prise + 1)) && !t.dormiDepuisPrise)
                return "Le patient doit d'abord dormir dans un lit (consigne de la prise " + t.prise + ").";
            for (JsonObject c : consignes(m, "manger_plat"))
                if (entier(c, "a", 2) == t.prise + 1 && !t.platMange)
                    return "Le patient doit d'abord manger " + ("viande_rouge".equals(c.get("plat").getAsString()) ? "un plat de viande rouge" : "un plat de fruits ou de légumes") + " du Gourmet.";
            for (JsonObject c : consignes(m, "soleil_apres_prise"))
                if (t.soleil < entier(c, "minutes", 10) * 60) return "Le patient doit d'abord rester " + entier(c, "minutes", 10) + " minutes au soleil.";
        }
        return null;
    }

    private static boolean dormirEntre(Defs.Maladie m, int prise) {
        for (JsonObject c : consignes(m, "dormir_entre")) if (entier(c, "a", 2) == prise) return true;
        for (JsonObject c : consignes(m, "dormir_apres")) if (contient(c, "prises", prise - 1)) return true;
        return false;
    }

    // ================================================================== administrer
    /**
     * Une préparation vient d'être administrée avec succès (≥ 50 %). qualite : note de la préparation ;
     * bonus : réussie à 95 % ou plus.
     */
    public static void administrer(EntityPlayerMP virus, EntityPlayerMP patient, SanteData d, Defs.Preparation prep, int qualite,
                                   boolean bonus, int rangVirus) {
        long now = System.currentTimeMillis();
        String nomVirus = Identite.nom(virus);
        if (bonus) bonus(patient, d, prep);
        if (prep.soin != null && !prep.soin.isEmpty()) {
            Blessures.soigner(patient, d, prep.soin, qualite, bonus);
            return;
        }
        if ("remede_du_second_souffle".equals(prep.id)) {
            int n = d.aVie.size();
            d.aVie.clear();
            d.dirty = true;
            patient.sendMessage(new TextComponentString(TextFormatting.GOLD + "" + TextFormatting.ITALIC
                    + "Un souffle neuf vous traverse : les traces que les maladies avaient laissées en vous s'effacent (" + n + ")."));
            return;
        }
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m == null || !m.preparations().contains(prep.id) || rangVirus < m.rang || d.convalescenceFin > 0) {
            sansEffet(patient);
            return;
        }
        Traitement t = d.traitement;
        if (t != null && !t.maladie.equals(m.id)) t = d.traitement = null;
        if (t == null) {
            if (!m.prises.get(0).items.contains(prep.id)) {
                sansEffet(patient);
                return;
            }
            t = new Traitement();
            t.maladie = m.id;
            d.traitement = t;
        }
        if (t.prise >= m.prises.size()) return;
        Defs.Prise p = m.prises.get(t.prise);
        if (!p.items.contains(prep.id) || t.faits.contains(prep.id)) {
            sansEffet(patient);
            return;
        }
        if (t.debutPrise == 0) {
            t.debutPrise = now;
            // à jeun avant cette prise
            for (JsonObject c : consignes(m, "a_jeun_avant"))
                if (contient(c, "prises", t.prise + 1) && now - d.dernierRepas < reel(c, "heures", 1) * Maladies.HEURE) {
                    echec(patient, d, "le patient avait mangé avant la prise " + (t.prise + 1));
                    return;
                }
        }
        t.faits.add(prep.id);
        d.dirty = true;
        if (!t.faits.containsAll(p.items)) {
            List<String> reste = new ArrayList<>();
            for (String x : p.items) if (!t.faits.contains(x)) {
                Defs.Preparation q = DonneesVirus.preparation(x);
                reste.add(q == null ? x : q.nom);
            }
            virus.sendMessage(new TextComponentString(TextFormatting.GRAY + "Prise " + (t.prise + 1) + " : reste à donner " + String.join(" + ", reste)
                    + " (dans les " + VirusConfig.dureePriseMinutes + " minutes)."));
            return;
        }
        // prise complète
        boolean premiere = t.prise == 0;
        t.rangVirus = rangVirus;
        t.nomVirus = nomVirus;
        t.px = patient.posX;
        t.py = patient.posY;
        t.pz = patient.posZ;
        t.dimension = patient.dimension;
        if (premiere) t.debutCure = now;
        boolean dormir = p.dormirApres;
        t.nouvellePrise(now);
        if (premiere) Ordonnances.remettre(virus, patient, m, nomVirus);
        if (t.prise >= m.prises.size()) {
            boolean attente = dormir || a(m, "dormir_apres_chaque_prise");
            for (JsonObject c : consignes(m, "dormir_apres")) if (contient(c, "prises", m.prises.size())) attente = true;
            if (attente) {
                t.attenteSommeil = true;
                patient.sendMessage(new TextComponentString(TextFormatting.GREEN + "Dernière prise donnée. " + TextFormatting.GOLD + "Allez dormir dans un lit pour que le traitement agisse."));
            } else {
                commencerConvalescence(patient, d);
            }
            return;
        }
        long[] f = fenetre(m, t);
        Defs.Prise suiv = m.prises.get(t.prise);
        String quand = suiv.moment != null && !suiv.moment.isEmpty()
                ? ("reveil".equals(suiv.moment) ? "au réveil" : "nuit".equals(suiv.moment) ? "la nuit" : "le soir")
                : "dans " + fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree((long) (suiv.delai * Maladies.HEURE)) + " (± " + VirusConfig.toleranceMinutes + " min)";
        TextComponentString msg = new TextComponentString(TextFormatting.GREEN + "Prise " + t.prise + "/" + m.prises.size() + " donnée. "
                + TextFormatting.GRAY + "Prochaine : " + libellePrise(suiv) + ", " + quand + ".");
        patient.sendMessage(msg);
        if (virus != patient) virus.sendMessage(msg.createCopy());
    }

    private static void sansEffet(EntityPlayerMP patient) {
        patient.sendMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Vous ne ressentez aucune amélioration."));
    }

    /** Effet bonus propre au remède (réussi à 95 % ou plus). */
    private static void bonus(EntityPlayerMP p, SanteData d, Defs.Preparation prep) {
        String b = prep.bonus == null ? "" : prep.bonus;
        String txt;
        switch (b) {
            case "regeneration": p.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 20 * 30, 0)); txt = "une douce chaleur répare vos chairs"; break;
            case "resistance": p.addPotionEffect(new PotionEffect(MobEffects.RESISTANCE, 20 * 120, 0)); txt = "vous vous sentez plus solide"; break;
            case "vigueur": p.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 20 * 120, 0)); txt = "une vigueur nouvelle vous gagne"; break;
            case "apaisement": d.prochainEpisode = Math.max(d.prochainEpisode, System.currentTimeMillis()) + 30 * Maladies.MINUTE; txt = "vos symptômes s'apaisent un moment"; break;
            case "convalescence": if (d.traitement != null) d.traitement.bonusConvalescence = true; txt = "votre convalescence sera plus courte"; break;
            case "immunite": d.bonusImmuniteHeures = 12; txt = "votre corps sera mieux défendu après la guérison"; break;
            case "rassasie": nutrition(p, 15, 0); txt = "vous vous sentez rassasié"; break;
            case "rafraichi": nutrition(p, 0, 15); txt = "vous vous sentez désaltéré"; break;
            case "soin_express": txt = "le soin agira plus vite"; break;
            default: return;
        }
        d.dirty = true;
        p.sendMessage(new TextComponentString(TextFormatting.AQUA + "✦ Préparation d'exception : " + txt + "."));
    }

    private static void nutrition(EntityPlayerMP p, double faim, double soif) {
        NutritionData n = Nutrition.get(p);
        if (n == null) return;
        n.addFaim(faim);
        n.addSoif(soif);
    }

    // ================================================================== échec, convalescence
    public static void echec(EntityPlayerMP p, SanteData d, String raison) {
        d.traitement = null;
        d.dirty = true;
        p.sendMessage(new TextComponentString(TextFormatting.RED + "✖ Votre traitement a échoué : " + raison + ". Il faudra tout recommencer."));
    }

    public static void commencerConvalescence(EntityPlayerMP p, SanteData d) {
        Traitement t = d.traitement;
        int rang = t == null ? 0 : Math.max(0, Math.min(3, t.rangVirus));
        int[] c = VirusConfig.convalescenceMinutes;
        long min = c == null || c.length == 0 ? 60 : c[Math.min(c.length - 1, rang)];
        if (t != null && t.bonusConvalescence) min = min / 2;
        d.convalescenceFin = System.currentTimeMillis() + Math.max(1, min) * Maladies.MINUTE;
        d.traitement = null;
        d.episodes.clear();
        d.dirty = true;
        p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Le traitement est terminé. Convalescence : " + fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(min * Maladies.MINUTE)
                + TextFormatting.GRAY + " (reposez-vous, le mal s'en va)."));
    }

    // ================================================================== chaque seconde
    public static void seconde(EntityPlayerMP p, SanteData d, long now) {
        if (d.convalescenceFin > 0) {
            if (now >= d.convalescenceFin) {
                Defs.Maladie m = DonneesVirus.maladie(d.maladie);
                Maladies.guerir(p, d, true);
                p.sendMessage(new TextComponentString(TextFormatting.GREEN + "" + TextFormatting.BOLD + "Vous êtes guéri" + (m == null ? "" : " : " + m.nom) + " !"
                        + TextFormatting.GRAY + " Vous êtes immunisé pendant " + VirusConfig.immuniteHeures + " h."));
            }
            return;
        }
        Traitement t = d.traitement;
        if (t == null) return;
        Defs.Maladie m = DonneesVirus.maladie(t.maladie);
        if (m == null || !t.maladie.equals(d.maladie)) {
            d.traitement = null;
            return;
        }
        if (t.attenteSommeil) {
            if (t.dormiDepuisPrise) {
                t.attenteSommeil = false;
                commencerConvalescence(p, d);
            } else if (now - t.dernierePrise > 3 * Maladies.HEURE) {
                echec(p, d, "le patient n'a pas dormi après la dernière prise");
            }
            return;
        }
        // prise oubliée, ou commencée sans être finie
        if (t.debutPrise > 0 && now - t.debutPrise > VirusConfig.dureePriseMinutes * Maladies.MINUTE) {
            echec(p, d, "la prise " + (t.prise + 1) + " n'a pas été donnée en entier à temps");
            return;
        }
        if (t.prise > 0 && t.debutPrise == 0 && now > fenetre(m, t)[1]) {
            echec(p, d, "la prise " + (t.prise + 1) + " a été oubliée");
            return;
        }
        if (t.prise == 0) return;
        // consignes de lieu, mesurées sur toute la cure
        BlockPos pos = p.getPosition();
        boolean ciel = p.world.canSeeSky(pos);
        boolean soleil = ciel && !estNuit(p.world) && !p.world.isRaining();
        boolean chaleur = Blessures.presDUneSourceDeChaleur(p);
        double tolerance = VirusConfig.toleranceConsigneMinutes * 60.0;
        if (soleil) t.soleil += 1;
        if (chaleur) t.feu += 1;
        String hors = null;
        double tol = tolerance;
        if (a(m, "au_chaud") && ciel && !chaleur) hors = "il n'est pas resté au chaud";
        if (a(m, "a_l_ombre") && soleil) hors = "il ne s'est pas tenu à l'ombre";
        for (JsonObject c : consignes(m, "ombre_pendant")) if (now - t.debutCure < reel(c, "heures", 1) * Maladies.HEURE && soleil) hors = "il ne s'est pas tenu à l'ombre";
        for (JsonObject c : consignes(m, "pres_du_feu")) if (now - t.debutCure < reel(c, "heures", 1) * Maladies.HEURE && !chaleur) hors = "il ne s'est pas tenu près d'un feu";
        if (a(m, "pas_mouiller") && p.isWet()) {
            hors = "il s'est mouillé";
            tol = Math.min(tolerance, 45);
        }
        for (JsonObject c : consignes(m, "y_min")) if (p.posY < entier(c, "y", 50)) {
            hors = "il est descendu sous Y " + entier(c, "y", 50);
            tol = Math.min(tolerance, 90);
        }
        for (JsonObject c : consignes(m, "y_max")) if (p.posY > entier(c, "y", 80)) hors = "il n'est pas resté sous Y " + entier(c, "y", 80);
        if (a(m, "pas_de_grotte") && !ciel && p.posY < 55) {
            hors = "il est entré dans une grotte";
            tol = Math.min(tolerance, 60);
        }
        if (a(m, "pas_d_eau") && p.isInWater()) {
            hors = "il est entré dans l'eau";
            tol = Math.min(tolerance, 5);
        }
        if (hors != null) {
            t.horsConsigne += 1;
            if (t.horsConsigne > tol) {
                echec(p, d, hors);
                return;
            }
            if ((int) t.horsConsigne % 20 == 1)
                p.sendStatusMessage(new TextComponentString(TextFormatting.GOLD + "Attention à la consigne de votre traitement : " + m.consigneTexte + "."), true);
        }
        if (a(m, "pas_de_nether") && p.dimension == -1) {
            echec(p, d, "il est allé dans le Nether");
            return;
        }
        for (JsonObject c : consignes(m, "distance_max")) {
            double dist = p.dimension != t.dimension ? 1e9 : Math.sqrt(p.getDistanceSq(t.px, t.py, t.pz));
            if (dist > entier(c, "blocs", 20)) {
                echec(p, d, "il s'est éloigné de plus de " + entier(c, "blocs", 20) + " blocs");
                return;
            }
        }
        if ((a(m, "repos") || a(m, "pas_de_sprint")) && p.isSprinting()) {
            t.sprint += 1;
            if (t.sprint > 3) {
                echec(p, d, "il a couru");
                return;
            }
        }
        for (JsonObject c : consignes(m, "dormir_dans_l_heure"))
            if (contient(c, "prises", t.prise) && !t.dormiDepuisPrise && now - t.dernierePrise > Maladies.HEURE) {
                echec(p, d, "il n'a pas dormi dans l'heure");
                return;
            }
    }

    // ================================================================== événements
    /** Le patient a mangé (plat du Gourmet ou nourriture d'ailleurs). */
    public static void repas(EntityPlayerMP p, SanteData d, boolean viande, boolean viandeRouge, boolean poisson, boolean fruitsLegumes, boolean plat) {
        long now = System.currentTimeMillis();
        d.dernierRepas = now;
        Traitement t = d.traitement;
        if (t == null) return;
        Defs.Maladie m = DonneesVirus.maladie(t.maladie);
        if (m == null) return;
        int v = t.prise;
        if (a(m, "sans_nourriture") && v >= 1) {
            echec(p, d, "il a mangé pendant la cure");
            return;
        }
        for (JsonObject c : consignes(m, "a_jeun_entre"))
            if (v >= entier(c, "de", 1) && v < entier(c, "a", 2)) {
                echec(p, d, "il a mangé entre la prise " + entier(c, "de", 1) + " et la prise " + entier(c, "a", 2));
                return;
            }
        for (JsonObject c : consignes(m, "a_jeun_apres"))
            if (v >= 1 && contient(c, "prises", v) && now - t.dernierePrise < reel(c, "heures", 2) * Maladies.HEURE) {
                echec(p, d, "il a mangé moins de " + (int) reel(c, "heures", 2) + " h après la prise " + v);
                return;
            }
        for (JsonObject c : consignes(m, "a_jeun_pendant"))
            if (v >= 1 && now - t.debutCure < reel(c, "heures", 3) * Maladies.HEURE) {
                echec(p, d, "il n'est pas resté à jeun " + (int) reel(c, "heures", 3) + " h");
                return;
            }
        if (v >= 1 && a(m, "pas_de_viande") && viande) {
            echec(p, d, "il a mangé de la viande");
            return;
        }
        if (v >= 1 && a(m, "pas_de_poisson") && poisson) {
            echec(p, d, "il a mangé du poisson");
            return;
        }
        for (JsonObject c : consignes(m, "manger_plat"))
            if (plat && v >= entier(c, "de", 1) && v < entier(c, "a", 2)) {
                boolean rouge = "viande_rouge".equals(c.get("plat").getAsString());
                if (rouge ? viandeRouge : fruitsLegumes) {
                    t.platMange = true;
                    d.dirty = true;
                }
            }
    }

    public static void alcool(EntityPlayerMP p, SanteData d) {
        long now = System.currentTimeMillis();
        d.dernierAlcool = now;
        Traitement t = d.traitement;
        if (t == null) return;
        Defs.Maladie m = DonneesVirus.maladie(t.maladie);
        if (m == null) return;
        for (JsonObject c : consignes(m, "pas_d_alcool")) {
            if (!c.has("heures") || t.debutCure == 0 || now - t.debutCure < reel(c, "heures", 24) * Maladies.HEURE) {
                echec(p, d, "il a bu de l'alcool");
                return;
            }
        }
    }

    public static void combat(EntityPlayerMP p, SanteData d) {
        d.dernierCombat = System.currentTimeMillis();
        Traitement t = d.traitement;
        if (t == null || t.prise < 1) return;
        Defs.Maladie m = DonneesVirus.maladie(t.maladie);
        if (m != null && (a(m, "repos") || a(m, "aucun_combat"))) echec(p, d, "il s'est battu");
    }

    public static void sommeil(EntityPlayerMP p, SanteData d) {
        d.dernierSommeil = System.currentTimeMillis();
        if (d.traitement != null && d.traitement.prise > 0) {
            d.traitement.dormiDepuisPrise = true;
            d.dirty = true;
        }
    }

    public static void minage(EntityPlayerMP p, SanteData d, BlockPos pos) {
        Traitement t = d.traitement;
        if (t == null || t.prise < 1) return;
        Defs.Maladie m = DonneesVirus.maladie(t.maladie);
        if (m == null) return;
        for (JsonObject c : consignes(m, "pas_miner_sous"))
            if (pos.getY() < entier(c, "y", 30)) {
                echec(p, d, "il a miné sous Y " + entier(c, "y", 30));
                return;
            }
    }
}
