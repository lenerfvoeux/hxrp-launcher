package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Serveur -> client : ouvre le mini-jeu d'une étape, avec la graine tirée par le serveur. */
public class MsgMiniJeu implements IMessage {
    public String geste = "", recette = "";
    public int etape, total, rang, param;
    public long graine;

    public MsgMiniJeu() {}

    public MsgMiniJeu(String geste, String recette, int etape, int total, int rang, long graine, int param) {
        this.geste = geste;
        this.recette = recette;
        this.etape = etape;
        this.total = total;
        this.rang = rang;
        this.graine = graine;
        this.param = param;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        geste = ByteBufUtils.readUTF8String(b);
        recette = ByteBufUtils.readUTF8String(b);
        etape = b.readByte();
        total = b.readByte();
        rang = b.readByte();
        graine = b.readLong();
        param = b.readByte();
    }

    @Override
    public void toBytes(ByteBuf b) {
        ByteBufUtils.writeUTF8String(b, geste);
        ByteBufUtils.writeUTF8String(b, recette);
        b.writeByte(etape);
        b.writeByte(total);
        b.writeByte(rang);
        b.writeLong(graine);
        b.writeByte(param);
    }

    public static class Handler implements IMessageHandler<MsgMiniJeu, IMessage> {
        @Override
        public IMessage onMessage(MsgMiniJeu msg, MessageContext ctx) {
            HxrpMetiers.proxy.ouvrirMiniJeu(msg);
            return null;
        }
    }
}
