package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Serveur -> client : ouvre le mini-jeu d'une étape. */
public class MsgMiniJeu implements IMessage {
    public String geste = "", recette = "";
    public int rang, etape, total;

    public MsgMiniJeu() {}

    public MsgMiniJeu(String geste, int rang, String recette, int etape, int total) {
        this.geste = geste; this.rang = rang; this.recette = recette; this.etape = etape; this.total = total;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        geste = ByteBufUtils.readUTF8String(b);
        recette = ByteBufUtils.readUTF8String(b);
        rang = b.readByte(); etape = b.readByte(); total = b.readByte();
    }

    @Override
    public void toBytes(ByteBuf b) {
        ByteBufUtils.writeUTF8String(b, geste);
        ByteBufUtils.writeUTF8String(b, recette);
        b.writeByte(rang); b.writeByte(etape); b.writeByte(total);
    }

    public static class Handler implements IMessageHandler<MsgMiniJeu, IMessage> {
        @Override
        public IMessage onMessage(MsgMiniJeu msg, MessageContext ctx) {
            HxrpMetiers.proxy.ouvrirMiniJeu(msg);
            return null;
        }
    }
}
