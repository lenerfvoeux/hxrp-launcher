package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.Cueillette;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Les six onglets créatifs du Virus, chacun rangé dans un ordre voulu (et non dans l'ordre d'enregistrement) :
 * Officine, Cueillette, Ressources, Préparations, Remèdes (dans l'ordre des maladies 1 → 52), Blocs du monde.
 */
public final class OngletsVirus {
    public static final CreativeTabs OFFICINE = onglet("virus", () -> VirusRegistre.MACHINES.get(Machine.YAGEN), OngletsVirus::officine);
    public static final CreativeTabs CUEILLETTE = onglet("virus_cueillette", "camomille", OngletsVirus::cueillette);
    public static final CreativeTabs RESSOURCES = onglet("virus_ressources", "lotus_de_l_aube", OngletsVirus::ressources);
    public static final CreativeTabs PREPARATIONS = onglet("virus_preparations", "base_d_onguent", OngletsVirus::preparations);
    public static final CreativeTabs REMEDES = onglet("virus_remedes", "elixir_du_lotus_de_l_aube", OngletsVirus::remedes);
    public static final CreativeTabs MONDE = onglet("virus_monde", () -> Cueillette.LOTUS, OngletsVirus::monde);

    private OngletsVirus() {}

    private static CreativeTabs onglet(String nom, String icone, Supplier<List<Item>> contenu) {
        return creer(nom, () -> {
            Item i = Objets.item(icone);
            return i == null ? ItemStack.EMPTY : new ItemStack(i);
        }, contenu);
    }

    private static CreativeTabs onglet(String nom, Supplier<Block> icone, Supplier<List<Item>> contenu) {
        return creer(nom, () -> {
            Block b = icone.get();
            return b == null ? ItemStack.EMPTY : new ItemStack(b);
        }, contenu);
    }

    private static CreativeTabs creer(String nom, Supplier<ItemStack> icone, Supplier<List<Item>> contenu) {
        return new CreativeTabs(HxrpMetiers.MODID + "." + nom) {
            @Override
            public ItemStack createIcon() {
                return icone.get();
            }

            @SideOnly(Side.CLIENT)
            @Override
            public void displayAllRelevantItems(NonNullList<ItemStack> liste) {
                for (Item i : contenu.get()) liste.add(new ItemStack(i));
            }
        };
    }

    // ================================================================== contenus
    private static void ajouter(Set<Item> l, String id) {
        Item i = Objets.item(id);
        if (i != null && i != Items.AIR) l.add(i);
    }

    private static void ajouter(Set<Item> l, Block b) {
        Item i = b == null ? Items.AIR : Item.getItemFromBlock(b);
        if (i != Items.AIR) l.add(i);
    }

    /** Machines et instruments, seringues, documents, puis la pioche de prospecteur. */
    static List<Item> officine() {
        Set<Item> l = new LinkedHashSet<>();
        ajouter(l, VirusRegistre.MEUBLE);
        for (Block b : VirusRegistre.MACHINES.values()) ajouter(l, b);
        ajouter(l, VirusRegistre.MICROSCOPE);
        ajouter(l, VirusRegistre.PRESENTOIR);
        for (String id : Arrays.asList("seringue_vide", "seringue_pleine", "ordonnance", "carnet_de_consultation")) ajouter(l, id);
        ajouter(l, VirusRegistre.GRIMOIRE);
        for (String id : Arrays.asList("parchemin_du_second_souffle", "preparation_ratee", "pioche_de_prospecteur")) ajouter(l, id);
        return new ArrayList<>(l);
    }

    /** Les plantes médicinales puis les champignons, chacun du plus commun au plus rare. */
    static List<Item> cueillette() {
        Set<Item> l = new LinkedHashSet<>();
        for (String cat : Arrays.asList("plante", "champignon")) {
            List<Defs.Ingredient> ings = new ArrayList<>();
            for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) if (cat.equals(d.categorie)) ings.add(d);
            ings.sort(Comparator.comparingInt(Cueillette::niveau));
            for (Defs.Ingredient d : ings) ajouter(l, d.id);
        }
        return new ArrayList<>(l);
    }

    /** Les raretés Hunter, les ressources animales et de ruche, puis les butins d'autres mods (boss) utiles au Virus. */
    static List<Item> ressources() {
        Set<Item> l = new LinkedHashSet<>();
        for (String cat : Arrays.asList("rare_hunter", "animal_ruche"))
            for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) if (cat.equals(d.categorie)) ajouter(l, d.id);
        for (Defs.Preparation p : DonneesVirus.preparationsDuJar().values())
            for (String x : p.ingredients) if (x.indexOf(':') > 0) ajouter(l, x);
        return new ArrayList<>(l);
    }

    private static boolean base(Defs.Preparation p) {
        return ("base".equals(p.forme) || "ingredient".equals(p.forme)) && !p.secret && Objets.item(p.produit()) instanceof ItemPreparation;
    }

    /** Les bases et ingrédients préparés (alcools, huiles, charbons, lingot…), puis les soins des blessures. */
    static List<Item> preparations() {
        Set<Item> l = new LinkedHashSet<>();
        for (Defs.Preparation p : DonneesVirus.preparationsDuJar().values()) if (base(p)) ajouter(l, p.produit());
        for (Defs.Blessure b : DonneesVirus.BLESSURES.values()) {
            Defs.Preparation p = DonneesVirus.preparationsDuJar().get(b.soin);
            if (p != null) ajouter(l, p.produit());
        }
        return new ArrayList<>(l);
    }

    /** Les remèdes dans l'ordre des maladies, leurs accompagnements, les six recettes du Second Souffle, puis le reste. */
    static List<Item> remedes() {
        Set<Item> l = new LinkedHashSet<>();
        List<Defs.Maladie> maladies = new ArrayList<>(DonneesVirus.MALADIES.values());
        maladies.sort(Comparator.comparingInt(m -> m.numero));
        for (Defs.Maladie m : maladies) ajouterPrep(l, m.remede);
        for (Defs.Maladie m : maladies) for (String a : m.accompagnements) ajouterPrep(l, a);
        for (String id : Arrays.asList("poudre_des_trente_quatre_plantes", "teinture_des_vingt_champignons", "essence_des_dix_raretes",
                "baume_des_quinze_ressources", "cendre_doree", "remede_du_second_souffle")) ajouterPrep(l, id);
        List<Item> ailleurs = new ArrayList<>(preparations());
        ailleurs.addAll(officine());
        for (Defs.Preparation p : DonneesVirus.preparationsDuJar().values()) {
            Item i = Objets.item(p.produit());
            if (i instanceof ItemPreparation && !ailleurs.contains(i)) l.add(i);
        }
        l.removeAll(ailleurs);
        return new ArrayList<>(l);
    }

    private static void ajouterPrep(Set<Item> l, String id) {
        Defs.Preparation p = DonneesVirus.preparationsDuJar().get(id);
        if (p != null) ajouter(l, p.produit());
    }

    /** Pour les admins : plantes, champignons de tronc, sols, lotus et source de sève. */
    static List<Item> monde() {
        Set<Item> l = new LinkedHashSet<>();
        for (Block b : Cueillette.PLANTES.values()) ajouter(l, b);
        for (Block b : Cueillette.TRONCS.values()) ajouter(l, b);
        for (Block b : Cueillette.SOLS.values()) ajouter(l, b);
        ajouter(l, Cueillette.LOTUS);
        ajouter(l, Cueillette.SEVE);
        return new ArrayList<>(l);
    }
}
