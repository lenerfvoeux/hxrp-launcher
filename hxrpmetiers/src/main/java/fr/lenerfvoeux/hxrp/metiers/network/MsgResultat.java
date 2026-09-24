package fr.lenerfvoeux.hxrp.metiers.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Cuisine;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Seances;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.item.Qualite;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client -> serveur : fin d'une étape. Le client envoie le journal de ses entrées horodatées ;
 * le serveur rejoue la partie avec sa propre graine et calcule la note lui-même.
 */
public class MsgResultat implements IMessage {
    /** Fermé pendant la présentation, avant de commencer : l'étape n'est pas jouée. */
    public boolean annule;
    public int tFin;
    public float noteClient;
    public Journal journal = new Journal();

    public MsgResultat() {}

    public MsgResultat(boolean annule, int tFin, float noteClient, Journal journal) {
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

    public static class Handler implements IMessageHandler<MsgResultat, IMessage> {
        @Override
        public IMessage onMessage(MsgResultat msg, MessageContext ctx) {
            EntityPlayerMP p = ctx.getServerHandler().player;
            p.getServerWorld().addScheduledTask(() -> traiter(p, msg));
            return null;
        }

        private static void traiter(EntityPlayerMP p, MsgResultat msg) {
            Seances.Seance s = Seances.prendre(p);
            if (s == null) return;
            if (msg.annule && s.debut == 0) return;
            ItemStack held = p.getHeldItem(EnumHand.MAIN_HAND);
            FoodEntry r = EnCours.recette(held);
            if (held.getItem() != ModRegistry.EN_COURS || r == null || !r.id.equals(s.recette) || EnCours.etape(held) != s.etape) {
                p.sendMessage(new TextComponentString(TextFormatting.RED + "Garde ta préparation en main pendant le mini-jeu."));
                return;
            }
            Seances.Verdict v = Seances.verifier(s, msg.journal, msg.tFin, msg.noteClient);
            if (v.suspect) HxrpMetiers.LOG.warn("Gourmet : résultat suspect de {} ({}) : {}", p.getName(), s.geste, v.raison);
            double note = v.note;
            EnCours.addNote(held, note);
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
                p.sendMessage(new TextComponentString(TextFormatting.GRAY + "Étape notée " + Math.round(note) + " % · suivante : "
                        + TextFormatting.GOLD + EnCours.geste(held) + TextFormatting.GRAY + " (" + EnCours.station(held).label + ")"));
            }
            p.inventoryContainer.detectAndSendChanges();
        }
    }
}
