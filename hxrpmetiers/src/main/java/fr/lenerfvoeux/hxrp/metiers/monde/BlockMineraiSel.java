package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import java.util.Random;

/** Minerai de sel : dans la pierre, entre les couches 20 et 70 ; se mine à la pioche et donne du sel. */
public class BlockMineraiSel extends Block {
    public BlockMineraiSel() {
        super(Material.ROCK);
        setRegistryName(HxrpMetiers.MODID, "minerai_de_sel");
        setTranslationKey(HxrpMetiers.MODID + ".minerai_de_sel");
        setHardness(2.5f);
        setResistance(5f);
        setSoundType(SoundType.STONE);
        setHarvestLevel("pickaxe", 0);
    }

    @Override public Item getItemDropped(IBlockState s, Random r, int fortune) { return ModRegistry.FOOD.get("sel"); }
    @Override public int quantityDropped(Random r) { return 2 + r.nextInt(3); }
    @Override public int quantityDroppedWithBonus(int fortune, Random r) { return quantityDropped(r) + (fortune > 0 ? r.nextInt(fortune + 1) : 0); }

    @Override
    public int getExpDrop(IBlockState s, IBlockAccess w, BlockPos pos, int fortune) {
        return MathHelper.getInt(RANDOM, 0, 2);
    }
}
