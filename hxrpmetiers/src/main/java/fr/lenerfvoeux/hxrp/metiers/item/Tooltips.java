package fr.lenerfvoeux.hxrp.metiers.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public final class Tooltips {
    private Tooltips() {}

    public static void fraicheur(ItemStack s, List<String> tip) {
        long now = HxrpMetiers.proxy.now();
        if (!Fraicheur.perishable(s)) {
            tip.add(TextFormatting.DARK_AQUA + "Ne périme pas");
            return;
        }
        if (!Fraicheur.stamped(s)) {
            tip.add(TextFormatting.GREEN + "Frais · se conserve " + Fraicheur.duree(Fraicheur.totalHours(s) * Fraicheur.HOUR));
            return;
        }
        long rem = Fraicheur.remaining(s, now);
        if (rem <= 0) {
            tip.add(TextFormatting.DARK_RED + "Avarié : inutilisable, à jeter");
            return;
        }
        double f = Fraicheur.fraction(s, now);
        TextFormatting c = f > 0.75 ? TextFormatting.GREEN : f > 0.25 ? TextFormatting.YELLOW : TextFormatting.RED;
        int pct = (int) Math.round(f * 100);
        if (Fraicheur.inFridgeMode(s))
            tip.add(TextFormatting.AQUA + "Au frigo · " + c + pct + " % · encore " + Fraicheur.duree(rem) + TextFormatting.DARK_AQUA + " (3x plus lent)");
        else
            tip.add(c + "Fraîcheur " + pct + " % · encore " + Fraicheur.duree(rem));
    }
}
