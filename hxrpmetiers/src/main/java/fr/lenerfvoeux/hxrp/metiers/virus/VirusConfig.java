package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraftforge.common.config.Config;

/** Réglages du Hunter Virus (config/hxrpmetiers_virus.cfg). Les recettes et maladies sont dans config/hxrpmetiers/virus/*.json. */
@Config(modid = HxrpMetiers.MODID, name = "hxrpmetiers_virus", category = "virus")
public final class VirusConfig {
    private VirusConfig() {}

    // ---------------------------------------------------------------- apparition des maladies
    @Config.Comment("Délai minimal entre deux maladies tirées au hasard (jours réels)")
    @Config.RangeDouble(min = 0.01, max = 365) public static double maladieJoursMin = 3;
    @Config.Comment("Délai maximal entre deux maladies tirées au hasard (jours réels)")
    @Config.RangeDouble(min = 0.01, max = 365) public static double maladieJoursMax = 5;
    @Config.Comment("Activer les maladies tirées au hasard")
    public static boolean maladiesAleatoires = true;
    @Config.Comment("Poids de tirage des maladies selon leur rang (0★, 1★, 2★, 3★)")
    public static int[] poidsParRang = {6, 4, 2, 1};
    @Config.Comment("Multiplicateur de poids d'une maladie liée au lieu où se trouve le joueur (marais, désert, grotte…)")
    @Config.RangeDouble(min = 1, max = 20) public static double poidsContexte = 3;
    @Config.Comment("Chance (%) de tomber malade en mangeant un plat étrange du Gourmet")
    @Config.RangeInt(min = 0, max = 100) public static int chancePlatEtrange = 35;
    @Config.Comment("Chance (%) de tomber malade en mangeant un plat raté du Gourmet")
    @Config.RangeInt(min = 0, max = 100) public static int chancePlatRate = 12;
    @Config.Comment("Chance (%) de tomber malade en mangeant un plat presque périmé (fraîcheur sous 25 %)")
    @Config.RangeInt(min = 0, max = 100) public static int chancePlatPerime = 20;
    @Config.Comment("Immunité après une guérison (heures réelles)")
    @Config.RangeInt(min = 0, max = 720) public static int immuniteHeures = 24;

    // ---------------------------------------------------------------- stades et symptômes
    @Config.Comment("Multiplicateur de la durée avant le dernier stade (1 = durées du cahier des charges)")
    @Config.RangeDouble(min = 0.001, max = 100) public static double vitesseStades = 1;
    @Config.Comment("Minutes réelles (en jeu) entre deux épisodes de symptômes : minimum")
    @Config.RangeDouble(min = 0.2, max = 600) public static double episodeMinutesMin = 5;
    @Config.Comment("Minutes réelles (en jeu) entre deux épisodes de symptômes : maximum")
    @Config.RangeDouble(min = 0.2, max = 600) public static double episodeMinutesMax = 10;
    @Config.Comment("Minutes entre deux épisodes d'un effet à vie « par moments »")
    @Config.RangeDouble(min = 1, max = 1440) public static double episodeVieMinutes = 25;

    // ---------------------------------------------------------------- blessures
    @Config.Comment("Fenêtre (minutes) pendant laquelle un nouveau dégât du même type aggrave une blessure")
    @Config.RangeInt(min = 1, max = 240) public static int fenetreBlessureMinutes = 10;
    @Config.Comment("Délai minimal (secondes) entre deux aggravations par une source continue (feu, cactus…)")
    @Config.RangeInt(min = 0, max = 600) public static int antiRepetitionSecondes = 15;
    @Config.Comment("Au stade 4, une blessure s'alourdit toutes les N minutes")
    @Config.RangeInt(min = 1, max = 1440) public static int aggravationMinutes = 30;
    @Config.Comment("Repos exigé (minutes, ni sprint ni combat) pour qu'un soin agisse aux stades 3 et 4")
    @Config.RangeInt(min = 0, max = 240) public static int reposSoinMinutes = 15;
    @Config.Comment("Les blessures peuvent tuer (sinon elles laissent toujours un demi-cœur)")
    public static boolean blessuresMortelles = false;
    @Config.Comment("Secondes d'exposition au froid (biome glacé dehors la nuit, ou eau glacée) pour une gelure")
    @Config.RangeInt(min = 10, max = 3600) public static int froidSecondes = 180;

