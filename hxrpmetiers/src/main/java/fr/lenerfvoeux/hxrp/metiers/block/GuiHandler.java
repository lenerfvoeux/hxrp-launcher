package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.client.GuiFrigo;
import fr.lenerfvoeux.hxrp.metiers.client.GuiPoubelle;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class GuiHandler implements IGuiHandler {
    public static final int FRIGO = 0, POUBELLE = 1;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (id == FRIGO) {
            TileEntity te = w.getTileEntity(pos);
            return te instanceof TileFrigo ? new ContainerFrigo(p.inventory, (TileFrigo) te) : null;
        }
        if (id == POUBELLE) return new ContainerPoubelle(p.inventory, pos);
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer p, World w, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (id == FRIGO) {
            TileEntity te = w.getTileEntity(pos);
            return te instanceof TileFrigo ? new GuiFrigo(new ContainerFrigo(p.inventory, (TileFrigo) te)) : null;
        }
        if (id == POUBELLE) return new GuiPoubelle(new ContainerPoubelle(p.inventory, pos));
        return null;
    }
}
