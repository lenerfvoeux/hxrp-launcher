package fr.lenerfvoeux.hxrp.bestiaire;

import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityAbomination;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityNecromancien;
import fr.lenerfvoeux.hxrp.bestiaire.entite.boss.EntityReineGuepe;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityArcherSquelette;
import fr.lenerfvoeux.hxrp.bestiaire.entite.sbire.EntityGuepeSoldat;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.EnumParticleTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Les espèces du bestiaire. Numéros d'entité : 0-49 boss et sbires, 50+ projectiles (voir Registre).
 * Les boss qui n'y sont pas encore arriveront par lots (la Baleine est reportée).
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
