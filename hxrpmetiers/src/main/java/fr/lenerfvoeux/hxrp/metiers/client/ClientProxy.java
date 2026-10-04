package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.CommonProxy;
import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.MsgSync;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit() {
        for (java.util.Map.Entry<String, Class<? extends net.minecraft.entity.EntityLiving>> e : fr.lenerfvoeux.hxrp.metiers.entite.ModEntites.CLASSES.entrySet())
            rendu(e.getValue(), e.getKey());
    }

    private static <T extends net.minecraft.entity.EntityLiving> void rendu(Class<T> c, String id) {
        net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(c,
                rm -> new fr.lenerfvoeux.hxrp.metiers.client.entite.RenduAnimal<T>(rm, id));
    }

    @Override
    public long now() {
        // Sur le client physique, le serveur intégré tourne aussi ici : même horloge, décalage ~0.
        return System.currentTimeMillis() + ClientData.offset;
    }

    @Override
    public void handleSync(MsgSync msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> ClientData.apply(msg));
    }

    @Override
    public void ouvrirCarnet(MsgRecettes msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiCarnet(msg)));
    }

    @Override
    public void ouvrirMiniJeu(MsgMiniJeu msg) {
        Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiMiniJeu(msg)));
    }

    @Override
    public long clientCooldownLeft(String key) {
        Long end = ClientData.COOLDOWNS.get(key);
        return end == null ? 0 : Math.max(0, end - ClientData.now());
    }

    @Override
    public EntityPlayer clientPlayer() { return Minecraft.getMinecraft().player; }
}
