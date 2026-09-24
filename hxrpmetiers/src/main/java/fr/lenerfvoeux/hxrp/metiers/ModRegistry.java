package fr.lenerfvoeux.hxrp.metiers;

import fr.lenerfvoeux.hxrp.metiers.block.BlockFrigo;
import fr.lenerfvoeux.hxrp.metiers.block.BlockPoubelle;
import fr.lenerfvoeux.hxrp.metiers.block.BlockStation;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.item.ItemEnCours;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.item.ItemBouteille;
import fr.lenerfvoeux.hxrp.metiers.item.ItemIngredient;
import fr.lenerfvoeux.hxrp.metiers.item.ItemPlat;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = HxrpMetiers.MODID)
public final class ModRegistry {
    public static final Map<String, Item> FOOD = new LinkedHashMap<>();
    public static final List<Item> ALL_ITEMS = new ArrayList<>();
    public static Block FRIGO, POUBELLE;
    public static final java.util.Map<Station, Block> STATIONS = new java.util.EnumMap<>(Station.class);
    public static Item EN_COURS;

    public static final CreativeTabs TAB_INGREDIENTS = tab("ingredients", "carotte");
    public static final CreativeTabs TAB_CUISINE = tab("cuisine", "farine");
    public static final CreativeTabs TAB_PLATS = tab("plats", "burger_classique");
    public static final CreativeTabs TAB_BOISSONS = tab("boissons", "jus_d_orange");

    private ModRegistry() {}

    private static CreativeTabs tab(String name, String icon) {
        return new CreativeTabs(HxrpMetiers.MODID + "." + name) {
            @Override
            public ItemStack createIcon() {
                Item i = ModRegistry.FOOD.get(icon);
                return i == null ? ItemStack.EMPTY : new ItemStack(i);
            }
        };
    }

    @SubscribeEvent
    public static void blocks(RegistryEvent.Register<Block> e) {
        FRIGO = new BlockFrigo();
        POUBELLE = new BlockPoubelle();
        e.getRegistry().registerAll(FRIGO, POUBELLE);
        for (Station s : Station.values()) {
            Block b = new BlockStation(s);
            STATIONS.put(s, b);
            e.getRegistry().register(b);
        }
    }

    @SubscribeEvent
    public static void items(RegistryEvent.Register<Item> e) {
        FoodDatabase.load();
        for (FoodEntry f : FoodDatabase.INGREDIENTS) add(e, f.isWaterBottle() ? new ItemBouteille(f) : new ItemIngredient(f), f, TAB_INGREDIENTS);
        for (FoodEntry f : FoodDatabase.EPICES) add(e, new ItemIngredient(f), f, TAB_CUISINE);
        for (FoodEntry f : FoodDatabase.PREPARATIONS) add(e, new ItemIngredient(f), f, TAB_CUISINE);
        for (FoodEntry f : FoodDatabase.PLATS) add(e, new ItemPlat(f), f, TAB_PLATS);
        for (FoodEntry f : FoodDatabase.BOISSONS) add(e, new ItemPlat(f), f, TAB_BOISSONS);
        EN_COURS = new ItemEnCours();
        e.getRegistry().register(EN_COURS);
        ALL_ITEMS.add(EN_COURS);
        java.util.List<Block> blocs = new java.util.ArrayList<>(java.util.Arrays.asList(FRIGO, POUBELLE));
        blocs.addAll(STATIONS.values());
        for (Block b : blocs) {
            Item ib = new ItemBlock(b).setRegistryName(b.getRegistryName());
            e.getRegistry().register(ib);
            ALL_ITEMS.add(ib);
        }
    }

    private static void add(RegistryEvent.Register<Item> e, Item item, FoodEntry f, CreativeTabs tab) {
        item.setCreativeTab(tab);
        e.getRegistry().register(item);
        FOOD.put(f.id, item);
        ALL_ITEMS.add(item);
    }
}
