package fr.lenerfvoeux.hxrp.phenix;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = HxrpPhenix.MODID, name = "hxrpphenix")
public class PhenixConfig {
    @Config.Comment("Points de vie du Phénix (barre de boss)")
    @Config.RangeDouble(min = 20, max = 100000)
    public static double pointsDeVie = 600;

    @Config.Comment("Multiplicateur des dégâts infligés par le Phénix")
    @Config.RangeDouble(min = 0, max = 20)
    public static double degats = 1.0;

    @Config.Comment("Distance (blocs) à laquelle le Phénix prend un joueur pour cible")
    @Config.RangeInt(min = 8, max = 64)
    public static int rayonCible = 24;

    @Config.Comment("Distance (blocs) au lieu de son invocation au-delà de laquelle il abandonne une poursuite et revient")
    @Config.RangeInt(min = 16, max = 256)
    public static int rayonArene = 48;

    @Config.Comment("Renaissance : à 0 PV, il devient un œuf de cendres (une seule fois)")
    public static boolean renaissance = true;

    @Config.Comment("Secondes laissées aux joueurs pour briser l'œuf de cendres")
    @Config.RangeInt(min = 3, max = 120)
    public static int dureeOeufSecondes = 10;

    @Config.Comment("Points de vie de l'œuf de cendres")
    @Config.RangeDouble(min = 1, max = 10000)
    public static double pointsDeVieOeuf = 60;

    @Config.Comment("Part de la vie rendue quand il renaît (0,3 = 30 %)")
    @Config.RangeDouble(min = 0.05, max = 1)
    public static double vieRenaissance = 0.3;

    @Config.Comment("Chaque coup de feu fait monter la Brûlure du Hunter Virus d'un stade (si hxrpmetiers est installé)")
    public static boolean brulureVirus = true;

    @Config.Comment("Plumes de phénix laissées à sa mort : minimum")
    @Config.RangeInt(min = 0, max = 16)
    public static int plumesMin = 1;

    @Config.Comment("Plumes de phénix laissées à sa mort : maximum")
    @Config.RangeInt(min = 0, max = 16)
    public static int plumesMax = 2;

    @Mod.EventBusSubscriber(modid = HxrpPhenix.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void changement(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (HxrpPhenix.MODID.equals(e.getModID())) ConfigManager.sync(HxrpPhenix.MODID, Config.Type.INSTANCE);
        }
    }
}
