package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgSautVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Effet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Random;

/**
 * Les effets des maladies côté client, sur ordre du serveur. Aucun shader (conflits avec OptiFine sur ce modpack) :
 * surcouches 2D sous l'interface (teintes, vignettes, paupières, brouillage, silhouettes), mouvements de caméra,
 * et entrées modifiées (immobilité, gauche/droite inversées, saut et course limités).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID, value = Side.CLIENT)
public final class EffetsClient {
    private static final Random RNG = new Random();
    private static int sprintTicks;
    private static long prochainPas, prochainSifflement, prochaineSilhouette, prochainMurmure;
    private static long silhouetteDebut;
    private static boolean silhouetteGauche;
    private static float driftYaw, driftPitch;

    private EffetsClient() {}

    private static double f(Effet e) {
        return ClientVirus.force(e);
    }

    // ================================================================== surcouches
    @SubscribeEvent
    public static void surcouches(RenderGameOverlayEvent.Pre e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || ClientVirus.EFFETS.isEmpty()) return;
        ScaledResolution sr = e.getResolution();
        double w = sr.getScaledWidth_double(), h = sr.getScaledHeight_double();
        double t = System.currentTimeMillis() / 1000.0;
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        // daltonismes : sur toute l'image
        double k;
        if ((k = f(Effet.INVERSE)) > 0) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ZERO);
            aplat(0, 0, w, h, 1, 1, 1, 1);
            if (k < 1) {   // fondu : on repasse un voile gris le temps de l'entrée et de la sortie
                normal();
                aplat(0, 0, w, h, 0.5f, 0.5f, 0.5f, (float) (1 - k) * 0.6f);
            }
        }
        if ((k = f(Effet.GRIS)) > 0) {
            GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
            float m = (float) (1 - 0.18 * k);
            aplat(0, 0, w, h, m, m, m, 1);
            normal();
            aplat(0, 0, w, h, 0.52f, 0.52f, 0.5f, (float) (0.5 * k));
        }
        if ((k = f(Effet.AMBRE)) > 0) teinte(w, h, k, 1.0f, 0.74f, 0.32f);
        if ((k = f(Effet.BLEU)) > 0) teinte(w, h, k, 0.55f, 0.68f, 1.0f);
        normal();
        // sensibilité à la lumière : écran surexposé en plein jour, à ciel ouvert
        if ((k = f(Effet.LUMIERE)) > 0 && mc.world != null && mc.world.isDaytime() && mc.world.canSeeSky(new BlockPos(mc.player.posX, mc.player.posY + 1, mc.player.posZ))) {
            float a = (float) (k * (0.42 + 0.08 * Math.sin(t * 2.3)));
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            aplat(0, 0, w, h, 1f, 0.98f, 0.9f, a);
            normal();
            aplat(0, 0, w, h, 1f, 1f, 0.96f, a * 0.5f);
        }
        // vision floue : brouillage intermittent
        if ((k = f(Effet.FLOUE)) > 0) {
            double vague = Math.max(0, Math.sin(t * 1.7) * 0.6 + Math.sin(t * 4.1) * 0.4);
            float a = (float) (k * (0.15 + 0.45 * vague));
            texture(TexturesVirus.bruit(), false);
            double ox = (t * 37) % 1, oy = (t * 23) % 1;
            quad(0, 0, w, h, ox, oy, ox + w / 96, oy + h / 96, 1, 1, 1, a);
            aplat(0, 0, w, h, 0.92f, 0.92f, 0.9f, (float) (k * 0.28 * vague));
        }
        // fièvre : bords rouges qui battent
        if ((k = f(Effet.ROUGE)) > 0) {
            double battement = Math.pow(Math.max(0, Math.sin(t * Math.PI * 1.6)), 3);
            texture(TexturesVirus.vignette(), true);
            quad(0, 0, w, h, 0, 0, 1, 1, 0.85f, 0.05f, 0.03f, (float) (k * (0.45 + 0.4 * battement)));
        }
        // vision tunnel : bords noircis
        if ((k = f(Effet.TUNNEL)) > 0) {
            texture(TexturesVirus.vignette(), true);
            for (int i = 0; i < 2; i++) quad(-w * 0.05, -h * 0.05, w * 1.05, h * 1.05, 0, 0, 1, 1, 0, 0, 0, (float) (k * 0.95));
        }
        // paupières lourdes : elles tombent par vagues, comme des clignements
        if ((k = f(Effet.PAUPIERES)) > 0) {
            double cycle = (t % 6.5) / 6.5;
            double ferme = Math.pow(Math.max(0, Math.sin(cycle * Math.PI)), 6) * 0.95 + 0.12 * (0.5 + 0.5 * Math.sin(t * 0.9));
            double hp = h * 0.62 * ferme * k;
            texture(TexturesVirus.paupiere(), true);
            quad(0, -h * 0.25, w, hp, 0, 0, 1, 1, 1, 1, 1, (float) Math.min(1, k * 1.1));
            quad(0, h - hp, w, h * 1.25, 0, 1, 1, 0, 1, 1, 1, (float) Math.min(1, k * 1.1));
        }
        // hallucinations : une silhouette au coin de l'œil
        if (f(Effet.HALLU) > 0 && silhouetteDebut > 0) {
            double age = (System.currentTimeMillis() - silhouetteDebut) / 1000.0;
            if (age < 2.2) {
                double a = Math.min(1, age / 0.6) * Math.min(1, (2.2 - age) / 0.8);
                double sh = h * 0.55, sw = sh / 2;
                double x = silhouetteGauche ? w * 0.03 : w * 0.97 - sw;
                texture(TexturesVirus.silhouette(), true);
                quad(x, h * 0.3, x + sw, h * 0.3 + sh, 0, 0, 1, 1, 1, 1, 1, (float) (a * 0.75));
            } else silhouetteDebut = 0;
        }
        normal();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.popMatrix();
    }

    private static void teinte(double w, double h, double k, float r, float g, float b) {
        GlStateManager.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
        aplat(0, 0, w, h, (float) (1 + (r - 1) * k), (float) (1 + (g - 1) * k), (float) (1 + (b - 1) * k), 1);
    }

    private static void normal() {
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    private static void texture(ResourceLocation rl, boolean lisse) {
        GlStateManager.enableTexture2D();
        Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        int filtre = lisse ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filtre);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filtre);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GlStateManager.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
    }

    private static void aplat(double x0, double y0, double x1, double y1, float r, float g, float b, float a) {
        GlStateManager.disableTexture2D();
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bb.pos(x0, y1, 0).color(r, g, b, a).endVertex();
        bb.pos(x1, y1, 0).color(r, g, b, a).endVertex();
        bb.pos(x1, y0, 0).color(r, g, b, a).endVertex();
        bb.pos(x0, y0, 0).color(r, g, b, a).endVertex();
        t.draw();
        GlStateManager.enableTexture2D();
    }

    private static void quad(double x0, double y0, double x1, double y1, double u0, double v0, double u1, double v1, float r, float g, float b, float a) {
        Tessellator t = Tessellator.getInstance();
        BufferBuilder bb = t.getBuffer();
        bb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        bb.pos(x0, y1, 0).tex(u0, v1).color(r, g, b, a).endVertex();
        bb.pos(x1, y1, 0).tex(u1, v1).color(r, g, b, a).endVertex();
        bb.pos(x1, y0, 0).tex(u1, v0).color(r, g, b, a).endVertex();
        bb.pos(x0, y0, 0).tex(u0, v0).color(r, g, b, a).endVertex();
        t.draw();
    }

    // ================================================================== caméra
    @SubscribeEvent
    public static void camera(EntityViewRenderEvent.CameraSetup e) {
        if (ClientVirus.EFFETS.isEmpty()) return;
        double t = System.currentTimeMillis() / 1000.0;
        double k;
        if ((k = f(Effet.TREMBLE)) > 0) {
            e.setYaw((float) (e.getYaw() + k * 0.9 * (Math.sin(t * 37) + Math.sin(t * 23.3) * 0.6)));
            e.setPitch((float) (e.getPitch() + k * 0.7 * (Math.sin(t * 31.7) + Math.sin(t * 19.1) * 0.6)));
        }
        if ((k = f(Effet.VERTIGE)) > 0) {
            e.setRoll((float) (e.getRoll() + k * 11 * Math.sin(t * 1.3) + k * 3 * Math.sin(t * 3.1)));
            e.setYaw((float) (e.getYaw() + k * 4 * Math.sin(t * 0.9)));
        }
        double toux = Math.max(f(Effet.TOUX), f(Effet.ETERNUE));
        if (toux > 0) e.setPitch((float) (e.getPitch() + 6 * toux * Math.abs(Math.sin(t * 18))));
    }

    // ================================================================== à chaque tick
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null || mc.world == null) {
            if (e.phase == TickEvent.Phase.END) ClientVirus.vider();
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            MouvementVirus.installer(p, mc);
            return;
        }
        if (mc.isGamePaused()) return;
        long now = System.currentTimeMillis();
        // course limitée ou impossible
        boolean nosprint = ClientVirus.actif(Effet.NOSPRINT);
        int limite = ClientVirus.actif(Effet.SPRINT5) ? 100 : ClientVirus.actif(Effet.SPRINT8) ? 160 : -1;
        if (p.isSprinting()) sprintTicks++;
        else sprintTicks = Math.max(0, sprintTicks - 2);
        if (nosprint || (limite > 0 && sprintTicks > limite)) {
            if (p.isSprinting()) {
                p.setSprinting(false);
                if (!nosprint) p.sendStatusMessage(new net.minecraft.util.text.TextComponentString("§7§oVous êtes à bout de souffle."), true);
            }
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
        }
        // mains tremblantes : le viseur dérive doucement
        double k = f(Effet.MAINS);
        if (k > 0) {
            double t = now / 1000.0;
            // décalage borné qui suit une courbe lente : on n'ajoute que sa variation d'un tick à l'autre
            float dy = (float) (k * 2.2 * (Math.sin(t * 1.3) + 0.4 * Math.sin(t * 4.1)));
            float dp = (float) (k * 1.6 * (Math.cos(t * 1.1) + 0.4 * Math.sin(t * 3.7)));
            p.rotationYaw += dy - driftYaw;
            p.rotationPitch = Math.max(-90, Math.min(90, p.rotationPitch + dp - driftPitch));
            driftYaw = dy;
            driftPitch = dp;
        } else if (driftYaw != 0 || driftPitch != 0) {
            p.rotationYaw -= driftYaw;
            p.rotationPitch = Math.max(-90, Math.min(90, p.rotationPitch - driftPitch));
            driftYaw = 0;
            driftPitch = 0;
        }
        SoundHandler sh = mc.getSoundHandler();
        // acouphènes : sifflement
        if (ClientVirus.actif(Effet.ACOUPHENES) && now > prochainSifflement && VirusSons.SIFFLEMENT != null) {
            sh.playSound(PositionedSoundRecord.getMasterRecord(VirusSons.SIFFLEMENT, 1.0f));
            prochainSifflement = now + 3200;
        }
        // hallucinations : pas derrière soi, murmures, silhouettes, fumerolles
        if (ClientVirus.actif(Effet.HALLU)) {
            if (now > prochainPas) {
                double a = Math.toRadians(p.rotationYaw + 180 + (RNG.nextDouble() - 0.5) * 90);
                double d = 3 + RNG.nextDouble() * 3;
                double x = p.posX - Math.sin(a) * d, z = p.posZ + Math.cos(a) * d;
                SoundEvent pas = RNG.nextBoolean() ? SoundEvents.BLOCK_GRASS_STEP : SoundEvents.BLOCK_STONE_STEP;
                for (int i = 0; i < 1 + RNG.nextInt(3); i++)
                    sh.playDelayedSound(new PositionedSoundRecord(pas, SoundCategory.PLAYERS, 0.35f, 0.9f + RNG.nextFloat() * 0.2f, (float) x, (float) p.posY, (float) z), i * 7);
                prochainPas = now + 4000 + RNG.nextInt(6000);
            }
            if (now > prochainMurmure && VirusSons.MURMURE != null) {
                double a = RNG.nextDouble() * Math.PI * 2;
                sh.playSound(new PositionedSoundRecord(VirusSons.MURMURE, SoundCategory.AMBIENT, 0.5f, 0.8f + RNG.nextFloat() * 0.4f,
                        (float) (p.posX + Math.cos(a) * 4), (float) (p.posY + 1), (float) (p.posZ + Math.sin(a) * 4)));
                prochainMurmure = now + 9000 + RNG.nextInt(9000);
            }
            if (now > prochaineSilhouette && silhouetteDebut == 0) {
                silhouetteDebut = now;
                silhouetteGauche = RNG.nextBoolean();
                prochaineSilhouette = now + 7000 + RNG.nextInt(8000);
            }
            if (RNG.nextInt(6) == 0) {
                double a = RNG.nextDouble() * Math.PI * 2, d = 2 + RNG.nextDouble() * 4;
                mc.world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, p.posX + Math.cos(a) * d, p.posY + RNG.nextDouble() * 2, p.posZ + Math.sin(a) * d, 0, 0.01, 0);
            }
        }
    }

    // ================================================================== saut et sons
    @SubscribeEvent
    public static void saut(LivingEvent.LivingJumpEvent e) {
        Minecraft mc = Minecraft.getMinecraft();
        if (e.getEntityLiving() != mc.player) return;
        if (ClientVirus.actif(Effet.NOSAUT)) e.getEntityLiving().motionY = 0;
        else if (ClientVirus.actif(Effet.SAUT_REDUIT)) e.getEntityLiving().motionY *= 0.68;
        if (ClientVirus.actif(Effet.DEGATS_SAUT)) Network.NET.sendToServer(new MsgSautVirus());
    }

    @SubscribeEvent
    public static void son(PlaySoundEvent e) {
        ISound s = e.getResultSound();
        if (s == null || ClientVirus.EFFETS.isEmpty()) return;
        ResourceLocation rl = s.getSoundLocation();
        if (ClientVirus.actif(Effet.MUET) && rl.getPath().equals("entity.player.hurt")) {
            e.setResultSound(null);
            return;
        }
        if (ClientVirus.actif(Effet.ACOUPHENES) && !rl.getNamespace().equals(HxrpMetiers.MODID)) e.setResultSound(new Etouffe(s));
    }

    /** Un son étouffé (acouphènes) : même son, volume réduit. */
    private static final class Etouffe implements ISound {
        private final ISound s;

        Etouffe(ISound s) { this.s = s; }

        @Override public ResourceLocation getSoundLocation() { return s.getSoundLocation(); }
        @Override public SoundEventAccessor createAccessor(SoundHandler h) { return s.createAccessor(h); }
        @Override public Sound getSound() { return s.getSound(); }
        @Override public SoundCategory getCategory() { return s.getCategory(); }
        @Override public boolean canRepeat() { return s.canRepeat(); }
        @Override public int getRepeatDelay() { return s.getRepeatDelay(); }
        @Override public float getVolume() { return s.getVolume() * 0.25f; }
        @Override public float getPitch() { return s.getPitch() * 0.92f; }
        @Override public float getXPosF() { return s.getXPosF(); }
        @Override public float getYPosF() { return s.getYPosF(); }
        @Override public float getZPosF() { return s.getZPosF(); }
        @Override public AttenuationType getAttenuationType() { return s.getAttenuationType(); }
    }
}
