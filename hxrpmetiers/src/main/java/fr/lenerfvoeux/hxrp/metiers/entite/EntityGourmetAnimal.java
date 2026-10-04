package fr.lenerfvoeux.hxrp.metiers.entite;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Base des animaux terrestres du Gourmet : santé, vitesse, ingrédient donné à la mort. */
public abstract class EntityGourmetAnimal extends EntityAnimal {
    private final String ingredient;
    private final int min, max;

    protected EntityGourmetAnimal(World w, String ingredient, float largeur, float hauteur, double sante, double vitesse, int min, int max) {
        super(w);
        this.ingredient = ingredient;
        this.min = min;
        this.max = max;
        setSize(largeur, hauteur);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(sante);
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(vitesse);
        setHealth((float) sante);
    }

    @Override
    protected void dropFewItems(boolean touche, int butin) {
        if (ingredient == null || isChild()) return;
        Item i = ModRegistry.FOOD.get(ingredient);
        if (i != null && max > 0) entityDropItem(new ItemStack(i, min + rand.nextInt(max - min + 1 + butin)), 0.0f);
        extras(butin);
    }

    /** Butin en plus (cuir, plumes…). */
    protected void extras(int butin) {}
}
