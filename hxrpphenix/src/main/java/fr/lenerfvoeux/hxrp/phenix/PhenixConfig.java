package fr.lenerfvoeux.hxrp.phenix;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Réglages propres au mod (config/hxrpphenix.cfg). Vie, armure, dégâts, portées, phases, appât et plumes sont
 * dans le classeur des boss, config/bestiaire_stats.xlsx (voir StatsPhenix).
 */
@Config(modid = HxrpPhenix.MODID, name = "hxrpphenix")
public class PhenixConfig {
    @Config.Comment("Secondes laissées aux joueurs pour briser l'œuf de cendres")
    @Config.RangeInt(min = 3, max = 120)
    public static int dureeOeufSecondes = 10;

    @Config.Comment("Points de vie de l'œuf de cendres")
    @Config.RangeDouble(min = 1, max = 10000)
    public static double pointsDeVieOeuf = 60;

    @Config.Comment("Chaque coup de feu fait monter la Brûlure du Hunter Virus d'un stade (si hxrpmetiers est installé)")
    public static boolean brulureVirus = true;

    @Mod.EventBusSubscriber(modid = HxrpPhenix.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void changement(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (HxrpPhenix.MODID.equals(e.getModID())) ConfigManager.sync(HxrpPhenix.MODID, Config.Type.INSTANCE);
        }
    }
}
