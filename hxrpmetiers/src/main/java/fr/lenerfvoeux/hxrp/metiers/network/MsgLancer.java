package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Cuisine;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.items.ItemHandlerHelper;

/** Client -> serveur : lancer une recette depuis le carnet du plan de travail. */
public class MsgLancer implements IMessage {
    public String id = "";
    public BlockPos pos = BlockPos.ORIGIN;

    public MsgLancer() {}

    public MsgLancer(String id, BlockPos pos) {
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

    public static class Handler implements IMessageHandler<MsgLancer, IMessage> {
        @Override
        public IMessage onMessage(MsgLancer msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                FoodEntry r = FoodDatabase.get(msg.id);
                if (r == null || r.steps.isEmpty()) return;
                // on ne cuisine qu'au plan de travail, à portée de main, et à son rang
                if (p.getDistanceSq(msg.pos) > 64 || p.world.getBlockState(msg.pos).getBlock() != ModRegistry.STATIONS.get(Station.PLAN_DE_TRAVAIL)) return;
                NutritionData d = Nutrition.get(p);
                if (r.rank > (d == null ? 0 : d.rangGourmet)) {
                    p.sendMessage(new TextComponentString(TextFormatting.RED + "Cette recette demande un Gourmet " + r.rank + "★."));
                    return;
                }
                ItemStack en = Cuisine.demarrer(p, r);
                if (en.isEmpty()) {
                    p.sendMessage(new TextComponentString(TextFormatting.RED + "Il te manque des ingrédients (ou ils sont avariés)."));
                    return;
                }
                ItemHandlerHelper.giveItemToPlayer(p, en);
                String geste = EnCours.geste(en);
                Station st = EnCours.station(en);
                p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Préparation lancée : " + r.name
                        + TextFormatting.GRAY + " · première étape : " + geste + " (" + (st == null ? "?" : st.label) + ")"));
            });
            return null;
        }
    }
}
