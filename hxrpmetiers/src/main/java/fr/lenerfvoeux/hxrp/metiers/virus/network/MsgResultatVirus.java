package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.SeancesVirus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client → serveur : fin d'un mini-jeu du Virus. Le client envoie le journal de ses entrées horodatées ;
 * le serveur rejoue la partie avec sa propre graine et calcule la note lui-même.
 */
public class MsgResultatVirus implements IMessage {
    /** Fermé pendant la présentation, avant de commencer : rien n'est joué ni consommé. */
    public boolean annule;
    public int tFin;
    public float noteClient;
    public Journal journal = new Journal();

    public MsgResultatVirus() {}

    public MsgResultatVirus(boolean annule, int tFin, float noteClient, Journal journal) {
        this.annule = annule;
        this.tFin = tFin;
        this.noteClient = noteClient;
        this.journal = journal;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        annule = b.readBoolean();
        tFin = b.readInt();
        noteClient = b.readFloat();
        int n = Math.min(b.readShort() & 0xFFFF, Journal.MAX);
        journal = new Journal();
        for (int i = 0; i < n && b.readableBytes() >= 9; i++) journal.ajouter(b.readInt(), b.readByte(), b.readShort(), b.readShort());
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeBoolean(annule);
        b.writeInt(tFin);
        b.writeFloat(noteClient);
        int n = Math.min(journal.n, Journal.MAX);
        b.writeShort(n);
        for (int i = 0; i < n; i++) {
            b.writeInt(journal.temps[i]);
            b.writeByte(journal.types[i]);
            b.writeShort(journal.a[i]);
            b.writeShort(journal.b[i]);
        }
    }

    public static class Handler implements IMessageHandler<MsgResultatVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgResultatVirus msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> SeancesVirus.terminer(p, msg));
            return null;
        }
    }
}
