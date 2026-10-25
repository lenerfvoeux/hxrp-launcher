package fr.lenerfvoeux.hxrp.bestiaire;

import fr.lenerfvoeux.hxrp.bestiaire.bloc.BlockAppat;
import fr.lenerfvoeux.hxrp.bestiaire.bloc.BlockGrainePiege;
import fr.lenerfvoeux.hxrp.bestiaire.bloc.BlockToile;
import fr.lenerfvoeux.hxrp.bestiaire.effet.PotionGivre;
import fr.lenerfvoeux.hxrp.bestiaire.bloc.TileAppat;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.item.ItemAppat;
import fr.lenerfvoeux.hxrp.bestiaire.item.ItemButin;
import fr.lenerfvoeux.hxrp.bestiaire.item.ItemOeufBoss;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocs (appâts), objets (appâts, œufs, butins, projectiles), entités, sons et l'onglet « HxRP · Bestiaire »,
 * qui range les appâts, puis les œufs, puis les butins. Les butins sont enregistrés ici ; hxrpmetiers les
 * retrouvera par leur identifiant (hxrpbestiaire:dard_de_reine…) pour ses recettes.
 */
@Mod.EventBusSubscriber(modid = HxrpBestiaire.MODID)
public final class Registre {
    /** Butins : identifiant (onglet Butins du classeur) → nom, boss, usage prévu. */
    public static final String[][] BUTINS = {
            {"dard_de_reine", "Dard de reine", "reine_guepe", "Hunter Virus"},
            {"gelee_royale_noire", "Gelée royale noire", "reine_guepe", "Gourmet"},
            {"miel_noir", "Miel noir", "reine_guepe", "Gourmet"},
            {"chair_d_abomination", "Chair d'abomination", "abomination", "Hunter Virus : échantillon de pathogène"},
            {"poussiere_d_os_maudite", "Poussière d'os maudite", "necromancien", "Hunter Virus"},
            {"soie_de_xerra", "Soie de Xerra", "xerra", "Hunter Virus : bandages"},
            {"coeur_de_givre_eternel", "Cœur de givre éternel", "roi_des_glaces", "Hunter Virus"},
            {"glace_eternelle", "Glace éternelle", "roi_des_glaces", "Gourmet"},
            {"corne_du_demon_noir", "Corne du démon noir", "demon_noir", "Hunter Virus"},
    };

    /** Blocs posés par les boss (toiles de Xerra, graines-pièges) et l'effet « sol gelé ». */
    public static Block TOILE, GRAINE;
    public static net.minecraft.potion.Potion GIVRE;

    private static final Map<String, Item> OBJETS_BUTIN = new LinkedHashMap<>();
    private static final Map<String, Item> OEUFS = new LinkedHashMap<>();
    private static final Map<String, Block> APPATS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS_APPAT = new LinkedHashMap<>();
    public static final Map<EntityProjectileBoss.Type, Item> PROJECTILES = new LinkedHashMap<>();

    public static final CreativeTabs ONGLET = new CreativeTabs(HxrpBestiaire.MODID) {
        @Override
        public ItemStack createIcon() {
            Item i = OEUFS.get("reine_guepe");
            return i == null ? ItemStack.EMPTY : new ItemStack(i);
        }

        @Override
        public void displayAllRelevantItems(NonNullList<ItemStack> l) {
            for (Item i : ITEMS_APPAT.values()) l.add(new ItemStack(i));
            for (Item i : OEUFS.values()) l.add(new ItemStack(i));
            for (Item i : OBJETS_BUTIN.values()) l.add(new ItemStack(i));
        }
    };

    private Registre() {}

    public static Item butin(String id) {
        return OBJETS_BUTIN.get(id);
    }

    public static Item oeuf(String boss) {
        return OEUFS.get(boss);
    }

    public static Item appat(String boss) {
        Espece e = Especes.boss(boss);
        return e == null ? null : ITEMS_APPAT.get(e.appat);
    }

    public static boolean estAppat(Block b) {
        return b instanceof BlockAppat;
    }

    /** Tous les objets du mod (pour les modèles du client). */
    public static List<Item> objets() {
        List<Item> l = new ArrayList<>();
        l.addAll(ITEMS_APPAT.values());
        l.addAll(OEUFS.values());
        l.addAll(OBJETS_BUTIN.values());
        l.addAll(PROJECTILES.values());
        return l;
    }

