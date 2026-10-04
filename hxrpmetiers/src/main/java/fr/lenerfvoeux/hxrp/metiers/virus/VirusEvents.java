package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgDonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.SeancesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Blessures;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Maladies;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.MoteurSante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitements;
import net.minecraft.block.BlockBed;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Random;

/** Événements côté serveur du Hunter Virus : données de santé, dégâts, soins naturels, sommeil, repas. */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class VirusEvents {
    private static final Random RNG = new Random();

    private VirusEvents() {}

    @SubscribeEvent
    public static void attacher(AttachCapabilitiesEvent<Entity> e) {
        if (e.getObject() instanceof EntityPlayer) e.addCapability(Sante.KEY, new Sante.Provider());
    }

    /** Mourir efface tout (blessures, maladie, effets à vie, immunité, traitement) ; changer de dimension ne change rien. */
    @SubscribeEvent
    public static void cloner(PlayerEvent.Clone e) {
        SanteData old = Sante.get(e.getOriginal()), neu = Sante.get(e.getEntityPlayer());
        if (old == null || neu == null) return;
        if (e.isWasDeath()) neu.copierApresMort(old);
        else neu.read(old.write());
    }

    @SubscribeEvent
    public static void connexion(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.player instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP) e.player;
        Network.NET.sendTo(new MsgDonneesVirus(), p);
        SanteData d = Sante.get(p);
        if (d == null) return;
        long now = System.currentTimeMillis();
        // après une absence, le prochain symptôme ne tombe pas à la seconde où l'on se connecte
        if (d.prochainEpisode < now) d.prochainEpisode = now + 90_000 + RNG.nextInt(150_000);
        d.dirty = true;
        MoteurSante.synchroniser(p, d, now, true);
    }

    @SubscribeEvent
    public static void deconnexion(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent e) {
        SeancesVirus.oublier(e.player.getUniqueID());
    }

    @SubscribeEvent
    public static void reapparition(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerRespawnEvent e) {
        if (e.player instanceof EntityPlayerMP) forcerSync((EntityPlayerMP) e.player);
    }

    @SubscribeEvent
    public static void dimension(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.player instanceof EntityPlayerMP) forcerSync((EntityPlayerMP) e.player);
    }

    private static void forcerSync(EntityPlayerMP p) {
        SanteData d = Sante.get(p);
        if (d != null) MoteurSante.synchroniser(p, d, System.currentTimeMillis(), true);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.player.world.isRemote || !(e.player instanceof EntityPlayerMP)) return;
        MoteurSante.tick((EntityPlayerMP) e.player);
    }

    // ================================================================== combat et blessures
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blesse(LivingHurtEvent e) {
        if (e.isCanceled() || e.getEntityLiving().world.isRemote) return;
        Entity src = e.getSource().getTrueSource();
        if (src instanceof EntityPlayerMP && src != e.getEntityLiving()) {
            SanteData a = Sante.get((EntityPlayerMP) src);
            if (a != null) Traitements.combat((EntityPlayerMP) src, a);
        }
        if (!(e.getEntityLiving() instanceof EntityPlayerMP)) return;
        EntityPlayerMP p = (EntityPlayerMP) e.getEntityLiving();
        SanteData d = Sante.get(p);
        if (d == null || e.getSource() == MoteurSante.MAL) return;
        if (src instanceof EntityLivingBase && src != p) Traitements.combat(p, d);
        Blessures.degat(p, d, e.getSource(), e.getAmount());
    }

    @SubscribeEvent
    public static void attaque(LivingAttackEvent e) {
        if (e.getEntityLiving().world.isRemote) return;
        Entity src = e.getSource().getTrueSource();
        if (src instanceof EntityPlayerMP && src != e.getEntityLiving()) {
            SanteData a = Sante.get((EntityPlayerMP) src);
            if (a != null) a.dernierCombat = System.currentTimeMillis();
        }
    }

    /** Récupération lente : une régénération naturelle sur deux est perdue ; brûlure profonde : plus aucune. */
    @SubscribeEvent
    public static void soin(LivingHealEvent e) {
        if (!(e.getEntityLiving() instanceof EntityPlayerMP) || e.getAmount() > 1.0f) return;
        SanteData d = Sante.get((EntityPlayerMP) e.getEntityLiving());
        if (d == null) return;
        if (d.pasDeRegen || (d.recupLente && RNG.nextBoolean())) e.setCanceled(true);
    }

    // ================================================================== sommeil, minage, lait
    @SubscribeEvent
    public static void reveil(PlayerWakeUpEvent e) {
        if (!(e.getEntityPlayer() instanceof EntityPlayerMP)) return;
        SanteData d = Sante.get(e.getEntityPlayer());
        if (d != null) Traitements.sommeil((EntityPlayerMP) e.getEntityPlayer(), d);
    }

    /** De jour, on ne peut pas dormir : s'allonger dans un lit compte comme le repos demandé par un traitement. */
    @SubscribeEvent
    public static void lit(PlayerInteractEvent.RightClickBlock e) {
        if (e.getWorld().isRemote || !(e.getEntityPlayer() instanceof EntityPlayerMP)) return;
        if (!(e.getWorld().getBlockState(e.getPos()).getBlock() instanceof BlockBed)) return;
        EntityPlayerMP p = (EntityPlayerMP) e.getEntityPlayer();
        SanteData d = Sante.get(p);
        if (d == null || d.traitement == null || Traitements.estNuit(e.getWorld())) return;
        Traitements.sommeil(p, d);
        p.sendMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "Vous vous allongez un long moment pour vous reposer."));
    }

    @SubscribeEvent
    public static void minage(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof EntityPlayerMP)) return;
        SanteData d = Sante.get(e.getPlayer());
        if (d != null) Traitements.minage((EntityPlayerMP) e.getPlayer(), d, e.getPos());
    }

    @SubscribeEvent
    public static void boire(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntityLiving() instanceof EntityPlayerMP) || e.getItem().getItem() != Items.MILK_BUCKET) return;
        EntityPlayerMP p = (EntityPlayerMP) e.getEntityLiving();
        SanteData d = Sante.get(p);
        if (d == null || !d.lait) return;
        p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 15, 0));
        Maladies.message(p, "Rien qu'à l'odeur du lait, votre estomac se retourne.");
    }
}
