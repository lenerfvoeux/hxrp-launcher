package fr.lenerfvoeux.hxrp.metiers.data;

import java.util.Collections;
import java.util.List;

/** Une entrée du tableur : ingrédient, épice, préparation, plat ou boisson. */
public class FoodEntry {
    public String id;
    public String name;
    public String cat;
    public String source;
    public String cut;
    /** Durée de vie en heures réelles (0 = ne périme pas). Pour les plats, fixée à la cuisson. */
    public int life;
    public String kind;
    public List<String> ingredients = Collections.emptyList();
    public List<String> steps = Collections.emptyList();
    public int rank;
    public int faim;
    public int soif;
    public String buff;

    public boolean isDish() { return "plat".equals(kind) || "boisson".equals(kind); }
    public boolean isDrink() { return "boisson".equals(kind); }
    public boolean isWaterBottle() { return "bouteille_d_eau".equals(id); }
    public boolean perishable() { return isDish() || life > 0; }
}
