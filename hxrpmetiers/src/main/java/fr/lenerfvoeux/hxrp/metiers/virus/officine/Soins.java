package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

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

    public static void administrer(EntityPlayerMP virus, EntityPlayerMP patient, net.minecraft.util.EnumHand main) {
        if (!verifierVirus(virus)) return;
        virus.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "L'administration arrive avec l'officine."), true);
    }

    public static void priseDeSang(EntityPlayerMP virus, EntityPlayerMP patient) {
        if (!verifierVirus(virus)) return;
        virus.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "La prise de sang arrive avec le diagnostic."), true);
    }
}
