package fr.lenerfvoeux.hxrp.metiers.virus.command;

import com.mojang.authlib.GameProfile;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockCoquillages;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockCulture;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockFeuillage;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockMineraiSel;
import fr.lenerfvoeux.hxrp.metiers.monde.BlockRuche;
import fr.lenerfvoeux.hxrp.metiers.monde.Recharge;
import fr.lenerfvoeux.hxrp.metiers.monde.TileRecharge;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPioche;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockChampiTronc;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockPlanteVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockSolVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockSourceSeve;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.BlockTroncEcorce;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.Cueillette;
import fr.lenerfvoeux.hxrp.metiers.virus.monde.TileHorloge;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Officine;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /virus verifier : joue chaque récolte au clic droit avec un joueur fictif, en l'air au-dessus du point d'apparition,
 * puis remet les lieux en état. Vérifie ce que rend chaque bloc, son état vide, son horloge, sa recharge, qu'il est
 * incassable et ne laisse rien tomber, la pioche de prospecteur (usure, émoussée, réparation) et la source de sève.
 */
final class EssaiRecolte {
    private static final GameProfile PROFIL = new GameProfile(UUID.fromString("6f1b0c2e-6a51-4c8e-9f0e-2a7b3c4d5e6f"), "[HxRP essai]");

    private final WorldServer w;
    private final FakePlayer p;
    private final BlockPos base;
    private final List<BlockPos> poses = new ArrayList<>();
    private final List<String> echecs = new ArrayList<>();
    private int verifs;

    private EssaiRecolte(WorldServer w) {
        this.w = w;
        this.p = FakePlayerFactory.get(w, PROFIL);
        BlockPos s = w.getSpawnPoint();
        this.base = new BlockPos(s.getX(), 200, s.getZ());
        p.setPosition(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
    }

    /** Écrit le détail des échecs à l'expéditeur et renvoie le bilan (null si tout va bien). */
    static String lancer(WorldServer w, ICommandSender s) {
        EssaiRecolte e = new EssaiRecolte(w);
        try {
            e.tout();
        } catch (RuntimeException ex) {
            e.echecs.add("exception : " + ex);
        } finally {
            e.ranger();
        }
        for (String x : e.echecs) s.sendMessage(new TextComponentString(TextFormatting.RED + "  ✘ " + x));
        return e.echecs.isEmpty() ? null : e.echecs.size() + " échec(s) sur " + e.verifs + " vérifications";
    }

    // ================================================================== outils
    private void verifier(boolean ok, String quoi) {
        verifs++;
        if (!ok) echecs.add(quoi);
    }

    private BlockPos ici(int k) {
        return base.add(k * 3, 0, 0);
    }

    private IBlockState poser(BlockPos pos, IBlockState s) {
        poses.add(pos);
        w.setBlockState(pos, s, 3);
        return w.getBlockState(pos);
    }

    private void ranger() {
        for (int i = poses.size() - 1; i >= 0; i--) w.setBlockState(poses.get(i), Blocks.AIR.getDefaultState(), 2);
        p.inventory.clear();
    }

    private int compter(Item i) {
        int n = 0;
        for (int k = 0; k < p.inventory.getSizeInventory(); k++) {
            ItemStack s = p.inventory.getStackInSlot(k);
            if (!s.isEmpty() && s.getItem() == i) n += s.getCount();
        }
        return n;
    }

    private void tenir(ItemStack s) {
        p.setHeldItem(EnumHand.MAIN_HAND, s);
    }

    private boolean clic(BlockPos pos) {
        IBlockState s = w.getBlockState(pos);
        return s.getBlock().onBlockActivated(w, pos, s, p, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5f, 0.5f, 0.5f);
    }

    /** Fait comme si la recharge était finie, puis laisse passer un tick aléatoire. */
    private IBlockState expirer(BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileRecharge) ((TileRecharge) te).demarrer(1, 1);
        if (te instanceof TileHorloge) ((TileHorloge) te).prochaine = 0;
        IBlockState s = w.getBlockState(pos);
        s.getBlock().updateTick(w, pos, s, w.rand);
        return w.getBlockState(pos);
    }

