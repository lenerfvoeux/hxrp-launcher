package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgEtatVirus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * Petit rappel de l'ordonnance, en haut à gauche, tant que le patient la porte sur lui :
 * la prochaine prise et le temps restant (ou la convalescence).
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID, value = Side.CLIENT)
public final class HudVirus {
    private HudVirus() {}

    private static boolean porteOrdonnance(EntityPlayer p) {
        if (VirusRegistre.ORDONNANCE == null) return false;
        for (ItemStack s : p.inventory.mainInventory) if (s.getItem() == VirusRegistre.ORDONNANCE) return true;
        for (ItemStack s : p.inventory.offHandInventory) if (s.getItem() == VirusRegistre.ORDONNANCE) return true;
        return false;
    }

    @SubscribeEvent
    public static void hud(RenderGameOverlayEvent.Post e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.gameSettings.showDebugInfo || mc.currentScreen != null) return;
        MsgEtatVirus s = ClientVirus.etat;
        if ((!s.traitement && s.convalescence <= 0) || !porteOrdonnance(mc.player)) return;
        long now = ClientVirus.maintenant();
        List<String> l = new ArrayList<>();
        int col = 0xF4E8C8;
        if (s.convalescence > 0) {
            l.add("§6Convalescence");
            l.add("§fencore " + Fraicheur.duree(Math.max(0, s.convalescence - now)));
        } else if (s.attenteSommeil) {
            l.add("§6" + s.maladie);
            l.add("§fAllez dormir dans un lit");
        } else {
            l.add("§6" + s.maladie + " §7· prise " + s.prise + "/" + s.total);
            l.add("§f" + s.items);
            if (s.prise <= 1) l.add("§7à donner par le Virus");
            else if ("reveil".equals(s.moment)) l.add("§7au réveil");
            else if (now < s.debut) l.add("§7dans §f" + Fraicheur.duree(s.debut - now) + (s.moment.isEmpty() ? "" : " §7(" + ("nuit".equals(s.moment) ? "la nuit" : "le soir") + ")"));
            else if (now <= s.fin) l.add("§amaintenant §7· encore " + Fraicheur.duree(s.fin - now));
            else l.add("§cprise manquée");
        }
        FontRenderer fr = mc.fontRenderer;
        int w = 0;
        for (String x : l) w = Math.max(w, fr.getStringWidth(x));
        int x0 = 4, y0 = 4, h = l.size() * 10 + 6;
        Gui.drawRect(x0 - 2, y0 - 2, x0 + w + 26, y0 + h - 2, 0x9A1B100C);
        Gui.drawRect(x0 - 2, y0 - 2, x0 + w + 26, y0 - 1, 0xFFE8871E);
        RenderHelper.enableGUIStandardItemLighting();
        mc.getRenderItem().renderItemAndEffectIntoGUI(new ItemStack(VirusRegistre.ORDONNANCE), x0, y0 + (h - 22) / 2);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        for (int i = 0; i < l.size(); i++) fr.drawStringWithShadow(l.get(i), x0 + 20, y0 + 1 + i * 10, col);
        GlStateManager.color(1, 1, 1, 1);
    }
}
