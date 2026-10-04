package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.Random;

/**
 * Récoltes du Virus hors des plantes : butins ajoutés aux animaux du Gourmet et vanilla (plumes, peaux, os…),
 * écorce prélevée à la hache sur les troncs, œuf cassé pour sa coquille.
 */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class RecolteVirus {
    private RecolteVirus() {}

    // ================================================================== animaux
    @SubscribeEvent
    public static void butin(LivingDropsEvent e) {
        EntityLivingBase m = e.getEntityLiving();
        if (m.world.isRemote || m instanceof EntityPlayer) return;
        if (m instanceof EntityAgeable && ((EntityAgeable) m).isChild()) return;
        ResourceLocation rl = EntityList.getKey(m);
        if (rl == null) return;
        String id = rl.getNamespace().equals("minecraft") ? rl.getPath() : rl.getNamespace().equals(HxrpMetiers.MODID) ? rl.getPath() : "";
        if (id.isEmpty()) return;
        Random r = m.world.rand;
        int pillage = e.getLootingLevel();
        boolean animal = true;
        switch (id) {
            case "chicken": donner(e, "plume", 60, 1, 2, pillage, r); break;
            case "canard": donner(e, "plume", 60, 1, 2, pillage, r); donner(e, "duvet", 50, 1, 1, pillage, r); break;
            case "dinde": donner(e, "plume", 70, 1, 3, pillage, r); break;
            case "sheep": donner(e, "laine", 50, 1, 1, pillage, r); break;
            case "pig": donner(e, "saindoux", 50, 1, 1, pillage, r); break;
            case "cow": donner(e, "peau_de_boeuf", 50, 1, 1, pillage, r); break;
            case "cerf": donner(e, "peau_de_cerf", 60, 1, 1, pillage, r); donner(e, "bois_de_cerf", 35, 1, 1, pillage, r); break;
            case "sanglier": donner(e, "defense_de_sanglier", 40, 1, 1, pillage, r); break;
            case "crabe": donner(e, "carapace_de_crabe", 60, 1, 1, pillage, r); break;
            case "squid": donner(e, "encre_de_calamar", 60, 1, 2, pillage, r); break;
            case "saumon": case "truite": case "thon": case "cabillaud": case "sardine": case "maquereau": case "anchois":
                donner(e, "ecailles", 60, 1, 2, pillage, r); break;
            case "chevre": case "rabbit": break;
            default: animal = false;
        }
        if (animal) donner(e, "os", 30, 1, 1, pillage, r);
    }

    private static void donner(LivingDropsEvent e, String id, int pourcent, int min, int max, int pillage, Random r) {
        Item i = Objets.item(id);
        if (i == null || r.nextInt(100) >= Math.min(100, pourcent + 10 * pillage)) return;
        int n = min + r.nextInt(Math.max(1, max - min + 1)) + (pillage > 0 ? r.nextInt(pillage + 1) : 0);
        Entity m = e.getEntity();
        e.getDrops().add(new EntityItem(m.world, m.posX, m.posY, m.posZ, new ItemStack(i, n)));
    }

    // ================================================================== écorce, œuf
    /** Clic droit à la hache sur un tronc de bouleau, ou de chêne au bord de l'eau (saule) : écorce. */
    @SubscribeEvent
    public static void ecorce(PlayerInteractEvent.RightClickBlock e) {
        ItemStack hache = e.getItemStack();
        if (!(hache.getItem() instanceof ItemAxe)) return;
        World w = e.getWorld();
        BlockPos pos = e.getPos();
        IBlockState s = w.getBlockState(pos);
        if (!(s.getBlock() instanceof BlockLog) || s.getBlock() instanceof BlockTroncEcorce) return;
        String v = Cueillette.variante(s);
        boolean bouleau = "birch".equals(v);
        boolean saule = "oak".equals(v) && presDeLEau(w, pos);
        if (!bouleau && !saule) return;
        BlockTroncEcorce nu = bouleau ? Cueillette.ECORCE_BOULEAU : Cueillette.ECORCE_SAULE;
        Item ecorce = Objets.item(bouleau ? "ecorce_de_bouleau" : "ecorce_de_saule");
        if (nu == null || ecorce == null) return;
        e.setCanceled(true);
        e.getEntityPlayer().swingArm(e.getHand());
        if (w.isRemote) return;
        w.setBlockState(pos, nu.getDefaultState().withProperty(BlockLog.LOG_AXIS, s.getValue(BlockLog.LOG_AXIS)), 3);
        ItemHandlerHelper.giveItemToPlayer(e.getEntityPlayer(), new ItemStack(ecorce, 1 + w.rand.nextInt(2)));
        hache.damageItem(1, e.getEntityPlayer());
        w.playSound(null, pos, SoundEvents.BLOCK_WOOD_HIT, SoundCategory.BLOCKS, 1f, 0.8f);
    }

    /** Un chêne de marais ou de rivière compte pour un saule. */
    private static boolean presDeLEau(World w, BlockPos pos) {
        net.minecraft.world.biome.Biome b = w.getBiome(pos);
        return BiomeDictionary.hasType(b, BiomeDictionary.Type.SWAMP) || BiomeDictionary.hasType(b, BiomeDictionary.Type.RIVER);
    }

    /** Accroupi + clic droit avec un œuf du Gourmet : on le casse pour garder la coquille. */
    @SubscribeEvent
    public static void oeuf(PlayerInteractEvent.RightClickItem e) {
        EntityPlayer p = e.getEntityPlayer();
        ItemStack s = e.getItemStack();
        if (!p.isSneaking() || s.isEmpty() || s.getItem() != ModRegistry.FOOD.get("oeuf")) return;
        Item coquille = Objets.item("coquille_d_oeuf");
        if (coquille == null) return;
        e.setCanceled(true);
        if (p.world.isRemote) return;
        s.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(coquille));
        p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
    }
}