    // ---------------------------------------------------------------- traitements
    @Config.Comment("Tolérance (minutes) autour de l'heure d'une prise")
    @Config.RangeInt(min = 0, max = 600) public static int toleranceMinutes = 30;
    @Config.Comment("Délai (minutes) pour donner toutes les préparations d'une même prise")
    @Config.RangeInt(min = 1, max = 600) public static int dureePriseMinutes = 30;
    @Config.Comment("Temps toléré (minutes) hors d'une consigne de lieu (au chaud, à l'ombre, au sec…) sur toute la cure")
    @Config.RangeInt(min = 0, max = 600) public static int toleranceConsigneMinutes = 10;
    @Config.Comment("Délai (heures) pour donner une prise « au réveil » ou « le soir » au plus tard")
    @Config.RangeInt(min = 1, max = 72) public static int delaiMaxMomentHeures = 14;
    @Config.Comment("Convalescence après la dernière prise selon le rang du Virus (minutes) : 0★, 1★, 2★, 3★")
    public static int[] convalescenceMinutes = {180, 60, 20, 3};
    @Config.Comment("Délai minimal entre deux prises de sang sur un même patient (heures réelles)")
    @Config.RangeDouble(min = 0, max = 168) public static double priseDeSangHeures = 2;

    // ---------------------------------------------------------------- officine
    @Config.Comment("Bonus ajouté à la somme des notes d'une préparation selon le rang du Virus : 0★, 1★, 2★, 3★")
    public static int[] bonusEtoiles = {0, 7, 14, 20};
    @Config.Comment("Note minimale (%) pour qu'une préparation fonctionne")
    @Config.RangeInt(min = 0, max = 100) public static int seuilReussite = 80;
    @Config.Comment("Note (%) à partir de laquelle un remède donne son effet bonus")
    @Config.RangeInt(min = 0, max = 100) public static int seuilBonus = 95;
    @Config.Comment("Sous cette note (%), une administration gâche la dose")
    @Config.RangeInt(min = 0, max = 100) public static int seuilAdministration = 50;
    @Config.Comment("Multiplicateur de la durée de macération dans les jarres (1 = heures réelles des recettes)")
    @Config.RangeDouble(min = 0, max = 10) public static double macerationFacteur = 1;
    @Config.Comment("Distance maximale (blocs) entre le Virus et son patient pendant un soin")
    @Config.RangeDouble(min = 1, max = 16) public static double distanceSoin = 5;

    // ---------------------------------------------------------------- monde
    @Config.Comment("Multiplicateur de génération des plantes et champignons du Virus (0 = aucune)")
    @Config.RangeDouble(min = 0, max = 10) public static double generationPlantes = 1;
    @Config.Comment("Multiplicateur de vitesse de pousse des plantes du Virus")
    @Config.RangeDouble(min = 0.05, max = 20) public static double poussePlantes = 1;
    @Config.Comment("Jours réels avant qu'un lotus de l'aube cueilli refleurisse")
    @Config.RangeDouble(min = 0, max = 60) public static double lotusJours = 3;
    @Config.Comment("Heures réelles entre deux récoltes de sève sur une source de l'Arbre-Monde")
    @Config.RangeDouble(min = 0, max = 720) public static double seveHeures = 24;

    // ---------------------------------------------------------------- personnage
    @Config.Comment("Tirer un nouveau groupe sanguin quand le joueur crée un nouveau personnage (mod Identity)")
    public static boolean nouveauGroupeParPersonnage = false;
}
