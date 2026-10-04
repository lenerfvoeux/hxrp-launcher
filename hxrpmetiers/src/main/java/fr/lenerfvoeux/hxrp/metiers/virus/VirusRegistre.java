package fr.lenerfvoeux.hxrp.metiers.virus;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockGrimoire;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockJarres;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockMachine;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockMeuble;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockMicroscope;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockPresentoir;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemCarnetConsultation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemEnCoursVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemIngredientVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemOrdonnance;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemParchemin;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPrepaRatee;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemSeringue;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemSeringuePleine;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockChampiTronc;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockLotus;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockNid;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockPlanteVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockSolVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockSourceSeve;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockTroncEcorce;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.Cueillette;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemLilyPad;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Enregistre les blocs, objets et sons du Hunter Virus. */
@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class VirusRegistre {
    /** Tous les objets du Virus (pour les modèles côté client). */
    public static final List<Item> OBJETS = new ArrayList<>();
    /** Blocs du Virus qui ont un objet (machines, meuble, sols…). */
    public static final List<Block> BLOCS = new ArrayList<>();
    /** Blocs du Virus sans objet propre (plantes, champignons de tronc, troncs écorcés). */
    public static final List<Block> BLOCS_MONDE = new ArrayList<>();
    public static final Map<Machine, Block> MACHINES = new EnumMap<>(Machine.class);

    public static Item SERINGUE, SERINGUE_PLEINE, ORDONNANCE, CARNET, PARCHEMIN, EN_COURS, RATEE;
    public static Block MEUBLE, MICROSCOPE, GRIMOIRE, PRESENTOIR;

    public static final CreativeTabs TAB = onglet("virus", "seringue_vide");
    public static final CreativeTabs TAB_INGREDIENTS = onglet("virus_ingredients", "echinacee");
    public static final CreativeTabs TAB_REMEDES = onglet("virus_remedes", "eau_florale_de_lavande");

    private VirusRegistre() {}

    private static CreativeTabs onglet(String nom, String icone) {
        return new CreativeTabs(HxrpMetiers.MODID + "." + nom) {
            @Override
            public ItemStack createIcon() {
                Item i = Objets.VIRUS.get(icone);
                return i == null ? ItemStack.EMPTY : new ItemStack(i);
            }
        };
    }

    // ================================================================== blocs
    @SubscribeEvent
    public static void blocs(RegistryEvent.Register<Block> e) {
        DonneesVirus.chargerJar();
        MEUBLE = bloc(e, new BlockMeuble());
        for (Machine m : Machine.values()) MACHINES.put(m, bloc(e, m == Machine.JARRES ? new BlockJarres() : new BlockMachine(m)));
        MICROSCOPE = bloc(e, new BlockMicroscope());
        GRIMOIRE = bloc(e, new BlockGrimoire());
        PRESENTOIR = bloc(e, new BlockPresentoir());
        // monde
        for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) {
            switch (d.recolte.type) {
                case "plante": {
                    BlockPlanteVirus b = new BlockPlanteVirus(d);
                    Cueillette.PLANTES.put(d.id, b);
                    monde(e, b);
                    break;
                }
                case "tronc": {
                    BlockChampiTronc b = new BlockChampiTronc(d);
                    Cueillette.TRONCS.put(d.id, b);
                    monde(e, b);
                    break;
                }
                case "sol": {
                    if (d.recolte.bloc == null || d.recolte.bloc.isEmpty() || Cueillette.SOLS.containsKey(d.recolte.bloc)) break;
                    BlockSolVirus b = new BlockSolVirus(d);
                    Cueillette.SOLS.put(d.recolte.bloc, b);
                    bloc(e, b).setCreativeTab(TAB_INGREDIENTS);
                    break;
                }
                default: break;
            }
        }
        Cueillette.ECORCE_BOULEAU = (BlockTroncEcorce) monde(e, new BlockTroncEcorce(true));
        Cueillette.ECORCE_SAULE = (BlockTroncEcorce) monde(e, new BlockTroncEcorce(false));
        Cueillette.NID = (BlockNid) bloc(e, new BlockNid());
        Cueillette.NID.setCreativeTab(TAB_INGREDIENTS);
        Cueillette.LOTUS = (BlockLotus) monde(e, new BlockLotus());
        Cueillette.SEVE = (BlockSourceSeve) bloc(e, new BlockSourceSeve());
    }

    private static Block bloc(RegistryEvent.Register<Block> e, Block b) {
        b.setCreativeTab(TAB);
        e.getRegistry().register(b);
        BLOCS.add(b);
        return b;
    }

    private static Block monde(RegistryEvent.Register<Block> e, Block b) {
        e.getRegistry().register(b);
        BLOCS_MONDE.add(b);
        return b;
    }

    // ================================================================== objets
    @SubscribeEvent
    public static void objets(RegistryEvent.Register<Item> e) {
        DonneesVirus.chargerJar();
        SERINGUE = objet(e, new ItemSeringue(), "seringue_vide", TAB);
        SERINGUE_PLEINE = objet(e, new ItemSeringuePleine(), "seringue_pleine", TAB);
        ORDONNANCE = objet(e, new ItemOrdonnance(), "ordonnance", TAB);
        CARNET = objet(e, new ItemCarnetConsultation(), "carnet_de_consultation", TAB);
        PARCHEMIN = objet(e, new ItemParchemin(), "parchemin_du_second_souffle", TAB);
        EN_COURS = objet(e, new ItemEnCoursVirus(), "preparation_en_cours_officine", null);
        RATEE = objet(e, new ItemPrepaRatee(), "preparation_ratee", TAB);
        for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) {
            if (ModRegistry.FOOD.containsKey(d.id) || ModRegistry.OBJETS_MONDE.containsKey(d.id)) {
                HxrpMetiers.LOG.warn("Virus : l'ingrédient {} existe déjà dans le Gourmet, ignoré", d.id);
                continue;
            }
            objet(e, new ItemIngredientVirus(d), d.id, TAB_INGREDIENTS);
        }
        for (Defs.Preparation p : DonneesVirus.preparationsDuJar().values()) {
            String id = p.produit();
            if (Objets.VIRUS.containsKey(id) || ModRegistry.FOOD.containsKey(id) || id.indexOf(':') >= 0) continue;
            objet(e, new ItemPreparation(id, pile(p.forme)), id, TAB_REMEDES);
        }
        for (Block b : BLOCS) {
            Item ib = new ItemBlock(b);
            ib.setRegistryName(b.getRegistryName());
            e.getRegistry().register(ib);
            OBJETS.add(ib);
        }
        Item lotus = new ItemLilyPad(Cueillette.LOTUS).setRegistryName(Cueillette.LOTUS.getRegistryName());
        lotus.setCreativeTab(TAB_INGREDIENTS);
        e.getRegistry().register(lotus);
        OBJETS.add(lotus);
    }

    private static int pile(String forme) {
        switch (forme == null ? "" : forme) {
            case "pilules": return 32;
            case "attelle": return 8;
            case "base": case "ingredient": return 64;
            default: return 16;
        }
    }

    public static Item objet(RegistryEvent.Register<Item> e, Item i, String id, CreativeTabs tab) {
        if (i.getRegistryName() == null) i.setRegistryName(HxrpMetiers.MODID, id);
        if (i.getTranslationKey().equals("item.null")) i.setTranslationKey(HxrpMetiers.MODID + "." + id);
        i.setCreativeTab(tab);
        e.getRegistry().register(i);
        OBJETS.add(i);
        Objets.VIRUS.put(id, i);
        return i;
    }

    @SubscribeEvent
    public static void sons(RegistryEvent.Register<SoundEvent> e) {
        VirusSons.enregistrer(e.getRegistry());
    }
}
