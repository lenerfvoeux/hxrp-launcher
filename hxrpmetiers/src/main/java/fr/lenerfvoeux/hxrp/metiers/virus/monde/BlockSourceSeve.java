package fr.lenerfvoeux.hxrp.metiers.virus.monde;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Source de sève de l'Arbre-Monde : jamais générée, posée par les admins (événement, lieu sacré).
 * Clic droit avec une fiole vide : une sève par jour réel (réglable) ; entre deux récoltes la source est tarie
 * (plus de coulées dorées), puis la sève revient d'elle-même.
 */
public class BlockSourceSeve extends Block {
    private static final long HEURE = 3_600_000L;
    /** Vrai juste après une récolte (méta 0 = coule, comme les sources d'avant la v0.7). */
    public static final PropertyBool TARIE = PropertyBool.create("tarie");

    public BlockSourceSeve() {
        super(Material.WOOD, MapColor.GOLD);
        setRegistryName(HxrpMetiers.MODID, "source_de_seve");
        setTranslationKey(HxrpMetiers.MODID + ".source_de_seve");
        setBlockUnbreakable();
        setResistance(6000000f);
        setSoundType(SoundType.WOOD);
        setLightLevel(9 / 15f);
        setTickRandomly(true);
        setDefaultState(blockState.getBaseState().withProperty(TARIE, false));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, TARIE); }
    @Override public IBlockState getStateFromMeta(int m) { return getDefaultState().withProperty(TARIE, (m & 1) == 1); }
    @Override public int getMetaFromState(IBlockState s) { return s.getValue(TARIE) ? 1 : 0; }

    @Override public boolean hasTileEntity(IBlockState s) { return true; }
    @Override public TileEntity createTileEntity(World w, IBlockState s) { return new TileHorloge(); }
    @Override public void getDrops(NonNullList<ItemStack> drops, IBlockAccess w, BlockPos pos, IBlockState s, int fortune) {}

    private static TileHorloge horloge(World w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        return te instanceof TileHorloge ? (TileHorloge) te : null;
    }

    /** La sève revient quand l'heure est passée (rattrapé au tick aléatoire ou au clic). */
    private IBlockState rattraper(World w, BlockPos pos, IBlockState s) {
        TileHorloge h = horloge(w, pos);
        if (w.isRemote || !s.getValue(TARIE) || (h != null && !h.pret())) return s;
        IBlockState n = s.withProperty(TARIE, false);
        w.setBlockState(pos, n, 2);
        return n;
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState s, Random r) {
        rattraper(w, pos, s);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState s, EntityPlayer p, EnumHand hand, EnumFacing f, float hx, float hy, float hz) {
        if (hand != EnumHand.MAIN_HAND) return false;
        ItemStack tenu = p.getHeldItem(hand);
        boolean fiole = tenu.getItem() == Items.GLASS_BOTTLE;
        if (!tenu.isEmpty() && !fiole) return false;
        if (w.isRemote) return true;
        s = rattraper(w, pos, s);
        TileHorloge h = horloge(w, pos);
        if (h == null) return true;
        if (!h.pret()) {
            if (!s.getValue(TARIE)) w.setBlockState(pos, s.withProperty(TARIE, true), 2);
            Recharge.dire(p, "La source est tarie. La sève coulera de nouveau dans " + Fraicheur.duree(h.prochaine - System.currentTimeMillis()) + ".");
            return true;
        }
        if (!fiole) {
            Recharge.dire(p, "Il te faut une fiole vide pour recueillir la sève.");
            return true;
        }
        Item i = Objets.item("seve_de_l_arbre_monde");
        if (i == null) return true;
        if (!p.capabilities.isCreativeMode) tenu.shrink(1);
        Recharge.donner(p, new ItemStack(i));
        h.attendre((long) (VirusConfig.seveHeures * HEURE));
        w.setBlockState(pos, s.withProperty(TARIE, true), 2);
        w.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1f, 0.7f);
        Recharge.gouttes(w, pos.offset(f).down(), EnumParticleTypes.DRIP_LAVA, 8);
        return true;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState s, World w, BlockPos pos, Random r) {
        if (s.getValue(TARIE) || r.nextInt(2) != 0) return;
        EnumFacing f = EnumFacing.Plane.HORIZONTAL.random(r);
        double x = pos.getX() + 0.5 + f.getXOffset() * 0.52, z = pos.getZ() + 0.5 + f.getZOffset() * 0.52;
        w.spawnParticle(EnumParticleTypes.DRIP_LAVA, x + (r.nextDouble() - 0.5) * 0.6 * (f.getXOffset() == 0 ? 1 : 0), pos.getY() + 0.2 + r.nextDouble() * 0.6,
                z + (r.nextDouble() - 0.5) * 0.6 * (f.getZOffset() == 0 ? 1 : 0), 0, 0, 0);
    }
}
