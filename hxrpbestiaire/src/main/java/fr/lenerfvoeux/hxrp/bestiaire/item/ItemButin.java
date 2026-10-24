package fr.lenerfvoeux.hxrp.bestiaire.item;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Un butin de boss : il servira aux métiers (Hunter Virus, Gourmet) ; les recettes viendront dans une prochaine passe. */
public class ItemButin extends Item {
    private final String boss, usage;

    public ItemButin(String boss, String usage) {
        this.boss = boss;
        this.usage = usage;
        setMaxStackSize(64);
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.RARE;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        Espece e = Especes.boss(boss);
        tip.add("§6Butin : " + (e != null ? e.nom : boss));
        tip.add("§7Usage : " + usage + " §8(recettes à venir)");
    }
}
