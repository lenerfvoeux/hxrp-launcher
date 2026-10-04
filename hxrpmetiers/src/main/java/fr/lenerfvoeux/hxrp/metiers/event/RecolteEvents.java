package fr.lenerfvoeux.hxrp.metiers.event;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockCulture;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockFeuillage;
import fr.lenerfvoeux.hxrp.metiers.monde.Climat;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityRabbit;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Récolte liée aux animaux vanilla et au monde : viandes en plus des butins vanilla, traite des vaches,
 * œufs des poules, graines trouvées dans les hautes herbes, fruits qu'on fait tomber des arbres.
 */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class RecolteEvents {
    private static final String PONTE = "hxrp_ponte";

    private RecolteEvents() {}

    private static Item food(String id) {
        return ModRegistry.FOOD.get(id);
    }

    /** Viandes du Gourmet en plus des butins vanilla (veau pour les veaux). */
    @SubscribeEvent
    public static void butin(LivingDropsEvent e) {
        EntityLivingBase m = e.getEntityLiving();
        if (m.world.isRemote) return;
        Random r = m.getRNG();
        int bonus = e.getLootingLevel();
        String id = null;
        int n = 0;
        if (m instanceof EntityCow) {
            id = m.isChild() ? "veau" : "boeuf";
            n = m.isChild() ? 1 : 1 + r.nextInt(2 + bonus);
        } else if (m instanceof EntityPig && !m.isChild()) {
            id = "porc";
            n = 1 + r.nextInt(2 + bonus);
        } else if (m instanceof EntityChicken && !m.isChild()) {
            id = "poulet";
            n = 1;
        } else if (m instanceof EntitySheep && !m.isChild()) {
            id = "mouton";
            n = 1 + r.nextInt(2 + bonus);
        } else if (m instanceof EntityRabbit && !m.isChild()) {
            id = "lapin";
            n = 1;
        } else if (m instanceof EntitySquid) {
            id = "calamar";
            n = 1 + r.nextInt(2 + bonus);
        }
        Item it = id == null ? null : food(id);
        if (it == null || n <= 0) return;
        e.getDrops().add(new EntityItem(m.world, m.posX, m.posY + 0.3, m.posZ, new ItemStack(it, n)));
    }

    /** Traire une vache avec une fiole vide donne du lait. */
    @SubscribeEvent
    public static void traire(PlayerInteractEvent.EntityInteract e) {
        Entity t = e.getTarget();
        EntityPlayer p = e.getEntityPlayer();
        ItemStack tenu = p.getHeldItem(e.getHand());
        if (!(t instanceof EntityCow) || ((EntityCow) t).isChild() || tenu.getItem() != Items.GLASS_BOTTLE) return;
        e.setCanceled(true);
        e.setCancellationResult(EnumActionResult.SUCCESS);
        if (e.getWorld().isRemote) return;
        if (!p.capabilities.isCreativeMode) tenu.shrink(1);
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(food("lait")));
        e.getWorld().playSound(null, t.posX, t.posY, t.posZ, SoundEvents.ENTITY_COW_MILK, SoundCategory.NEUTRAL, 1.0f, 1.0f);
    }

    /** Les poules pondent des œufs du Gourmet de temps en temps. */
    @SubscribeEvent
    public static void ponte(LivingEvent.LivingUpdateEvent e) {
        if (!(e.getEntityLiving() instanceof EntityChicken)) return;
        EntityChicken c = (EntityChicken) e.getEntityLiving();
        if (c.world.isRemote || c.isChild() || c.ticksExisted % 20 != 0) return;
        NBTTagCompound d = c.getEntityData();
        int moyenne = ModConfig.pontePouleMinutes * 1200;
        if (!d.hasKey(PONTE)) d.setInteger(PONTE, moyenne / 2 + c.getRNG().nextInt(moyenne));
        int reste = d.getInteger(PONTE) - 20;
        if (reste <= 0) {
            c.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0f, (c.getRNG().nextFloat() - c.getRNG().nextFloat()) * 0.2f + 1.0f);
            c.entityDropItem(new ItemStack(food("oeuf")), 0);
            reste = moyenne / 2 + c.getRNG().nextInt(moyenne);
        }
        d.setInteger(PONTE, reste);
    }

    /** Casser des hautes herbes peut donner les graines d'une culture du climat local. */
    @SubscribeEvent
    public static void herbes(BlockEvent.HarvestDropsEvent e) {
        IBlockState s = e.getState();
        if (e.getWorld().isRemote || e.isSilkTouching() || (s.getBlock() != Blocks.TALLGRASS && s.getBlock() != Blocks.DOUBLE_PLANT)) return;
        Random r = e.getWorld().rand;
        if (r.nextInt(ModConfig.grainesDansLHerbe) != 0) return;
        String climat = Climat.de(e.getWorld().getBiome(e.getPos()));
        List<BlockCulture> ok = new ArrayList<>();
        for (BlockCulture c : ModRegistry.CULTURES.values()) if (c.def.climat.equals(climat)) ok.add(c);
        if (ok.isEmpty()) for (BlockCulture c : ModRegistry.CULTURES.values()) if (c.def.climat.equals(Climat.TEMPERE)) ok.add(c);
        if (ok.isEmpty()) return;
        e.getDrops().add(new ItemStack(ok.get(r.nextInt(ok.size())).graine()));
    }

    /** Taper sur des feuilles chargées de fruits mûrs fait tomber les fruits au lieu de casser le feuillage. */
    @SubscribeEvent
    public static void cueillir(PlayerInteractEvent.LeftClickBlock e) {
        IBlockState s = e.getWorld().getBlockState(e.getPos());
        if (!(s.getBlock() instanceof BlockFeuillage) || s.getValue(BlockFeuillage.FRUIT) != 2) return;
        if (e.getEntityPlayer().capabilities.isCreativeMode) return;
        ((BlockFeuillage) s.getBlock()).recolter(e.getWorld(), e.getPos(), s, e.getEntityPlayer());
        e.setCanceled(true);
        e.setUseBlock(Event.Result.DENY);
        e.setUseItem(Event.Result.DENY);
    }
}
