package fr.lenerfvoeux.hxrp.metiers.cuisine;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.item.Qualite;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Le moteur de cuisine : ce qu'on peut faire, ce qu'on consomme, et la note finale. */
public final class Cuisine {
    private Cuisine() {}

    /** Recettes réalisables avec ce qu'a le joueur en poche, à son rang. */
    public static List<FoodEntry> realisables(EntityPlayer p) {
        NutritionData d = Nutrition.get(p);
        int rang = d == null ? 0 : d.rangGourmet;
        List<FoodEntry> out = new ArrayList<>();
        for (FoodEntry e : FoodDatabase.ALL.values()) {
            if (e.steps.isEmpty() || e.rank > rang) continue;
            if (ingredientsPresents(p, e)) out.add(e);
        }
        out.sort((a, b) -> a.rank != b.rank ? a.rank - b.rank : a.name.compareToIgnoreCase(b.name));
        return out;
    }

    /**
     * Le carnet du plan de travail : toutes les recettes accessibles au rang du joueur, de 0 à 3 étoiles,
     * avec pour chacune ce qui lui manque.
     */
    public static List<MsgRecettes.Ligne> carnet(EntityPlayer p) {
        NutritionData d = Nutrition.get(p);
        int rang = d == null ? 0 : d.rangGourmet;
        long now = System.currentTimeMillis();
        List<MsgRecettes.Ligne> out = new ArrayList<>();
        for (FoodEntry e : FoodDatabase.ALL.values()) {
            if (e.steps.isEmpty() || e.rank > rang) continue;
            MsgRecettes.Ligne l = new MsgRecettes.Ligne();
            l.id = e.id;
            for (String id : e.ingredients) if (trouve(p, id, now) < 0) l.manquants.add(id);
            l.ok = l.manquants.isEmpty();
            out.add(l);
        }
        out.sort((a, b) -> {
            FoodEntry x = FoodDatabase.get(a.id), y = FoodDatabase.get(b.id);
            return x.rank != y.rank ? x.rank - y.rank : x.name.compareToIgnoreCase(y.name);
        });
        return out;
    }

    public static boolean ingredientsPresents(EntityPlayer p, FoodEntry r) {
        long now = System.currentTimeMillis();
        for (String id : r.ingredients) if (trouve(p, id, now) < 0) return false;
        return true;
    }

    /** Index d'un ingrédient utilisable (non avarié) dans l'inventaire, -1 sinon. */
    private static int trouve(EntityPlayer p, String id, long now) {
        Item item = ModRegistry.FOOD.get(id);
        if (item == null) return -1;
        for (int i = 0; i < p.inventory.getSizeInventory(); i++) {
            ItemStack s = p.inventory.getStackInSlot(i);
            if (s.isEmpty() || s.getItem() != item) continue;
            if (Fraicheur.rotten(s, now)) continue;
            return i;
        }
        return -1;
    }

    /**
     * Consomme les ingrédients et rend la préparation en cours.
     * La fraîcheur retenue est celle de l'ingrédient le plus abîmé, et la péremption du plat
     * celle de l'ingrédient qui périme le plus tôt.
     */
    public static ItemStack demarrer(EntityPlayer p, FoodEntry r) {
        if (!ingredientsPresents(p, r)) return ItemStack.EMPTY;
        long now = System.currentTimeMillis();
        double pire = 1;
        long expMin = Long.MAX_VALUE;
        List<Double> preps = new ArrayList<>();
        for (String id : r.ingredients) {
            int slot = trouve(p, id, now);
            if (slot < 0) return ItemStack.EMPTY;
            ItemStack s = p.inventory.getStackInSlot(slot);
            if (Fraicheur.perishable(s) && Fraicheur.stamped(s)) {
                pire = Math.min(pire, Fraicheur.fraction(s, now));
                expMin = Math.min(expMin, now + Fraicheur.remaining(s, now));
            }
            // une préparation faite en cuisine apporte sa propre note au plat
            FoodEntry e = Fraicheur.entry(s);
            if (e != null && e.isPreparation() && Qualite.rated(s)) preps.add((double) Qualite.quality(s));
            p.inventory.decrStackSize(slot, 1);
        }
        int base = dureeDeVie(r);
        int life = expMin == Long.MAX_VALUE ? base : (int) Math.max(1, Math.min(base, (expMin - now) / Fraicheur.HOUR));
        NutritionData d = Nutrition.get(p);
        double[] notes = new double[preps.size()];
        for (int i = 0; i < notes.length; i++) notes[i] = preps.get(i);
        return EnCours.create(r, pire, d == null ? 0 : d.rangGourmet, life, notes);
    }

    /** Durée de vie d'un plat ou d'une préparation à la sortie de cuisine (h) : jamais plus que sa durée propre. */
    static int dureeDeVie(FoodEntry r) {
        if (r.life > 0) return r.life;
        return r.isDish() ? fr.lenerfvoeux.hxrp.metiers.ModConfig.peremptionPlatCommandeHeures : 48;
    }

    /** Transforme la préparation terminée en plat (ou en préparation intermédiaire). */
    public static ItemStack terminer(ItemStack enCours) {
        FoodEntry r = EnCours.recette(enCours);
        if (r == null) return ItemStack.EMPTY;
        Item item = ModRegistry.FOOD.get(r.id);
        if (item == null) return ItemStack.EMPTY;
        double note = EnCours.moyennePonderee(enCours, r) - Fraicheur.penalty(EnCours.fraicheur(enCours));
        ItemStack out = new ItemStack(item);
        Qualite.set(out, (int) Math.round(Math.max(0, Math.min(100, note))), EnCours.rang(enCours));
        long now = System.currentTimeMillis();
        Fraicheur.setExpiration(out, Fraicheur.bucket(now + EnCours.life(enCours) * Fraicheur.HOUR, now), EnCours.life(enCours));
        return out;
    }

    /** Difficulté d'un mini-jeu selon le rang : plus d'étoiles, plus dur. */
    public static double largeurZone(int rang) {
        return new double[]{1.0, 0.8, 0.62, 0.48}[Math.max(0, Math.min(3, rang))];
    }
}
