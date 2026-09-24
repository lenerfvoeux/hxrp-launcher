package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.CommonProxy;
import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.MsgSync;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;

public class ClientProxy extends CommonProxy {
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
