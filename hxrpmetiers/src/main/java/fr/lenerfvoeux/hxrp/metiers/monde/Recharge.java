package fr.lenerfvoeux.hxrp.metiers.monde;

import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPioche;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

/**
 * Récolte sans casser, commune au Gourmet et au Virus. Tout se récolte au clic droit ; le bloc passe alors dans un état
 * vide (stade 0, sans fruits, vide, épuisé, fouillé…) et garde l'heure réelle de sa récolte dans une TileRecharge.
 * Il se remplit tout seul : le temps écoulé, même chunk déchargé, est rattrapé au premier tick aléatoire du bloc,
 * ou dès qu'un joueur clique dessus. Ces blocs sont incassables en survie (seuls les admins en créatif les cassent)
 * et ne donnent rien quand ils disparaissent.
 */
public final class Recharge {
    public static final long MINUTE = 60_000L;

    /**
     * Matière des plantes sauvages récoltables : traversable et transparente comme une fleur (les blocs doivent
     * aussi être isPassable et isFoliage), mais l'eau, la lave et les seaux ne l'emportent pas : une source
     * d'ingrédient ne doit pas pouvoir être noyée.
     */
    public static final Material TENACE = new Material(MapColor.FOLIAGE) {
        {
            setTranslucent();
        }

        @Override public boolean blocksLight() { return false; }
    };

    /** Matière des cultures : comme TENACE, mais pas « solide », sinon la terre labourée dessous redevient de la terre. */
    public static final Material CULTURE = new Material(MapColor.FOLIAGE) {
        {
            setTranslucent();
        }

        @Override public boolean isSolid() { return false; }
        @Override public boolean blocksLight() { return false; }
    };

    private Recharge() {}

    /** Un bloc qui se recharge après la récolte. */
    public interface Bloc {
        /** Plein, donc récoltable ? */
        boolean plein(IBlockState s);

        /** L'état à montrer pour cet avancement de la recharge (0 juste récolté, 1 plein). */
        IBlockState etat(IBlockState s, double avancement);

        /** L'avancement que montre cet état, pour lancer l'horloge d'un bloc vide qui n'en a pas (posé, ancien monde). */
        double avancement(IBlockState s);

        /** Durée d'une recharge complète (ms). */
        long duree(IBlockState s);
    }

    @Nullable
    public static TileRecharge horloge(World w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        return te instanceof TileRecharge ? (TileRecharge) te : null;
    }

    /** Rattrape le temps écoulé (côté serveur) et renvoie l'état à jour du bloc. */
    public static IBlockState rattraper(World w, BlockPos pos, IBlockState s, Bloc b) {
        if (w.isRemote || b.plein(s)) return s;
        TileRecharge h = horloge(w, pos);
        if (h == null) return s;
        if (!h.demarree()) h.demarrer(b.duree(s), b.avancement(s));
        IBlockState n = b.etat(s, h.avancement());
        if (n != s) w.setBlockState(pos, n, 2);
        return n;
    }

    /** Après une récolte : le bloc passe à l'état vide et sa recharge commence. */
    public static void vider(World w, BlockPos pos, IBlockState vide, long duree) {
        w.setBlockState(pos, vide, 2);
        TileRecharge h = horloge(w, pos);
        if (h != null) h.demarrer(duree, 0);
    }

    /** « · encore 25 min » si l'horloge du bloc tourne, sinon rien. */
    public static String reste(World w, BlockPos pos) {
        TileRecharge h = horloge(w, pos);
        return h == null || !h.demarree() ? "" : " · encore " + Fraicheur.duree(h.reste());
    }

    /** Un message dans la barre d'action. */
    public static void dire(EntityPlayer p, String s) {
        p.sendStatusMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + s), true);
    }

    /** La récolte arrive dans l'inventaire (à ses pieds s'il est plein). */
    public static void donner(EntityPlayer p, ItemStack s) {
        if (!s.isEmpty()) ItemHandlerHelper.giveItemToPlayer(p, s);
    }

    /** Son et éclats de la récolte (morceaux du bloc donné : feuilles, roche, terre…). */
    public static void effets(World w, BlockPos pos, IBlockState eclats, double hauteur, SoundEvent son, float volume, float ton) {
        if (w instanceof WorldServer)
            ((WorldServer) w).spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5, pos.getY() + hauteur, pos.getZ() + 0.5,
                    14, 0.25, 0.15, 0.25, 0.05, Block.getStateId(eclats));
        w.playSound(null, pos, son, SoundCategory.BLOCKS, volume, ton);
    }

    /** Éclats jaillissant de la face cliquée d'un bloc plein (roche, terre), et le son du geste. */
    public static void eclatsFace(World w, BlockPos pos, EnumFacing f, IBlockState eclats, SoundEvent son, float volume, float ton) {
        if (w instanceof WorldServer)
            ((WorldServer) w).spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5 + f.getXOffset() * 0.6,
                    pos.getY() + 0.5 + f.getYOffset() * 0.6, pos.getZ() + 0.5 + f.getZOffset() * 0.6,
                    16, 0.2, 0.2, 0.2, 0.05, Block.getStateId(eclats));
        w.playSound(null, pos, son, SoundCategory.BLOCKS, volume, ton);
    }

    /** Gouttes ou bulles autour du bloc (vase, sève). */
    public static void gouttes(World w, BlockPos pos, EnumParticleTypes type, int n) {
        if (w instanceof WorldServer)
            ((WorldServer) w).spawnParticle(type, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, n, 0.3, 0.1, 0.3, 0.02);
    }

    /**
     * La pioche de prospecteur tenue (main principale, sinon l'autre), prête à servir, ou une pile vide. Sans pioche,
     * le joueur l'apprend s'il a la main vide (avec un autre objet en main, le clic droit garde son usage : poser une
     * torche…) ; si sa pioche est émoussée, il l'apprend toujours. Le bloc rend alors {@link #sansPioche}.
     */
    public static ItemStack pioche(EntityPlayer p, EnumHand hand) {
        ItemStack s = ItemPioche.tenue(p);
        if (s.isEmpty()) {
            if (!p.world.isRemote && p.getHeldItem(hand).isEmpty()) dire(p, "Il te faut une pioche de prospecteur.");
            return ItemStack.EMPTY;
        }
        if (ItemPioche.emoussee(s)) {
            if (!p.world.isRemote) dire(p, "Ta pioche de prospecteur est émoussée : répare-la à la table de préparation avec un lingot d'ostéacier.");
            return ItemStack.EMPTY;
        }
        return s;
    }

    /** Ce que rend onBlockActivated quand pioche() n'a rien donné : le clic est pris si la main est vide ou tient la pioche. */
    public static boolean sansPioche(EntityPlayer p, EnumHand hand) {
        ItemStack t = p.getHeldItem(hand);
        return t.isEmpty() || ItemPioche.est(t) || !ItemPioche.tenue(p).isEmpty();
    }
}
