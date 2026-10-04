package fr.lenerfvoeux.hxrp.metiers.virus.network;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Serveur → client, à la connexion : les fichiers de config/hxrpmetiers/virus du serveur (compressés), pour que
 * le Grimoire, le carnet de l'officine et les mini-jeux affichent les recettes du serveur.
 */
public class MsgDonneesVirus implements IMessage {
    public String[] textes;

    public MsgDonneesVirus() {
        textes = DonneesVirus.textes();
    }

    @Override
    public void fromBytes(ByteBuf b) {
        int n = b.readInt();
        if (n <= 0 || n > 8 * 1024 * 1024) {
            textes = null;
            return;
        }
        byte[] z = new byte[n];
        b.readBytes(z);
        try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(z)))) {
            int k = in.readInt();
            String[] t = new String[k];
            for (int i = 0; i < k; i++) {
                byte[] s = new byte[in.readInt()];
                in.readFully(s);
                t[i] = new String(s, StandardCharsets.UTF_8);
            }
            textes = t;
        } catch (IOException e) {
            textes = null;
        }
    }

    @Override
    public void toBytes(ByteBuf b) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(bo))) {
            out.writeInt(textes.length);
            for (String t : textes) {
                byte[] s = (t == null ? "" : t).getBytes(StandardCharsets.UTF_8);
                out.writeInt(s.length);
                out.write(s);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        byte[] z = bo.toByteArray();
        b.writeInt(z.length);
        b.writeBytes(z);
    }

    public static class Handler implements IMessageHandler<MsgDonneesVirus, IMessage> {
        @Override
        public IMessage onMessage(MsgDonneesVirus msg, MessageContext ctx) {
            HxrpMetiers.proxy.messageVirus(msg);
            return null;
        }
    }
}
