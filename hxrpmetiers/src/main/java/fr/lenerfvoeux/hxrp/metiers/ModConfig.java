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
    @Config.Comment("Chance (en %) qu'un chunk de climat adapté reçoive un ou deux arbres fruitiers")
    @Config.RangeInt(min = 0, max = 100) public static int chanceArbreFruitier = 12;
    @Config.Comment("Nombre de filons de sel par chunk (couches 20 à 70)")
    @Config.RangeInt(min = 0, max = 30) public static int filonsDeSel = 5;
    @Config.Comment("Minutes réelles pour qu'une culture récoltée redevienne mûre (même chunk déchargé)")
    @Config.RangeInt(min = 1, max = 10080) public static int rechargeCulturesMinutes = 40;
    @Config.Comment("Minutes réelles pour que les fruits d'un feuillage récolté repoussent")
    @Config.RangeInt(min = 1, max = 10080) public static int rechargeFruitsMinutes = 45;
    @Config.Comment("Minutes réelles pour qu'un banc de moules ou d'huîtres vidé se remplisse")
    @Config.RangeInt(min = 1, max = 10080) public static int rechargeCoquillagesMinutes = 60;
    @Config.Comment("Minutes réelles pour qu'un minerai de sel épuisé se recharge")
    @Config.RangeInt(min = 1, max = 10080) public static int rechargeSelMinutes = 90;
    @Config.Comment("Minutes réelles pour qu'une ruche sauvage vidée se remplisse")
    @Config.RangeInt(min = 1, max = 10080) public static int rechargeRucheMinutes = 60;
    @Config.Comment("Minutes de jeu (en moyenne) entre deux œufs pondus par une poule")
    @Config.RangeInt(min = 1, max = 120) public static int pontePouleMinutes = 8;

    @Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
    public static class Sync {
        @SubscribeEvent
        public static void onChange(ConfigChangedEvent.OnConfigChangedEvent e) {
            if (e.getModID().equals(HxrpMetiers.MODID)) ConfigManager.sync(HxrpMetiers.MODID, Config.Type.INSTANCE);
        }
    }
}
