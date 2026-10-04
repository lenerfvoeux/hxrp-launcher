package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Bloc plein du monde qui renferme un ingrédient (terre truffière, vase à algue noire, roches…) : on le creuse
 * ou on le gratte, il rend l'ingrédient et la matière ordinaire dont il est fait.
 */
public class BlockSolVirus extends Block {
    public final Defs.Ingredient def;
    public final String id;
    private final boolean roche;

    public BlockSolVirus(Defs.Ingredient d) {
        super(materiau(d.recolte.bloc), couleur(d.recolte.bloc));
        this.def = d;
        this.id = d.recolte.bloc;
        this.roche = materiau(d.recolte.bloc) == Material.ROCK;
        setRegistryName(HxrpMetiers.MODID, id);
        setTranslationKey(HxrpMetiers.MODID + "." + id);
        setHardness(roche ? 1.8f : 0.7f);
        setResistance(roche ? 8f : 2.5f);
        setSoundType(roche ? SoundType.STONE : "vase_a_algue_noire".equals(id) ? SoundType.SLIME : SoundType.GROUND);
        setHarvestLevel(roche ? "pickaxe" : "shovel", 0);
        if ("roche_des_abysses".equals(id)) setLightLevel(6 / 15f);
        if ("roche_a_fer_sang".equals(id)) setLightLevel(2 / 15f);
    }

    private static Material materiau(String b) {
        switch (b == null ? "" : b) {
            case "terre_truffiere": return Material.GROUND;
            case "vase_a_algue_noire": return Material.CLAY;
            default: return Material.ROCK;
        }
    }

    private static MapColor couleur(String b) {
        switch (b == null ? "" : b) {
            case "terre_truffiere": return MapColor.BROWN;
            case "vase_a_algue_noire": return MapColor.BLACK;
            case "roche_a_fer_sang": return MapColor.RED;
            case "roche_des_abysses": return MapColor.CYAN;
            default: return MapColor.GREEN;
        }
    }

    /** La matière ordinaire qui reste quand on a pris l'ingrédient. */
    private ItemStack reste() {
        switch (id) {
            case "terre_truffiere": return new ItemStack(Blocks.DIRT);
            case "vase_a_algue_noire": return new ItemStack(Blocks.DIRT);
            case "pierre_moussue_humide": return new ItemStack(Blocks.MOSSY_COBBLESTONE);
            default: return new ItemStack(Blocks.COBBLESTONE);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
        Random r = w instanceof World ? ((World) w).rand : RANDOM;
        Item i = Objets.item(def.id);
        if (i != null) drops.add(new ItemStack(i, Cueillette.quantite(def, r, fortune)));
        drops.add(reste());
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult t, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(this);
    }

    /** La roche des abysses scintille, la vase fait des bulles. */
    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if ("roche_des_abysses".equals(id) && r.nextInt(4) == 0) {
            for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.values()) {
                BlockPos o = pos.offset(f);
                if (!w.isAirBlock(o)) continue;
                w.spawnParticle(EnumParticleTypes.END_ROD, o.getX() + r.nextDouble(), o.getY() + r.nextDouble(), o.getZ() + r.nextDouble(), 0, 0.005, 0);
                return;
            }
        }
    }
}
