package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.item.ItemIngredient;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.Cueillette;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Un ingrédient du Hunter Virus : se range au frigo et périt comme ceux du Gourmet (même fraîcheur en temps réel).
 * Les plantes et champignons se replantent : clic droit sur le bon sol pour reposer la plante.
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

    @Override
    public EnumActionResult onItemUse(EntityPlayer p, World w, BlockPos pos, EnumHand hand, EnumFacing face, float x, float y, float z) {
        return Cueillette.replanter(p, w, pos, hand, face, def) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        super.addInformation(s, w, tip, flag);
        String r = def.rarete == null ? "" : def.rarete;
        TextFormatting c = r.startsWith("légendaire") ? TextFormatting.GOLD : r.startsWith("très rare") ? TextFormatting.LIGHT_PURPLE
                : r.startsWith("rare") ? TextFormatting.AQUA : r.startsWith("peu") ? TextFormatting.GREEN : TextFormatting.DARK_GRAY;
        tip.add(c + "Rareté : " + r);
        if (Cueillette.replantable(def)) tip.add(TextFormatting.DARK_GRAY + "Clic droit sur " + Cueillette.libelleSol(def) + " : replanter");
    }
}
