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
        // le tonneau du Kraken se pose sur l'eau, en visant la surface (onItemRightClick)
        if ("kraken".equals(espece.id)) return EnumActionResult.PASS;
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

    /**
     * Le tonneau du Kraken flotte : on vise la surface de l'eau (comme un nénuphar) et il se pose juste au-dessus.
     * Les autres appâts se posent au sol (onItemUse).
     */
    @Override
    public net.minecraft.util.ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer joueur, EnumHand main) {
        ItemStack s = joueur.getHeldItem(main);
        if (espece.arrivee != Espece.Arrivee.SUR_PLACE || !"kraken".equals(espece.id))
            return super.onItemRightClick(w, joueur, main);
        net.minecraft.util.math.RayTraceResult r = rayTrace(w, joueur, true);
        if (r == null || r.typeOfHit != net.minecraft.util.math.RayTraceResult.Type.BLOCK)
            return new net.minecraft.util.ActionResult<>(EnumActionResult.PASS, s);
        BlockPos eau = r.getBlockPos();
        if (w.getBlockState(eau).getMaterial() != net.minecraft.block.material.Material.WATER)
            return new net.minecraft.util.ActionResult<>(EnumActionResult.PASS, s);
        BlockPos ici = eau.up();
        String refus = refus(w, ici);
        if (refus != null || !w.isAirBlock(ici)) {
            if (!w.isRemote) joueur.sendStatusMessage(new TextComponentString("§c" + espece.nomAppat + " : " + (refus != null ? refus : "la place est prise.")), true);
            return new net.minecraft.util.ActionResult<>(EnumActionResult.FAIL, s);
        }
        if (!w.isRemote) {
            w.setBlockState(ici, block.getDefaultState());
            if (!joueur.capabilities.isCreativeMode) s.shrink(1);
            joueur.sendStatusMessage(new TextComponentString("§6" + espece.nomAppat + " posé… " + espece.nom + " le sentira bientôt."), true);
        }
        return new net.minecraft.util.ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    /** Pourquoi l'appât ne peut pas être posé ici, ou null. */
    @Nullable
    public String refus(World w, BlockPos ici) {
        Biome b = w.getBiome(ici);
        if ("kraken".equals(espece.id)) {
            if (!w.isAirBlock(ici)) return "il se pose à la surface de l'eau.";
            // sur l'eau profonde : au moins quatre blocs d'eau sous le tonneau
            for (int k = 1; k <= 4; k++)
                if (w.getBlockState(ici.down(k)).getMaterial() != net.minecraft.block.material.Material.WATER) return "il faut le poser sur l'eau profonde.";
            return null;
        }
        if (!w.getBlockState(ici.down()).isSideSolid(w, ici.down(), EnumFacing.UP)) return "il se pose au sol.";
        switch (espece.id) {
            case "calamar_des_sables":
                return w.getBlockState(ici.down()).getMaterial() == net.minecraft.block.material.Material.SAND ? null : "il se pose sur le sable.";
            case "demon_de_lave":
                for (BlockPos q : BlockPos.getAllInBoxMutable(ici.add(-3, -2, -3), ici.add(3, 1, 3)))
                    if (w.getBlockState(q).getMaterial() == net.minecraft.block.material.Material.LAVA) return null;
                return "il faut le poser à moins de 3 blocs de la lave.";
            case "ai":
                if (!BiomeDictionary.hasType(b, BiomeDictionary.Type.SWAMP)) return "il faut un marais.";
                return w.isDaytime() ? "il ne s'allume que la nuit." : null;
            case "reine_guepe":
                if (!BiomeDictionary.hasType(b, BiomeDictionary.Type.JUNGLE) && !BiomeDictionary.hasType(b, BiomeDictionary.Type.FOREST))
                    return "il faut une jungle ou une forêt.";
                return null;
            case "abomination":
            case "xerra":
                return w.canSeeSky(ici) ? "il faut le poser sous terre, à l'abri du ciel." : null;
            case "roi_des_glaces":
                if (!BiomeDictionary.hasType(b, BiomeDictionary.Type.COLD) && !BiomeDictionary.hasType(b, BiomeDictionary.Type.SNOWY))
                    return "il faut un biome froid.";
                return null;
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