    @SubscribeEvent
    public static void blocs(RegistryEvent.Register<Block> e) {
        for (Espece s : Especes.BOSS) {
            if (s.appat.isEmpty()) continue;
            Block b = new BlockAppat(s).setRegistryName(HxrpBestiaire.MODID, s.appat)
                    .setTranslationKey(HxrpBestiaire.MODID + "." + s.appat).setCreativeTab(ONGLET);
            APPATS.put(s.appat, b);
            e.getRegistry().register(b);
        }
        GameRegistry.registerTileEntity(TileAppat.class, new ResourceLocation(HxrpBestiaire.MODID, "appat"));
        TOILE = new BlockToile().setRegistryName(HxrpBestiaire.MODID, "toile_de_xerra").setTranslationKey(HxrpBestiaire.MODID + ".toile_de_xerra");
        GRAINE = new BlockGrainePiege().setRegistryName(HxrpBestiaire.MODID, "graine_piege").setTranslationKey(HxrpBestiaire.MODID + ".graine_piege");
        e.getRegistry().register(TOILE);
        e.getRegistry().register(GRAINE);
    }

    @SubscribeEvent
    public static void effets(RegistryEvent.Register<net.minecraft.potion.Potion> e) {
        GIVRE = new PotionGivre();
        e.getRegistry().register(GIVRE);
    }

    /** Les toiles brûlent (le feu libère l'arène de Xerra). Appelé à l'initialisation. */
    static void feu() {
        net.minecraft.init.Blocks.FIRE.setFireInfo(TOILE, 60, 100);
        net.minecraft.init.Blocks.FIRE.setFireInfo(GRAINE, 60, 100);
    }

    @SubscribeEvent
    public static void objets(RegistryEvent.Register<Item> e) {
        for (Map.Entry<String, Block> a : APPATS.entrySet()) {
            Item i = new ItemAppat(a.getValue()).setRegistryName(a.getValue().getRegistryName());
            ITEMS_APPAT.put(a.getKey(), i);
            e.getRegistry().register(i);
        }
        for (Espece s : Especes.BOSS) {
            String id = "oeuf_" + s.id;
            Item i = new ItemOeufBoss(s).setRegistryName(HxrpBestiaire.MODID, id).setTranslationKey(HxrpBestiaire.MODID + "." + id)
                    .setCreativeTab(ONGLET);
            OEUFS.put(s.id, i);
            e.getRegistry().register(i);
        }
        for (String[] b : BUTINS) {
            Item i = new ItemButin(b[2], b[3]).setRegistryName(HxrpBestiaire.MODID, b[0]).setTranslationKey(HxrpBestiaire.MODID + "." + b[0])
                    .setCreativeTab(ONGLET);
            OBJETS_BUTIN.put(b[0], i);
            e.getRegistry().register(i);
        }
        for (EntityProjectileBoss.Type t : EntityProjectileBoss.Type.values()) {
            String id = "projectile_" + t.name().toLowerCase(java.util.Locale.ROOT);
            Item i = new Item().setRegistryName(HxrpBestiaire.MODID, id).setTranslationKey(HxrpBestiaire.MODID + "." + id);
            PROJECTILES.put(t, i);
            e.getRegistry().register(i);
        }
    }

    @SubscribeEvent
    public static void entites(RegistryEvent.Register<EntityEntry> e) {
        for (Espece s : Especes.TOUTES)
            e.getRegistry().register(EntityEntryBuilder.create().entity(s.classe).id(new ResourceLocation(HxrpBestiaire.MODID, s.id), s.numero)
                    .name(HxrpBestiaire.MODID + "." + s.id).tracker(s.boss ? 160 : 80, s.boss ? 1 : 2, true).build());
        e.getRegistry().register(EntityEntryBuilder.create().entity(EntityProjectileBoss.class)
                .id(new ResourceLocation(HxrpBestiaire.MODID, "projectile"), 50).name(HxrpBestiaire.MODID + ".projectile")
                .tracker(80, 1, true).build());
    }

    @SubscribeEvent
    public static void sons(RegistryEvent.Register<SoundEvent> e) {
        Sons.enregistrer(e.getRegistry());
    }
}
