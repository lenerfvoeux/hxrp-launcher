package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Blessures;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Maladies;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitements;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Ce que le reste du mod (le Gourmet) et les autres mods (le Phénix, par réflexion) demandent au Hunter Virus.
 * Toutes les méthodes sont sans danger si le joueur n'a pas encore de données de santé.
 */
public final class VirusAPI {
    private static final Random RNG = new Random();

    private VirusAPI() {}

    // ================================================================== métier
    public static boolean estVirus(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d != null && d.estVirus();
    }

    public static int rangVirus(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d == null ? -1 : d.rangVirus;
    }

    // ================================================================== Gourmet : faim et soif
    /** Multiplicateur de la vitesse à laquelle la faim descend (faim accélérée : 1,5 ; très accélérée : 2). */
    public static double multFaim(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d == null ? 1 : d.multFaim;
    }

    public static double multSoif(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d == null ? 1 : d.multSoif;
    }

    /** Part de la nourriture qui profite (foie abîmé, bouche douloureuse…). */
    public static double facteurNourriture(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d == null ? 1 : d.facteurNourriture;
    }

    /** Un plat ou une boisson du Gourmet vient d'être consommé (avant que la pile ne diminue). */
    public static void platMange(EntityPlayerMP p, ItemStack s, FoodEntry e, int qualite) {
        SanteData d = Sante.get(p);
        if (d == null || e == null) return;
        boolean viande = false, rouge = false, poisson = false, vegetal = false, lait = false;
        for (String id : deplier(e)) {
            FoodEntry f = FoodDatabase.get(id);
            String cat = f == null ? "" : String.valueOf(f.cat);
            if ("Viandes".equals(cat)) viande = true;
            if (id.equals("boeuf") || id.equals("veau") || id.equals("mouton") || id.equals("cerf") || id.equals("sanglier")) rouge = true;
            if ("Poissons et fruits de mer".equals(cat)) poisson = true;
            if ("Fruits".equals(cat) || "Légumes".equals(cat)) vegetal = true;
            if (id.startsWith("lait") || id.equals("creme") || id.equals("yaourt") || id.equals("fromage")) lait = true;
        }
        Traitements.repas(p, d, viande, rouge, poisson, vegetal, !e.isDrink());
        if (lait && d.lait) {
            p.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, 20 * 15, 0));
            Maladies.message(p, "Rien qu'à l'odeur du lait, votre estomac se retourne.");
        }
        Maladies.platMange(p, d, e, qualite, Fraicheur.perishable(s) ? Fraicheur.fraction(s, System.currentTimeMillis()) : 1);
    }

    private static List<String> deplier(FoodEntry e) {
        List<String> out = new ArrayList<>();
        deplier(e, out, 0);
        return out;
    }

    private static void deplier(FoodEntry e, List<String> out, int prof) {
        if (e == null || prof > 4) return;
        for (String id : e.ingredients) {
            out.add(id);
            FoodEntry f = FoodDatabase.get(id);
            if (f != null && f.isPreparation()) deplier(f, out, prof + 1);
        }
    }

    /** Nourriture vanilla ou d'un autre mod. */
    public static void nourritureVanille(EntityPlayerMP p, ItemStack s) {
        SanteData d = Sante.get(p);
        if (d == null || !(s.getItem() instanceof ItemFood)) return;
        ResourceLocation r = s.getItem().getRegistryName();
        String n = r == null ? "" : r.getPath().toLowerCase(Locale.ROOT);
        boolean poisson = n.contains("fish") || n.contains("salmon") || n.contains("cod");
        boolean rouge = n.contains("beef") || n.contains("mutton") || n.contains("venison");
        boolean viande = rouge || poisson || ((ItemFood) s.getItem()).isWolfsFavoriteMeat();
        Traitements.repas(p, d, viande, rouge, poisson, false, false);
    }

    /** Un ingrédient du Gourmet vient d'être consommé par la cuisine : l'œuf laisse sa coquille. */
    public static void ingredientConsomme(EntityPlayer p, String id) {
        if (!"oeuf".equals(id) || p.world.isRemote) return;
        Item c = Objets.item("coquille_d_oeuf");
        if (c != null) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(c));
    }

    /** Ruche sauvage pleine, main vide : cire d'abeille, et un peu de propolis. Renvoie vrai si quelque chose a été récolté. */
    public static boolean recolterRuche(EntityPlayer p) {
        Item cire = Objets.item("cire_d_abeille"), propolis = Objets.item("propolis");
        if (cire == null) return false;
        if (p.world.isRemote) return true;
        ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(cire, 1 + RNG.nextInt(2)));
        if (propolis != null && RNG.nextInt(100) < 35) ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(propolis));
        return true;
    }

    // ================================================================== autres mods
    /**
     * Le Phénix (mod hxrpphenix) appelle cette méthode par réflexion à chaque coup de feu : la Brûlure monte
     * d'un stade (au plus une fois toutes les 3 secondes).
     */
    public static void brulure(EntityPlayer p, int stades) {
        if (!(p instanceof EntityPlayerMP)) return;
        SanteData d = Sante.get(p);
        if (d == null) return;
        SanteData.Blessure b = d.blessures.get(Blessures.BRULURE);
        if (b != null && b.stade > 0 && System.currentTimeMillis() - b.dernierStade < 3000) return;
        if (b != null && b.stade > 0) b.dernierStade = System.currentTimeMillis() - 60_000;
        Blessures.blesser((EntityPlayerMP) p, d, Blessures.BRULURE, Math.max(1, stades), true);
    }
}
