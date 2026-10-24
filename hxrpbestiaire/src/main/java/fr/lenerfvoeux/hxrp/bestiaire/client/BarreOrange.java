package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BossInfoClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.BossInfo;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * Barre de boss orange de la 2e phase : Minecraft n'a pas d'orange, le serveur envoie donc du jaune (que les boss
 * vanilla n'utilisent pas) et le client la repeint ici : barre blanche de bars.png teintée, crans par-dessus, nom.
 * Si le mod hxrpphenix fait de même, le premier qui dessine annule l'événement et l'autre ne la redessine pas.
 */
@Mod.EventBusSubscriber(modid = HxrpBestiaire.MODID, value = Side.CLIENT)
public final class BarreOrange {
    private static final ResourceLocation BARRES = new ResourceLocation("textures/gui/bars.png");
    private static final float R = 1.0F, V = 0.52F, B = 0.08F, FOND = 0.55F;

    private BarreOrange() {}

    @SubscribeEvent
    public static void barre(RenderGameOverlayEvent.BossInfo e) {
        BossInfoClient info = e.getBossInfo();
        if (info.getColor() != BossInfo.Color.YELLOW) return;
        e.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        Gui g = mc.ingameGUI;
        int x = e.getX(), y = e.getY();
        int blanc = BossInfo.Color.WHITE.ordinal() * 10;
        int crans = info.getOverlay() == BossInfo.Overlay.PROGRESS ? -1 : 80 + (info.getOverlay().ordinal() - 1) * 10;
        mc.getTextureManager().bindTexture(BARRES);
        // fond (partie vide) puis remplissage, en orange ; les crans gardent leur couleur
        GlStateManager.color(R * FOND, V * FOND, B * FOND, 1.0F);
        g.drawTexturedModalRect(x, y, 0, blanc, 182, 5);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (crans >= 0) g.drawTexturedModalRect(x, y, 0, crans, 182, 5);
        int plein = (int) (info.getPercent() * 183.0F);
        if (plein > 0) {
            GlStateManager.color(R, V, B, 1.0F);
            g.drawTexturedModalRect(x, y, 0, blanc + 5, plein, 5);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            if (crans >= 0) g.drawTexturedModalRect(x, y, 0, crans + 5, plein, 5);
        }
        String nom = info.getName().getFormattedText();
        mc.fontRenderer.drawStringWithShadow(nom, x + 91 - mc.fontRenderer.getStringWidth(nom) / 2F, y - 9, 0xFFFFFF);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
