package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Cuisine;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.item.Qualite;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Client -> serveur : note d'une étape de mini-jeu. */
public class MsgResultat implements IMessage {
    public float note;
    public int dureeMs;

    public MsgResultat() {}
    public MsgResultat(float note, int dureeMs) { this.note = note; this.dureeMs = dureeMs; }

    @Override public void fromBytes(ByteBuf b) { note = b.readFloat(); dureeMs = b.readInt(); }
    @Override public void toBytes(ByteBuf b) { b.writeFloat(note); b.writeInt(dureeMs); }

    public static class Handler implements IMessageHandler<MsgResultat, IMessage> {
        @Override
        public IMessage onMessage(MsgResultat msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> {
                ItemStack held = p.getHeldItem(EnumHand.MAIN_HAND);
                if (held.getItem() != ModRegistry.EN_COURS) return;
                double note = Math.max(0, Math.min(100, msg.note));
                // garde-fou : une étape bâclée en moins de 2 secondes ne peut pas être parfaite
                if (msg.dureeMs < 2000) note = Math.min(note, 60);
                EnCours.addNote(held, note);
                FoodEntry r = EnCours.recette(held);
                if (r == null) return;
                if (EnCours.fini(held)) {
                    ItemStack plat = Cuisine.terminer(held);
                    p.setHeldItem(EnumHand.MAIN_HAND, plat);
                    int q = Qualite.quality(plat);
                    Qualite pal = Qualite.of(q);
                    p.sendMessage(new TextComponentString(pal.color + r.name + " terminé · " + pal.label + " · " + q + " %"));
                    NutritionData d = Nutrition.get(p);
                    if (d != null) {
                        int gain = (int) Math.round((5 + r.rank * 5) * (q / 100.0) * (r.steps.size() / 2.0 + 1));
                        boolean monte = d.addXp(gain);
                        p.sendMessage(new TextComponentString(TextFormatting.DARK_AQUA + "+" + gain + " XP Gourmet"
                                + TextFormatting.GRAY + " (" + d.xpGourmet + "/" + (d.rangGourmet < 3 ? NutritionData.PALIERS[d.rangGourmet] : d.xpGourmet) + ")"));
                        if (monte) p.sendMessage(new TextComponentString(TextFormatting.GOLD + "Tu passes Gourmet " + d.rangGourmet + "★ ! Les recettes de ce rang s'ouvrent, et les gestes deviennent plus exigeants."));
                        Network.sync(p);
                    }
                } else {
                    p.sendMessage(new TextComponentString(TextFormatting.GRAY + "Étape réussie à " + Math.round(note) + " % · suivante : "
                            + TextFormatting.GOLD + EnCours.geste(held) + TextFormatting.GRAY + " (" + EnCours.station(held).label + ")"));
                }
            });
            return null;
        }
    }
}
