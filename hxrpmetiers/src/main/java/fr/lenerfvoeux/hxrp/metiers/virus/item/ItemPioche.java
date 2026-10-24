package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Pioche de prospecteur, fabriquée par le Virus : le seul outil pour récolter les minerais et les roches (minerai de
 * sel, pierre moussue humide) au clic droit. Ce n'est pas une pioche vanilla : elle ne casse rien et ne mine pas plus
 * vite. Elle s'use d'un cran par récolte ; au bout de 250 elle est émoussée (elle ne disparaît pas) et attend une
 * réparation à la table de préparation : un lingot d'ostéacier rend 125 utilisations. Ni enclume, ni enchantement.
 */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public class ItemPioche extends Item {
    public static final int UTILISATIONS = 250, REPARATION = 125;

    public ItemPioche() {
        setMaxStackSize(1);
        setMaxDamage(UTILISATIONS);
        setNoRepair();
        setFull3D();
    }

    public static boolean est(ItemStack s) {
        return !s.isEmpty() && s.getItem() instanceof ItemPioche;
    }

    /** La pioche tenue en main principale, sinon dans l'autre main, ou une pile vide. */
    public static ItemStack tenue(EntityPlayer p) {
        ItemStack s = p.getHeldItem(EnumHand.MAIN_HAND);
        if (est(s)) return s;
        s = p.getHeldItem(EnumHand.OFF_HAND);
        return est(s) ? s : ItemStack.EMPTY;
    }

    public static int restantes(ItemStack s) {
        return Math.max(0, s.getMaxDamage() - s.getItemDamage());
    }

    public static boolean emoussee(ItemStack s) {
        return restantes(s) <= 0;
    }

    /** Une récolte de plus (sans jamais casser la pioche). */
    public static void user(ItemStack s) {
        if (est(s)) s.setItemDamage(Math.min(s.getMaxDamage(), s.getItemDamage() + 1));
    }

    /** Réparation à la table : rend REPARATION utilisations. */
    public static void reparer(ItemStack s) {
        if (est(s)) s.setItemDamage(Math.max(0, s.getItemDamage() - REPARATION));
    }

    /** Elle ne mine pas plus vite que la main… */
    @Override
    public float getDestroySpeed(ItemStack s, IBlockState state) {
        return 1f;
    }

    /** … et ne s'use pas en tapant sur des blocs. */
    @Override
    public boolean onBlockDestroyed(ItemStack s, World w, IBlockState state, net.minecraft.util.math.BlockPos pos, net.minecraft.entity.EntityLivingBase e) {
        return false;
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack s) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack s, ItemStack livre) {
        return false;
    }

    /** Pas de réparation ni de fusion à l'enclume : seulement à la table de préparation. */
    @SubscribeEvent
    public static void enclume(AnvilUpdateEvent e) {
        if (est(e.getLeft()) || est(e.getRight())) e.setCanceled(true);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        int n = restantes(s);
        tip.add((n == 0 ? TextFormatting.RED : n < 50 ? TextFormatting.GOLD : TextFormatting.GREEN) + "Utilisations : " + n + " / " + s.getMaxDamage());
        if (n == 0) tip.add(TextFormatting.RED + "Émoussée : à réparer avant de servir");
        tip.add(TextFormatting.GRAY + "Clic droit sur le minerai de sel ou la pierre moussue humide");
        tip.add(TextFormatting.GRAY + "Ne casse aucun bloc");
        tip.add(TextFormatting.DARK_GRAY + "Réparation : table de préparation + 1 lingot d'ostéacier (+" + REPARATION + ")");
    }
}
