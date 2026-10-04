package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Effet;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitement;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitements;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.EnumMap;
import java.util.Map;

/**
 * Serveur → client : rang de Virus, effets à dessiner (avec leur fin, en heure serveur) et ce qu'affiche
 * le rappel de l'ordonnance (prochaine prise, fenêtre, convalescence).
 */
public class MsgEtatVirus implements IMessage {
    public int rangVirus = -1;
    public EnumMap<Effet, Long> effets = new EnumMap<>(Effet.class);
    public String maladie = "", items = "", moment = "";
    public int prise, total;
    public long debut, fin, convalescence, serveur;
    public boolean attenteSommeil, traitement;

    public MsgEtatVirus() {}

    public static MsgEtatVirus de(EntityPlayerMP p, SanteData d, long now) {
        MsgEtatVirus m = new MsgEtatVirus();
        m.rangVirus = d.rangVirus;
        m.serveur = now;
        for (Map.Entry<Effet, Long> e : d.actifs.entrySet())
            if ((e.getKey().client || e.getKey() == Effet.DEGATS_SAUT) && e.getValue() > now) m.effets.put(e.getKey(), e.getValue());
        m.convalescence = d.convalescenceFin;
        Traitement t = d.traitement;
        Defs.Maladie ma = t == null ? null : DonneesVirus.maladie(t.maladie);
        if (ma != null) {
            m.traitement = true;
            m.maladie = ma.nom;
            m.total = ma.prises.size();
            m.prise = Math.min(t.prise + 1, m.total);
            m.attenteSommeil = t.attenteSommeil;
            if (t.prise < ma.prises.size()) {
                Defs.Prise pr = ma.prises.get(t.prise);
                m.items = Traitements.libellePrise(pr);
                m.moment = pr.moment == null ? "" : pr.moment;
                long[] f = Traitements.fenetre(ma, t);
                m.debut = f[0];
                m.fin = f[1];
            }
        }
        return m;
    }

    /** Ce qui, s'il change, mérite un nouvel envoi. */
    public String signature() {
        StringBuilder b = new StringBuilder();
        b.append(rangVirus).append('|').append(traitement).append(prise).append(attenteSommeil).append(convalescence).append(items);
        for (Map.Entry<Effet, Long> e : effets.entrySet()) b.append(e.getKey().ordinal()).append(':').append(e.getValue() / 1000).append(',');
        return b.toString();
    }

    @Override
    public void fromBytes(ByteBuf b) {
        rangVirus = b.readByte();
        serveur = b.readLong();
        int n = b.readUnsignedByte();
        Effet[] v = Effet.values();
        for (int i = 0; i < n; i++) {
            int o = b.readUnsignedByte();
            long f = b.readLong();
            if (o < v.length) effets.put(v[o], f);
        }
        traitement = b.readBoolean();
        maladie = ByteBufUtils.readUTF8String(b);
        items = ByteBufUtils.readUTF8String(b);
        moment = ByteBufUtils.readUTF8String(b);
        prise = b.readByte();
        total = b.readByte();
        debut = b.readLong();
        fin = b.readLong();
        convalescence = b.readLong();
        attenteSommeil = b.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf b) {
        b.writeByte(rangVirus);
        b.writeLong(serveur);
        b.writeByte(Math.min(255, effets.size()));
        int k = 0;
        for (Map.Entry<Effet, Long> e : effets.entrySet()) {
            if (k++ >= 255) break;
            b.writeByte(e.getKey().ordinal());
            b.writeLong(e.getValue());
        }
        b.writeBoolean(traitement);
        ByteBufUtils.writeUTF8String(b, maladie);
        ByteBufUtils.writeUTF8String(b, items);
        ByteBufUtils.writeUTF8String(b, moment);
        b.writeByte(prise);
        b.writeByte(total);
        b.writeLong(debut);
        b.writeLong(fin);
        b.writeLong(convalescence);
        b.writeBoolean(attenteSommeil);
    }

    public static class Handler implements IMessageHandler<MsgEtatVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgEtatVirus msg, MessageContext ctx) {
            HxrpMetiers.proxy.messageVirus(msg);
            return null;
        }
    }
}
