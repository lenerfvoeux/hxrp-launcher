package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Serveur → client : ouvre un mini-jeu du Virus, avec la graine tirée par le serveur.
 * type : étape de l'officine, administration d'un remède, prise de sang ou microscope (voir SeancesVirus).
 * prep : la préparation (étape, administration) ; sujet : le patient ou le flacon analysé.
 */
public class MsgJeuVirus implements IMessage {
    public int type, etape, total, rang, param;
    public String cle = "", prep = "", sujet = "";
    public long graine;

    public MsgJeuVirus() {}

    public MsgJeuVirus(int type, String cle, String prep, String sujet, int etape, int total, int rang, long graine, int param) {
        this.type = type;
        this.cle = cle;
        this.prep = prep == null ? "" : prep;
        this.sujet = sujet == null ? "" : sujet;
        this.etape = etape;
        this.total = total;
        this.rang = rang;
        this.graine = graine;
        this.param = param;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        type = b.readByte();
        cle = ByteBufUtils.readUTF8String(b);
        prep = ByteBufUtils.readUTF8String(b);
        sujet = ByteBufUtils.readUTF8String(b);
        etape = b.readByte();
        total = b.readByte();
        rang = b.readByte();
        graine = b.readLong();
        param = b.readByte();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeByte(type);
        ByteBufUtils.writeUTF8String(b, cle);
        ByteBufUtils.writeUTF8String(b, prep);
        ByteBufUtils.writeUTF8String(b, sujet);
        b.writeByte(etape);
        b.writeByte(total);
        b.writeByte(rang);
        b.writeLong(graine);
        b.writeByte(param);
    }

    public static class Handler implements IMessageHandler<MsgJeuVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgJeuVirus msg, MessageContext ctx) {
            HxrpMetiers.proxy.messageVirus(msg);
            return null;
        }
    }
}
