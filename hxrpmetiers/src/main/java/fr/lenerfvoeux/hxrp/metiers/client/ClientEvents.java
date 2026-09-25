package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModConfig;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.event.FoodEffects;
import fr.lenerfvoeux.hxrp.metiers.item.IFoodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID, value = Side.CLIENT)
public final class ClientEvents {
    private static final ResourceLocation HUD = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/hud.png");

    private ClientEvents() {}

    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        for (Item i : ModRegistry.ALL_ITEMS)
            ModelLoader.setCustomModelResourceLocation(i, 0, new ModelResourceLocation(i.getRegistryName(), "inventory"));
        // feuillages : seul l'état des fruits change le modèle
        for (net.minecraft.block.Block f : ModRegistry.FEUILLES.values())
            ModelLoader.setCustomStateMapper(f, new net.minecraft.client.renderer.block.statemap.StateMap.Builder()
                    .ignore(net.minecraft.block.BlockLeaves.CHECK_DECAY, net.minecraft.block.BlockLeaves.DECAYABLE).build());
    }

    /** Remplace la barre de faim vanilla par nos deux jauges (faim et soif, sur 60). */
    @SubscribeEvent
    public static void hud(RenderGameOverlayEvent.Pre e) {
        if (e.getType() != RenderGameOverlayEvent.ElementType.FOOD) return;
        e.setCanceled(true);
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer p = mc.player;
        if (p == null) return;
        boolean drains = !p.capabilities.isCreativeMode && !p.isSpectator();
        int w = e.getResolution().getScaledWidth(), h = e.getResolution().getScaledHeight();
        int right = w / 2 + 91;
        mc.getTextureManager().bindTexture(HUD);
        GlStateManager.enableBlend();
        GlStateManager.color(1, 1, 1, 1);
        int y = h - GuiIngameForge.right_height;
        bar(right, y, ClientData.faimVue(drains) / 60.0, 9, 0);
        GuiIngameForge.right_height += 10;
        y = h - GuiIngameForge.right_height;
        bar(right, y, ClientData.soifVue(drains) / 60.0, 16, 9);
        GuiIngameForge.right_height += 10;
        GlStateManager.disableBlend();
    }

    private static void bar(int right, int y, double f, int fillV, int iconV) {
        int x = right - 81;
        boolean low = f < 0.15 && (System.currentTimeMillis() / 400) % 2 == 0;
        Gui.drawModalRectWithCustomSizedTexture(x, y, low ? 89 : 80, iconV, 9, 9, 128, 32);
        Gui.drawModalRectWithCustomSizedTexture(x + 9, y, 0, 0, 72, 9, 128, 32);
        int fw = (int) Math.round(70 * Math.max(0, Math.min(1, f)));
        if (fw > 0) Gui.drawModalRectWithCustomSizedTexture(x + 10, y + 1, 0, fillV, fw, 7, 128, 32);
    }

    /** Infos sur les nourritures vanilla : valeur sur 60 et cooldown. */
    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) {
        ItemStack s = e.getItemStack();
        if (!(s.getItem() instanceof ItemFood) || s.getItem() instanceof IFoodItem) return;
        int v = (int) Math.round(((ItemFood) s.getItem()).getHealAmount(s) * ModConfig.multiplicateurVanilla);
        e.getToolTip().add(TextFormatting.DARK_GREEN + "Faim +" + v);
        long cd = HxrpMetiers.proxy.clientCooldownLeft(FoodEffects.key(s));
        if (cd > 0) e.getToolTip().add(TextFormatting.GOLD + "Déjà mangé · envie d'autre chose pendant " + Fraicheur.duree(cd));
    }
}
