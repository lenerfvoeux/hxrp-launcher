package fr.lenerfvoeux.hxrp.metiers.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Ingrédient brut, épice/condiment ou préparation : non comestible, sert en cuisine. */
public class ItemIngredient extends Item implements IFoodItem {
    protected final FoodEntry entry;

    public ItemIngredient(FoodEntry e) {
        this.entry = e;
        setRegistryName(HxrpMetiers.MODID, e.id);
        setTranslationKey(HxrpMetiers.MODID + "." + e.id);
        setMaxStackSize(64);
    }

    @Override public FoodEntry entry() { return entry; }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String n = super.getItemStackDisplayName(stack);
        return Fraicheur.rotten(stack, HxrpMetiers.proxy.now()) ? TextFormatting.GRAY + n + " (avarié)" : n;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        String kind = "preparation".equals(entry.kind) ? "Préparation" : "epice".equals(entry.kind) ? entry.cat : entry.cat;
        tip.add(TextFormatting.GRAY + kind + (entry.source != null ? " · " + entry.source : ""));
        Tooltips.fraicheur(stack, tip);
        if ("preparation".equals(entry.kind) && flag.isAdvanced()) tip.add(TextFormatting.DARK_GRAY + "Étapes : " + String.join(", ", entry.steps));
    }
}
