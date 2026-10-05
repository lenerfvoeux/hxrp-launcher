package fr.lenerfvoeux.hxrp.phenix.entite;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Plume de phénix posée au sol : le feu, la lave et les explosions ne la détruisent pas, et elle rougeoie. */
public class EntityPlumeObjet extends EntityItem {
    public EntityPlumeObjet(World w) {
        super(w);
        isImmuneToFire = true;
    }

    public EntityPlumeObjet(World w, double x, double y, double z, ItemStack s) {
        super(w, x, y, z, s);
        isImmuneToFire = true;
    }

    @Override
    public boolean attackEntityFrom(DamageSource src, float n) {
        if (src.isFireDamage() || src.isExplosion()) return false;
        return super.attackEntityFrom(src, n);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (world.isRemote && rand.nextInt(10) == 0)
            world.spawnParticle(EnumParticleTypes.REDSTONE, posX + (rand.nextDouble() - 0.5) * 0.3, posY + 0.35, posZ + (rand.nextDouble() - 0.5) * 0.3, 1.0, 0.55, 0.1);
    }

    @Override
    public boolean isBurning() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getBrightnessForRender() {
        return 0xF000F0;
    }
}