    private boolean horloge(BlockPos pos) {
        TileRecharge h = Recharge.horloge(w, pos);
        return h != null && h.demarree() && h.avancement() < 0.01;
    }

    /** Incassable en survie et sans butin. */
    private void solide(String nom, BlockPos pos) {
        IBlockState s = w.getBlockState(pos);
        verifier(s.getBlockHardness(w, pos) < 0, nom + " : cassable en survie");
        NonNullList<ItemStack> l = NonNullList.create();
        s.getBlock().getDrops(l, w, pos, s, 0);
        verifier(l.isEmpty(), nom + " : laisse tomber " + l);
    }

    // ================================================================== les cas
    private void tout() {
        culture(ici(0));
        feuillage(ici(1));
        coquillages(ici(2));
        sel(ici(3));
        ruche(ici(4));
        plante(ici(5));
        champignon(ici(6));
        sols(ici(7));
        ecorce(ici(9));
        reparation();
        seve(ici(10));
    }

    private void culture(BlockPos pos) {
        BlockCulture c = ModRegistry.CULTURES.values().iterator().next();
        Item fruit = ModRegistry.FOOD.get(c.def.id);
        poser(pos.down(), Blocks.FARMLAND.getDefaultState());
        poser(pos, c.withAge(2));
        p.inventory.clear();
        tenir(ItemStack.EMPTY);
        solide("culture", pos);
        verifier(w.getBlockState(pos).getMaterial().blocksMovement(), "culture : l'eau l'emporterait");
        verifier(clic(pos) && compter(fruit) >= c.def.min, "culture : rien récolté");
        verifier(w.getBlockState(pos).getValue(BlockCulture.AGE) == 0 && horloge(pos), "culture : pas revenue au stade 1 avec son horloge");
        int n = compter(fruit);
        clic(pos);
        verifier(compter(fruit) == n, "culture : récoltée deux fois");
        verifier(expirer(pos).getValue(BlockCulture.AGE) == 2, "culture : pas redevenue mûre");
        verifier(Recharge.horloge(w, pos) == null, "culture : garde son horloge une fois mûre");
        verifier(w.getBlockState(pos.down()).getBlock() == Blocks.FARMLAND, "culture : la terre labourée a disparu");
    }

    private void feuillage(BlockPos pos) {
        BlockFeuillage f = ModRegistry.FEUILLES.values().iterator().next();
        while (!BlockFeuillage.porteur(pos)) pos = pos.south();
        poser(pos, f.getDefaultState().withProperty(BlockLeaves.DECAYABLE, false).withProperty(BlockLeaves.CHECK_DECAY, false)
                .withProperty(BlockFeuillage.FRUIT, 2));
        p.inventory.clear();
        solide("feuillage", pos);
        verifier(clic(pos) && compter(f.fruit()) >= 1, "feuillage : pas de fruit");
        verifier(w.getBlockState(pos).getValue(BlockFeuillage.FRUIT) == 1 && horloge(pos), "feuillage : pas vidé");
        verifier(expirer(pos).getValue(BlockFeuillage.FRUIT) == 2, "feuillage : fruits pas revenus");
    }

    private void coquillages(BlockPos pos) {
        Item moule = ModRegistry.FOOD.get("moule");
        poser(pos.down(), Blocks.SAND.getDefaultState());
        poser(pos, ModRegistry.MOULES.getDefaultState());
        p.inventory.clear();
        solide("banc de moules", pos);
        verifier(clic(pos) && compter(moule) >= 1, "banc de moules : rien ramassé");
        verifier(w.getBlockState(pos).getValue(BlockCoquillages.VIDE) && horloge(pos), "banc de moules : pas vide");
        verifier(!expirer(pos).getValue(BlockCoquillages.VIDE), "banc de moules : pas rempli");
    }

