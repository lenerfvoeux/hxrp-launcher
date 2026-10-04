package fr.lenerfvoeux.hxrp.metiers.virus.command;

import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Qualites;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgDonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Blessures;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Identite;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Maladies;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.MoteurSante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitement;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Traitements;
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
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Commandes d'administration du Hunter Virus (permission 2) :
 * <pre>
 * /virus rang &lt;joueur&gt; &lt;0-3|aucun&gt;
 * /virus maladie donner|soigner|info &lt;joueur&gt; [maladie] [stade]
 * /virus blessure &lt;joueur&gt; &lt;type&gt; &lt;stade&gt;|aucune
 * /virus sang &lt;joueur&gt;
 * /virus immunite &lt;joueur&gt; &lt;on|off&gt;
 * /virus parchemin &lt;joueur&gt;
 * /virus donner &lt;joueur&gt; &lt;objet&gt; [quantité] [qualité]
 * /virus traitement &lt;joueur&gt; [annuler|avancer]
 * /virus avie &lt;joueur&gt; [retirer]
 * /virus jarres                       (termine les macérations des jarres à 8 blocs)
 * /virus prise &lt;joueur&gt;               (autorise une nouvelle prise de sang tout de suite)
 * /virus recharger
 * </pre>
 */
public class CommandVirus extends CommandBase {
    private static final List<String> SUB = Arrays.asList("rang", "maladie", "blessure", "sang", "immunite", "parchemin", "donner", "traitement", "avie", "jarres", "prise", "recharger");

    @Override public String getName() { return "virus"; }
    @Override public int getRequiredPermissionLevel() { return 2; }

