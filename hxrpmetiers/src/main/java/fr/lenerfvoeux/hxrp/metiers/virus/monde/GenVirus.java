package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
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
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.Set;

/**
 * Génération des plantes, champignons et raretés du Virus dans le monde principal : selon le milieu (biome),
 * la rareté et le moment. Tout est décalé de 8 blocs dans le chunk pour ne jamais déborder sur un chunk pas encore
 * généré (pas de génération en cascade).
 */
public class GenVirus implements IWorldGenerator {
    /** Chance par chunk d'une touffe, selon la rareté : commune, peu commune, rare, très rare, légendaire. */
    private static final double[] TOUFFE = {1 / 6.0, 1 / 12.0, 1 / 25.0, 1 / 50.0, 1 / 120.0};
    private static final int[] TAILLE = {5, 4, 2, 1, 1};
    private WorldGenMinable ferSang;

    public static void enregistrer() {
        GameRegistry.registerWorldGenerator(new GenVirus(), 20);
    }

    @Override
    public void generate(java.util.Random r, int cx, int cz, World w, IChunkGenerator gen, IChunkProvider prov) {
        if (w.provider.getDimension() != 0 || VirusConfig.generationPlantes <= 0) return;
        int x0 = cx * 16 + 8, z0 = cz * 16 + 8;
        BlockPos centre = w.getHeight(new BlockPos(x0 + 8, 0, z0 + 8));
        Biome b = w.getBiome(centre);
        Set<String> milieux = MilieuVirus.de(b, centre);
        for (BlockPlanteVirus p : Cueillette.PLANTES.values()) plantes(w, r, x0, z0, milieux, p);
        for (BlockChampiTronc t : Cueillette.TRONCS.values()) troncs(w, r, x0, z0, milieux, t);
        sols(w, r, x0, z0, milieux);
        if (milieux.contains("montagne") && chance(r, 1 / 10.0)) nid(w, r, x0, z0);
        if (milieux.contains("riviere") && chance(r, 1 / 30.0)) lotus(w, r, x0, z0);
    }

    private static boolean chance(java.util.Random r, double c) {
        return r.nextDouble() < c * VirusConfig.generationPlantes;
    }

    private static boolean dans(Set<String> milieux, Defs.Ingredient d) {
        for (String m : d.recolte.milieux) if (milieux.contains(m)) return true;
        return false;
    }

    private static int ageAuHasard(java.util.Random r) {
        int k = r.nextInt(10);
        return k < 6 ? 2 : k < 8 ? 1 : 0;
    }

    // ================================================================== plantes et champignons au sol
    private void plantes(World w, java.util.Random r, int x0, int z0, Set<String> milieux, BlockPlanteVirus p) {
        Defs.Ingredient d = p.def;
        int niv = Cueillette.niveau(d);
        if (d.recolte.milieux.contains("grotte_profonde")) {
            if (chance(r, TOUFFE[niv] * 2)) grotte(w, r, x0, z0, p);
            return;
        }
        if (!dans(milieux, d) || !chance(r, TOUFFE[niv])) return;
        int cx = x0 + 3 + r.nextInt(10), cz = z0 + 3 + r.nextInt(10);
        int n = 1 + r.nextInt(TAILLE[niv]);
        for (int k = 0, poses = 0; k < n * 4 && poses < n; k++) {
            BlockPos pos = w.getTopSolidOrLiquidBlock(new BlockPos(cx + r.nextInt(7) - 3, 0, cz + r.nextInt(7) - 3));
            if (!place(w, pos, d)) continue;
            w.setBlockState(pos, p.getDefaultState().withProperty(BlockPlanteVirus.AGE, ageAuHasard(r)), 2);
            poses++;
        }
    }

    private boolean place(World w, BlockPos pos, Defs.Ingredient d) {
        if (!Cueillette.libre(w, pos) || !Cueillette.solOk(d, w.getBlockState(pos.down()))) return false;
        if (w.getBlockState(pos).getMaterial() == Material.SNOW && !"neige".equals(d.recolte.sol)) return false;
        if (d.recolte.ymin > 0 && pos.getY() < d.recolte.ymin) return false;
        if (d.recolte.ombre && w.canSeeSky(pos)) return false;
        if (d.recolte.presChene && !presDUnChene(w, pos)) return false;
        return true;
    }

