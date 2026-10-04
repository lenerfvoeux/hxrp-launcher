package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.block.ContainerFrigo;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiFrigo extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/frigo.png");

    public GuiFrigo(ContainerFrigo c) {
        super(c);
        xSize = 176;
        ySize = 168;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("container.hxrpmetiers.frigo"), 8, 5, 0x1E3A52);
        fontRenderer.drawString("x3 plus lent", xSize - 20 - fontRenderer.getStringWidth("x3 plus lent"), 5, 0x2A5A8A);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ySize - 93, 0x1E3A52);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEX);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
