package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Ordonnances;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/** Ordonnance remise au patient. Clic droit : la lire. Tant qu'il la porte, un petit rappel affiche la prochaine prise. */
public class ItemOrdonnance extends Item {
    public ItemOrdonnance() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack s) {
        NBTTagCompound t = s.getTagCompound();
        return t == null || !t.hasKey(Ordonnances.NOM) ? super.getItemStackDisplayName(s) : "Ordonnance · " + t.getString(Ordonnances.NOM);
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
            tip.add(TextFormatting.GRAY + "Papier vierge : le Virus la remplit en soignant.");
            return;
        }
        tip.add(TextFormatting.GRAY + "Patient : " + TextFormatting.WHITE + t.getString(Ordonnances.PATIENT));
        tip.add(TextFormatting.GRAY + "Prescrite par " + t.getString(Ordonnances.VIRUS) + ", le "
                + new SimpleDateFormat("dd/MM à HH:mm").format(new Date(t.getLong(Ordonnances.DATE))));
        NBTTagList l = t.getTagList(Ordonnances.PRISES, 8);
        for (int i = 0; i < l.tagCount(); i++) tip.add(TextFormatting.DARK_AQUA + "• " + l.getStringTagAt(i));
        String c = t.getString(Ordonnances.CONSIGNE);
        if (!c.isEmpty()) tip.add(TextFormatting.GOLD + "Consigne : " + c);
        tip.add(TextFormatting.DARK_GRAY + "Clic droit pour la lire");
    }
}
