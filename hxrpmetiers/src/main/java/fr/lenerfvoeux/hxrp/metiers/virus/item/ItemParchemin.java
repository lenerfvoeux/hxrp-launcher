package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
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

/** Le parchemin du Second Souffle : une page arrachée au Grimoire, que seul un Virus 3★ sait lire. */
public class ItemParchemin extends Item {
    public ItemParchemin() {
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.EPIC;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        if (w.isRemote) HxrpMetiers.proxy.ouvrirLectureVirus(s);
        return new ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add(TextFormatting.GOLD + "" + TextFormatting.ITALIC + "Une page qui n'existe dans aucun Grimoire.");
        tip.add(TextFormatting.DARK_GRAY + "Clic droit pour la lire (Virus 3★)");
    }
}
