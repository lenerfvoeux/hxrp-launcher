package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Qualites;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Identite;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Maladies;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.MoteurSante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitements;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

/** Les gestes du Virus sur un patient : prise de sang, administration des remèdes et des soins. */
public final class Soins {
    private Soins() {}

    /** Seul un Hunter Virus soigne. */
    public static boolean verifierVirus(EntityPlayerMP p) {
        SanteData d = Sante.get(p);
        if (d != null && d.estVirus()) return true;
        p.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Seul un Hunter Virus sait faire ça."), true);
        return false;
    }

    private static int rang(EntityPlayerMP p) {
        SanteData d = Sante.get(p);
        return d == null ? 0 : Math.max(0, Math.min(3, d.rangVirus));
    }

    private static void dire(EntityPlayerMP p, String s) {
        p.sendMessage(new TextComponentString(s));
    }

    /** Le patient est-il encore là (connecté, même monde, à portée) ? */
    static EntityPlayerMP patient(EntityPlayerMP virus, java.util.UUID id, double marge) {
        if (id == null) return null;
        net.minecraft.server.MinecraftServer srv = virus.world.getMinecraftServer();
        EntityPlayerMP p = virus.getUniqueID().equals(id) ? virus : srv == null ? null : srv.getPlayerList().getPlayerByUUID(id);
        if (p == null || p.world != virus.world || p.isDead) return null;
        if (p != virus && virus.getDistance(p) > VirusConfig.distanceSoin + marge) return null;
        return p;
    }

