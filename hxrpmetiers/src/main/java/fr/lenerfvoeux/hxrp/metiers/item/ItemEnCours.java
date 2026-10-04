package fr.lenerfvoeux.hxrp.metiers.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Préparation en cours : on la porte de station en station jusqu'au plat fini. */
public class ItemEnCours extends Item {
    public ItemEnCours() {
        setRegistryName(HxrpMetiers.MODID, "preparation_en_cours");
        setTranslationKey(HxrpMetiers.MODID + ".preparation_en_cours");
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        FoodEntry r = EnCours.recette(s);
        return r == null ? super.getItemStackDisplayName(s) : "Préparation : " + r.name;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        FoodEntry r = EnCours.recette(s);
        if (r == null) return;
        int i = EnCours.etape(s);
        tip.add(TextFormatting.GRAY + "Étape " + Math.min(i + 1, r.steps.size()) + "/" + r.steps.size());
        String geste = EnCours.geste(s);
        if (geste != null) {
            Station st = Station.forGeste(geste);
            tip.add(TextFormatting.GOLD + "À faire : " + geste + TextFormatting.GRAY + " · " + st.label);
        }
        for (int k = 0; k < r.steps.size(); k++) {
            String prefix = k < i ? TextFormatting.GREEN + " ✔ " : k == i ? TextFormatting.YELLOW + " ➜ " : TextFormatting.DARK_GRAY + " · ";
            tip.add(prefix + r.steps.get(k));
        }
        if (i > 0) tip.add(TextFormatting.AQUA + "Moyenne des étapes : " + Math.round(EnCours.moyenne(s)) + " %");
        int f = (int) Math.round(EnCours.fraicheur(s) * 100);
        tip.add((f > 75 ? TextFormatting.GREEN : f > 25 ? TextFormatting.YELLOW : TextFormatting.RED)
                + "Fraîcheur des ingrédients : " + f + " %");
    }
}
