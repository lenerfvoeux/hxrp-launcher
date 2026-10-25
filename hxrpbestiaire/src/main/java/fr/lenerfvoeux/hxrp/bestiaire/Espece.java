package fr.lenerfvoeux.hxrp.bestiaire;

import net.minecraft.entity.EntityLiving;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une espèce du bestiaire : un boss ou un sbire. Le modèle GeckoLib (geo, animations, texture) porte le nom
 * {@link #modele} et vient d'un modèle Blockbench converti par tools/bestiaire/gen.py ; un sbire « boss en petit »
 * reprend le modèle de son boss à une plus petite échelle.
 */
public final class Espece {
    /** Comment le boss rejoint son appât. */
    public enum Arrivee {
        /** Il vole depuis 40-60 blocs (classeur, onglet Appâts) et se pose sur l'appât. */
        VOL,
        /** Il marche depuis 40-60 blocs jusqu'à l'appât. */
        MARCHE,
        /** Il surgit sur place : sort du sol, se matérialise… (animation d'apparition). */
        SUR_PLACE,
        /** Il descend du plafond (ou du ciel) au bout d'un fil, juste au-dessus de l'appât. */
        PLAFOND,
        /** Il bondit depuis 40-60 blocs et retombe sur l'appât. */
        SAUT
    }

    /** Comment l'espèce se déplace en combat : à pied, en vol, ou à la surface de son liquide (eau, lave). */
    public enum Deplacement { SOL, VOL, NAGE }

    public final String id;
    public final String nom;
    public final String modele;
    public final boolean boss;
    public final Class<? extends EntityLiving> classe;
    public final int numero;
    public float largeur = 1, hauteur = 2, echelle = 1;
    public Deplacement deplacement = Deplacement.SOL;
    /** NAGE : le liquide où il vit, et la hauteur de son origine par rapport à la surface (blocs). */
    public net.minecraft.block.material.Material liquide = net.minecraft.block.material.Material.WATER;
    public float flottaison;
    /** Animations de base (sans le préfixe « animation.<modele>. »). */
    public String repos = "idle", marche = "walk", mort = "death", apparition = "";
    /** Os cachés hors des animations qui les montrent (rayons, effets…) : os -> animations où il est visible. */
    public final Map<String, List<String>> cachees = new LinkedHashMap<>();
    public SoundEvent sonAmbiant = SoundEvents.ENTITY_ZOMBIE_AMBIENT, sonBlesse = SoundEvents.ENTITY_ZOMBIE_HURT,
            sonMort = SoundEvents.ENTITY_ZOMBIE_DEATH;
    public float hauteurSon = 1F;
    // boss
    public Arrivee arrivee = Arrivee.MARCHE;
    public String appat = "", nomAppat = "", conditionAppat = "", signeAppat = "", arriveeAppat = "";
    public EnumParticleTypes particuleAppat = EnumParticleTypes.SMOKE_NORMAL;
    public int couleurOeuf = 0x303030, couleurTaches = 0xA02020;
    public String lieu = "";
    /** Sbire : l'id du boss (classeur, onglet Sbires). */
    public String bossDuSbire = "";
    /** Nom féminin (la Reine-guêpe, l'Abomination…) : accords des messages. */
    public boolean feminin;

    Espece(String id, String nom, String modele, boolean boss, Class<? extends EntityLiving> classe, int numero) {
        this.id = id;
        this.nom = nom;
        this.modele = modele;
        this.boss = boss;
        this.classe = classe;
        this.numero = numero;
    }

    Espece taille(float largeur, float hauteur) {
        this.largeur = largeur;
        this.hauteur = hauteur;
        return this;
    }

    Espece echelle(float e) {
        this.echelle = e;
        return this;
    }

    Espece vol() {
        this.deplacement = Deplacement.VOL;
        return this;
    }

    Espece nage(net.minecraft.block.material.Material liquide, float flottaison) {
        this.deplacement = Deplacement.NAGE;
        this.liquide = liquide;
        this.flottaison = flottaison;
        return this;
    }

    /** Ni gravité ni marche : il vole ou nage. */
    public boolean sansGravite() {
        return deplacement != Deplacement.SOL;
    }

    Espece anims(String repos, String marche, String mort) {
        this.repos = repos;
        this.marche = marche;
        this.mort = mort;
        return this;
    }

    Espece apparition(String a) {
        this.apparition = a;
        return this;
    }

    Espece cacher(String os, String... visibleDans) {
        cachees.put(os, visibleDans.length == 0 ? Collections.<String>emptyList() : Arrays.asList(visibleDans));
        return this;
    }

    Espece sons(SoundEvent ambiant, SoundEvent blesse, SoundEvent mort, float hauteur) {
        this.sonAmbiant = ambiant;
        this.sonBlesse = blesse;
        this.sonMort = mort;
        this.hauteurSon = hauteur;
        return this;
    }

    Espece appat(String id, String nom, Arrivee arrivee, String condition, String signe, String arriveeTexte, EnumParticleTypes particule) {
        this.appat = id;
        this.nomAppat = nom;
        this.arrivee = arrivee;
        this.conditionAppat = condition;
        this.signeAppat = signe;
        this.arriveeAppat = arriveeTexte;
        this.particuleAppat = particule;
        return this;
    }

    Espece oeuf(int couleur, int taches) {
        this.couleurOeuf = couleur;
        this.couleurTaches = taches;
        return this;
    }

    Espece feminin() {
        this.feminin = true;
        return this;
    }

    /** « e » si le nom est féminin, pour accorder : vaincu + e(). */
    public String e() {
        return feminin ? "e" : "";
    }

    Espece lieu(String l) {
        this.lieu = l;
        return this;
    }

    Espece sbireDe(String boss) {
        this.bossDuSbire = boss;
        return this;
    }

    /** Nom complet d'une animation GeckoLib de cette espèce. */
    public String animation(String court) {
        return "animation." + modele + "." + court;
    }
}
