package fr.lenerfvoeux.hxrp.metiers.client.minijeu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * La toile du mini-jeu branchée sur une DynamicTexture : on écrit les pixels, on envoie la texture
 * d'un bloc à la carte graphique, on l'affiche à l'échelle entière du pixel art.
 */
public final class ToileGL {
    public final Toile toile;
    private final DynamicTexture tex;
    private final ResourceLocation rl;

    public ToileGL(String nom, int w, int h) {
        tex = new DynamicTexture(w, h);
        toile = new Toile(w, h, tex.getTextureData());
        rl = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(nom, tex);
    }

    public void envoyer() {
        tex.updateDynamicTexture();
    }

    public void dessiner(int x, int y, int echelle) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, toile.w, toile.h, toile.w * echelle, toile.h * echelle, toile.w, toile.h);
        GlStateManager.disableBlend();
    }

    public void liberer() {
        Minecraft.getMinecraft().getTextureManager().deleteTexture(rl);
    }
}
