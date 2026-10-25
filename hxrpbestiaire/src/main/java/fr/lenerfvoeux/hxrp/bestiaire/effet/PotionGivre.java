package fr.lenerfvoeux.hxrp.bestiaire.effet;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * « Sol gelé » (Rugissement glacial du Roi des glaces) : tant qu'il dure, le joueur glisse sur le sol comme sur de la
 * glace (le client recalcule son élan, voir client.Glissade). Icône propre, dessinée par le mod.
 */
public class PotionGivre extends Potion {
    private static final ResourceLocation ICONE = new ResourceLocation(HxrpBestiaire.MODID, "textures/gui/givre.png");

    public PotionGivre() {
        super(true, 0x9ED3F0);
        setPotionName("effect." + HxrpBestiaire.MODID + ".givre");
        setRegistryName(HxrpBestiaire.MODID, "givre");
    }

    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, Gui gui, int x, int y, float z) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONE);
        Gui.drawModalRectWithCustomSizedTexture(x + 6, y + 7, 0, 0, 18, 18, 18, 18);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, Gui gui, int x, int y, float z, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(ICONE);
        net.minecraft.client.renderer.GlStateManager.color(1F, 1F, 1F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x + 3, y + 3, 0, 0, 18, 18, 18, 18);
    }
}
