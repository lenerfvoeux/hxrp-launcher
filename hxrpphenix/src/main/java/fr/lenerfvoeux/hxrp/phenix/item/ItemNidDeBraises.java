package fr.lenerfvoeux.hxrp.phenix.item;

import fr.lenerfvoeux.hxrp.phenix.stats.StatsPhenix;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** L'appât du Phénix, donné par un admin (/phenix appat) : se pose au sol, à l'air libre. */
public class ItemNidDeBraises extends ItemBlock {
    public ItemNidDeBraises(Block b) {
        super(b);
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.EPIC;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer joueur, World w, BlockPos pos, EnumHand main, EnumFacing face,
                                      float hx, float hy, float hz) {
        BlockPos ici = w.getBlockState(pos).getBlock().isReplaceable(w, pos) ? pos : pos.offset(face);
        if (!w.getBlockState(ici.down()).isSideSolid(w, ici.down(), EnumFacing.UP) || !w.canSeeSky(ici)) {
            if (!w.isRemote) joueur.sendStatusMessage(new TextComponentString("§cLe nid de braises se pose au sol, à l'air libre."), true);
            return EnumActionResult.FAIL;
        }
        EnumActionResult r = super.onItemUse(joueur, w, pos, main, face, hx, hy, hz);
        if (r == EnumActionResult.SUCCESS && !w.isRemote)
            joueur.sendStatusMessage(new TextComponentString("§6Le nid de braises fume… le Phénix le sentira bientôt."), true);
        return r;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add("§6Un nid de branches rougeoyantes : l'appât du Phénix de feu.");
        tip.add(String.format("§7Posé au sol à l'air libre, il l'attire en %d à %d s ;", (int) StatsPhenix.appatDelaiMin, (int) StatsPhenix.appatDelaiMax));
        tip.add(String.format("§7le Phénix arrive du ciel, de %d à %d blocs, et le dévore.", (int) StatsPhenix.appatDistanceMin, (int) StatsPhenix.appatDistanceMax));
        tip.add("§cRéservé aux admins.");
    }
}
