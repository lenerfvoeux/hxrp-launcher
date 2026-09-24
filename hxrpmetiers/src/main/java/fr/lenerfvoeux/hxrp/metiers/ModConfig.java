package fr.lenerfvoeux.hxrp.metiers;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = HxrpMetiers.MODID, name = "hxrpmetiers")
public class ModConfig {
    @Config.Comment("Minutes de jeu pour qu'une barre de faim pleine (60) se vide")
    @Config.RangeInt(min = 1) public static int dureeFaimMinutes = 60;
    @Config.Comment("Minutes de jeu pour qu'une barre de soif pleine (60) se vide")
    @Config.RangeInt(min = 1) public static int dureeSoifMinutes = 60;
    @Config.Comment("Cooldown d'un plat du Gourmet (heures réelles)")
    public static int cooldownPlatsHeures = 72;
    @Config.Comment("Cooldown d'une boisson du Gourmet (heures réelles)")
    public static int cooldownBoissonsHeures = 24;
    @Config.Comment("Cooldown des nourritures vanilla ou d'autres mods (heures réelles)")
    public static int cooldownAutresHeures = 72;
    @Config.Comment("Soif rendue par la bouteille d'eau (sur 60)")
    public static int soifBouteille = 20;
    @Config.Comment("Multiplicateur appliqué aux points de faim des nourritures vanilla (barre 3x plus grande)")
    public static double multiplicateurVanilla = 3.0;
    @Config.Comment("Palier d'empilement en minutes : les aliments datés dans la même tranche s'empilent (0 = désactivé)")
    @Config.RangeInt(min = 0, max = 120) public static int palierEmpilementMinutes = 5;
    @Config.Comment("Aligner un aliment neuf sur le plus vieux du même type déjà dans l'inventaire, pour qu'ils s'empilent")
    public static boolean alignerSurStackExistant = true;
    @Config.Comment("Péremption par défaut (heures) d'un plat donné par commande")
    public static int peremptionPlatCommandeHeures = 48;

    @Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onChange(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (e.getModID().equals(HxrpMetiers.MODID)) ConfigManager.sync(HxrpMetiers.MODID, Config.Type.INSTANCE);
        }
    }
}
