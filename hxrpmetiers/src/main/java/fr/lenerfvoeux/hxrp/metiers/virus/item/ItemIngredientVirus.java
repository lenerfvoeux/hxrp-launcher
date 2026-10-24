package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.item.ItemIngredient;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Un ingrédient du Hunter Virus : se range au frigo et périt comme ceux du Gourmet (même fraîcheur en temps réel).
 * Il ne se replante pas : les plantes et champignons ne viennent que des blocs du monde.
 */
public class ItemIngredientVirus extends ItemIngredient {
    public final Defs.Ingredient def;

    public ItemIngredientVirus(Defs.Ingredient d) {
        super(entree(d));
        this.def = d;
    }

    private static FoodEntry entree(Defs.Ingredient d) {
        FoodEntry e = new FoodEntry();
        e.id = d.id;
        e.name = d.nom;
        e.cat = categorie(d.categorie);
        e.source = d.source;
        e.life = Math.max(0, d.vie);
        e.kind = "ingredient";
        return e;
    }

    public static String categorie(String c) {
        switch (c == null ? "" : c) {
            case "plante": return "Plante médicinale";
            case "champignon": return "Champignon";
            case "rare_hunter": return "Ressource rare";
            case "animal_ruche": return "Ressource animale";
            default: return "Ingrédient d'officine";
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        super.addInformation(s, w, tip, flag);
        String r = def.rarete == null ? "" : def.rarete;
        TextFormatting c = r.startsWith("légendaire") ? TextFormatting.GOLD : r.startsWith("très rare") ? TextFormatting.LIGHT_PURPLE
                : r.startsWith("rare") ? TextFormatting.AQUA : r.startsWith("peu") ? TextFormatting.GREEN : TextFormatting.DARK_GRAY;
        tip.add(c + "Rareté : " + r);
    }
}