    private void sel(BlockPos pos) {
        Item sel = ModRegistry.FOOD.get("sel");
        poser(pos, ModRegistry.MINERAI_SEL.getDefaultState());
        p.inventory.clear();
        tenir(ItemStack.EMPTY);
        solide("minerai de sel", pos);
        clic(pos);
        verifier(compter(sel) == 0 && !w.getBlockState(pos).getValue(BlockMineraiSel.EPUISE), "minerai de sel : récolté sans pioche");
        ItemStack pioche = new ItemStack(VirusRegistre.PIOCHE);
        tenir(pioche);
        verifier(clic(pos) && compter(sel) >= 2, "minerai de sel : rien avec la pioche");
        verifier(p.getHeldItemMainhand().getItemDamage() == 1, "pioche : pas usée d'un cran");
        verifier(w.getBlockState(pos).getValue(BlockMineraiSel.EPUISE) && horloge(pos), "minerai de sel : pas épuisé");
        verifier(!expirer(pos).getValue(BlockMineraiSel.EPUISE), "minerai de sel : pas rechargé");
        // pioche émoussée : elle ne sert plus, mais ne disparaît pas
        p.getHeldItemMainhand().setItemDamage(ItemPioche.UTILISATIONS);
        int n = compter(sel);
        clic(pos);
        verifier(compter(sel) == n && ItemPioche.est(p.getHeldItemMainhand()), "pioche émoussée : a encore servi, ou a disparu");
        tenir(ItemStack.EMPTY);
    }

    private void ruche(BlockPos pos) {
        Item cire = Objets.item("cire_d_abeille"), miel = ModRegistry.FOOD.get("miel");
        poser(pos, ModRegistry.RUCHE.getDefaultState().withProperty(BlockRuche.PLEINE, true));
        p.inventory.clear();
        tenir(ItemStack.EMPTY);
        solide("ruche", pos);
        verifier(clic(pos) && compter(cire) >= 1, "ruche : pas de cire");
        verifier(!w.getBlockState(pos).getValue(BlockRuche.PLEINE) && horloge(pos), "ruche : pas vidée");
        verifier(expirer(pos).getValue(BlockRuche.PLEINE), "ruche : pas remplie");
        tenir(new ItemStack(Items.GLASS_BOTTLE));
        verifier(clic(pos) && compter(miel) == 1, "ruche : pas de miel à la fiole");
        tenir(ItemStack.EMPTY);
    }

    private void plante(BlockPos pos) {
        BlockPlanteVirus b = Cueillette.PLANTES.get("camomille");
        if (b == null) b = Cueillette.PLANTES.values().iterator().next();
        Item ing = Objets.item(b.def.id);
        poser(pos.down(), Blocks.GRASS.getDefaultState());
        poser(pos, b.getDefaultState());
        p.inventory.clear();
        solide("plante du Virus", pos);
        verifier(w.getBlockState(pos).getMaterial().blocksMovement(), "plante du Virus : l'eau l'emporterait");
        verifier(b.isPassable(w, pos), "plante du Virus : on ne la traverse plus");
        verifier(clic(pos) && compter(ing) >= 1, "plante du Virus : rien cueilli");
        verifier(b.age(w.getBlockState(pos)) == 0 && horloge(pos), "plante du Virus : pas revenue au stade 1");
        verifier(b.age(expirer(pos)) == 2, "plante du Virus : pas repoussée");
        long d = Cueillette.duree(b.def);
        verifier(d == fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig.rechargeMinutes[Cueillette.niveau(b.def)] * Recharge.MINUTE,
                "plante du Virus : durée de recharge " + d + " ms");
    }

