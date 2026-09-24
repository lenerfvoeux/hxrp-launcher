package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.HashMap;
import java.util.Map;

/** Serveur -> client : faim, soif, rang, cooldowns et heure du serveur. */
public class MsgSync implements IMessage {
    public float faim, soif;
    public int rang, xp;
    public long serverTime;
    public Map<String, Long> cooldowns = new HashMap<>();

    public MsgSync() {}

    public MsgSync(NutritionData d, long serverTime) {
        faim = (float) d.faim;
        soif = (float) d.soif;
        rang = d.rangGourmet;
        xp = d.xpGourmet;
        cooldowns.putAll(d.cooldowns);
        this.serverTime = serverTime;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        faim = b.readFloat();
        soif = b.readFloat();
        rang = b.readByte();
        xp = b.readInt();
        serverTime = b.readLong();
        int n = b.readShort();
        for (int i = 0; i < n; i++) cooldowns.put(ByteBufUtils.readUTF8String(b), b.readLong());
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeFloat(faim);
        b.writeFloat(soif);
        b.writeByte(rang);
        b.writeInt(xp);
        b.writeLong(serverTime);
        b.writeShort(cooldowns.size());
        for (Map.Entry<String, Long> e : cooldowns.entrySet()) {
            ByteBufUtils.writeUTF8String(b, e.getKey());
            b.writeLong(e.getValue());
        }
    }

    public static class Handler implements IMessageHandler<MsgSync, IMessage> {
        @Override
        public IMessage onMessage(MsgSync msg, MessageContext ctx) {
            HxrpMetiers.proxy.handleSync(msg);
            return null;
        }
    }
}
