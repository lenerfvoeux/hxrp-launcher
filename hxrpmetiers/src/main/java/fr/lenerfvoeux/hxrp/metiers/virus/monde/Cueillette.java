package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Les règles de la récolte du Virus : sur quel sol pousse chaque plante, sur quel bois chaque champignon,
 * quand on peut cueillir (nuit, aube), combien on récolte et en combien de temps le bloc se recharge.
 * Les ingrédients ne se replantent pas (v0.7) : ils ne viennent que des blocs du monde.
 */
public final class Cueillette {
    /** Blocs de plante et de tronc par identifiant d'ingrédient (remplis à l'enregistrement). */
    public static final Map<String, BlockPlanteVirus> PLANTES = new LinkedHashMap<>();
    public static final Map<String, BlockChampiTronc> TRONCS = new LinkedHashMap<>();
    public static final Map<String, BlockSolVirus> SOLS = new LinkedHashMap<>();
    public static BlockTroncEcorce ECORCE_BOULEAU, ECORCE_SAULE;
    public static BlockLotus LOTUS;
    public static BlockSourceSeve SEVE;

    private Cueillette() {}

    // ================================================================== rareté
    public static int niveau(Defs.Ingredient d) {
        String r = d.rarete == null ? "" : d.rarete;
        if (r.startsWith("légendaire")) return 4;
        if (r.startsWith("très rare")) return 3;
        if (r.startsWith("rare")) return 2;
        if (r.startsWith("peu")) return 1;
        return 0;
    }

    public static boolean commune(Defs.Ingredient d) {
        return niveau(d) <= 1;
    }

    /** Durée (ms) de la recharge d'un bloc récolté, selon la rareté de son ingrédient. */
    public static long duree(Defs.Ingredient d) {
        int[] m = VirusConfig.rechargeMinutes;
        int min = m == null || m.length == 0 ? 60 : m[Math.min(m.length - 1, niveau(d))];
        return Math.max(1, min) * Recharge.MINUTE;
    }

    public static int quantite(Defs.Ingredient d, Random r) {
        return niveau(d) <= 1 ? 1 + r.nextInt(2) : 1;
    }

    /** Raison pour laquelle on ne peut pas cueillir maintenant (fleur de nuit, pollen de l'aube), ou null. */
    public static String refusCueillette(Defs.Ingredient d, World w) {
        long t = w.getWorldTime() % 24000;
        if (d.recolte.nuit && (t < 13000 || t >= 23000)) return "La fleur reste close : elle ne s'ouvre que la nuit.";
        if (d.recolte.aube && !(t >= 22500 || t < 1500)) return "Le pollen ne se détache qu'à l'aube.";
        return null;
    }

    // ================================================================== sols et bois
    public static boolean solOk(Defs.Ingredient d, IBlockState s) {
        Block b = s.getBlock();
        String sol = d.recolte.sol == null ? "herbe" : d.recolte.sol;
        boolean terre = b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.FARMLAND || b == Blocks.MYCELIUM;
        boolean pierre = b == Blocks.STONE || b == Blocks.COBBLESTONE || b == Blocks.MOSSY_COBBLESTONE || b == Blocks.GRAVEL;
        switch (sol) {
            case "sable": return b == Blocks.SAND || b == Blocks.HARDENED_CLAY || b == Blocks.STAINED_HARDENED_CLAY || (d.recolte.arbuste && terre);
            case "neige": return b == Blocks.SNOW || b == Blocks.GRASS || b == Blocks.DIRT || b == Blocks.PACKED_ICE;
            case "pierre": return pierre || b == Blocks.DIRT;
            case "montagne": return terre || pierre || b == Blocks.SNOW;
            case "foret": return terre;
            default: return terre && b != Blocks.MYCELIUM || (d.recolte.champignon && b == Blocks.MYCELIUM);
        }
    }

    /** Le bloc est-il un tronc du bois demandé (chene, bouleau, chene_noir, tous) ? */
    public static boolean boisOk(String bois, IBlockState s) {
        Block b = s.getBlock();
        if (!(b instanceof BlockLog)) return false;
        String v = variante(s);
        switch (bois == null ? "tous" : bois) {
            case "chene": return "oak".equals(v);
            case "bouleau": return "birch".equals(v);
            case "chene_noir": return "dark_oak".equals(v);
            default: return true;
        }
    }

    /** oak, birch, dark_oak… ou "" pour un tronc d'un autre mod. Nos troncs écorcés comptent pour leur bois. */
    public static String variante(IBlockState s) {
        Block b = s.getBlock();
        if (b instanceof BlockTroncEcorce) return ((BlockTroncEcorce) b).bouleau ? "birch" : "oak";
        try {
            if (b instanceof BlockOldLog) return s.getValue(BlockOldLog.VARIANT).getName();
            if (b instanceof BlockNewLog) return s.getValue(BlockNewLog.VARIANT).getName();
        } catch (IllegalArgumentException ignored) {
            // bloc d'un autre mod dérivé de BlockLog sans ces propriétés
        }
        return "";
    }

    public static boolean pierre(IBlockState s) {
        return s.getBlock() == Blocks.STONE && s.getValue(BlockStone.VARIANT) == BlockStone.EnumType.STONE;
    }

    /** Le bloc posé par la génération est-il de la bonne matière pour une plante (pas d'eau, pas de feuilles) ? */
    static boolean libre(World w, BlockPos pos) {
        IBlockState s = w.getBlockState(pos);
        return s.getBlock().isReplaceable(w, pos) && s.getMaterial() != Material.WATER && s.getMaterial() != Material.LAVA;
    }
}