    private void champignon(BlockPos pos) {
        BlockChampiTronc t = null;
        for (BlockChampiTronc x : Cueillette.TRONCS.values()) if ("chene".equals(x.def.recolte.bois) || "tous".equals(x.def.recolte.bois)) t = x;
        if (t == null) return;
        Item ing = Objets.item(t.def.id);
        poser(pos, Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.OAK));
        BlockPos c = pos.east();
        poser(c, t.getDefaultState().withProperty(BlockChampiTronc.FACING, EnumFacing.WEST));
        p.inventory.clear();
        solide("champignon de tronc", c);
        verifier(clic(c) && compter(ing) >= 1, "champignon de tronc : rien cueilli");
        verifier(w.getBlockState(c).getValue(BlockChampiTronc.AGE) == 0 && horloge(c), "champignon de tronc : pas revenu au stade 1");
        verifier(expirer(c).getValue(BlockChampiTronc.AGE) == 2, "champignon de tronc : pas repoussé");
    }

    private void sols(BlockPos pos) {
        for (BlockSolVirus b : Cueillette.SOLS.values()) {
            Item ing = Objets.item(b.def.id);
            poser(pos, b.getDefaultState());
            p.inventory.clear();
            tenir(ItemStack.EMPTY);
            solide(b.id, pos);
            clic(pos);
            if (b.roche) {
                verifier(compter(ing) == 0, b.id + " : gratté sans pioche");
                tenir(new ItemStack(VirusRegistre.PIOCHE));
                clic(pos);
            }
            verifier(compter(ing) >= 1, b.id + " : rien trouvé");
            verifier(w.getBlockState(pos).getValue(BlockSolVirus.VIDE) && horloge(pos), b.id + " : pas fouillé");
            verifier(!expirer(pos).getValue(BlockSolVirus.VIDE), b.id + " : pas rechargé");
            pos = pos.up(2);
        }
        tenir(ItemStack.EMPTY);
    }

    private void ecorce(BlockPos pos) {
        Item ecorce = Objets.item("ecorce_de_bouleau");
        poser(pos, Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.BIRCH));
        p.inventory.clear();
        tenir(ItemStack.EMPTY);
        ForgeHooks.onRightClickBlock(p, EnumHand.MAIN_HAND, pos, EnumFacing.NORTH, new Vec3d(0.5, 0.5, 0));
        verifier(w.getBlockState(pos).getBlock() instanceof BlockTroncEcorce && compter(ecorce) >= 1, "écorce : pas prélevée à la main");
        verifier(horloge(pos), "écorce : pas d'horloge");
        solide("tronc écorcé", pos);
        verifier(expirer(pos).getBlock() == Blocks.LOG, "écorce : pas repoussée");
    }

    private void reparation() {
        Item lingot = Objets.item("lingot_d_osteacier");
        p.inventory.clear();
        ItemStack pioche = new ItemStack(VirusRegistre.PIOCHE);
        pioche.setItemDamage(ItemPioche.UTILISATIONS);
        tenir(pioche);
        p.inventory.addItemStackToInventory(new ItemStack(lingot, 2));
        Officine.utiliser(p, Machine.TABLE, base);
        verifier(ItemPioche.restantes(p.getHeldItemMainhand()) == ItemPioche.REPARATION && compter(lingot) == 1,
                "pioche : réparation à la table (" + ItemPioche.restantes(p.getHeldItemMainhand()) + " utilisations, " + compter(lingot) + " lingot)");
        Officine.utiliser(p, Machine.TABLE, base);
        verifier(ItemPioche.restantes(p.getHeldItemMainhand()) == ItemPioche.UTILISATIONS && compter(lingot) == 0, "pioche : seconde réparation");
        tenir(ItemStack.EMPTY);
    }

    private void seve(BlockPos pos) {
        Item seve = Objets.item("seve_de_l_arbre_monde");
        poser(pos, Cueillette.SEVE.getDefaultState());
        p.inventory.clear();
        tenir(ItemStack.EMPTY);
        clic(pos);
        verifier(compter(seve) == 0, "sève : recueillie sans fiole");
        tenir(new ItemStack(Items.GLASS_BOTTLE));
        verifier(clic(pos) && compter(seve) == 1 && w.getBlockState(pos).getValue(BlockSourceSeve.TARIE), "sève : pas recueillie à la fiole");
        verifier(!expirer(pos).getValue(BlockSourceSeve.TARIE), "sève : la source ne coule plus");
        tenir(ItemStack.EMPTY);
    }
}