    private static boolean presDUnChene(World w, BlockPos pos) {
        for (BlockPos q : BlockPos.getAllInBoxMutable(pos.add(-3, 0, -3), pos.add(3, 2, 3))) {
            String v = Cueillette.variante(w.getBlockState(q));
            if ("oak".equals(v) || "dark_oak".equals(v)) return true;
        }
        return false;
    }

    /** Champignon luminescent : sur le sol des grottes profondes, dans le noir. */
    private void grotte(World w, java.util.Random r, int x0, int z0, BlockPlanteVirus p) {
        for (int k = 0; k < 24; k++) {
            BlockPos pos = new BlockPos(x0 + r.nextInt(16), 6 + r.nextInt(24), z0 + r.nextInt(16));
            if (!w.isAirBlock(pos) || w.canSeeSky(pos) || !Cueillette.solOk(p.def, w.getBlockState(pos.down()))) continue;
            w.setBlockState(pos, p.getDefaultState().withProperty(BlockPlanteVirus.AGE, ageAuHasard(r)), 2);
            if (r.nextBoolean()) return;
        }
    }

    // ================================================================== champignons de tronc
    private void troncs(World w, java.util.Random r, int x0, int z0, Set<String> milieux, BlockChampiTronc t) {
        Defs.Ingredient d = t.def;
        if (!dans(milieux, d) || !chance(r, TOUFFE[Cueillette.niveau(d)] * 1.5)) return;
        int poses = 0, voulu = 1 + r.nextInt(Math.max(1, TAILLE[Cueillette.niveau(d)] - 1));
        for (int k = 0; k < 20 && poses < voulu; k++) {
            BlockPos top = w.getHeight(new BlockPos(x0 + r.nextInt(16), 0, z0 + r.nextInt(16)));
            for (int dy = 1; dy < 16; dy++) {
                BlockPos log = top.down(dy);
                IBlockState s = w.getBlockState(log);
                if (!(s.getBlock() instanceof BlockLog)) continue;
                if (!Cueillette.boisOk(d.recolte.bois, s)) break;
                EnumFacing f = EnumFacing.Plane.HORIZONTAL.random(r);
                BlockPos ici = log.offset(f);
                if (w.isAirBlock(ici) && ici.getY() - w.getTopSolidOrLiquidBlock(ici).getY() < 4) {
                    w.setBlockState(ici, t.getDefaultState().withProperty(BlockChampiTronc.FACING, f.getOpposite())
                            .withProperty(BlockChampiTronc.AGE, ageAuHasard(r)), 2);
                    poses++;
                }
                break;
            }
        }
    }

    // ================================================================== sols
    private void sols(World w, java.util.Random r, int x0, int z0, Set<String> milieux) {
        BlockSolVirus truffe = Cueillette.SOLS.get("terre_truffiere"), mousse = Cueillette.SOLS.get("pierre_moussue_humide"),
                vase = Cueillette.SOLS.get("vase_a_algue_noire"), fer = Cueillette.SOLS.get("roche_a_fer_sang"),
                abysses = Cueillette.SOLS.get("roche_des_abysses");
        if (truffe != null && (milieux.contains("foret") || milieux.contains("foret_sombre")) && chance(r, 1 / 6.0)) truffes(w, r, x0, z0, truffe);
        if (mousse != null && chance(r, 1 / 2.0)) paroi(w, r, x0, z0, mousse, 20, 60, 3);
        if (abysses != null && chance(r, 1 / 3.0)) paroi(w, r, x0, z0, abysses, 3, 12, 2);
        if (vase != null && milieux.contains("marais") && chance(r, 1 / 3.0)) vase(w, r, x0, z0, vase);
        if (fer != null && chance(r, 1 / 2.0)) {
            if (ferSang == null) ferSang = new WorldGenMinable(fer.getDefaultState(), 4);
            ferSang.generate(w, r, new BlockPos(x0 + r.nextInt(16), 5 + r.nextInt(15), z0 + r.nextInt(16)));
        }
    }

