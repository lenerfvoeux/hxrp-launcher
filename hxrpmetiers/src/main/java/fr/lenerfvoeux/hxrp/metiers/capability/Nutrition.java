package fr.lenerfvoeux.hxrp.metiers.capability;

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

public final class Nutrition {
    @CapabilityInject(NutritionData.class)
    public static Capability<NutritionData> CAP = null;
    public static final ResourceLocation KEY = new ResourceLocation(HxrpMetiers.MODID, "nutrition");

    private Nutrition() {}

    public static void register() {
        CapabilityManager.INSTANCE.register(NutritionData.class, new Capability.IStorage<NutritionData>() {
            @Nullable @Override
            public NBTBase writeNBT(Capability<NutritionData> c, NutritionData d, EnumFacing s) { return d.write(); }
            @Override
            public void readNBT(Capability<NutritionData> c, NutritionData d, EnumFacing s, NBTBase n) {
                if (n instanceof NBTTagCompound) d.read((NBTTagCompound) n);
            }
        }, NutritionData::new);
    }

    @Nullable
    public static NutritionData get(EntityPlayer p) {
        return p == null || CAP == null ? null : p.getCapability(CAP, null);
    }

    public static class Provider implements ICapabilitySerializable<NBTTagCompound> {
        private final NutritionData data = new NutritionData();
        @Override public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f) { return c == CAP; }
        @Nullable @Override public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f) { return c == CAP ? CAP.cast(data) : null; }
        @Override public NBTTagCompound serializeNBT() { return data.write(); }
        @Override public void deserializeNBT(NBTTagCompound n) { data.read(n); }
    }
}
