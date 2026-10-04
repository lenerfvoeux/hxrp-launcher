package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Une préparation ratée (sous 80 %) : bonne à jeter. */
public class ItemPrepaRatee extends Item {
    public ItemPrepaRatee() {
        setMaxStackSize(16);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null || !t.hasKey("v_rate") ? super.getItemStackDisplayName(s) : t.getString("v_rate") + " (ratée)";
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        NBTTagCompound t = s.getTagCompound();
        if (t != null && t.hasKey(Qualites.NOTE))
            tip.add(TextFormatting.RED + "Réussie à " + t.getInteger(Qualites.NOTE) + " % seulement (il faut " + VirusConfig.seuilReussite + " %)");
        tip.add(TextFormatting.DARK_GRAY + "Inutilisable : à jeter");
    }
}
