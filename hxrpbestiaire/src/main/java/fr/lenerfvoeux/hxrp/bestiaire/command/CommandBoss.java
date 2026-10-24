package fr.lenerfvoeux.hxrp.bestiaire.command;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Registre;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityProjectileBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
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
import java.util.Collections;
import java.util.List;

/**
 * /<boss> invoquer [x y z] · retirer [rayon] · attaque <id> · info · oeuf [joueur] · appat [joueur]   (permission 2).
 * Les ids d'attaque sont ceux du classeur (onglet Attaques). Pour essayer les phases : /<boss> vie <pourcentage>.
 */
public class CommandBoss extends CommandBase {
    private final Espece espece;

    public CommandBoss(Espece e) {
        this.espece = e;
    }

    @Override
    public String getName() {
        return espece.id;
    }

    @Override
    public String getUsage(ICommandSender s) {
        return "/" + espece.id + " invoquer [x y z] | retirer [rayon] | attaque <" + String.join("|", attaques()) + "> | info | vie <%> | oeuf [joueur] | appat [joueur]";
    }

    private List<String> attaques() {
        return new ArrayList<>(Stats.boss(espece.id).attaques.keySet());
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer serveur, ICommandSender s, String[] a) throws CommandException {
        if (a.length == 0) throw new WrongUsageException(getUsage(s));
        // sans joueur connecté, Minecraft cesse de faire vivre les entités au bout de 15 s : une commande les réveille
        for (WorldServer w : serveur.worlds) w.resetUpdateEntityTick();
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
                    p = new Vec3d(e.posX - Math.sin(y) * 8, e.posY, e.posZ + Math.cos(y) * 8);
                } else {
                    BlockPos sol = w.getTopSolidOrLiquidBlock(w.getSpawnPoint());
                    p = new Vec3d(sol.getX() + 0.5, sol.getY(), sol.getZ() + 0.5);
                }
                if (!w.isBlockLoaded(new BlockPos(p))) throw new CommandException("Ce lieu n'est pas chargé.");
                Entity e = s.getCommandSenderEntity();
                EntityBoss.invoquer(espece, w, p.x, p.y, p.z, e != null ? e.rotationYaw + 180F : 0F);
                notifyCommandListener(s, this, "%s invoqué" + espece.e() + " en %s %s %s", espece.nom, (int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
                return;
            }
            case "retirer": {
                double r = a.length >= 2 ? parseDouble(a[1], 1, 100000) : -1;
                int n = 0;
                for (WorldServer w : serveur.worlds) {
                    for (Entity e : new ArrayList<>(w.loadedEntityList)) {
                        boolean boss = espece.classe.isInstance(e);
                        if (!boss && !(e instanceof EntitySbire) && !(e instanceof EntityProjectileBoss)) continue;
                        if (r > 0 && (e.world != s.getEntityWorld() || e.getDistanceSq(s.getPosition()) > r * r)) continue;
                        if (boss) {
                            ((EntityBoss) e).retirerSbires();
                            n++;
                            e.setDead();
                        } else if (e instanceof EntitySbire && espece.id.equals(((EntitySbire) e).espece().bossDuSbire)) {
                            e.setDead();
                        }
                    }
                }
                notifyCommandListener(s, this, "%s %s retiré" + espece.e() + "(s)", n, espece.nom);
                return;
            }
            case "attaque": {
                if (a.length < 2) throw new WrongUsageException("/" + espece.id + " attaque <" + String.join("|", attaques()) + ">");
                EntityBoss b = lePlusProche(serveur, s);
                if (b == null) throw new CommandException(espece.nom + " : aucun" + (espece.feminin ? "e" : "") + " chargé" + espece.e() + (s instanceof MinecraftServer ? "." : " à moins de 128 blocs."));
                if (b.coup(a[1]) == null) throw new CommandException("Attaque inconnue : " + a[1] + " (" + String.join(", ", attaques()) + ")");
                if (!b.forcer(a[1])) throw new CommandException(espece.nom + " ne peut pas attaquer maintenant (arrivée, renaissance ou mort).");
                notifyCommandListener(s, this, "%s lance : %s", espece.nom, a[1]);
                return;
            }
            case "vie": {
                if (a.length < 2) throw new WrongUsageException("/" + espece.id + " vie <pourcentage>");
                double k = parseDouble(a[1], 1, 100) / 100;
                EntityBoss b = lePlusProche(serveur, s);
                if (b == null) throw new CommandException(espece.nom + " : aucun" + (espece.feminin ? "e" : "") + " chargé" + espece.e() + (s instanceof MinecraftServer ? "." : " à moins de 128 blocs."));
                b.fixerVie(k);
                notifyCommandListener(s, this, "%s ramené" + espece.e() + " à %s %% de sa vie", espece.nom, (int) Math.round(k * 100));
                return;
            }
            case "info": {
                int n = 0;
                for (WorldServer w : serveur.worlds)
                    for (Entity e : w.loadedEntityList)
                        if (espece.classe.isInstance(e) && e.isEntityAlive()) {
                            s.sendMessage(new TextComponentString("§6" + ((EntityBoss) e).resume() + " §7(dim " + w.provider.getDimension() + ")"));
                            n++;
                        }
                if (n == 0) s.sendMessage(new TextComponentString("§7" + espece.nom + " : aucun" + (espece.feminin ? "e" : "") + " chargé" + espece.e() + "."));
                s.sendMessage(new TextComponentString("§7" + Stats.resume(espece.id)));
                return;
            }
            case "oeuf":
            case "appat": {
                EntityPlayerMP p = a.length >= 2 ? getPlayer(serveur, s, a[1]) : getCommandSenderAsPlayer(s);
                Item i = "oeuf".equals(a[0]) ? Registre.oeuf(espece.id) : Registre.appat(espece.id);
                if (i == null) throw new CommandException("Objet introuvable.");
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(i));
                notifyCommandListener(s, this, "%s donné à %s", new ItemStack(i).getDisplayName(), p.getName());
                return;
            }
            default:
                throw new WrongUsageException(getUsage(s));
        }
    }

    /** Le boss le plus proche de l'expéditeur (à moins de 128 blocs) ; depuis la console, le plus proche du point d'apparition. */
    @Nullable
    private EntityBoss lePlusProche(MinecraftServer serveur, ICommandSender s) {
        boolean console = s instanceof MinecraftServer;
        EntityBoss mieux = null;
        double md = console ? Double.MAX_VALUE : 128 * 128;
        for (WorldServer w : serveur.worlds) {
            if (!console && w != s.getEntityWorld()) continue;
            BlockPos ref = console ? w.getSpawnPoint() : s.getPosition();
            for (Entity e : w.loadedEntityList) {
                if (!espece.classe.isInstance(e) || !e.isEntityAlive()) continue;
                double d = e.getDistanceSq(ref);
                if (d < md) {
                    md = d;
                    mieux = (EntityBoss) e;
                }
            }
        }
        return mieux;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer serveur, ICommandSender s, String[] a, @Nullable BlockPos cible) {
        if (a.length == 1) return getListOfStringsMatchingLastWord(a, "invoquer", "retirer", "attaque", "info", "vie", "oeuf", "appat");
        if (a.length == 2 && "attaque".equals(a[0])) return getListOfStringsMatchingLastWord(a, attaques());
        if (a.length == 2 && ("oeuf".equals(a[0]) || "appat".equals(a[0]))) return getListOfStringsMatchingLastWord(a, serveur.getOnlinePlayerNames());
        if (a.length >= 2 && a.length <= 4 && "invoquer".equals(a[0])) return getTabCompletionCoordinate(a, 1, cible);
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] a, int i) {
        return i == 1 && a.length >= 1 && ("oeuf".equals(a[0]) || "appat".equals(a[0]));
    }
}
