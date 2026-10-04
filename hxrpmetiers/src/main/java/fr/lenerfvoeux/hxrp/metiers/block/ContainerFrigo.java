package fr.lenerfvoeux.hxrp.metiers.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerFrigo extends Container {
    public final TileFrigo te;

    /** Emplacement du frigo (ignoré par le scan d'inventaire). */
    public static class SlotFrigo extends SlotItemHandler {
        public SlotFrigo(IItemHandler h, int i, int x, int y) { super(h, i, x, y); }
    }

    public ContainerFrigo(InventoryPlayer pi, TileFrigo te) {
        this.te = te;
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new SlotFrigo(te.inv, c + r * 9, 8 + c * 18, 18 + r * 18));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c + r * 9 + 9, 8 + c * 18, 85 + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c, 8 + c * 18, 143));
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return !te.isInvalid() && p.getDistanceSq(te.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        if (index < 27) {
            ItemStack preview = te.inv.extractItem(index, 64, true);
            ItemStack moving = preview.copy();
            if (!mergeItemStack(moving, 27, 63, true)) return ItemStack.EMPTY;
            int moved = preview.getCount() - moving.getCount();
            if (moved > 0) te.inv.extractItem(index, moved, false);
        } else {
            ItemStack s = slot.getStack().copy();
            for (int i = 0; i < 27 && !s.isEmpty(); i++) s = te.inv.insertItem(i, s, false);
            slot.putStack(s);
        }
        return ItemStack.EMPTY;
    }
}
