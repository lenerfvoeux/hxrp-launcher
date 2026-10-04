package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Retrouver un objet d'après l'identifiant utilisé dans les recettes du Virus : ingrédient ou préparation du Virus,
 * objet du Gourmet (riz, miel…), ou objet d'un autre mod (« hxrpphenix:plume_de_phenix »). « eau » = bouteille d'eau.
 */
public final class Objets {
    /** Objets du Virus par identifiant (ingrédients, préparations, outils). */
    public static final Map<String, Item> VIRUS = new LinkedHashMap<>();

    private Objets() {}

    public static Item item(String id) {
        if (id == null || id.isEmpty()) return null;
        if ("eau".equals(id)) id = "bouteille_d_eau";
        Item i = VIRUS.get(id);
        if (i != null) return i;
        i = ModRegistry.FOOD.get(id);
        if (i != null) return i;
        if (id.indexOf(':') > 0) {
            Item x = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            return x == null || x == net.minecraft.init.Items.AIR ? null : x;
        }
        i = ModRegistry.OBJETS_MONDE.get(id);
        return i;
    }

    /** Identifiant d'un objet au sens des recettes (chemin seul pour les objets de ce mod). */
    public static String id(ItemStack s) {
        if (s.isEmpty()) return "";
        ResourceLocation r = s.getItem().getRegistryName();
        if (r == null) return "";
        return HxrpMetiers.MODID.equals(r.getNamespace()) ? r.getPath() : r.toString();
    }

    public static ItemStack pile(String id) {
        Item i = item(id);
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    /** Nom affiché d'un identifiant de recette. */
    public static String nom(String id) {
        ItemStack s = pile(id);
        return s.isEmpty() ? id : s.getDisplayName();
    }
}
