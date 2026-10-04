package fr.lenerfvoeux.hxrp.metiers.virus.network;

import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** Messages du Hunter Virus, sur le canal du mod (discriminants à partir de 10, ceux du Gourmet vont de 0 à 9). */
public final class ReseauVirus {
    private ReseauVirus() {}

    public static void init(SimpleNetworkWrapper net) {
        net.registerMessage(MsgEtatVirus.Handler.class, MsgEtatVirus.class, 10, Side.CLIENT);
        net.registerMessage(MsgDonneesVirus.Handler.class, MsgDonneesVirus.class, 11, Side.CLIENT);
        net.registerMessage(MsgSautVirus.Handler.class, MsgSautVirus.class, 12, Side.SERVER);
        net.registerMessage(MsgFormulaire.Handler.class, MsgFormulaire.class, 13, Side.CLIENT);
        net.registerMessage(MsgPreparer.Handler.class, MsgPreparer.class, 14, Side.SERVER);
        net.registerMessage(MsgJeuVirus.Handler.class, MsgJeuVirus.class, 15, Side.CLIENT);
        net.registerMessage(MsgDebutVirus.Handler.class, MsgDebutVirus.class, 16, Side.SERVER);
        net.registerMessage(MsgResultatVirus.Handler.class, MsgResultatVirus.class, 17, Side.SERVER);
        net.registerMessage(MsgCarnet.Handler.class, MsgCarnet.class, 18, Side.SERVER);
    }
}
