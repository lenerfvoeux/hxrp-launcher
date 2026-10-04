package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.Soins;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
 * Seringue stérile, prise à volonté dans le meuble à tiroirs. Clic droit sur un patient : prise de sang
 * (seul un Hunter Virus sait le faire) ; accroupi + clic droit dans le vide : sur soi-même.
 * Sert aussi de contenant aux remèdes injectables.
 */
public class ItemSeringue extends Item {
    public ItemSeringue() {
        setMaxStackSize(16);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack s, EntityPlayer p, EntityLivingBase cible, EnumHand hand) {
        if (!(cible instanceof EntityPlayer)) return false;
        if (!p.world.isRemote) Soins.priseDeSang((EntityPlayerMP) p, (EntityPlayerMP) cible);
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        if (!p.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, s);
        if (!w.isRemote) Soins.priseDeSang((EntityPlayerMP) p, (EntityPlayerMP) p);
        return new ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add(TextFormatting.GRAY + "Clic droit sur un patient : prise de sang");
        tip.add(TextFormatting.GRAY + "Accroupi + clic droit : sur soi-même");
        tip.add(TextFormatting.DARK_GRAY + "Une prise toutes les 2 h par patient");
    }
}