    @Override
    public String getUsage(ICommandSender s) {
        return "/virus <rang|maladie|blessure|sang|immunite|parchemin|donner|traitement|avie|jarres|prise|recharger> ...";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender s, String[] a) throws CommandException {
        if (a.length < 1) throw new WrongUsageException(getUsage(s));
        switch (a[0]) {
            case "rang": {
                if (a.length < 3) throw new WrongUsageException("/virus rang <joueur> <0-3|aucun>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                d.rangVirus = "aucun".equalsIgnoreCase(a[2]) ? -1 : parseInt(a[2], 0, 3);
                d.dirty = true;
                sync(p, d);
                ok(s, d.rangVirus < 0 ? p.getName() + " n'est plus Hunter Virus" : p.getName() + " est maintenant Hunter Virus " + d.rangVirus + "★");
                if (d.rangVirus >= 0 && p != s) p.sendMessage(new TextComponentString(TextFormatting.GREEN + "Vous êtes Hunter Virus " + d.rangVirus + "★."));
                break;
            }
            case "maladie": maladie(server, s, a); break;
            case "blessure": {
                if (a.length < 4) throw new WrongUsageException("/virus blessure <joueur> <type> <0-4|aucune>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                Defs.Blessure b = DonneesVirus.blessure(a[2]);
                if (b == null) throw new CommandException("Blessure inconnue : " + a[2] + " (" + String.join(", ", DonneesVirus.BLESSURES.keySet()) + ")");
                int st = "aucune".equalsIgnoreCase(a[3]) ? 0 : parseInt(a[3], 0, 4);
                Blessures.fixer(d, b.id, st);
                if (st > 0) p.sendMessage(new TextComponentString(TextFormatting.RED + "" + TextFormatting.ITALIC + b.stades.get(st - 1).message));
                sync(p, d);
                ok(s, b.nom + " de " + p.getName() + " : " + (st == 0 ? "aucune" : "stade " + st));
                break;
            }
            case "sang": {
                if (a.length < 2) throw new WrongUsageException("/virus sang <joueur>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                ok(s, p.getName() + " (" + Identite.nom(p) + ") · groupe " + (d.groupe.isEmpty() ? "pas encore tiré" : d.groupe)
                        + " · sexe " + Identite.libelleSexe(Identite.sexe(p)));
                break;
            }
            case "immunite": {
                if (a.length < 3) throw new WrongUsageException("/virus immunite <joueur> <on|off>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                boolean on = "on".equalsIgnoreCase(a[2]);
                long now = System.currentTimeMillis();
                d.immuniteFin = on ? now + VirusConfig.immuniteHeures * Maladies.HEURE : 0;
                if (!on && d.prochaineMaladie > now + Maladies.delaiAleatoire()) d.prochaineMaladie = now + Maladies.delaiAleatoire();
                d.dirty = true;
                ok(s, "Immunité de " + p.getName() + " : " + (on ? "active " + VirusConfig.immuniteHeures + " h" : "retirée"));
                break;
            }
            case "parchemin": {
                if (a.length < 2) throw new WrongUsageException("/virus parchemin <joueur>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                Item i = Objets.item("parchemin_du_second_souffle");
                if (i == null) throw new CommandException("Parchemin introuvable");
                ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(i));
                ok(s, "Parchemin du Second Souffle donné à " + p.getName());
                break;
            }
            case "donner": {
                if (a.length < 3) throw new WrongUsageException("/virus donner <joueur> <objet> [quantité] [qualité 0-100]");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                Item i = Objets.item(a[2]);
                if (i == null) throw new CommandException("Objet inconnu : " + a[2]);
                int n = a.length > 3 ? parseInt(a[3], 1, 64) : 1;
                ItemStack st = new ItemStack(i, n);
                if (Qualites.estPreparation(st)) Qualites.noter(st, a.length > 4 ? parseInt(a[4], 0, 100) : 90, Math.max(0, data(p).rangVirus));
                fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.stamp(st, System.currentTimeMillis());
                ItemHandlerHelper.giveItemToPlayer(p, st);
                ok(s, n + " × " + st.getDisplayName() + " donné(s) à " + p.getName());
                break;
            }
            case "traitement": {
                if (a.length < 2) throw new WrongUsageException("/virus traitement <joueur> [annuler|avancer]");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                Traitement t = d.traitement;
                if (a.length > 2 && "annuler".equals(a[2])) {
                    d.traitement = null;
                    d.convalescenceFin = 0;
                    d.dirty = true;
                    ok(s, "Traitement de " + p.getName() + " annulé");
                } else if (a.length > 2 && "avancer".equals(a[2])) {
                    if (t == null) throw new CommandException("Aucun traitement en cours");
                    t.dernierePrise -= 24 * Maladies.HEURE;
                    t.debutCure -= 24 * Maladies.HEURE;
                    d.dirty = true;
                    ok(s, "La fenêtre de la prochaine prise est décalée (pour tester)");
                } else {
                    Defs.Maladie m = t == null ? null : DonneesVirus.maladie(t.maladie);
                    if (m == null) ok(s, p.getName() + " : aucun traitement" + (d.convalescenceFin > 0 ? " · en convalescence, encore "
                            + fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(d.convalescenceFin - System.currentTimeMillis()) : ""));
                    else ok(s, p.getName() + " · " + m.nom + " · prise " + Math.min(t.prise + 1, m.prises.size()) + "/" + m.prises.size()
                            + (t.prise < m.prises.size() ? " : " + Traitements.libellePrise(m.prises.get(t.prise)) : "")
                            + " · déjà donné : " + t.faits + (t.attenteSommeil ? " · attend le sommeil" : ""));
                }
                break;
            }
            case "avie": {
                if (a.length < 2) throw new WrongUsageException("/virus avie <joueur> [retirer]");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = data(p);
                if (a.length > 2 && "retirer".equals(a[2])) {
                    d.aVie.clear();
                    d.dirty = true;
                    ok(s, "Effets à vie de " + p.getName() + " retirés");
                } else {
                    List<String> l = new ArrayList<>();
                    for (String id : d.aVie) {
                        Defs.Maladie m = DonneesVirus.maladie(id);
                        l.add(m == null ? id : m.nom);
                    }
                    ok(s, p.getName() + " · effets à vie : " + (l.isEmpty() ? "aucun" : String.join(", ", l)));
                }
                break;
            }
            case "jarres": {
                BlockPos c = s.getPosition();
                int n = 0;
                for (BlockPos q : BlockPos.getAllInBoxMutable(c.add(-8, -4, -8), c.add(8, 4, 8))) {
                    net.minecraft.tileentity.TileEntity te = s.getEntityWorld().getTileEntity(q);
                    if (te instanceof fr.lenerfvoeux.hxrp.metiers.virus.block.TileJarres) n += ((fr.lenerfvoeux.hxrp.metiers.virus.block.TileJarres) te).finir();
                }
                ok(s, n + " macération(s) terminée(s) : les jarres sont prêtes à être ouvertes");
                break;
            }
            case "prise": {
                if (a.length < 2) throw new WrongUsageException("/virus prise <joueur>");
                EntityPlayerMP p = getPlayer(server, s, a[1]);
                SanteData d = Sante.get(p);
                if (d == null) throw new CommandException("Pas de données de santé pour " + p.getName());
                d.dernierePriseDeSang = 0;
                d.dirty = true;
                ok(s, p.getName() + " peut de nouveau subir une prise de sang");
                break;
            }
            case "recharger": {
                DonneesVirus.chargerConfig(Loader.instance().getConfigDir());
                for (EntityPlayerMP p : server.getPlayerList().getPlayers()) Network.NET.sendTo(new MsgDonneesVirus(), p);
                ok(s, "Données du Virus rechargées : " + DonneesVirus.PREPARATIONS.size() + " préparations, " + DonneesVirus.MALADIES.size() + " maladies");
                break;
            }
            default:
                throw new WrongUsageException(getUsage(s));
        }
    }

    private void maladie(MinecraftServer server, ICommandSender s, String[] a) throws CommandException {
        if (a.length < 3) throw new WrongUsageException("/virus maladie <donner|soigner|info> <joueur> [maladie] [stade]");
        EntityPlayerMP p = getPlayer(server, s, a[2]);
        SanteData d = data(p);
        switch (a[1]) {
            case "donner": {
                if (a.length < 4) throw new WrongUsageException("/virus maladie donner <joueur> <maladie> [stade 1-n]");
                Defs.Maladie m = DonneesVirus.maladie(a[3]);
                if (m == null) throw new CommandException("Maladie inconnue : " + a[3]);
                int st = a.length > 4 ? parseInt(a[4], 1, m.stades.size()) - 1 : 0;
                if (!Maladies.eligible(p, d, m)) s.sendMessage(new TextComponentString(TextFormatting.GOLD + "Remarque : " + m.nom + " ne touche normalement pas ce joueur ("
                        + m.toucheTexte + ")."));
                Maladies.contracter(p, d, m, st, true);
                sync(p, d);
                ok(s, p.getName() + " a maintenant " + m.nom + " (stade " + (st + 1) + "/" + m.stades.size() + ")");
                break;
            }
            case "soigner": {
                Maladies.guerir(p, d, true);
                sync(p, d);
                ok(s, p.getName() + " est guéri (immunité " + VirusConfig.immuniteHeures + " h)");
                break;
            }
            case "info": {
                Defs.Maladie m = DonneesVirus.maladie(d.maladie);
                long now = System.currentTimeMillis();
                StringBuilder b = new StringBuilder(p.getName()).append(" · groupe ").append(d.groupe.isEmpty() ? "?" : d.groupe).append(" · ");
                if (m == null) b.append("aucune maladie");
                else b.append(m.nom).append(" (").append(m.numero).append(", ").append(m.rang).append("★) stade ")
                        .append(MoteurSante.stadeActuel(d) + 1).append("/").append(m.stades.size());
                if (d.convalescenceFin > 0) b.append(" · convalescence ").append(fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(d.convalescenceFin - now));
                if (d.immuniteFin > now) b.append(" · immunisé encore ").append(fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(d.immuniteFin - now));
                if (d.prochaineMaladie > now) b.append(" · prochaine maladie dans ").append(fr.lenerfvoeux.hxrp.metiers.data.Fraicheur.duree(d.prochaineMaladie - now));
                if (!d.aVie.isEmpty()) b.append(" · à vie : ").append(d.aVie.size());
                List<String> bl = new ArrayList<>();
                for (java.util.Map.Entry<String, SanteData.Blessure> e : d.blessures.entrySet())
                    bl.add(e.getKey() + " " + e.getValue().stade + (e.getValue().reposRequis > 0 ? " (soin, repos)" : ""));
                if (!bl.isEmpty()) b.append(" · blessures : ").append(String.join(", ", bl));
                ok(s, b.toString());
                break;
            }
            default:
                throw new WrongUsageException("/virus maladie <donner|soigner|info> <joueur> [maladie] [stade]");
        }
    }

    private static void sync(EntityPlayerMP p, SanteData d) {
        d.dirty = true;
        MoteurSante.synchroniser(p, d, System.currentTimeMillis(), true);
    }

    private static SanteData data(EntityPlayerMP p) throws CommandException {
        SanteData d = Sante.get(p);
        if (d == null) throw new CommandException("Données de santé introuvables");
        return d;
    }

    private static void ok(ICommandSender s, String msg) {
        s.sendMessage(new TextComponentString(TextFormatting.GREEN + msg));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender s, String[] a, @Nullable BlockPos pos) {
        if (a.length == 1) return getListOfStringsMatchingLastWord(a, SUB);
        String sub = a[0].toLowerCase(Locale.ROOT);
        if (sub.equals("maladie")) {
            if (a.length == 2) return getListOfStringsMatchingLastWord(a, "donner", "soigner", "info");
            if (a.length == 3) return getListOfStringsMatchingLastWord(a, server.getOnlinePlayerNames());
            if (a.length == 4 && a[1].equals("donner")) return getListOfStringsMatchingLastWord(a, DonneesVirus.MALADIES.keySet());
            return Collections.emptyList();
        }
        if (sub.equals("recharger") || sub.equals("jarres")) return Collections.emptyList();
        if (a.length == 2) return getListOfStringsMatchingLastWord(a, server.getOnlinePlayerNames());
        if (a.length == 3) {
            switch (sub) {
                case "rang": return getListOfStringsMatchingLastWord(a, "0", "1", "2", "3", "aucun");
                case "blessure": return getListOfStringsMatchingLastWord(a, DonneesVirus.BLESSURES.keySet());
                case "immunite": return getListOfStringsMatchingLastWord(a, "on", "off");
                case "donner": return getListOfStringsMatchingLastWord(a, Objets.VIRUS.keySet());
                case "traitement": return getListOfStringsMatchingLastWord(a, "annuler", "avancer");
                case "avie": return getListOfStringsMatchingLastWord(a, "retirer");
                default: return Collections.emptyList();
            }
        }
        if (a.length == 4 && sub.equals("blessure")) return getListOfStringsMatchingLastWord(a, "1", "2", "3", "4", "aucune");
        return Collections.emptyList();
    }

    @Override
    public boolean isUsernameIndex(String[] a, int i) {
        if (a.length == 0) return false;
        if ("maladie".equals(a[0])) return i == 2;
        return i == 1 && !"recharger".equals(a[0]);
    }
}
