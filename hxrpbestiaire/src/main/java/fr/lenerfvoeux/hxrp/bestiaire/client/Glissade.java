package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Sol gelé : le joueur local glisse comme sur de la glace. Au sol ordinaire (adhérence 0,6), Minecraft freine
 * l'élan de 0,546 par tick et accélère fort ; sur la glace (0,98), il le garde à 0,892 et accélère 4,4 fois moins.
 * On recalcule donc l'élan de la fin du tick à partir de celui du début : élan gardé comme sur la glace, poussée
 * des touches réduite d'autant. Quelques flocons de givre sous les pieds.
 */
@Mod.EventBusSubscriber(modid = HxrpBestiaire.MODID, value = Side.CLIENT)
public final class Glissade {
    private static final double SOL = 0.6 * 0.91, GLACE = 0.98 * 0.91;
    private static final double POUSSEE = Math.pow(SOL / GLACE, 3);
    private static double mx, mz;
    private static boolean suivi;

    private Glissade() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.side != net.minecraftforge.fml.relauncher.Side.CLIENT || e.player != Minecraft.getMinecraft().player) return;
        EntityPlayer p = e.player;
        if (Registre.GIVRE == null || !p.isPotionActive(Registre.GIVRE) || p.capabilities.isFlying || p.isInWater()) {
            suivi = false;
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            BlockPos dessous = new BlockPos(MathHelper.floor(p.posX), MathHelper.floor(p.getEntityBoundingBox().minY - 1.0), MathHelper.floor(p.posZ));
            float adh = p.world.getBlockState(dessous).getBlock().getSlipperiness(p.world.getBlockState(dessous), p.world, dessous, p);
            suivi = p.onGround && adh < 0.8F;
            mx = p.motionX;
            mz = p.motionZ;
            return;
        }
        if (!suivi || !p.onGround) return;
        // poussée des touches de ce tick, telle que Minecraft l'a appliquée sur sol ordinaire
        double px = p.motionX / SOL - mx, pz = p.motionZ / SOL - mz;
        p.motionX = (mx + px * POUSSEE) * GLACE;
        p.motionZ = (mz + pz * POUSSEE) * GLACE;
        if (p.ticksExisted % 3 == 0 && p.motionX * p.motionX + p.motionZ * p.motionZ > 0.01)
            p.world.spawnParticle(EnumParticleTypes.SNOW_SHOVEL, p.posX, p.posY + 0.05, p.posZ, -p.motionX * 0.5, 0.05, -p.motionZ * 0.5);
    }
}
