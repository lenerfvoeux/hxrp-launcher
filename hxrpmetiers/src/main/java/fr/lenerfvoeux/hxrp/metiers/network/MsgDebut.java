package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.cuisine.Seances;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> serveur : le joueur a cliqué pour commencer le mini-jeu (démarre l'horloge côté serveur). */
public class MsgDebut implements IMessage {
    public MsgDebut() {}

    @Override public void fromBytes(ByteBuf b) {}
    @Override public void toBytes(ByteBuf b) {}

    public static class Handler implements IMessageHandler<MsgDebut, IMessage> {
        @Override
        public IMessage onMessage(MsgDebut msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> Seances.commencer(p));
            return null;
        }
    }
}
