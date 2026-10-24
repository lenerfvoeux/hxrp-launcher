package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Projectile dessiné comme un objet plat tourné vers la caméra, avec l'icône de son type. */
public class RenduProjectile extends RenderSnowball<EntityProjectileBoss> {
    public RenduProjectile(RenderManager rm) {
        super(rm, Items.ARROW, Minecraft.getMinecraft().getRenderItem());
    }

    @Override
    public ItemStack getStackToRender(EntityProjectileBoss e) {
        Item i = Registre.PROJECTILES.get(e.type());
        return new ItemStack(i != null ? i : Items.ARROW);
    }
}
