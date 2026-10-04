package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

/**
 * Meuble à tiroirs : 54 cases, plus le tiroir des seringues stériles (case qui se remplit toute seule ;
 * on peut y reposer une seringue vide).
 */
public class ContainerMeuble extends Container {
    public static final int Y_TIROIRS = 18, Y_SERINGUES = 132, Y_INVENTAIRE = 164, Y_BARRE = 222;
    public final TileMeuble te;

    /** Le tiroir sans fond : il rend toujours une seringue vide, et avale celles qu'on y repose. */
    public static class Seringues implements IItemHandler {
        @Override public int getSlots() { return 1; }

        @Nonnull @Override
        public ItemStack getStackInSlot(int slot) { return VirusRegistre.SERINGUE == null ? ItemStack.EMPTY : new ItemStack(VirusRegistre.SERINGUE); }

        @Nonnull @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return stack.getItem() == VirusRegistre.SERINGUE ? ItemStack.EMPTY : stack;
        }

        @Nonnull @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return amount <= 0 || VirusRegistre.SERINGUE == null ? ItemStack.EMPTY : new ItemStack(VirusRegistre.SERINGUE, Math.min(amount, 16));
        }

        @Override public int getSlotLimit(int slot) { return 1; }
    }

    public static class SlotSeringues extends SlotItemHandler {
        public SlotSeringues(IItemHandler h, int x, int y) { super(h, 0, x, y); }
        @Override public boolean isItemValid(@Nonnull ItemStack stack) { return stack.getItem() == VirusRegistre.SERINGUE; }
        @Override public void putStack(@Nonnull ItemStack stack) {}
        @Override public void onSlotChanged() {}
    }

    public ContainerMeuble(InventoryPlayer pi, TileMeuble te) {
        this.te = te;
        for (int r = 0; r < 6; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new SlotItemHandler(te.inv, c + r * 9, 8 + c * 18, Y_TIROIRS + r * 18));
        addSlotToContainer(new SlotSeringues(new Seringues(), 8, Y_SERINGUES));
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c + r * 9 + 9, 8 + c * 18, Y_INVENTAIRE + r * 18));
        for (int c = 0; c < 9; c++) addSlotToContainer(new Slot(pi, c, 8 + c * 18, Y_BARRE));
    }

    @Override
    public boolean canInteractWith(EntityPlayer p) {
        return !te.isInvalid() && p.getDistanceSq(te.getPos().add(0.5, 0.5, 0.5)) <= 64;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer p, int index) {
        Slot slot = inventorySlots.get(index);
        if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;
        final int tiroirs = TileMeuble.CASES, seringues = tiroirs, debutJoueur = tiroirs + 1, fin = debutJoueur + 36;
        if (index == seringues) {
            ItemStack s = new ItemStack(VirusRegistre.SERINGUE);
            mergeItemStack(s, debutJoueur, fin, true);
            return ItemStack.EMPTY;
        }
        ItemStack s = slot.getStack().copy();
        if (index < tiroirs) {
            if (!mergeItemStack(s, debutJoueur, fin, true)) return ItemStack.EMPTY;
        } else {
            if (s.getItem() == VirusRegistre.SERINGUE) {
                slot.putStack(ItemStack.EMPTY);
                return ItemStack.EMPTY;
            }
            if (!mergeItemStack(s, 0, tiroirs, false)) return ItemStack.EMPTY;
        }
        slot.putStack(s.isEmpty() ? ItemStack.EMPTY : s);
        return ItemStack.EMPTY;
    }
}
