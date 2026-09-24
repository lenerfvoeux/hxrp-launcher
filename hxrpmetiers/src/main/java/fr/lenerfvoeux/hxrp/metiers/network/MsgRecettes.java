package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Serveur -> client : le carnet du plan de travail (recettes du rang, réalisables ou non, et ce qui manque). */
public class MsgRecettes implements IMessage {
    public static class Ligne {
        public String id = "";
        public boolean ok;
        public List<String> manquants = new ArrayList<>();
    }

    public List<Ligne> lignes = new ArrayList<>();
    public BlockPos pos = BlockPos.ORIGIN;

    public MsgRecettes() {}

    public MsgRecettes(List<Ligne> lignes, BlockPos pos) {
        this.lignes = lignes;
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        pos = BlockPos.fromLong(b.readLong());
        int n = Math.min(b.readShort(), 1000);
        for (int i = 0; i < n; i++) {
            Ligne l = new Ligne();
            l.id = ByteBufUtils.readUTF8String(b);
            l.ok = b.readBoolean();
            int m = b.readByte();
            for (int k = 0; k < m; k++) l.manquants.add(ByteBufUtils.readUTF8String(b));
            lignes.add(l);
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeLong(pos.toLong());
        b.writeShort(lignes.size());
        for (Ligne l : lignes) {
            ByteBufUtils.writeUTF8String(b, l.id);
            b.writeBoolean(l.ok);
            b.writeByte(Math.min(l.manquants.size(), 20));
            for (int k = 0; k < Math.min(l.manquants.size(), 20); k++) ByteBufUtils.writeUTF8String(b, l.manquants.get(k));
        }
    }

    public static class Handler implements IMessageHandler<MsgRecettes, IMessage> {
        @Override
        public IMessage onMessage(MsgRecettes msg, MessageContext ctx) {
            HxrpMetiers.proxy.ouvrirCarnet(msg);
            return null;
        }
    }
}
