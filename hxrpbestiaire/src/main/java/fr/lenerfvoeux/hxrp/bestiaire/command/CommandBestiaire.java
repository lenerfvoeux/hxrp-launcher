package fr.lenerfvoeux.hxrp.bestiaire.command;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.Especes;
import fr.lenerfvoeux.hxrp.bestiaire.HxrpBestiaire;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntityBoss;
import fr.lenerfvoeux.hxrp.bestiaire.entite.EntitySbire;
import fr.lenerfvoeux.hxrp.bestiaire.stats.Stats;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * /bestiaire recharger : relit config/bestiaire_stats.xlsx pour tous les boss (et le Phénix si son mod est là).
 * /bestiaire liste : les boss, leurs commandes et ceux qui sont chargés.   (permission 2)
 */
public class CommandBestiaire extends CommandBase {
    @Override
    public String getName() {
        return "bestiaire";
    }

    @Override
    public String getUsage(ICommandSender s) {
        return "/bestiaire recharger | liste";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(MinecraftServer serveur, ICommandSender s, String[] a) throws CommandException {
        if (a.length == 0) throw new WrongUsageException(getUsage(s));
        for (WorldServer w : serveur.worlds) w.resetUpdateEntityTick();
        switch (a[0]) {
            case "recharger": {
                Stats.charger(Loader.instance().getConfigDir());
                int n = 0;
                for (WorldServer w : serveur.worlds)
                    for (Entity e : w.loadedEntityList)
                        if (e instanceof EntityBoss && e.isEntityAlive()) {
                            ((EntityBoss) e).reglagesRelus();
                            n++;
                        }
                String phenix = rechargerPhenix(serveur);
                notifyCommandListener(s, this, "Réglages du bestiaire relus (%s), %s boss mis à jour%s", Stats.source, n, phenix);
                return;
            }
            case "liste": {
                for (Espece e : Especes.BOSS) {
                    int n = 0;
                    for (WorldServer w : serveur.worlds)
                        for (Entity x : w.loadedEntityList) if (e.classe.isInstance(x) && x.isEntityAlive()) n++;
                    s.sendMessage(new TextComponentString(String.format("§6%s §7(/%s) : %d PV, %d phase(s), %d chargé(s)", e.nom, e.id,
                            (int) Stats.boss(e.id).phases[1].pv, Stats.boss(e.id).nombrePhases(), n)));
                }
                int sbires = 0;
                for (WorldServer w : serveur.worlds)
                    for (Entity x : w.loadedEntityList) if (x instanceof EntitySbire && x.isEntityAlive()) sbires++;
                s.sendMessage(new TextComponentString("§7Sbires chargés : " + sbires));
                return;
            }
            default:
                throw new WrongUsageException(getUsage(s));
        }
    }

    /** Le Phénix lit le même classeur : on lui demande de le relire aussi (par réflexion, son mod est facultatif). */
    private static String rechargerPhenix(MinecraftServer serveur) {
        if (!Loader.isModLoaded("hxrpphenix")) return "";
        try {
            Class<?> stats = Class.forName("fr.lenerfvoeux.hxrp.phenix.stats.StatsPhenix");
            stats.getMethod("charger", java.io.File.class).invoke(null, Loader.instance().getConfigDir());
            Class<?> phenix = Class.forName("fr.lenerfvoeux.hxrp.phenix.entite.EntityPhenix");
            Method relus = phenix.getMethod("reglagesRelus");
            for (WorldServer w : serveur.worlds)
                for (Entity e : w.loadedEntityList) if (phenix.isInstance(e) && e.isEntityAlive()) relus.invoke(e);
            return ", Phénix compris";
        } catch (Throwable t) {
            HxrpBestiaire.LOG.warn("Bestiaire : le Phénix n'a pas pu relire le classeur ({})", t.toString());
            return "";
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer serveur, ICommandSender s, String[] a, @Nullable BlockPos cible) {
        if (a.length == 1) return getListOfStringsMatchingLastWord(a, "recharger", "liste");
        return Collections.emptyList();
    }
}
