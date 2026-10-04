package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Génération du monde : arbres fruitiers selon le climat, filons de sel, ruches sauvages sur les troncs,
 * bancs de moules et d'huîtres sur les plages. Tout est décalé de 8 blocs dans le chunk
 * pour ne jamais déborder sur un chunk pas encore généré.
 */
public class GenMonde implements IWorldGenerator {
    private WorldGenMinable sel;

    @Override
    public void generate(Random r, int cx, int cz, World w, IChunkGenerator gen, IChunkProvider prov) {
        if (w.provider.getDimension() != 0) return;
        int x0 = cx * 16 + 8, z0 = cz * 16 + 8;
        Biome b = w.getBiome(new BlockPos(x0 + 8, 64, z0 + 8));
        String climat = Climat.de(b);
        arbres(w, r, x0, z0, climat);
        sel(w, r, x0, z0);
        if (r.nextInt(6) == 0 && (climat.equals(Climat.TEMPERE) || climat.equals(Climat.CHAUD) || climat.equals(Climat.TROPICAL))) ruche(w, r, x0, z0);
        if (climat.equals(Climat.PLAGE) || climat.equals(Climat.OCEAN)) coquillages(w, r, x0, z0);
    }

    private void arbres(World w, Random r, int x0, int z0, String climat) {
        if (r.nextInt(100) >= ModConfig.chanceArbreFruitier) return;
        List<BlockFeuillage> ok = new ArrayList<>();
        for (BlockFeuillage f : ModRegistry.FEUILLES.values()) if (f.arbre.climat.equals(climat)) ok.add(f);
        if (ok.isEmpty()) return;
        BlockFeuillage f = ok.get(r.nextInt(ok.size()));
        int n = 1 + r.nextInt(2);
        for (int k = 0; k < n; k++) {
            BlockPos p = w.getHeight(new BlockPos(x0 + r.nextInt(16), 0, z0 + r.nextInt(16)));
            IBlockState sol = w.getBlockState(p.down());
            if (sol.getBlock() == Blocks.GRASS || sol.getBlock() == Blocks.DIRT) ArbreFruitier.generer(w, p, f, r, true);
        }
    }

    private void sel(World w, Random r, int x0, int z0) {
        if (sel == null) sel = new WorldGenMinable(ModRegistry.MINERAI_SEL.getDefaultState(), 7);
        for (int k = 0; k < ModConfig.filonsDeSel; k++)
            sel.generate(w, r, new BlockPos(x0 + r.nextInt(16), 20 + r.nextInt(50), z0 + r.nextInt(16)));
    }

    /** Une ruche accrochée sur le côté d'un tronc, à hauteur d'homme ou un peu plus. */
    private void ruche(World w, Random r, int x0, int z0) {
        for (int essai = 0; essai < 12; essai++) {
            BlockPos top = w.getHeight(new BlockPos(x0 + r.nextInt(16), 0, z0 + r.nextInt(16)));
            for (int dy = 0; dy < 10; dy++) {
                BlockPos p = top.down(dy);
                if (!(w.getBlockState(p).getBlock() instanceof BlockLog)) continue;
                for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                    BlockPos h = p.offset(f);
                    if (w.isAirBlock(h) && !w.isAirBlock(h.down(2))) {
                        w.setBlockState(h, ModRegistry.RUCHE.getDefaultState().withProperty(BlockRuche.FACING, f).withProperty(BlockRuche.PLEINE, true), 2);
                        return;
                    }
                }
            }
        }
    }

    /** Moules et huîtres sur le sable ou le gravier juste au bord de l'eau. */
    private void coquillages(World w, Random r, int x0, int z0) {
        for (int k = 0; k < 6; k++) {
            BlockPos p = w.getHeight(new BlockPos(x0 + r.nextInt(16), 0, z0 + r.nextInt(16)));
            Block sol = w.getBlockState(p.down()).getBlock();
            if (p.getY() < 60 || p.getY() > 66 || (sol != Blocks.SAND && sol != Blocks.GRAVEL) || !w.isAirBlock(p)) continue;
            boolean eau = false;
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) eau |= w.getBlockState(p.down().offset(f)).getMaterial().isLiquid() || w.getBlockState(p.offset(f)).getMaterial().isLiquid();
            if (!eau) continue;
            Block c = r.nextBoolean() ? ModRegistry.MOULES : ModRegistry.HUITRES;
            w.setBlockState(p, c.getDefaultState(), 2);
        }
    }
}
