package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Écrans du Virus ouverts sur ordre du serveur (mini-jeux, formulaire de l'officine…). */
@SideOnly(Side.CLIENT)
public final class ClientJeux {
    private ClientJeux() {}

    public static void recevoir(IMessage m) {
    }

    public static void modeles() {
    }

    /** Lire une ordonnance, un carnet de consultation ou un parchemin. */
    public static void lire(net.minecraft.item.ItemStack s) {
    }
}
