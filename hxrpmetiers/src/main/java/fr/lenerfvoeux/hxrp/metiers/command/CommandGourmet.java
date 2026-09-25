package fr.lenerfvoeux.hxrp.metiers.command;

import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.capability.Nutrition;
import fr.lenerfvoeux.hxrp.metiers.capability.NutritionData;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.item.IFoodItem;
import fr.lenerfvoeux.hxrp.metiers.item.Qualite;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /gourmet donner <joueur> <id> [quantité] [qualité] [rang]
 * /gourmet rang <joueur> <0-3>
 * /gourmet faim|soif <joueur> <0-60>
 * /gourmet cooldown <joueur>          (remet à zéro)
 * /gourmet peremption <heures>        (item en main, pour tester)
 * /gourmet info <joueur>
 */
public class CommandGourmet extends CommandBase {
    private static final List<String> SUB = Arrays.asList("donner", "rang", "xp", "faim", "soif", "cooldown", "peremption", "info");

    @Override public String getName() { return "gourmet"; }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public String getUsage(ICommandSender s) {
        return "/gourmet <donner|rang|faim|soif|cooldown|peremption|info> ...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender s, String[] a) throws CommandException {
        if (a.length < 1) throw new WrongUsageException(getUsage(s));
        switch (a[0]) {
            case "donner": {
                if (a.length < 3) throw new WrongUsageException("/gourmet donner <joueur> <id> [quantité] [qualité 0-100] [rang 0-3]");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                Item item = ModRegistry.FOOD.get(a[2]);
                if (item == null) throw new CommandException("Aliment inconnu : " + a[2]);
                int n = a.length > 3 ? parseInt(a[3], 1, 64) : 1;
                ItemStack st = new ItemStack(item, n);
                FoodEntry e = ((IFoodItem) item).entry();
                if (e.isDish() || e.isPreparation()) Qualite.set(st, a.length > 4 ? parseInt(a[4], 0, 100) : 85, a.length > 5 ? parseInt(a[5], 0, 3) : 0);
                Fraicheur.stamp(st, System.currentTimeMillis());
                ItemHandlerHelper.giveItemToPlayer(p, st);
                ok(s, n + " × " + st.getDisplayName() + " donné(s) à " + p.getName());
                break;
            }
            case "rang": {
                if (a.length < 3) throw new WrongUsageException("/gourmet rang <joueur> <0-3>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                NutritionData d = data(p);
                d.rangGourmet = parseInt(a[2], 0, 3);
                d.dirty = true;
                Network.sync(p);
                ok(s, p.getName() + " est maintenant Gourmet " + d.rangGourmet + "★");
                break;
            }
            case "xp": {
                if (a.length < 3) throw new WrongUsageException("/gourmet xp <joueur> <quantité>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                NutritionData d = data(p);
                boolean monte = d.addXp(parseInt(a[2], 0, 100000));
                Network.sync(p);
                ok(s, p.getName() + " : " + d.xpGourmet + " XP · Gourmet " + d.rangGourmet + "★" + (monte ? " (rang gagné)" : ""));
                break;
            }
            case "faim":
            case "soif": {
                if (a.length < 3) throw new WrongUsageException("/gourmet " + a[0] + " <joueur> <0-60>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                NutritionData d = data(p);
                double v = parseDouble(a[2], 0, 60);
                if (a[0].equals("faim")) d.setFaim(v); else d.setSoif(v);
                Network.sync(p);
                ok(s, a[0] + " de " + p.getName() + " : " + (int) v + "/60");
                break;
            }
            case "cooldown": {
                if (a.length < 2) throw new WrongUsageException("/gourmet cooldown <joueur>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                NutritionData d = data(p);
                d.cooldowns.clear();
                d.dirty = true;
                Network.sync(p);
                ok(s, "Cooldowns de " + p.getName() + " remis à zéro");
                break;
            }
            case "peremption": {
                if (a.length < 2) throw new WrongUsageException("/gourmet peremption <heures> (item en main)");
                EntityPlayerMP p = getCommandSenderAsPlayer(s);
                ItemStack st = p.getHeldItemMainhand();
                if (!Fraicheur.perishable(st)) throw new CommandException("L'item en main ne périme pas");
                double h = parseDouble(a[1], 0, 10000);
                Fraicheur.setExpiration(st, System.currentTimeMillis() + (long) (h * Fraicheur.HOUR), Fraicheur.totalHours(st));
                p.inventoryContainer.detectAndSendChanges();
                ok(s, "Péremption fixée à " + h + " h");
                break;
            }
            case "info": {
                EntityPlayerMP p = a.length > 1 ? getPlayer(server, s, a[1]) : getCommandSenderAsPlayer(s);
                NutritionData d = data(p);
                ok(s, p.getName() + " · faim " + (int) d.faim + "/60 · soif " + (int) d.soif + "/60 · Gourmet " + d.rangGourmet + "★ · " + d.cooldowns.size() + " cooldown(s)");
                break;
            }
            default:
                throw new WrongUsageException(getUsage(s));
        }
    }

    private static NutritionData data(EntityPlayerMP p) throws CommandException {
        NutritionData d = Nutrition.get(p);
        if (d == null) throw new CommandException("Données de nutrition introuvables");
        return d;
    }

    private static void ok(ICommandSender s, String msg) {
        s.sendMessage(new TextComponentString(TextFormatting.GREEN + msg));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender s, String[] a, @Nullable BlockPos pos) {
        if (a.length == 1) return getListOfStringsMatchingLastWord(a, SUB);
        if (a.length == 2 && !a[0].equals("peremption")) return getListOfStringsMatchingLastWord(a, server.getOnlinePlayerNames());
        if (a.length == 3 && a[0].equals("donner")) return getListOfStringsMatchingLastWord(a, ModRegistry.FOOD.keySet());
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] a, int i) {
        return i == 1 && a.length > 0 && !a[0].equals("peremption");
    }
}
