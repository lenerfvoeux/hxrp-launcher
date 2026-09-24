package fr.lenerfvoeux.hxrp.metiers.block;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Cuisine;
import fr.lenerfvoeux.hxrp.metiers.cuisine.EnCours;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Seances;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
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

/** Une station de cuisine. Clic droit : carnet de recettes, ou mini-jeu de l'étape en cours. */
public class BlockStation extends BlockOriente {
    public final Station station;

    public BlockStation(Station s) {
        super(s == Station.PLAN_DE_TRAVAIL || s == Station.DOSEUR ? Material.WOOD : s == Station.MORTIER ? Material.ROCK : Material.IRON, s.boite);
        this.station = s;
        setRegistryName(HxrpMetiers.MODID, s.id());
        setTranslationKey(HxrpMetiers.MODID + "." + s.id());
        setHardness(2.5f);
        setSoundType(s == Station.PLAN_DE_TRAVAIL || s == Station.DOSEUR ? SoundType.WOOD : s == Station.MORTIER ? SoundType.STONE : SoundType.METAL);
        setCreativeTab(ModRegistry.TAB_CUISINE);
        if (s.lumiere > 0) setLightLevel(s.lumiere / 15f);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState st, EntityPlayer p, EnumHand hand, EnumFacing f, float x, float y, float z) {
        if (hand != EnumHand.MAIN_HAND) return false;
        if (w.isRemote) return true;
        EntityPlayerMP mp = (EntityPlayerMP) p;
        ItemStack held = p.getHeldItemMainhand();
        if (held.getItem() == ModRegistry.EN_COURS) {
            Station att = EnCours.station(held);
            String geste = EnCours.geste(held);
            FoodEntry r = EnCours.recette(held);
            if (r == null || geste == null || att == null) return true;
            if (att != station) {
                p.sendMessage(new TextComponentString(TextFormatting.GOLD + "Cette étape se fait sur : " + att.label + " (" + geste + ")"));
                return true;
            }
            Network.NET.sendTo(Seances.ouvrir(mp, pos, r, EnCours.etape(held), geste, EnCours.rang(held)), mp);
            return true;
        }
        if (station == Station.PLAN_DE_TRAVAIL) {
            Network.NET.sendTo(new MsgRecettes(Cuisine.carnet(p), pos), mp);
        } else {
            p.sendMessage(new TextComponentString(TextFormatting.GRAY + station.label + " · gestes : " + String.join(", ", station.gestes)
                    + ". Commence une recette au plan de travail, puis reviens avec ta préparation."));
        }
        return true;
    }
}
