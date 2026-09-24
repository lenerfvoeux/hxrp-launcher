package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;

/** Frigo : 27 emplacements, la nourriture y pourrit 3 fois moins vite. */
public class TileFrigo extends TileEntity {
    public final ItemStackHandler inv = new ItemStackHandler(27) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) return stack;
            ItemStack conv = Fraicheur.toFridge(stack.copy(), System.currentTimeMillis());
            ItemStack rem = super.insertItem(slot, conv, simulate);
            return rem.isEmpty() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, rem.getCount());
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack ex = super.extractItem(slot, amount, simulate);
            return ex.isEmpty() ? ex : Fraicheur.fromFridge(ex.copy(), System.currentTimeMillis());
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            super.setStackInSlot(slot, stack.isEmpty() ? stack : Fraicheur.toFridge(stack.copy(), System.currentTimeMillis()));
        }

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
