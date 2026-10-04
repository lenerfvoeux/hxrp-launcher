package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.block.BlockOriente;
import fr.lenerfvoeux.hxrp.metiers.virus.block.TilePresentoir;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Les objets exposés sur le présentoir : deux par étagère, face au client, légèrement inclinés. */
@SideOnly(Side.CLIENT)
public class RenduPresentoir extends TileEntitySpecialRenderer<TilePresentoir> {
    /** Position (dans le repère du bloc tourné vers le nord) : x, y de chaque emplacement. */
    private static final double[][] PLACES = {{0.73, 0.58}, {0.27, 0.58}, {0.73, 0.11}, {0.27, 0.11}};

    @Override
    public void render(TilePresentoir te, double x, double y, double z, float pt, int destroy, float alpha) {
        if (te.getWorld() == null) return;
        IBlockState s = te.getWorld().getBlockState(te.getPos());
        if (!s.getPropertyKeys().contains(BlockOriente.FACING)) return;
        EnumFacing f = s.getValue(BlockOriente.FACING);
        float rot = -f.getHorizontalAngle() + 180;
        for (int k = 0; k < TilePresentoir.PLACES; k++) {
            ItemStack o = te.objet(k);
            if (o.isEmpty()) continue;
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y, z + 0.5);
            GlStateManager.rotate(rot, 0, 1, 0);
            GlStateManager.translate(PLACES[k][0] - 0.5, PLACES[k][1] + 0.16, -0.12);
            GlStateManager.rotate(-12, 1, 0, 0);
            GlStateManager.scale(0.42, 0.42, 0.42);
            RenderHelper.enableStandardItemLighting();
            Minecraft.getMinecraft().getRenderItem().renderItem(o, ItemCameraTransforms.TransformType.FIXED);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        }
    }
}
