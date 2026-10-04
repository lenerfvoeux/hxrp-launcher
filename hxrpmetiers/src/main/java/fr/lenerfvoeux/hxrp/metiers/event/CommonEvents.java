package fr.lenerfvoeux.hxrp.metiers.event;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.block.ContainerFrigo;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.item.IFoodItem;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class CommonEvents {
    public static final DamageSource SOIF = new DamageSource("hxrp_soif").setDamageBypassesArmor().setDamageIsAbsolute();

    private CommonEvents() {}

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof EntityPlayer) e.addCapability(Nutrition.KEY, new Nutrition.Provider());
    }

    @SubscribeEvent
    public static void clone(PlayerEvent.Clone e) {
        NutritionData old = Nutrition.get(e.getOriginal()), neu = Nutrition.get(e.getEntityPlayer());
        if (old == null || neu == null) return;
        if (e.isWasDeath()) neu.copyPersistent(old); // mort : faim et soif repartent pleines
        else neu.read(old.write());
    }

    @SubscribeEvent
    public static void login(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        if (e.player instanceof EntityPlayerMP) Network.sync((EntityPlayerMP) e.player);
    }

    @SubscribeEvent
    public static void logout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent e) {
        fr.lenerfvoeux.hxrp.metiers.cuisine.Seances.oublier(e.player.getUniqueID());
    }

    @SubscribeEvent
    public static void respawn(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent e) {
        if (e.player instanceof EntityPlayerMP) Network.sync((EntityPlayerMP) e.player);
    }

    @SubscribeEvent
    public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.player instanceof EntityPlayerMP) Network.sync((EntityPlayerMP) e.player);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || !(e.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP) e.player;
        NutritionData d = Nutrition.get(p);
        if (d == null) return;
        boolean exempt = p.capabilities.isCreativeMode || p.isSpectator();
        if (!exempt && p.isEntityAlive()) {
            d.faim = Math.max(0, d.faim - NutritionData.MAX / (ModConfig.dureeFaimMinutes * 1200.0));
            d.soif = Math.max(0, d.soif - NutritionData.MAX / (ModConfig.dureeSoifMinutes * 1200.0));
            if (d.soif <= 0) {
                p.attackEntityFrom(SOIF, Float.MAX_VALUE);
            }
        }
        // La barre vanilla (0-20) suit notre faim (0-60) : sprint, régénération et famine restent cohérents.
        int vanilla = (int) Math.ceil(d.faim / 3.0);
        if (p.getFoodStats().getFoodLevel() != vanilla) p.getFoodStats().setFoodLevel(Math.max(0, Math.min(20, vanilla)));

        if (p.ticksExisted % 20 == 0) {
            long now = System.currentTimeMillis();
            d.purge(now);
            scan(p, now);
        }
        if (d.dirty || p.ticksExisted % 40 == 0) Network.sync(p);
    }

    /**
     * Date les aliments neufs, ramène à l'heure normale ceux sortis du frigo,
     * et aligne un aliment neuf sur le plus vieux du même type déjà en poche pour qu'ils s'empilent.
     */
    private static void scan(EntityPlayer p, long now) {
        List<ItemStack> inv = new ArrayList<>();
        inv.addAll(p.inventory.mainInventory);
        inv.addAll(p.inventory.offHandInventory);
        for (ItemStack s : inv) if (s.getItem() instanceof IFoodItem) Fraicheur.fromFridge(s, now);

        Map<Item, Long> plusVieux = new HashMap<>();
        if (ModConfig.alignerSurStackExistant)
            for (ItemStack s : inv) {
                if (!(s.getItem() instanceof IFoodItem) || !Fraicheur.stamped(s)) continue;
                long exp = now + Fraicheur.remaining(s, now);
                if (exp > now) plusVieux.merge(s.getItem(), exp, Math::min);
            }

        for (ItemStack s : inv) {
            if (!(s.getItem() instanceof IFoodItem) || Fraicheur.stamped(s)) continue;
            Fraicheur.stamp(s, now);
            Long vieux = plusVieux.get(s.getItem());
            if (vieux != null && Fraicheur.stamped(s) && vieux < now + Fraicheur.remaining(s, now))
                Fraicheur.setExpiration(s, vieux, Fraicheur.totalHours(s));
        }

        if (p.openContainer != null && p.openContainer != p.inventoryContainer) {
            for (Slot sl : p.openContainer.inventorySlots) {
                if (sl instanceof ContainerFrigo.SlotFrigo) continue;
                ItemStack s = sl.getStack();
                if (s.getItem() instanceof IFoodItem) {
                    Fraicheur.fromFridge(s, now);
                    Fraicheur.stamp(s, now);
                }
            }
        }
    }

    @SubscribeEvent
    public static void itemSpawn(EntityJoinWorldEvent e) {
        if (e.getWorld().isRemote || !(e.getEntity() instanceof EntityItem)) return;
        ItemStack s = ((EntityItem) e.getEntity()).getItem();
        if (s.getItem() instanceof IFoodItem) {
            long now = System.currentTimeMillis();
            Fraicheur.fromFridge(s, now);
            Fraicheur.stamp(s, now);
        }
    }

    /** Nourritures vanilla (ou d'autres mods) : elles aussi ont un cooldown. */
    @SubscribeEvent
    public static void useStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntityLiving() instanceof EntityPlayer)) return;
        ItemStack s = e.getItem();
        if (!(s.getItem() instanceof ItemFood) || s.getItem() instanceof IFoodItem) return;
        EntityPlayer p = (EntityPlayer) e.getEntityLiving();
        String key = FoodEffects.key(s);
        long left;
        if (p.world.isRemote) left = HxrpMetiers.proxy.clientCooldownLeft(key);
        else {
            NutritionData d = Nutrition.get(p);
            left = d == null ? 0 : d.cooldownLeft(key, System.currentTimeMillis());
        }
        if (left > 0) {
            e.setCanceled(true);
            if (!p.world.isRemote) FoodEffects.cooldownMessage(p, false, left);
        }
    }

    @SubscribeEvent
    public static void useFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntityLiving() instanceof EntityPlayerMP)) return;
        ItemStack s = e.getItem();
        if (!(s.getItem() instanceof ItemFood) || s.getItem() instanceof IFoodItem) return;
        EntityPlayerMP p = (EntityPlayerMP) e.getEntityLiving();
        NutritionData d = Nutrition.get(p);
        if (d == null) return;
        d.addFaim(((ItemFood) s.getItem()).getHealAmount(s) * ModConfig.multiplicateurVanilla);
        d.setCooldown(FoodEffects.key(s), System.currentTimeMillis() + ModConfig.cooldownAutresHeures * Fraicheur.HOUR);
        Network.sync(p);
    }
}
