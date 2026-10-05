package fr.lenerfvoeux.hxrp.phenix.item;

import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeObjet;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Plume de phénix : ne brûle pas (ni feu ni lave), luit dans l'inventaire, reste une heure au sol.
 * Le Hunter Virus la réclame pour le Remède du Second Souffle (id « hxrpphenix:plume_de_phenix »).
 */
public class ItemPlumeDePhenix extends Item {
    public ItemPlumeDePhenix() {
        setMaxStackSize(16);
    }

    @Override
    public boolean hasEffect(ItemStack s) {
        return true;
    }

    @Override
    public EnumRarity getRarity(ItemStack s) {
        return EnumRarity.EPIC;
    }

    @Override
    public boolean hasCustomEntity(ItemStack s) {
        return true;
    }

    @Nullable
    @Override
    public Entity createEntity(World w, Entity lieu, ItemStack s) {
        EntityPlumeObjet e = new EntityPlumeObjet(w, lieu.posX, lieu.posY, lieu.posZ, s);
        e.motionX = lieu.motionX;
        e.motionY = lieu.motionY;
        e.motionZ = lieu.motionZ;
        if (lieu instanceof EntityItem) {
            e.setThrower(((EntityItem) lieu).getThrower());
            e.setOwner(((EntityItem) lieu).getOwner());
        }
        e.setPickupDelay(30);
        return e;
    }

    @Override
    public int getEntityLifespan(ItemStack s, World w) {
        return 20 * 60 * 60;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        tip.add("§6Tombée d'un phénix de feu.");
        tip.add("§7Elle ne brûle pas, et reste tiède au creux de la main.");
        tip.add("§8Ingrédient du Remède du Second Souffle.");
    }
}
