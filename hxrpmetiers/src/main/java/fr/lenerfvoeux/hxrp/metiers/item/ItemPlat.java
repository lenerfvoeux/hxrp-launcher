package fr.lenerfvoeux.hxrp.metiers.item;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.event.FoodEffects;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

/** Plat ou boisson du Gourmet : porte sa qualité, le rang du cuisinier et sa fraîcheur. */
public class ItemPlat extends Item implements IFoodItem {
    private final FoodEntry entry;

    public ItemPlat(FoodEntry e) {
        this.entry = e;
        setRegistryName(HxrpMetiers.MODID, e.id);
        setTranslationKey(HxrpMetiers.MODID + "." + e.id);
        setMaxStackSize(16);
    }

    @Override public FoodEntry entry() { return entry; }

    @Override public int getMaxItemUseDuration(ItemStack s) { return entry.isDrink() ? 32 : 40; }
    @Override public EnumAction getItemUseAction(ItemStack s) { return entry.isDrink() ? EnumAction.DRINK : EnumAction.EAT; }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String n = super.getItemStackDisplayName(stack);
        if (Fraicheur.rotten(stack, HxrpMetiers.proxy.now())) return TextFormatting.GRAY + n + " (avarié)";
        if (!entry.isDrink() && Qualite.of(Qualite.quality(stack)) == Qualite.ETRANGE) return TextFormatting.DARK_PURPLE + "Plat étrange" + TextFormatting.GRAY + " (" + n + ")";
        return n;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        long now = HxrpMetiers.proxy.now();
        if (Fraicheur.rotten(s, now)) {
            if (!w.isRemote) FoodEffects.message(p, TextFormatting.RED + "C'est avarié… impossible d'avaler ça.");
            return new ActionResult<>(EnumActionResult.FAIL, s);
        }
        long cd = w.isRemote ? HxrpMetiers.proxy.clientCooldownLeft(FoodEffects.key(s)) : cooldownServer(p, s, now);
        if (cd > 0) {
            if (!w.isRemote) FoodEffects.cooldownMessage(p, entry.isDrink(), cd);
            return new ActionResult<>(EnumActionResult.FAIL, s);
        }
        p.setActiveHand(hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    private static long cooldownServer(EntityPlayer p, ItemStack s, long now) {
        NutritionData d = Nutrition.get(p);
        return d == null ? 0 : d.cooldownLeft(FoodEffects.key(s), now);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack s, World w, EntityLivingBase e) {
        if (!w.isRemote && e instanceof EntityPlayerMP) FoodEffects.eatPlat((EntityPlayerMP) e, s, entry);
        if (!(e instanceof EntityPlayer) || !((EntityPlayer) e).capabilities.isCreativeMode) s.shrink(1);
        return s;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        Qualite q = Qualite.of(Qualite.quality(s));
        tip.add(q.color + q.label + TextFormatting.GRAY + " · " + Qualite.quality(s) + " %");
        int r = Qualite.cookRank(s);
        tip.add(TextFormatting.GRAY + entry.cat + " · recette " + stars(entry.rank) + TextFormatting.GRAY + " · cuisiné par un Gourmet " + stars(r));
        String val = "";
        if (entry.faim > 0) val += "Faim +" + entry.faim;
        if (entry.soif > 0) val += (val.isEmpty() ? "" : " · ") + "Soif +" + entry.soif;
        if (!val.isEmpty()) tip.add(TextFormatting.DARK_GREEN + val + TextFormatting.DARK_GRAY + " (valeurs de base)");
        if (q == Qualite.TRES_BON || q == Qualite.EXCEPTION)
            tip.add(TextFormatting.AQUA + "Effet : " + entry.buff + " · " + FoodEffects.buffMinutes(entry.rank, q) + " min");
        Tooltips.fraicheur(s, tip);
        long cd = HxrpMetiers.proxy.clientCooldownLeft(FoodEffects.key(s));
        if (cd > 0) tip.add(TextFormatting.GOLD + (entry.isDrink() ? "Déjà bu" : "Déjà mangé") + " · envie d'autre chose pendant " + Fraicheur.duree(cd));
    }

    static String stars(int n) {
        return TextFormatting.YELLOW + (n + "★");
    }
}
