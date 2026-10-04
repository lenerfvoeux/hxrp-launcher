package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.SeancesVirus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client → serveur : le Virus a cliqué pour commencer le mini-jeu (démarre l'horloge côté serveur). */
public class MsgDebutVirus implements IMessage {
    public MsgDebutVirus() {}

    @Override public void fromBytes(ByteBuf b) {}
    @Override public void toBytes(ByteBuf b) {}

    public static class Handler implements IMessageHandler<MsgDebutVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgDebutVirus msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> SeancesVirus.commencer(p));
            return null;
        }
    }
}
