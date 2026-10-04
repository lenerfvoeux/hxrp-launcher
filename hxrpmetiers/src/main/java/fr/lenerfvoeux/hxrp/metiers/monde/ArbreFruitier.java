package fr.lenerfvoeux.hxrp.metiers.monde;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Construit un arbre fruitier : tronc en bois vanilla et feuillage de l'espèce.
 * Formes : rond (pommier), ovale (poirier), large (noyer, figuier, olivier), buisson (noisetier),
 * palmier (cocotier) et bananier.
 */
public final class ArbreFruitier {
    private ArbreFruitier() {}

    /** Une pousse devient un arbre (si la place le permet). */
    public static void pousser(World w, BlockPos pos, BlockPousse p, Random r) {
        IBlockState avant = w.getBlockState(pos);
        w.setBlockState(pos, Blocks.AIR.getDefaultState(), 4);
        if (!generer(w, pos, p.feuilles, r, false)) w.setBlockState(pos, avant, 4);
    }

    static IBlockState bois(String b) {
        switch (b == null ? "oak" : b) {
            case "spruce": return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE);
            case "birch": return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH);
            case "jungle": return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.JUNGLE);
            case "acacia": return Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.ACACIA);
            case "dark_oak": return Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, BlockPlanks.EnumType.DARK_OAK);
            default: return Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.OAK);
        }
    }

    /**
     * @param monde vrai à la génération du monde : quelques fruits sont déjà mûrs.
     * @return faux si la place manque (rien n'est posé).
     */
    public static boolean generer(World w, BlockPos base, BlockFeuillage f, Random r, boolean monde) {
        String forme = f.arbre.forme;
        int h;
        switch (forme) {
            case "palmier": h = 6 + r.nextInt(3); break;
            case "bananier": h = 3 + r.nextInt(2); break;
            case "buisson": h = 2; break;
            case "large": h = 3 + r.nextInt(2); break;
            case "ovale": h = 5 + r.nextInt(2); break;
            default: h = 4 + r.nextInt(2);
        }
        if (base.getY() < 1 || base.getY() + h + 3 >= w.getHeight()) return false;
        IBlockState sol = w.getBlockState(base.down());
        if (!sol.getBlock().canSustainPlant(sol, w, base.down(), EnumFacing.UP, (BlockPousse) f.pousse)) return false;
        for (int y = 0; y <= h; y++) if (!libre(w, base.up(y))) return false;
        IBlockState tronc = bois(f.arbre.bois);
        IBlockState feuille = f.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false).withProperty(BlockLeaves.DECAYABLE, true);
        sol.getBlock().onPlantGrow(sol, w, base.down(), base);
        for (int y = 0; y < h; y++) w.setBlockState(base.up(y), tronc, 2);
        BlockPos top = base.up(h);
        switch (forme) {
            case "palmier":
            case "bananier": {
                int bras = forme.equals("palmier") ? 3 : 2;
                poser(w, top, feuille, r, monde);
                poser(w, top.up(), feuille, r, monde);
                for (EnumFacing d : EnumFacing.Plane.HORIZONTAL) {
                    for (int k = 1; k <= bras; k++) poser(w, top.offset(d, k).down(k == bras ? 1 : 0), feuille, r, monde);
                    poser(w, top.offset(d).offset(d.rotateY()), feuille, r, monde);
                }
                break;
            }
            case "buisson":
                boule(w, base.up(1), 2, 1, feuille, r, monde, true);
                boule(w, base.up(3), 1, 0, feuille, r, monde, false);
                break;
            case "large":
                boule(w, top.down(1), 3, 1, feuille, r, monde, true);
                boule(w, top.up(1), 2, 0, feuille, r, monde, true);
                break;
            case "ovale":
                boule(w, top.down(3), 1, 0, feuille, r, monde, false);
                boule(w, top.down(1), 2, 1, feuille, r, monde, true);
                boule(w, top.up(1), 1, 1, feuille, r, monde, false);
                break;
            default:
                boule(w, top.down(1), 2, 1, feuille, r, monde, true);
                boule(w, top.up(1), 1, 0, feuille, r, monde, false);
        }
        return true;
    }

    /** Couches de feuilles de rayon « rayon » sur « demi » couches de part et d'autre de c (coins parfois absents). */
    private static void boule(World w, BlockPos c, int rayon, int demi, IBlockState f, Random r, boolean monde, boolean coins) {
        for (int dy = -demi; dy <= demi; dy++) {
            for (int dx = -rayon; dx <= rayon; dx++) {
                for (int dz = -rayon; dz <= rayon; dz++) {
                    boolean coin = Math.abs(dx) == rayon && Math.abs(dz) == rayon;
                    if (coin && (!coins || r.nextInt(2) == 0)) continue;
                    poser(w, c.add(dx, dy, dz), f, r, monde);
                }
            }
        }
    }

    private static void poser(World w, BlockPos p, IBlockState f, Random r, boolean monde) {
        IBlockState s = w.getBlockState(p);
        if (!s.getBlock().isAir(s, w, p) && !s.getBlock().isLeaves(s, w, p) && s.getMaterial() != Material.PLANTS && s.getMaterial() != Material.VINE) return;
        if (s.getBlock().isLeaves(s, w, p) && !(s.getBlock() instanceof BlockFeuillage)) return;
        int fruit = 0;
        if (BlockFeuillage.porteur(p)) fruit = monde ? r.nextInt(3) : r.nextInt(2);
        w.setBlockState(p, f.withProperty(BlockFeuillage.FRUIT, fruit), 2);
    }

    private static boolean libre(World w, BlockPos p) {
        IBlockState s = w.getBlockState(p);
        return s.getBlock().isAir(s, w, p) || s.getBlock().isLeaves(s, w, p) || s.getMaterial() == Material.PLANTS
                || s.getMaterial() == Material.VINE || s.getBlock().isReplaceable(w, p);
    }
}
