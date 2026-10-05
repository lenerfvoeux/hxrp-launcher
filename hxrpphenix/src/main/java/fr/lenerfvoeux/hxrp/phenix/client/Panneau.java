package fr.lenerfvoeux.hxrp.phenix.client;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.opengl.GL11;

/** Image tournée vers la caméra, à pleine lumière, avec un halo additif (boule de feu, plume enflammée). */
final class Panneau {
    private Panneau() {}

    /** Dessine l'image n° image (sur images) d'une bande verticale, de côté taille, tournée de angle autour de la vue. */
    static void dessiner(RenderManager rm, Entity e, double x, double y, double z, float taille, int image, int images, float angle, boolean halo) {
        GlStateManager.pushMatrix();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.enableRescaleNormal();
        GlStateManager.rotate(180.0F - rm.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((rm.options != null && rm.options.thirdPersonView == 2 ? -1 : 1) * -rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(angle, 0F, 0F, 1F);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);
        GlStateManager.disableLighting();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.05F);
        float v0 = (float) image / images, v1 = (float) (image + 1) / images;
        quad(taille, v0, v1, 1F);
        if (halo) {
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(false);
            quad(taille * 1.6F, v0, v1, 0.35F);
            GlStateManager.depthMask(true);
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableBlend();
        }
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableLighting();
        lumiereDuMonde(e);
        GlStateManager.disableRescaleNormal();
        GlStateManager.popMatrix();
    }

    /** Remet la lumière du lieu où se trouve l'entité (après un rendu à pleine lumière). */
    static void lumiereDuMonde(Entity e) {
        int i = e.world.getCombinedLight(new BlockPos(e.posX, e.posY + e.getEyeHeight(), e.posZ), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, (float) (i % 65536), (float) (i / 65536));
    }

    private static void quad(float t, float v0, float v1, float alpha) {
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder b = tes.getBuffer();
        float h = t / 2;
        GlStateManager.color(1F, 1F, 1F, alpha);
        b.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
        b.pos(-h, -h, 0).tex(0, v1).normal(0F, 1F, 0F).endVertex();
        b.pos(h, -h, 0).tex(1, v1).normal(0F, 1F, 0F).endVertex();
        b.pos(h, h, 0).tex(1, v0).normal(0F, 1F, 0F).endVertex();
        b.pos(-h, h, 0).tex(0, v0).normal(0F, 1F, 0F).endVertex();
        tes.draw();
        GlStateManager.color(1F, 1F, 1F, 1F);
    }
}
