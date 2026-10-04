package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.Officine;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client → serveur : lancer une préparation depuis le formulaire de la table de préparation. */
public class MsgPreparer implements IMessage {
    public String id = "";
    public BlockPos pos = BlockPos.ORIGIN;

    public MsgPreparer() {}

    public MsgPreparer(String id, BlockPos pos) {
        this.id = id;
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf b) {
        id = ByteBufUtils.readUTF8String(b);
        pos = BlockPos.fromLong(b.readLong());
    }

    @Override
    public void toBytes(ByteBuf b) {
        ByteBufUtils.writeUTF8String(b, id);
        b.writeLong(pos.toLong());
    }

    public static class Handler implements IMessageHandler<MsgPreparer, IMessage> {
        @Override
        public IMessage onMessage(MsgPreparer msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> Officine.lancer(p, msg.id, msg.pos));
            return null;
        }
    }
}
