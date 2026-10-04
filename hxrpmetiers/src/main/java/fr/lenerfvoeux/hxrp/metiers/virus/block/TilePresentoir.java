package fr.lenerfvoeux.hxrp.metiers.virus.block;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

/** Les quatre objets exposés sur le présentoir (synchronisés avec les clients pour l'affichage). */
public class TilePresentoir extends TileEntity {
    public static final int PLACES = 4;
    private final ItemStack[] objets = new ItemStack[PLACES];

    public TilePresentoir() {
        for (int i = 0; i < PLACES; i++) objets[i] = ItemStack.EMPTY;
    }

    public ItemStack objet(int k) { return objets[k]; }

    public void poser(int k, ItemStack s) {
        objets[k] = s;
        changer();
    }

    public ItemStack prendre(int k) {
        ItemStack s = objets[k];
        objets[k] = ItemStack.EMPTY;
        changer();
        return s;
    }

    private void changer() {
        markDirty();
        if (world != null && !world.isRemote) {
            IBlockState st = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, st, st, 3);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound t) {
        super.writeToNBT(t);
        for (int i = 0; i < PLACES; i++) if (!objets[i].isEmpty()) t.setTag("o" + i, objets[i].writeToNBT(new NBTTagCompound()));
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        for (int i = 0; i < PLACES; i++) objets[i] = t.hasKey("o" + i) ? new ItemStack(t.getCompoundTag("o" + i)) : ItemStack.EMPTY;
    }

    @Override public NBTTagCompound getUpdateTag() { return writeToNBT(new NBTTagCompound()); }
    @Override public SPacketUpdateTileEntity getUpdatePacket() { return new SPacketUpdateTileEntity(pos, 0, getUpdateTag()); }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(pos, pos.add(1, 1, 1));
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World w, net.minecraft.util.math.BlockPos p, IBlockState ancien, IBlockState nouveau) {
        return ancien.getBlock() != nouveau.getBlock();
    }
}
