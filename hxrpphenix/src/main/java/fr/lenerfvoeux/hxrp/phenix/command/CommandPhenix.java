package fr.lenerfvoeux.hxrp.phenix.command;

import fr.lenerfvoeux.hxrp.phenix.Registre;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityBouleDeFeu;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix;
import fr.lenerfvoeux.hxrp.phenix.entite.EntityPlumeArdente;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /phenix invoquer [x y z] · /phenix retirer [rayon] · /phenix attaque <boule|souffle|pluie|plongee|tempete|perche>
 * · /phenix info · /phenix oeuf [joueur] · /phenix plume [joueur] [nombre]   (permission 2)
 */
public class CommandPhenix extends CommandBase {
    private static final List<String> ATTAQUES = Arrays.asList("boule", "souffle", "pluie", "plongee", "tempete", "perche");

    @Override
    public String getName() {
        return "phenix";
    }

    @Override
    public String getUsage(ICommandSender s) {
        return "/phenix invoquer [x y z] | retirer [rayon] | attaque <boule|souffle|pluie|plongee|tempete|perche> | info | oeuf [joueur] | plume [joueur] [nombre]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer serveur, ICommandSender s, String[] a) throws CommandException {
        if (a.length == 0) throw new WrongUsageException(getUsage(s));
        switch (a[0]) {
            case "invoquer": {
                World w = s.getEntityWorld();
                Vec3d p;
                if (a.length >= 4) {
                    Vec3d base = s.getPositionVector();
                    p = new Vec3d(parseCoordinate(base.x, a[1], true).getResult(), parseCoordinate(base.y, a[2], false).getResult(),
                            parseCoordinate(base.z, a[3], true).getResult());
                } else if (s.getCommandSenderEntity() != null) {
                    Entity e = s.getCommandSenderEntity();
                    float y = e.rotationYaw * (float) Math.PI / 180F;
                    p = new Vec3d(e.posX - Math.sin(y) * 6, e.posY, e.posZ + Math.cos(y) * 6);
                } else {
                    throw new WrongUsageException("Depuis la console : /phenix invoquer <x> <y> <z>");
                }
                if (!w.isBlockLoaded(new BlockPos(p))) throw new CommandException("Ce lieu n'est pas chargé.");
                Entity e = s.getCommandSenderEntity();
                EntityPhenix.eclore(w, p.x, p.y, p.z, e != null ? e.rotationYaw + 180F : 0F);
                notifyCommandListener(s, this, "Le Phénix de feu est invoqué en %s %s %s", (int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
                return;
            }
            case "retirer": {
                double r = a.length >= 2 ? parseDouble(a[1], 1, 100000) : -1;
                int n = 0;
                for (WorldServer w : serveur.worlds) {
                    for (Entity e : new ArrayList<>(w.loadedEntityList)) {
                        if (!(e instanceof EntityPhenix || e instanceof EntityBouleDeFeu || e instanceof EntityPlumeArdente)) continue;
                        if (r > 0 && (e.world != s.getEntityWorld() || e.getDistanceSq(s.getPosition()) > r * r)) continue;
                        if (e instanceof EntityPhenix) n++;
                        e.setDead();
                    }
                }
                notifyCommandListener(s, this, "%s phénix retiré(s)", n);
                return;
            }
            case "attaque": {
                if (a.length < 2 || !ATTAQUES.contains(a[1])) throw new WrongUsageException("/phenix attaque <boule|souffle|pluie|plongee|tempete|perche>");
                EntityPhenix p = lePlusProche(s);
                if (p == null) throw new CommandException("Aucun Phénix à moins de 128 blocs.");
                int[] etats = {EntityPhenix.BOULE, EntityPhenix.SOUFFLE, EntityPhenix.PLUIE, EntityPhenix.PLONGEE, EntityPhenix.TEMPETE, EntityPhenix.PERCHE};
                if (!p.forcer(etats[ATTAQUES.indexOf(a[1])])) throw new CommandException("Le Phénix ne peut pas attaquer maintenant (œuf, renaissance ou mort).");
                notifyCommandListener(s, this, "Le Phénix lance : %s", a[1]);
                return;
            }
            case "info": {
                int n = 0;
                for (WorldServer w : serveur.worlds)
                    for (Entity e : w.loadedEntityList)
                        if (e instanceof EntityPhenix && e.isEntityAlive()) {
                            s.sendMessage(new TextComponentString("§6" + ((EntityPhenix) e).resume() + " §7(dim " + w.provider.getDimension() + ")"));
                            n++;
                        }
                if (n == 0) s.sendMessage(new TextComponentString("§7Aucun Phénix chargé."));
                return;
            }
            case "oeuf": {
                EntityPlayerMP p = a.length >= 2 ? getPlayer(serveur, s, a[1]) : getCommandSenderAsPlayer(s);
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Registre.OEUF));
                notifyCommandListener(s, this, "Œuf de phénix donné à %s", p.getName());
                return;
            }
            case "plume": {
                EntityPlayerMP p = a.length >= 2 ? getPlayer(serveur, s, a[1]) : getCommandSenderAsPlayer(s);
                int n = a.length >= 3 ? parseInt(a[2], 1, 64) : 1;
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(Registre.PLUME, n));
                notifyCommandListener(s, this, "%s plume(s) de phénix donnée(s) à %s", n, p.getName());
                return;
            }
            default:
                throw new WrongUsageException(getUsage(s));
        }
    }

    @Nullable
    private static EntityPhenix lePlusProche(ICommandSender s) {
        EntityPhenix mieux = null;
        double md = 128 * 128;
        for (Entity e : s.getEntityWorld().loadedEntityList) {
            if (!(e instanceof EntityPhenix) || !e.isEntityAlive()) continue;
            double d = e.getDistanceSq(s.getPosition());
            if (d < md) {
                md = d;
                mieux = (EntityPhenix) e;
            }
        }
        return mieux;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer serveur, ICommandSender s, String[] a, @Nullable BlockPos cible) {
        if (a.length == 1) return getListOfStringsMatchingLastWord(a, "invoquer", "retirer", "attaque", "info", "oeuf", "plume");
        if (a.length == 2 && "attaque".equals(a[0])) return getListOfStringsMatchingLastWord(a, ATTAQUES);
        if (a.length == 2 && ("oeuf".equals(a[0]) || "plume".equals(a[0]))) return getListOfStringsMatchingLastWord(a, serveur.getOnlinePlayerNames());
        if (a.length >= 2 && a.length <= 4 && "invoquer".equals(a[0])) return getTabCompletionCoordinate(a, 1, cible);
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] a, int i) {
        return i == 1 && a.length >= 1 && ("oeuf".equals(a[0]) || "plume".equals(a[0]));
    }
}
