package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
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

/** Seringue pleine du sang d'un patient, à analyser au microscope. */
public class ItemSeringuePleine extends Item {
    public ItemSeringuePleine() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? super.getItemStackDisplayName(s) : super.getItemStackDisplayName(s) + " · " + t.getString(Diagnostic.PATIENT);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) return;
        tip.add(TextFormatting.GRAY + "Prélevé le " + Diagnostic.date(t.getLong(Diagnostic.DATE)));
        tip.add(TextFormatting.GRAY + "" + t.getCompoundTag(Diagnostic.SANG).getKeySet().size() + " valeur(s) lisible(s)");
        tip.add(TextFormatting.DARK_GRAY + "Clic droit sur le microscope d'analyse");
    }
}
