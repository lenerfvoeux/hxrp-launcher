package fr.lenerfvoeux.hxrp.metiers.virus.block;

import fr.lenerfvoeux.hxrp.metiers.virus.officine.EnCoursVirus;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Trois jarres : une préparation en cours scellée dans chacune, prête à une heure réelle donnée. */
public class TileJarres extends TileEntity {
    public static final int JARRES = 3;
    private final ItemStack[] contenu = new ItemStack[JARRES];
    private final long[] pret = new long[JARRES];
    /** Côté client : nombre de jarres scellées reçu du serveur (-1 côté serveur). */
    private int vues = -1;

    public TileJarres() {
        for (int i = 0; i < JARRES; i++) contenu[i] = ItemStack.EMPTY;
    }

    public ItemStack contenu(int i) { return contenu[i]; }
    public long pret(int i) { return pret[i]; }

    public int scellees() {
        if (vues >= 0) return vues;
        int n = 0;
        for (ItemStack s : contenu) if (!s.isEmpty()) n++;
        return n;
    }

    public int libre() {
        for (int i = 0; i < JARRES; i++) if (contenu[i].isEmpty()) return i;
        return -1;
    }

    /** Scelle une préparation (dont l'étape « jarres » est déjà notée) jusqu'à l'heure dite. */
    public boolean sceller(ItemStack s, long jusqua) {
        int i = libre();
        if (i < 0) return false;
        contenu[i] = s.copy();
        pret[i] = jusqua;
        changer();
        return true;
    }

    /** Première jarre prête, ou -1. */
    public int prete(long now) {
        for (int i = 0; i < JARRES; i++) if (!contenu[i].isEmpty() && now >= pret[i]) return i;
        return -1;
    }

    public ItemStack ouvrir(int i) {
        ItemStack s = contenu[i];
        contenu[i] = ItemStack.EMPTY;
        pret[i] = 0;
        changer();
        return s;
    }

    /** Termine toutes les macérations tout de suite (commande d'administration). */
    public int finir() {
        int n = 0;
        for (int i = 0; i < JARRES; i++) if (!contenu[i].isEmpty() && pret[i] > 0) {
            pret[i] = 0;
            n++;
        }
        if (n > 0) changer();
        return n;
    }

    /**
     * Ce qui sort d'une jarre cassée : la préparation revient à l'étape des jarres (la note de cette étape est retirée),
     * sauf si la macération était finie.
     */
    List<ItemStack> viderPourCasse() {
        long now = System.currentTimeMillis();
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < JARRES; i++) {
            ItemStack s = contenu[i];
            if (s.isEmpty()) continue;
            if (now < pret[i]) EnCoursVirus.annulerDerniereNote(s);
            out.add(s);
            contenu[i] = ItemStack.EMPTY;
        }
        return out;
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
        for (int i = 0; i < JARRES; i++) {
            if (contenu[i].isEmpty()) continue;
            t.setTag("j" + i, contenu[i].writeToNBT(new NBTTagCompound()));
            t.setLong("p" + i, pret[i]);
        }
        return t;
    }

    @Override
    public void readFromNBT(NBTTagCompound t) {
        super.readFromNBT(t);
        for (int i = 0; i < JARRES; i++) {
            contenu[i] = t.hasKey("j" + i) ? new ItemStack(t.getCompoundTag("j" + i)) : ItemStack.EMPTY;
            pret[i] = t.getLong("p" + i);
        }
    }

    // le client n'a besoin que du nombre de jarres scellées (pour le modèle)
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound t = super.getUpdateTag();
        t.setByte("n", (byte) scellees());
        return t;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound t) {
        super.readFromNBT(t);
        vues = Math.max(0, Math.min(JARRES, t.getByte("n")));
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
        if (world != null) world.markBlockRangeForRenderUpdate(pos, pos);
    }

    @Override
    public boolean shouldRefresh(World w, BlockPos p, IBlockState ancien, IBlockState nouveau) {
        return ancien.getBlock() != nouveau.getBlock();
    }
}
