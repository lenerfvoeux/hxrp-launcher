package fr.lenerfvoeux.hxrp.bestiaire;

import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityAbomination;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityAi;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityCalamarDesSables;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityDemonDeLave;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityKraken;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityDemonNoir;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityNecromancien;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityReineGuepe;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityRoiDesGlaces;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityXerra;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityAraigneeXerra;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityArcherSquelette;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityGobelinArcher;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityGobelinBouclier;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityGuepeSoldat;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityPetitKraken;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityRatDesGlaces;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumParticleTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Les espèces du bestiaire. Numéros d'entité : 0-49 boss et sbires, 50+ projectiles (voir Registre).
 * Onze boss, en trois lots ; la Baleine est reportée.
 */
public final class Especes {
    public static final List<Espece> TOUTES = new ArrayList<>();
    public static final List<Espece> BOSS = new ArrayList<>();

    public static final Espece REINE_GUEPE = boss("reine_guepe", "Reine-guêpe", "reine_guepe", EntityReineGuepe.class, 0)
            .taille(2.4F, 5.5F).vol()
            .appat("rayon_de_miel_royal", "Rayon de miel royal", Espece.Arrivee.VOL, "en jungle ou en forêt",
                    "Un bourdonnement sourd monte de la forêt…", "dévore le rayon de miel royal et fond sur vous !", EnumParticleTypes.DRIP_LAVA)
            .oeuf(0x26141A, 0xC4182C).lieu("Jungles et forêts profondes").feminin();
    public static final Espece GUEPE_SOLDAT = sbire("guepe_soldat", "Guêpe soldat", "reine_guepe", EntityGuepeSoldat.class, 1)
            .taille(0.8F, 1.4F).echelle(0.26F).vol().sbireDe("reine_guepe").feminin();

    public static final Espece ABOMINATION = boss("abomination", "Abomination, porteuse de Zobae", "abomination", EntityAbomination.class, 2)
            .taille(1.6F, 3.8F).apparition("spawn")
            .appat("charnier", "Charnier", Espece.Arrivee.SUR_PLACE, "sous terre",
                    "Des mouches tournent au-dessus du charnier, une odeur atroce monte…", "jaillit du sol, affamée !", EnumParticleTypes.VILLAGER_HAPPY)
            .oeuf(0x2A1C1C, 0x8E1B1B).lieu("Souterrains").feminin();

    public static final Espece NECROMANCIEN = boss("necromancien", "Nécromancien", "necromancien", EntityNecromancien.class, 3)
            .taille(2.0F, 6.6F).anims("idle", "flotte", "mort").apparition("apparition")
            .appat("cercueil_profane", "Cercueil profané", Espece.Arrivee.SUR_PLACE, "au sol",
                    "Des mains décharnées sortent de terre autour du cercueil…", "se matérialise au-dessus du cercueil !", EnumParticleTypes.SMOKE_LARGE)
            .oeuf(0x1B1F2E, 0xC81E2A).lieu("Cimetières, ruines");
    public static final Espece ARCHER_SQUELETTE = sbire("archer_squelette", "Archer squelette", "archer_squelette", EntityArcherSquelette.class, 4)
            .taille(0.9F, 3.1F).apparition("spawn").sbireDe("necromancien");

    public static final Espece XERRA = boss("xerra", "Xerra, mère-araignée", "xerra", EntityXerra.class, 5)
            .taille(4.0F, 3.8F)
            .appat("cocon_de_proie", "Cocon de proie", Espece.Arrivee.PLAFOND, "sous terre (pas de ciel)",
                    "Des araignées sortent des murs, la soie tremble au plafond…", "descend du plafond et dévore le cocon !", EnumParticleTypes.TOWN_AURA)
            .oeuf(0x2A0A10, 0xD8402A).lieu("Grottes profondes").feminin();
    /** Araignée de Xerra : Xerra en petit, sans la femme qui la chevauche. */
    public static final Espece ARAIGNEE_XERRA = sbire("araignee_xerra", "Araignée de Xerra", "xerra", EntityAraigneeXerra.class, 6)
            .taille(1.2F, 0.85F).echelle(0.22F).cacher("woman").sbireDe("xerra").feminin();

    public static final Espece ROI_DES_GLACES = boss("roi_des_glaces", "Roi des glaces", "roi_des_glaces", EntityRoiDesGlaces.class, 7)
            .taille(3.0F, 3.9F)
            .appat("cerf_pris_dans_la_glace", "Cerf pris dans la glace", Espece.Arrivee.MARCHE, "dans un biome froid",
                    "La neige tombe plus fort, un grondement glacé approche…", "brise la glace, dévore le cerf et appelle son escorte !",
                    EnumParticleTypes.SNOW_SHOVEL)
            .oeuf(0x9ED3E6, 0x2F6E9A).lieu("Pics glacés");
    public static final Espece RAT_DES_GLACES = sbire("rat_des_glaces", "Rat des glaces", "rat_des_glaces", EntityRatDesGlaces.class, 8)
            .taille(0.9F, 1.2F).sbireDe("roi_des_glaces");
    public static final Espece GOBELIN_BOUCLIER = sbire("gobelin_bouclier", "Gobelin bouclier", "gobelin_bouclier", EntityGobelinBouclier.class, 9)
            .taille(1.0F, 1.9F).sbireDe("roi_des_glaces");
    public static final Espece GOBELIN_ARCHER = sbire("gobelin_archer", "Gobelin archer", "gobelin_archer", EntityGobelinArcher.class, 10)
            .taille(0.9F, 1.75F).sbireDe("roi_des_glaces");

