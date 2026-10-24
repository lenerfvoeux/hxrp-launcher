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
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPioche;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPrepaRatee;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemSeringue;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemSeringuePleine;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockChampiTronc;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockLotus;
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
    /** Blocs du Virus qui ont un objet (machines, meuble, sols, plantes et champignons de tronc pour les admins…). */
    public static final List<Block> BLOCS = new ArrayList<>();
    /** Blocs du Virus sans objet propre (troncs écorcés, lotus qui a son objet nénuphar). */
    public static final List<Block> BLOCS_MONDE = new ArrayList<>();
    public static final Map<Machine, Block> MACHINES = new EnumMap<>(Machine.class);

    public static Item SERINGUE, SERINGUE_PLEINE, ORDONNANCE, CARNET, PARCHEMIN, EN_COURS, RATEE, PIOCHE;
    public static Block MEUBLE, MICROSCOPE, GRIMOIRE, PRESENTOIR;

    private VirusRegistre() {}

    // ================================================================== blocs
    @SubscribeEvent
    public static void blocs(RegistryEvent.Register<Block> e) {
        DonneesVirus.chargerJar();
        MEUBLE = bloc(e, new BlockMeuble(), OngletsVirus.OFFICINE);
        for (Machine m : Machine.values()) MACHINES.put(m, bloc(e, m == Machine.JARRES ? new BlockJarres() : new BlockMachine(m), OngletsVirus.OFFICINE));
        MICROSCOPE = bloc(e, new BlockMicroscope(), OngletsVirus.OFFICINE);
        GRIMOIRE = bloc(e, new BlockGrimoire(), OngletsVirus.OFFICINE);
        PRESENTOIR = bloc(e, new BlockPresentoir(), OngletsVirus.OFFICINE);
        // monde
        for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) {
            switch (d.recolte.type) {
                case "plante": {
                    BlockPlanteVirus b = new BlockPlanteVirus(d);
                    Cueillette.PLANTES.put(d.id, b);
                    bloc(e, b, OngletsVirus.MONDE);
                    break;
                }
                case "tronc": {
                    BlockChampiTronc b = new BlockChampiTronc(d);
                    Cueillette.TRONCS.put(d.id, b);
                    bloc(e, b, OngletsVirus.MONDE);
                    break;
                }
                case "sol": {
                    if (d.recolte.bloc == null || d.recolte.bloc.isEmpty() || Cueillette.SOLS.containsKey(d.recolte.bloc)) break;
                    BlockSolVirus b = new BlockSolVirus(d);
                    Cueillette.SOLS.put(d.recolte.bloc, b);
                    bloc(e, b, OngletsVirus.MONDE);
                    break;
                }
                default: break;
            }
        }
        Cueillette.ECORCE_BOULEAU = (BlockTroncEcorce) monde(e, new BlockTroncEcorce(true));
        Cueillette.ECORCE_SAULE = (BlockTroncEcorce) monde(e, new BlockTroncEcorce(false));
        Cueillette.LOTUS = (BlockLotus) monde(e, new BlockLotus());
        Cueillette.LOTUS.setCreativeTab(OngletsVirus.MONDE);
        Cueillette.SEVE = (BlockSourceSeve) bloc(e, new BlockSourceSeve(), OngletsVirus.MONDE);
    }

    private static Block bloc(RegistryEvent.Register<Block> e, Block b, CreativeTabs onglet) {
        b.setCreativeTab(onglet);
        e.getRegistry().register(b);
        BLOCS.add(b);
        return b;
    }

    private static Block monde(RegistryEvent.Register<Block> e, Block b) {
        b.setCreativeTab(null);
        e.getRegistry().register(b);
        BLOCS_MONDE.add(b);
        return b;
    }

    // ================================================================== objets
    @SubscribeEvent
    public static void objets(RegistryEvent.Register<Item> e) {
        DonneesVirus.chargerJar();
        SERINGUE = objet(e, new ItemSeringue(), "seringue_vide", OngletsVirus.OFFICINE);
        SERINGUE_PLEINE = objet(e, new ItemSeringuePleine(), "seringue_pleine", OngletsVirus.OFFICINE);
        ORDONNANCE = objet(e, new ItemOrdonnance(), "ordonnance", OngletsVirus.OFFICINE);
        CARNET = objet(e, new ItemCarnetConsultation(), "carnet_de_consultation", OngletsVirus.OFFICINE);
        PARCHEMIN = objet(e, new ItemParchemin(), "parchemin_du_second_souffle", OngletsVirus.OFFICINE);
        EN_COURS = objet(e, new ItemEnCoursVirus(), "preparation_en_cours_officine", null);
        RATEE = objet(e, new ItemPrepaRatee(), "preparation_ratee", OngletsVirus.OFFICINE);
        PIOCHE = objet(e, new ItemPioche(), "pioche_de_prospecteur", OngletsVirus.OFFICINE);
        for (Defs.Ingredient d : DonneesVirus.ingredientsDuJar().values()) {
            if (ModRegistry.FOOD.containsKey(d.id) || ModRegistry.OBJETS_MONDE.containsKey(d.id)) {
                HxrpMetiers.LOG.warn("Virus : l'ingrédient {} existe déjà dans le Gourmet, ignoré", d.id);
                continue;
            }
            boolean cueilli = "plante".equals(d.categorie) || "champignon".equals(d.categorie);
            objet(e, new ItemIngredientVirus(d), d.id, cueilli ? OngletsVirus.CUEILLETTE : OngletsVirus.RESSOURCES);
        }
        for (Defs.Preparation p : DonneesVirus.preparationsDuJar().values()) {
            String id = p.produit();
            if (Objets.VIRUS.containsKey(id) || ModRegistry.FOOD.containsKey(id) || id.indexOf(':') >= 0) continue;
            boolean preparation = (("base".equals(p.forme) || "ingredient".equals(p.forme)) && !p.secret) || (p.soin != null && !p.soin.isEmpty());
            objet(e, new ItemPreparation(id, pile(p.forme)), id, preparation ? OngletsVirus.PREPARATIONS : OngletsVirus.REMEDES);
        }
        for (Block b : BLOCS) {
            Item ib = new ItemBlock(b);
            ib.setRegistryName(b.getRegistryName());
            e.getRegistry().register(ib);
            OBJETS.add(ib);
        }
        Item lotus = new ItemLilyPad(Cueillette.LOTUS).setRegistryName(Cueillette.LOTUS.getRegistryName());
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
