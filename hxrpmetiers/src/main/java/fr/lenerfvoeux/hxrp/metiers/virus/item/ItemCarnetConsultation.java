package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Carnet de consultation d'un patient : nom, sexe, groupe, valeurs lues, symptômes cochés, date de la dernière prise.
 * Il s'ouvre au clic droit et se donne à un autre Virus, qui voit l'analyse.
 */
public class ItemCarnetConsultation extends Item {
    public ItemCarnetConsultation() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null ? super.getItemStackDisplayName(s) : super.getItemStackDisplayName(s) + " · " + t.getString(Diagnostic.PATIENT);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        if (w.isRemote && s.hasTagCompound()) HxrpMetiers.proxy.ouvrirLectureVirus(s);
        return new ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        NBTTagCompound t = s.getTagCompound();
        if (t == null) {
            tip.add(TextFormatting.GRAY + "Vierge : il s'ouvre à la première analyse d'un patient.");
            return;
        }
        tip.add(TextFormatting.GRAY + "Dernière prise de sang : " + Diagnostic.date(t.getLong(Diagnostic.DATE)));
        tip.add(TextFormatting.GRAY + "" + t.getTagList(Diagnostic.SYMPTOMES, 8).tagCount() + " symptôme(s) coché(s)");
        tip.add(TextFormatting.DARK_GRAY + "Clic droit pour l'ouvrir");
    }
}