    public static final Espece DEMON_NOIR = boss("demon_noir", "Démon noir du Continent noir", "demon_noir", EntityDemonNoir.class, 11)
            .taille(3.5F, 9.0F).apparition("spawn")
            .appat("carcasse_de_sanglier", "Carcasse de sanglier", Espece.Arrivee.SAUT, "au sol",
                    "Des corbeaux tournent au-dessus de la carcasse…", "atterrit d'un saut sur la carcasse et la dévore !", EnumParticleTypes.SMOKE_NORMAL)
            .oeuf(0x1A1420, 0xE0A070).lieu("Ruines du Continent noir");

    public static final Espece KRAKEN = boss("kraken", "Kraken des abysses", "kraken", EntityKraken.class, 12)
            .taille(5.0F, 5.0F).nage(Material.WATER, 0F).anims("low_idle", "low_walk", "low_immerse").apparition("emerge").cacher("vfx_blade")
            .appat("tonneau_appat_poisson", "Tonneau d'appât de poisson", Espece.Arrivee.SUR_PLACE, "sur l'eau profonde",
                    "L'eau se met à bouillonner autour du tonneau…", "émerge sous le tonneau et le broie !", EnumParticleTypes.WATER_SPLASH)
            .oeuf(0x2B1B3A, 0x5BD04A).lieu("Océan profond");
    /** Petit kraken : le Kraken en petit. */
    public static final Espece PETIT_KRAKEN = sbire("petit_kraken", "Petit kraken", "kraken", EntityPetitKraken.class, 13)
            .taille(0.9F, 0.8F).echelle(0.12F).anims("low_idle", "low_walk", "low_immerse").cacher("vfx_blade").sbireDe("kraken");

    public static final Espece CALAMAR_DES_SABLES = boss("calamar_des_sables", "Calamar des sables", "calamar_des_sables", EntityCalamarDesSables.class, 14)
            .taille(4.5F, 8.0F).anims("idle", "walk", "low_immerse").apparition("emerge").cacher("vfx_blade")
            .appat("tambour_des_sables", "Tambour des sables", Espece.Arrivee.SUR_PLACE, "sur le sable",
                    "Le tambour bat tout seul, le sol se met à vibrer…", "surgit du sable sous le tambour et l'avale !", EnumParticleTypes.CLOUD)
            .oeuf(0xC9A35A, 0x7A4A1E).lieu("Désert");

    public static final Espece DEMON_DE_LAVE = boss("demon_de_lave", "Démon de lave", "demon_de_lave", EntityDemonDeLave.class, 15)
            .taille(4.0F, 9.0F).nage(Material.LAVA, -3F).anims("idle", "idle", "death").apparition("surgissement")
            .appat("idole_d_obsidienne", "Idole d'obsidienne", Espece.Arrivee.SUR_PLACE, "à moins de 3 blocs de la lave",
                    "La lave se met à bouillonner…", "sort du lac de lave et fracasse l'idole !", EnumParticleTypes.LAVA)
            .oeuf(0x3A0C08, 0xFF6A10).lieu("Lac de lave");

    public static final Espece AI = boss("ai", "Aï, forme de vie gazeuse", "ai", EntityAi.class, 16)
            .taille(2.0F, 6.2F).vol().apparition("formation")
            .appat("lanterne_des_voeux", "Lanterne des vœux", Espece.Arrivee.SUR_PLACE, "la nuit, dans un marais",
                    "Le brouillard monte, la flamme noire de la lanterne vacille…", "se forme dans la brume et éteint la lanterne !",
                    EnumParticleTypes.SMOKE_LARGE)
            .oeuf(0x14121A, 0x8A2BE2).lieu("Marais de Numere, la nuit");

    private Especes() {}

    private static Espece boss(String id, String nom, String modele, Class<? extends EntityLiving> c, int n) {
        Espece e = new Espece(id, nom, modele, true, c, n);
        TOUTES.add(e);
        BOSS.add(e);
        return e;
    }

    private static Espece sbire(String id, String nom, String modele, Class<? extends EntityLiving> c, int n) {
        Espece e = new Espece(id, nom, modele, false, c, n);
        TOUTES.add(e);
        return e;
    }

    public static Espece boss(String id) {
        for (Espece e : BOSS) if (e.id.equals(id)) return e;
        return null;
    }

    public static Espece parAppat(String appat) {
        for (Espece e : BOSS) if (e.appat.equals(appat)) return e;
        return null;
    }
}
