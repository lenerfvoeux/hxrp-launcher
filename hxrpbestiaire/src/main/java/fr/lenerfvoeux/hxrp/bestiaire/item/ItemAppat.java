package fr.lenerfvoeux.hxrp.bestiaire.item;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.bloc.BlockAppat;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
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
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** L'appât d'un boss (admins, /<boss> appat) : il se pose au sol, aux conditions de son boss (onglet Appâts). */
public class ItemAppat extends ItemBlock {
    private final Espece espece;

    public ItemAppat(Block b) {
        super(b);
        this.espece = ((BlockAppat) b).espece;
        setMaxStackSize(1);
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.EPIC;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer joueur, World w, BlockPos pos, EnumHand main, EnumFacing face, float hx, float hy, float hz) {
        BlockPos ici = w.getBlockState(pos).getBlock().isReplaceable(w, pos) ? pos : pos.offset(face);
        String refus = refus(w, ici);
        if (refus != null) {
            if (!w.isRemote) joueur.sendStatusMessage(new TextComponentString("§c" + espece.nomAppat + " : " + refus), true);
            return EnumActionResult.FAIL;
        }
        EnumActionResult r = super.onItemUse(joueur, w, pos, main, face, hx, hy, hz);
        if (r == EnumActionResult.SUCCESS && !w.isRemote)
            joueur.sendStatusMessage(new TextComponentString("§6" + espece.nomAppat + " posé… " + espece.nom + " le sentira bientôt."), true);
        return r;
    }

    /** Pourquoi l'appât ne peut pas être posé ici, ou null. */
    @Nullable
    public String refus(World w, BlockPos ici) {
        if (!w.getBlockState(ici.down()).isSideSolid(w, ici.down(), EnumFacing.UP)) return "il se pose au sol.";
        Biome b = w.getBiome(ici);
        switch (espece.id) {
            case "reine_guepe":
                if (!BiomeDictionary.hasType(b, BiomeDictionary.Type.JUNGLE) && !BiomeDictionary.hasType(b, BiomeDictionary.Type.FOREST))
                    return "il faut une jungle ou une forêt.";
                return null;
            case "abomination":
            case "xerra":
                return w.canSeeSky(ici) ? "il faut le poser sous terre, à l'abri du ciel." : null;
            default:
                return null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        Stats.Boss b = Stats.boss(espece.id);
        tip.add("§6Appât de " + espece.nom + ".");
        tip.add("§7À poser " + espece.conditionAppat + " ; " + espece.nom + " arrive en " + (int) b.appatDelaiMin + " à " + (int) b.appatDelaiMax + " s");
        tip.add(espece.arrivee == Espece.Arrivee.SUR_PLACE ? "§7et surgit sur place, puis le dévore." : "§7de " + (int) b.appatDistanceMin + " à "
                + (int) b.appatDistanceMax + " blocs, et le dévore.");
        tip.add("§cRéservé aux admins.");
    }
}