    // ================================================================== administrer
    /** Clic droit sur le patient avec un remède ou un soin : vérifie, puis lance le geste (pilules : tout de suite). */
    public static void administrer(EntityPlayerMP virus, EntityPlayerMP patient, EnumHand main) {
        if (!verifierVirus(virus)) return;
        ItemStack s = virus.getHeldItem(main);
        if (!(s.getItem() instanceof ItemPreparation)) return;
        Defs.Preparation prep = ((ItemPreparation) s.getItem()).def();
        if (prep == null || !prep.administrable()) return;
        if (patient(virus, patient.getUniqueID(), 0) == null) {
            virus.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Approche-toi du patient."), true);
            return;
        }
        SanteData d = Sante.get(patient);
        if (d == null) return;
        String err = Traitements.verifierAvant(patient, d, prep);
        if (err != null) {
            dire(virus, TextFormatting.GOLD + err);
            return;
        }
        String geste = JeuxVirus.gesteAdministration(prep.forme);
        if (geste == null) {
            appliquer(virus, patient, s, prep, 100);
            return;
        }
        if (patient != virus)
            patient.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + Identite.nom(virus) + " vous administre : " + prep.nom), true);
        SeancesVirus.ouvrir(virus, SeancesVirus.ADMIN, geste, prep.id, Identite.nom(patient), 0, 1, rang(virus),
                JeuxVirus.parametreAdministration(prep.forme), patient.getPosition(), patient.getUniqueID(), main);
    }

    static void annule(EntityPlayerMP virus, SeancesVirus.Seance s) {
        virus.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "Geste annulé : rien n'est perdu."), true);
    }

    static void finAdministration(EntityPlayerMP virus, SeancesVirus.Seance s, double note) {
        ItemStack held = virus.getHeldItem(s.main);
        Defs.Preparation prep = held.getItem() instanceof ItemPreparation ? ((ItemPreparation) held.getItem()).def() : null;
        if (prep == null || !prep.id.equals(s.prep)) {
            dire(virus, TextFormatting.RED + "Garde le remède en main pendant le geste.");
            return;
        }
        EntityPlayerMP patient = patient(virus, s.patient, 3);
        if (patient == null) {
            dire(virus, TextFormatting.RED + "Le patient s'est éloigné : rien n'a été donné.");
            return;
        }
        if (note < VirusConfig.seuilAdministration) {
            held.shrink(1);
            dire(virus, TextFormatting.RED + "Geste raté (" + Math.round(note) + " %) : la dose est gâchée.");
            if (patient != virus) dire(patient, TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Le remède finit à côté.");
            virus.inventoryContainer.detectAndSendChanges();
            return;
        }
        appliquer(virus, patient, held, prep, note);
    }

    private static void appliquer(EntityPlayerMP virus, EntityPlayerMP patient, ItemStack s, Defs.Preparation prep, double geste) {
        SanteData d = Sante.get(patient);
        if (d == null) return;
        String err = Traitements.verifierAvant(patient, d, prep);
        if (err != null) {
            dire(virus, TextFormatting.GOLD + err);
            return;
        }
        int qualite = Qualites.notee(s) ? Qualites.note(s) : VirusConfig.seuilReussite;
        boolean bonus = qualite >= VirusConfig.seuilBonus;
        s.shrink(1);
        virus.inventoryContainer.detectAndSendChanges();
        virus.world.playSound(null, patient.posX, patient.posY, patient.posZ,
                "seringue".equals(prep.forme) ? VirusSons.SERINGUE : VirusSons.VERRE, SoundCategory.PLAYERS, 0.7f, 1.1f);
        dire(virus, TextFormatting.GREEN + "Administré : " + prep.nom + TextFormatting.GRAY
                + (JeuxVirus.gesteAdministration(prep.forme) == null ? "" : " · geste " + Math.round(geste) + " %")
                + (patient != virus ? " · à " + Identite.nom(patient) : ""));
        Traitements.administrer(virus, patient, d, prep, qualite, bonus, rang(virus));
        MoteurSante.synchroniser(patient, d, System.currentTimeMillis(), true);
    }

    // ================================================================== prise de sang
    /** Clic droit sur le patient avec une seringue vide. */
    public static void priseDeSang(EntityPlayerMP virus, EntityPlayerMP patient, EnumHand main) {
        if (!verifierVirus(virus)) return;
        if (patient(virus, patient.getUniqueID(), 0) == null) {
            virus.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Approche-toi du patient."), true);
            return;
        }
        SanteData d = Sante.get(patient);
        if (d == null) return;
        long now = System.currentTimeMillis();
        long attente = d.dernierePriseDeSang + (long) (VirusConfig.priseDeSangHeures * Maladies.HEURE) - now;
        if (attente > 0) {
            dire(virus, TextFormatting.GOLD + "Une prise de sang toutes les " + Fraicheur.duree((long) (VirusConfig.priseDeSangHeures * Maladies.HEURE))
                    + " par patient : encore " + Fraicheur.duree(attente) + ".");
            return;
        }
        if (patient != virus)
            patient.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + Identite.nom(virus) + " vous fait une prise de sang."), true);
        SeancesVirus.ouvrir(virus, SeancesVirus.SANG, "injection", "", Identite.nom(patient), 0, 1, rang(virus), 1,
                patient.getPosition(), patient.getUniqueID(), main);
    }

    static void finPriseDeSang(EntityPlayerMP virus, SeancesVirus.Seance s, double note) {
        ItemStack held = virus.getHeldItem(s.main);
        if (held.getItem() != VirusRegistre.SERINGUE) {
            dire(virus, TextFormatting.RED + "Garde la seringue en main pendant la prise de sang.");
            return;
        }
        EntityPlayerMP patient = patient(virus, s.patient, 3);
        if (patient == null) {
            dire(virus, TextFormatting.RED + "Le patient s'est éloigné.");
            return;
        }
        SanteData d = Sante.get(patient);
        if (d == null) return;
        d.dernierePriseDeSang = System.currentTimeMillis();
        d.dirty = true;
        held.shrink(1);
        virus.world.playSound(null, patient.posX, patient.posY, patient.posZ, VirusSons.SERINGUE, SoundCategory.PLAYERS, 0.7f, 0.9f);
        if (note < 50) {
            dire(virus, TextFormatting.RED + "Prise de sang ratée (" + Math.round(note) + " %) : la veine a roulé, la seringue est perdue.");
            if (patient != virus) dire(patient, TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Aïe. Ça a raté.");
        } else {
            ItemStack plein = Diagnostic.remplir(virus, patient, d, rang(virus), note);
            ItemHandlerHelper.giveItemToPlayer(virus, plein);
            dire(virus, TextFormatting.GREEN + "Prise de sang réussie (" + Math.round(note) + " %)" + TextFormatting.GRAY
                    + " : seringue pleine au nom de " + Identite.nom(patient)
                    + (note < 80 ? ", mais une partie des valeurs ne sera pas lisible." : ". À analyser au microscope."));
        }
        virus.inventoryContainer.detectAndSendChanges();
    }
}
