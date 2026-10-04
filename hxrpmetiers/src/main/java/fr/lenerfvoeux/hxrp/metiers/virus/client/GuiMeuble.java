package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.block.ContainerMeuble;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Meuble à tiroirs en bois laqué : 54 tiroirs et le tiroir des seringues stériles. */
@SideOnly(Side.CLIENT)
public class GuiMeuble extends GuiContainer {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/virus/meuble.png");
    private static final int ENCRE = 0xF4E2B8;

    public GuiMeuble(ContainerMeuble c) {
        super(c);
        xSize = 176;
        ySize = 246;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        super.drawScreen(mx, my, pt);
        renderHoveredToolTip(mx, my);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mx, int my) {
        fontRenderer.drawString(I18n.format("tile.hxrpmetiers.meuble_a_tiroirs.name"), 8, 6, ENCRE);
        fontRenderer.drawString("Seringues stériles", 31, ContainerMeuble.Y_SERINGUES + 1, 0x3A2418);
        fontRenderer.drawString("à volonté · reposez-y les vides", 31, ContainerMeuble.Y_SERINGUES + 10, 0x7A6450);
        fontRenderer.drawString(I18n.format("container.inventory"), 8, ContainerMeuble.Y_INVENTAIRE - 11, ENCRE);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float pt, int mx, int my) {
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEX);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
    }
}
