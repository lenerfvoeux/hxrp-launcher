package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nullable;

/** La capability de santé du Hunter Virus, attachée à chaque joueur. */
public final class Sante {
    @CapabilityInject(SanteData.class)
    public static Capability<SanteData> CAP = null;
    public static final ResourceLocation KEY = new ResourceLocation(HxrpMetiers.MODID, "sante");

    private Sante() {}

    public static void register() {
        CapabilityManager.INSTANCE.register(SanteData.class, new Capability.IStorage<SanteData>() {
            @Nullable @Override
            public NBTBase writeNBT(Capability<SanteData> c, SanteData d, EnumFacing s) { return d.write(); }
            @Override
            public void readNBT(Capability<SanteData> c, SanteData d, EnumFacing s, NBTBase n) {
                if (n instanceof NBTTagCompound) d.read((NBTTagCompound) n);
            }
        }, SanteData::new);
    }

    @Nullable
    public static SanteData get(EntityPlayer p) {
        return p == null || CAP == null ? null : p.getCapability(CAP, null);
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final SanteData data = new SanteData();
        @Override public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f) { return c == CAP; }
        @Nullable @Override public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f) { return c == CAP ? CAP.cast(data) : null; }
        @Override public NBTTagCompound serializeNBT() { return data.write(); }
        @Override public void deserializeNBT(NBTTagCompound n) { data.read(n); }
    }
}
