package fr.lenerfvoeux.hxrp.metiers.virus.block;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/** Les 54 tiroirs du meuble de l'apothicaire. */
public class TileMeuble extends TileEntity {
    public static final int CASES = 54;

    public final ItemStackHandler inv = new ItemStackHandler(CASES) {
        @Override
        protected void onContentsChanged(int slot) { markDirty(); }
    };

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        t.setTag("inv", inv.serializeNBT());
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        inv.deserializeNBT(t.getCompoundTag("inv"));
    }

    @Override
    public boolean hasCapability(Capability<?> c, @Nullable EnumFacing f) {
        return c == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(c, f);
    }

    @Nullable @Override
    public <T> T getCapability(Capability<T> c, @Nullable EnumFacing f) {
        return c == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(inv) : super.getCapability(c, f);
    }
}
