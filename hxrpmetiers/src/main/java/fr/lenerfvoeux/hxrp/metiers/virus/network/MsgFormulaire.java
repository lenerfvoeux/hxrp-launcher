package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Serveur → client : le formulaire de la table de préparation (ce que le Virus sait faire, et ce qui lui manque). */
public class MsgFormulaire implements IMessage {
    public static final class Ligne {
        public String id = "";
        public boolean ok;
        public final List<String> manquants = new ArrayList<>();
    }

    public BlockPos pos = BlockPos.ORIGIN;
    public int rang;
    public final List<Ligne> lignes = new ArrayList<>();

    public MsgFormulaire() {}

    public MsgFormulaire(BlockPos pos, int rang, List<Ligne> lignes) {
        this.pos = pos;
        this.rang = rang;
        this.lignes.addAll(lignes);
    }

    @Override
    public void fromBytes(ByteBuf b) {
        pos = BlockPos.fromLong(b.readLong());
        rang = b.readByte();
        int n = Math.min(b.readShort(), 2048);
        for (int i = 0; i < n; i++) {
            Ligne l = new Ligne();
            l.id = ByteBufUtils.readUTF8String(b);
            l.ok = b.readBoolean();
            int m = Math.min(b.readByte(), 32);
            for (int k = 0; k < m; k++) l.manquants.add(ByteBufUtils.readUTF8String(b));
            lignes.add(l);
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeLong(pos.toLong());
        b.writeByte(rang);
        b.writeShort(lignes.size());
        for (Ligne l : lignes) {
            ByteBufUtils.writeUTF8String(b, l.id);
            b.writeBoolean(l.ok);
            int m = Math.min(l.manquants.size(), 32);
            b.writeByte(m);
            for (int k = 0; k < m; k++) ByteBufUtils.writeUTF8String(b, l.manquants.get(k));
        }
    }

    public static class Handler implements IMessageHandler<MsgFormulaire, IMessage> {
        @Override
        public IMessage onMessage(MsgFormulaire msg, MessageContext ctx) {
            HxrpMetiers.proxy.messageVirus(msg);
            return null;
        }
    }
}
