package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.List;

/** Client → serveur : le Virus a coché ou décoché des symptômes dans le carnet de consultation (case de l'inventaire). */
public class MsgCarnet implements IMessage {
    public int slot;
    public final List<String> symptomes = new ArrayList<>();

    public MsgCarnet() {}

    public MsgCarnet(int slot, List<String> symptomes) {
        this.slot = slot;
        this.symptomes.addAll(symptomes);
    }

    @Override
    public void fromBytes(ByteBuf b) {
        slot = b.readShort();
        int n = Math.min(b.readByte(), 40);
        for (int i = 0; i < n; i++) symptomes.add(ByteBufUtils.readUTF8String(b));
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeShort(slot);
        int n = Math.min(symptomes.size(), 40);
        b.writeByte(n);
        for (int i = 0; i < n; i++) ByteBufUtils.writeUTF8String(b, symptomes.get(i));
    }

    public static class Handler implements IMessageHandler<MsgCarnet, IMessage> {
        @Override
        public IMessage onMessage(MsgCarnet msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> Diagnostic.cocher(p, msg.slot, msg.symptomes));
            return null;
        }
    }
}
