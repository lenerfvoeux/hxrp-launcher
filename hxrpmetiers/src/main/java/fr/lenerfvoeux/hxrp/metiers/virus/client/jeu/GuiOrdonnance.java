package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Ordonnances;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/** L'ordonnance remise au patient : une feuille à en-tête de l'officine, les prises et la consigne. */
@SideOnly(Side.CLIENT)
public class GuiOrdonnance extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/virus/ordonnance.png");
    private static final int W = 200, H = 240;
    private static final int ENCRE = 0x23304A, ENCRE_CLAIRE = 0x6A7488, ROUGE = 0x9A2A1E;

    private final NBTTagCompound t;
    private int x0, y0;

    public GuiOrdonnance(ItemStack s) {
        this.t = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x0, y0, 0, 0, W, H, 256, 256);
        int x = x0 + 16, y = y0 + 40, w = W - 32;
        centre("ORDONNANCE", y0 + 16, ROUGE);
        fontRenderer.drawString("Patient : " + t.getString(Ordonnances.PATIENT), x, y, ENCRE);
        y += 11;
        fontRenderer.drawString(fontRenderer.trimStringToWidth("Maladie : " + t.getString(Ordonnances.NOM), w), x, y, ENCRE);
        y += 11;
        fontRenderer.drawString("Le " + Diagnostic.date(t.getLong(Ordonnances.DATE)), x, y, ENCRE_CLAIRE);
        y += 16;
        NBTTagList l = t.getTagList(Ordonnances.PRISES, 8);
        for (int i = 0; i < l.tagCount(); i++) {
            List<String> lignes = fontRenderer.listFormattedStringToWidth("• " + l.getStringTagAt(i), w);
            for (String s : lignes) {
                if (y > y0 + H - 50) break;
                fontRenderer.drawString(s, x, y, ENCRE);
                y += 10;
            }
            y += 2;
        }
        String c = t.getString(Ordonnances.CONSIGNE);
        if (!c.isEmpty()) {
            y += 4;
            for (String s : fontRenderer.listFormattedStringToWidth("Consigne : " + c, w)) {
                if (y > y0 + H - 36) break;
                fontRenderer.drawString(s, x, y, ROUGE);
                y += 10;
            }
        }
        String sig = t.getString(Ordonnances.VIRUS);
        fontRenderer.drawString("Dr " + sig, x0 + W - 18 - fontRenderer.getStringWidth("Dr " + sig), y0 + H - 26, ENCRE_CLAIRE);
        super.drawScreen(mx, my, pt);
    }

    private void centre(String s, int y, int c) {
        fontRenderer.drawString(s, x0 + W / 2 - fontRenderer.getStringWidth(s) / 2, y, c);
    }
}
