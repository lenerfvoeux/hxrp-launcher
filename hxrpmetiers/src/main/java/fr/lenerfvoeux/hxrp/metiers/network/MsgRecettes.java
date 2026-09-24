package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Serveur -> client : les recettes réalisables, pour ouvrir le carnet du plan de travail. */
public class MsgRecettes implements IMessage {
    public static class Ligne {
        public String id, nom, etapes;
        public int rang;
    }

    public List<Ligne> lignes = new ArrayList<>();
    public BlockPos pos = BlockPos.ORIGIN;

    public MsgRecettes() {}

    public MsgRecettes(List<FoodEntry> src, BlockPos pos) {
        this.pos = pos;
        for (FoodEntry e : src) {
            Ligne l = new Ligne();
            l.id = e.id;
            l.nom = e.name;
            l.rang = e.rank;
            l.etapes = String.join(", ", e.steps);
            lignes.add(l);
        }
    }

    @Override
    public void fromBytes(ByteBuf b) {
        pos = BlockPos.fromLong(b.readLong());
        int n = b.readShort();
        for (int i = 0; i < n; i++) {
            Ligne l = new Ligne();
            l.id = ByteBufUtils.readUTF8String(b);
            l.nom = ByteBufUtils.readUTF8String(b);
            l.etapes = ByteBufUtils.readUTF8String(b);
            l.rang = b.readByte();
            lignes.add(l);
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeLong(pos.toLong());
        b.writeShort(lignes.size());
        for (Ligne l : lignes) {
            ByteBufUtils.writeUTF8String(b, l.id);
            ByteBufUtils.writeUTF8String(b, l.nom);
            ByteBufUtils.writeUTF8String(b, l.etapes);
            b.writeByte(l.rang);
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
