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
    }
}
