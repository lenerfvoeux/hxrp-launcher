package fr.lenerfvoeux.hxrp.metiers.item;

import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
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

/** Bouteille d'eau (boutique) : ni péremption ni cooldown. */
public class ItemBouteille extends ItemIngredient {
    public ItemBouteille(FoodEntry e) {
        super(e);
        setMaxStackSize(16);
    }

    @Override public int getMaxItemUseDuration(ItemStack s) { return 32; }
    @Override public EnumAction getItemUseAction(ItemStack s) { return EnumAction.DRINK; }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        p.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, p.getHeldItem(hand));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack s, World w, EntityLivingBase e) {
        if (!w.isRemote && e instanceof EntityPlayerMP) {
            NutritionData d = Nutrition.get((EntityPlayer) e);
            if (d != null) {
                d.addSoif(ModConfig.soifBouteille);
                Network.sync((EntityPlayerMP) e);
            }
        }
        if (!(e instanceof EntityPlayer) || !((EntityPlayer) e).capabilities.isCreativeMode) s.shrink(1);
        return s;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add(TextFormatting.AQUA + "Soif +" + ModConfig.soifBouteille);
        tip.add(TextFormatting.DARK_AQUA + "Ne périme pas · pas de cooldown");
    }
}
