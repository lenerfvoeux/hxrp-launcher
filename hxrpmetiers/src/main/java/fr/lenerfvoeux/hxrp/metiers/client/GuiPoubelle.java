package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.ContainerPoubelle;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiPoubelle extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/poubelle.png");

    public GuiPoubelle(ContainerPoubelle c) {
        super(c);
        xSize = 176;
        ySize = 132;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("container.hxrpmetiers.poubelle"), 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 94, 0x404040);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEX);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
