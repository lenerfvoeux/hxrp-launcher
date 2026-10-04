package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.EnCoursVirus;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Une préparation de l'officine en cours : elle porte sa recette, l'étape atteinte et les notes déjà obtenues. */
public class ItemEnCoursVirus extends Item {
    public ItemEnCoursVirus() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        Defs.Preparation x = EnCoursVirus.preparation(s);
        return x == null ? super.getItemStackDisplayName(s) : x.nom + " (en cours)";
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        Defs.Preparation x = EnCoursVirus.preparation(s);
        if (x == null) return;
        int e = EnCoursVirus.etape(s);
        double[] n = EnCoursVirus.notes(s);
        for (int i = 0; i < x.etapes.size(); i++) {
            String l = (i + 1) + ". " + x.etapes.get(i).libelle();
            if (i < n.length) tip.add(TextFormatting.DARK_GREEN + "✔ " + l + TextFormatting.GRAY + " · " + Math.round(n[i]) + " %");
            else if (i == e) tip.add(TextFormatting.GOLD + "➜ " + l);
            else tip.add(TextFormatting.DARK_GRAY + "   " + l);
        }
        tip.add(TextFormatting.GRAY + "Virus " + EnCoursVirus.rang(s) + "★ · bonus d'étoiles +" + bonus(EnCoursVirus.rang(s)));
        tip.add(TextFormatting.DARK_GRAY + "Clic droit sur la machine de l'étape en cours");
    }

    private static int bonus(int r) {
        int[] b = VirusConfig.bonusEtoiles;
        return b == null || b.length == 0 ? 0 : b[Math.min(b.length - 1, r)];
    }
}
