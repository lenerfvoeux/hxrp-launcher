package fr.lenerfvoeux.hxrp.metiers.block;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class ContainerPoubelle extends Container {
    public final InventoryBasic bin = new InventoryBasic("container.hxrpmetiers.poubelle", false, 9);
    private final BlockPos pos;

    public ContainerPoubelle(InventoryPlayer pi, BlockPos pos) {
        this.pos = pos;
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(bin, c, 8 + c * 18, 18));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c + r * 9 + 9, 8 + c * 18, 49 + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c, 8 + c * 18, 107));
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return p.getDistanceSq(pos.add(0.5, 0.5, 0.5)) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        ItemStack s = slot.getStack();
        ItemStack copy = s.copy();
        if (index < 9) { if (!mergeItemStack(s, 9, 45, true)) return ItemStack.EMPTY; }
        else if (!mergeItemStack(s, 0, 9, false)) return ItemStack.EMPTY;
        if (s.isEmpty()) slot.putStack(ItemStack.EMPTY); else slot.onSlotChanged();
        return copy;
    }

    @Override
    public void onContainerClosed(EntityPlayer p) {
        super.onContainerClosed(p);
        bin.clear(); // tout ce qui a été jeté disparaît
    }
}
