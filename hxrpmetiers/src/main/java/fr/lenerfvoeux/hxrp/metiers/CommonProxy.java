package fr.lenerfvoeux.hxrp.metiers;

import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.MsgSync;
import net.minecraft.entity.player.EntityPlayer;

public class CommonProxy {
    public void preInit() {}
    /** Heure de référence : l'heure réelle du serveur (côté client, corrigée du décalage). */
    public long now() { return System.currentTimeMillis(); }
    public void handleSync(MsgSync msg) {}
    public void ouvrirCarnet(MsgRecettes msg) {}
    public void ouvrirMiniJeu(MsgMiniJeu msg) {}
    /** Cooldown restant (ms) pour un aliment, vu du côté logique demandé. */
    public long clientCooldownLeft(String key) { return 0; }
    public EntityPlayer clientPlayer() { return null; }
    /** Messages du Hunter Virus pour le client (ignorés côté serveur). */
    public void messageVirus(net.minecraftforge.fml.common.network.simpleimpl.IMessage msg) {}
    /** Ouvre la lecture d'une ordonnance, d'un carnet ou d'un parchemin (client). */
    public void ouvrirLectureVirus(net.minecraft.item.ItemStack s) {}
    /** Ouvre le Grimoire des maladies (client). */
    public void ouvrirGrimoire() {}
}
