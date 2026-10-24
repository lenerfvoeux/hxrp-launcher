package fr.lenerfvoeux.hxrp.bestiaire.item;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Œuf d'apparition d'un boss (admins) : posé au sol, le boss apparaît sur place. */
public class ItemOeufBoss extends Item {
    public final Espece espece;

    public ItemOeufBoss(Espece e) {
        this.espece = e;
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasEffect(ItemStack s) {
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer joueur, World w, BlockPos pos, EnumHand main, EnumFacing face, float hx, float hy, float hz) {
        ItemStack s = joueur.getHeldItem(main);
        BlockPos ici = w.getBlockState(pos).getBlock().isReplaceable(w, pos) ? pos : pos.offset(face);
        if (!joueur.canPlayerEdit(ici, face, s)) return EnumActionResult.FAIL;
        if (!w.isRemote) {
            EntityBoss.invoquer(espece, w, ici.getX() + 0.5, ici.getY(), ici.getZ() + 0.5, joueur.rotationYaw + 180F);
            if (!joueur.capabilities.isCreativeMode) s.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add("§7Posé au sol, " + espece.nom + " apparaît sur place.");
        if (!espece.lieu.isEmpty()) tip.add("§8Lieu : " + espece.lieu);
        tip.add("§cRéservé aux admins.");
    }
}
