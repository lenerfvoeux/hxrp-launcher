package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.virus.sante.Effet;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.MoteurSante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client → serveur : le joueur vient de sauter alors qu'une côte fêlée le fait souffrir à chaque saut. */
public class MsgSautVirus implements IMessage {
    public MsgSautVirus() {}

    @Override public void fromBytes(ByteBuf b) {}
    @Override public void toBytes(ByteBuf b) {}

    public static class Handler implements IMessageHandler<MsgSautVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgSautVirus msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                SanteData d = Sante.get(p);
                long now = System.currentTimeMillis();
                if (d == null || !d.actif(Effet.DEGATS_SAUT) || now - d.dernierSautDouloureux < 1500) return;
                d.dernierSautDouloureux = now;
                MoteurSante.degatsLegers(p, 1);
            });
            return null;
        }
    }
}