    /** Truffes : un ou deux blocs de terre truffière sous l'herbe, au pied d'un chêne. */
    private void truffes(World w, java.util.Random r, int x0, int z0, BlockSolVirus truffe) {
        for (int k = 0; k < 16; k++) {
            BlockPos sol = w.getTopSolidOrLiquidBlock(new BlockPos(x0 + 2 + r.nextInt(12), 0, z0 + 2 + r.nextInt(12))).down();
            if (w.getBlockState(sol).getBlock() != Blocks.GRASS || !presDUnChene(w, sol.up())) continue;
            for (int i = 0; i < 1 + r.nextInt(2); i++) {
                BlockPos t = sol.down(1 + r.nextInt(2)).add(r.nextInt(3) - 1, 0, r.nextInt(3) - 1);
                if (Cueillette.terre(w.getBlockState(t))) w.setBlockState(t, truffe.getDefaultState(), 2);
            }
            return;
        }
    }

    /** Roche apparente des grottes (paroi touchant l'air), entre deux altitudes. */
    private void paroi(World w, java.util.Random r, int x0, int z0, BlockSolVirus bloc, int y0, int y1, int n) {
        int poses = 0;
        for (int k = 0; k < 40 && poses < n; k++) {
            BlockPos pos = new BlockPos(x0 + r.nextInt(16), y0 + r.nextInt(Math.max(1, y1 - y0)), z0 + r.nextInt(16));
            if (!Cueillette.pierre(w.getBlockState(pos))) continue;
            boolean air = false;
            for (EnumFacing f : EnumFacing.values()) if (w.isAirBlock(pos.offset(f))) air = true;
            if (!air || w.canSeeSky(pos.up())) continue;
            w.setBlockState(pos, bloc.getDefaultState(), 2);
            poses++;
        }
    }

    /** Vase à algue noire : le fond des mares du marais. */
    private void vase(World w, java.util.Random r, int x0, int z0, BlockSolVirus vase) {
        int poses = 0;
        for (int k = 0; k < 30 && poses < 5; k++) {
            BlockPos top = w.getHeight(new BlockPos(x0 + r.nextInt(16), 0, z0 + r.nextInt(16)));
            BlockPos fond = top.down();
            int prof = 0;
            while (prof < 4 && w.getBlockState(fond).getMaterial() == Material.WATER) {
                fond = fond.down();
                prof++;
            }
            if (prof == 0) continue;
            IBlockState s = w.getBlockState(fond);
            if (s.getBlock() == Blocks.DIRT || s.getBlock() == Blocks.CLAY || s.getBlock() == Blocks.SAND || s.getBlock() == Blocks.GRASS) {
                w.setBlockState(fond, vase.getDefaultState(), 2);
                poses++;
            }
        }
    }

    // ================================================================== nid, lotus
    /** Nid d'aigle-araignée : au bord d'une falaise, en hauteur, au-dessus d'un à-pic. */
    private void nid(World w, java.util.Random r, int x0, int z0) {
        if (Cueillette.NID == null) return;
        for (int k = 0; k < 20; k++) {
            BlockPos pos = w.getTopSolidOrLiquidBlock(new BlockPos(x0 + 2 + r.nextInt(12), 0, z0 + 2 + r.nextInt(12)));
            if (pos.getY() < 85 || !w.isAirBlock(pos) || !w.getBlockState(pos.down()).isSideSolid(w, pos.down(), EnumFacing.UP)) continue;
            for (EnumFacing f : EnumFacing.Plane.HORIZONTAL) {
                BlockPos bord = pos.offset(f, 2);
                if (pos.getY() - w.getTopSolidOrLiquidBlock(bord).getY() >= 7) {
                    w.setBlockState(pos, Cueillette.NID.getDefaultState().withProperty(BlockNid.OEUFS, 1 + r.nextInt(2)), 2);
                    return;
                }
            }
        }
    }

    /** Lotus de l'aube : une fleur, sur l'eau calme d'une rivière. */
    private void lotus(World w, java.util.Random r, int x0, int z0) {
        if (Cueillette.LOTUS == null) return;
        for (int k = 0; k < 12; k++) {
            BlockPos pos = w.getHeight(new BlockPos(x0 + 2 + r.nextInt(12), 0, z0 + 2 + r.nextInt(12)));
            IBlockState eau = w.getBlockState(pos.down());
            if (!w.isAirBlock(pos) || eau.getMaterial() != Material.WATER || eau.getBlock().getMetaFromState(eau) != 0) continue;
            w.setBlockState(pos, Cueillette.LOTUS.getDefaultState(), 2);
            return;
        }
    }
}
