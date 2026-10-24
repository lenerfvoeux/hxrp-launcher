package fr.lenerfvoeux.hxrp.bestiaire.bloc;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * L'appât d'un boss, donné par un admin : un modèle 3D posé au sol (rayon de miel, charnier, cercueil…). Des signes
 * annoncent le boss (TileAppat), qui arrive 30 à 90 s plus tard et le dévore. Incassable en survie, sans butin ;
 * un admin en créatif peut le retirer.
 */
public class BlockAppat extends Block {
    public final Espece espece;
    private final AxisAlignedBB forme;

    public BlockAppat(Espece e) {
        super(Material.WOOD, MapColor.BROWN);
        this.espece = e;
        setSoundType(SoundType.WOOD);
        setBlockUnbreakable();
        setResistance(6000000F);
        switch (e.appat) {
            case "rayon_de_miel_royal":
                forme = new AxisAlignedBB(0.125, 0, 0.3125, 0.875, 0.9375, 0.6875);
                setSoundType(SoundType.SLIME);
                setLightLevel(0.3F);
                break;
            case "charnier":
                forme = new AxisAlignedBB(0.0625, 0, 0.0625, 0.9375, 0.5, 0.9375);
                setSoundType(SoundType.SLIME);
                break;
            case "cercueil_profane":
                forme = new AxisAlignedBB(0.1875, 0, 0, 0.8125, 0.625, 1);
                break;
            default:
                forme = new AxisAlignedBB(0.0625, 0, 0.0625, 0.9375, 0.5, 0.9375);
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState s) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World w, IBlockState s) {
        return new TileAppat();
    }

    @Override
    public AxisAlignedBB getBoundingBox(IBlockState s, IBlockAccess w, BlockPos pos) {
        return forme;
    }

    @Override
    public boolean isOpaqueCube(IBlockState s) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState s) {
        return false;
    }

    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess w, IBlockState s, BlockPos pos, EnumFacing face) {
        return face == EnumFacing.DOWN ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {
    }

    @Override
    public ItemStack getPickBlock(IBlockState s, RayTraceResult cible, World w, BlockPos pos, EntityPlayer p) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    @Override
    public int getFlammability(IBlockAccess w, BlockPos pos, EnumFacing face) {
        return 0;
    }

    /** Côté client : la particule de l'appât (miel qui coule, mouches, fumée…). */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        double x = pos.getX() + 0.5, y = pos.getY() + forme.maxY, z = pos.getZ() + 0.5;
        if (r.nextInt(3) == 0)
            w.spawnParticle(espece.particuleAppat, x + (r.nextDouble() - 0.5) * 0.6, y - r.nextDouble() * 0.3, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
    }
}
