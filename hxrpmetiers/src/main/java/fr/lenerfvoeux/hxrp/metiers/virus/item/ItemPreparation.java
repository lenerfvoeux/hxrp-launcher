package fr.lenerfvoeux.hxrp.metiers.virus.item;

import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.client.ClientVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Soins;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
import java.util.Locale;

/**
 * Une préparation de l'officine (remède, soin, base) : elle porte sa note et le rang du Virus qui l'a faite.
 * Les remèdes s'administrent par un Hunter Virus : clic droit sur le patient, ou accroupi + clic droit pour soi-même.
 */
public class ItemPreparation extends Item {
    public final String id;

    public ItemPreparation(String id, int pile) {
        this.id = id;
        setMaxStackSize(pile);
    }

    public Defs.Preparation def() {
        return DonneesVirus.recettePour(id);
    }

    @Override
    public boolean itemInteractionForEntity(ItemStack s, EntityPlayer p, EntityLivingBase cible, EnumHand hand) {
        Defs.Preparation d = def();
        if (d == null || !d.administrable() || !(cible instanceof EntityPlayer)) return false;
        if (!p.world.isRemote) Soins.administrer((EntityPlayerMP) p, (EntityPlayerMP) cible, hand);
        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World w, EntityPlayer p, EnumHand hand) {
        ItemStack s = p.getHeldItem(hand);
        Defs.Preparation d = def();
        if (d == null || !d.administrable() || !p.isSneaking()) return new ActionResult<>(EnumActionResult.PASS, s);
        if (!w.isRemote) Soins.administrer((EntityPlayerMP) p, (EntityPlayerMP) p, hand);
        return new ActionResult<>(EnumActionResult.SUCCESS, s);
    }

    @Override
    public boolean hasEffect(ItemStack s) {
        return Qualites.notee(s) && Qualites.note(s) >= VirusConfig.seuilBonus;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack s, @Nullable World w, List<String> tip, ITooltipFlag flag) {
        Defs.Preparation d = def();
        if (d == null) return;
        tip.add(TextFormatting.GRAY + forme(d.forme) + (d.rang > 0 ? TextFormatting.YELLOW + " · " + d.rang + "★" : ""));
        if (Qualites.notee(s)) {
            int n = Qualites.note(s);
            TextFormatting c = n >= VirusConfig.seuilBonus ? TextFormatting.AQUA : TextFormatting.GREEN;
            tip.add(c + "Réussie à " + n + " %" + TextFormatting.GRAY + " · préparée par un Virus " + Qualites.rang(s) + "★");
            if (n >= VirusConfig.seuilBonus) tip.add(TextFormatting.AQUA + "Préparation d'exception : effet bonus");
        }
        Defs.Blessure soigne = d.soin == null ? null : DonneesVirus.blessure(d.soin);
        if (soigne != null) tip.add(TextFormatting.DARK_GREEN + "Soigne : " + soigne.nom);
        if (d.administrable()) {
            tip.add(TextFormatting.DARK_GRAY + administration(d.forme));
            if (ClientVirus.rangVirus < 0) tip.add(TextFormatting.DARK_GRAY + "Seul un Hunter Virus sait l'administrer");
        } else tip.add(TextFormatting.DARK_GRAY + "Sert à l'officine");
        if (flag.isAdvanced()) tip.add(TextFormatting.DARK_GRAY + d.texte);
    }

    public static String forme(String f) {
        switch (f == null ? "" : f) {
            case "fiole": return "Fiole";
            case "seringue": return "Seringue";
            case "pilules": return "Pilules";
            case "onguent": return "Onguent";
            case "bandage": return "Bandage";
            case "attelle": return "Attelle";
            case "fumigation": return "Fumigation";
            case "base": return "Ingrédient préparé";
            default: return f == null ? "" : f.substring(0, 1).toUpperCase(Locale.ROOT) + f.substring(1);
        }
    }

    public static String administration(String f) {
        switch (f == null ? "" : f) {
            case "fiole": return "Clic droit sur le patient : trois gorgées";
            case "seringue": return "Clic droit sur le patient : trouver la veine, injecter";
            case "pilules": return "Clic droit sur le patient : à avaler";
            case "onguent": return "Clic droit sur le patient : étaler en trois passages";
            case "bandage": case "attelle": return "Clic droit sur le patient : serrer en trois passages";
            case "fumigation": return "Clic droit sur le patient : lui faire respirer la fumée";
            default: return "";
        }
    }
}
