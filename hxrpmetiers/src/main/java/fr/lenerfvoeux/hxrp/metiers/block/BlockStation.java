package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Cuisine;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;

/** Une station de cuisine. Clic droit : liste des recettes, ou mini-jeu de l'étape en cours. */
public class BlockStation extends Block {
    public final Station station;

    public BlockStation(Station s) {
        super(s == Station.PLAN_DE_TRAVAIL ? Material.WOOD : Material.IRON);
        this.station = s;
        setRegistryName(HxrpMetiers.MODID, s.id());
        setTranslationKey(HxrpMetiers.MODID + "." + s.id());
        setHardness(2.5f);
        setSoundType(s == Station.PLAN_DE_TRAVAIL ? SoundType.WOOD : SoundType.METAL);
        setCreativeTab(ModRegistry.TAB_CUISINE);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState st, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (w.isRemote) return true;
        EntityPlayerMP mp = (EntityPlayerMP) p;
        ItemStack held = p.getHeldItem(hand);
        if (held.getItem() == ModRegistry.EN_COURS) {
            Station att = EnCours.station(held);
            String geste = EnCours.geste(held);
            FoodEntry r = EnCours.recette(held);
            if (r == null || geste == null) return true;
            if (att != station) {
                p.sendMessage(new TextComponentString(TextFormatting.GOLD + "Cette étape se fait sur : " + att.label + " (" + geste + ")"));
                return true;
            }
            Network.NET.sendTo(new MsgMiniJeu(geste, EnCours.rang(held), r.name, EnCours.etape(held) + 1, r.steps.size()), mp);
            return true;
        }
        if (station == Station.PLAN_DE_TRAVAIL) {
            List<FoodEntry> ok = Cuisine.realisables(p);
            Network.NET.sendTo(new MsgRecettes(ok, pos), mp);
        } else {
            p.sendMessage(new TextComponentString(TextFormatting.GRAY + station.label + " · gestes : " + String.join(", ", station.gestes)
                    + ". Commence une recette au plan de travail, puis reviens avec ta préparation."));
        }
        return true;
    }
}
