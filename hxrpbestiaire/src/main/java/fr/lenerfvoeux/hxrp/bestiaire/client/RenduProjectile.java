package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Projectile dessiné comme un objet plat tourné vers la caméra, avec l'icône et la taille de son type. */
public class RenduProjectile extends RenderSnowball<EntityProjectileBoss> {
    public RenduProjectile(RenderManager rm) {
        super(rm, Items.ARROW, Minecraft.getMinecraft().getRenderItem());
    }

    @Override
    public void doRender(EntityProjectileBoss e, double x, double y, double z, float lacet, float pt) {
        float k = e.type().taille;
        if (k == 1F) {
            super.doRender(e, x, y, z, lacet, pt);
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.scale(k, k, k);
        super.doRender(e, 0, 0, 0, lacet, pt);
        GlStateManager.popMatrix();
    }

    @Override
    public ItemStack getStackToRender(EntityProjectileBoss e) {
        Item i = Registre.PROJECTILES.get(e.type());
        return new ItemStack(i != null ? i : Items.ARROW);
    }
}
