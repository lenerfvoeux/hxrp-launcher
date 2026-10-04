package fr.lenerfvoeux.hxrp.metiers.virus.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgDonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgEtatVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Effet;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.EnumMap;
import java.util.Map;

/** Ce que le client sait de sa propre santé : effets à dessiner, rang de Virus, rappel de l'ordonnance. */
@SideOnly(Side.CLIENT)
public final class ClientVirus {
    public static int rangVirus = -1;
    public static final EnumMap<Effet, Long> EFFETS = new EnumMap<>(Effet.class);
    /** Heure client de début de chaque effet (pour les fondus). */
    public static final EnumMap<Effet, Long> DEBUTS = new EnumMap<>(Effet.class);
    public static MsgEtatVirus etat = new MsgEtatVirus();

    private ClientVirus() {}

    /** Appelé sur le fil principal du client. */
    public static void recevoir(IMessage m) {
        if (m instanceof MsgEtatVirus) {
            MsgEtatVirus e = (MsgEtatVirus) m;
            etat = e;
            rangVirus = e.rangVirus;
            long now = maintenant();
            for (Map.Entry<Effet, Long> x : e.effets.entrySet()) if (!actif(x.getKey())) DEBUTS.put(x.getKey(), now);
            EFFETS.clear();
            EFFETS.putAll(e.effets);
        } else if (m instanceof MsgDonneesVirus) {
            MsgDonneesVirus d = (MsgDonneesVirus) m;
            // en solo, le serveur intégré partage déjà les mêmes données : on n'y touche pas
            if (d.textes != null && d.textes.length == DonneesVirus.FICHIERS.length && !Minecraft.getMinecraft().isIntegratedServerRunning()) {
                try {
                    DonneesVirus.appliquer(d.textes);
                } catch (RuntimeException ex) {
                    HxrpMetiers.LOG.error("Virus : données du serveur illisibles", ex);
                }
            }
        } else {
            fr.lenerfvoeux.hxrp.metiers.virus.client.jeu.ClientJeux.recevoir(m);
        }
    }

    public static long maintenant() {
        return HxrpMetiers.proxy.now();
    }

    public static boolean actif(Effet e) {
        Long f = EFFETS.get(e);
        return f != null && f > maintenant();
    }

    /** Force d'un effet (0-1) avec un fondu d'une seconde à l'entrée et à la sortie. */
    public static double force(Effet e) {
        Long f = EFFETS.get(e);
        long now = maintenant();
        if (f == null || f <= now) return 0;
        Long d = DEBUTS.get(e);
        double entree = d == null ? 1 : Math.min(1, (now - d) / 1000.0);
        double sortie = f == Long.MAX_VALUE ? 1 : Math.min(1, (f - now) / 1000.0);
        return Math.max(0, Math.min(entree, sortie));
    }

    public static void vider() {
        EFFETS.clear();
        DEBUTS.clear();
        rangVirus = -1;
        etat = new MsgEtatVirus();
    }
}
