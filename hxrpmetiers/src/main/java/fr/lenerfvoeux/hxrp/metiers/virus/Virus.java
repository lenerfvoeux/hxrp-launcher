package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Le Hunter Virus : l'apothicaire du serveur. Intégré au mod hxrpmetiers à côté du Gourmet.
 * Données (config/hxrpmetiers/virus/*.json), santé des joueurs, officine, diagnostic, maladies et blessures.
 */
public final class Virus {
    private Virus() {}

    public static void preInit(FMLPreInitializationEvent e) {
        DonneesVirus.chargerJar();
        DonneesVirus.chargerConfig(Loader.instance().getConfigDir());
        Sante.register();
    }

    public static void init() {
        fr.lenerfvoeux.hxrp.metiers.virus.block.VirusTiles.enregistrer();
        fr.lenerfvoeux.hxrp.metiers.virus.monde.GenVirus.enregistrer();
        HxrpMetiers.LOG.info("Virus : {} préparations à l'officine, {} maladies au Grimoire", DonneesVirus.PREPARATIONS.size(), DonneesVirus.MALADIES.size());
    }
}
