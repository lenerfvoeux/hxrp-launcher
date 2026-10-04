package fr.lenerfvoeux.hxrp.metiers.virus.officine;

import fr.lenerfvoeux.hxrp.metiers.data.Fraicheur;
import fr.lenerfvoeux.hxrp.metiers.item.IFoodItem;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Qualites;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.block.BlockMachine;
import fr.lenerfvoeux.hxrp.metiers.virus.block.TileJarres;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Machine;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgFormulaire;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Sante;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.SanteData;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le moteur de l'officine : ce que le Virus peut préparer, ce qu'il consomme, et la note finale :
 * (somme des notes des étapes + bonus d'étoiles) ÷ nombre d'étapes, plafonnée à 100, moins la pénalité de fraîcheur.
 * Sous 80 % : préparation ratée, inutilisable.
 */
public final class Officine {
    private static final long HEURE = 3_600_000L;

    private Officine() {}

    private static void dire(EntityPlayer p, String s) {
        p.sendMessage(new TextComponentString(s));
    }

    // ================================================================== aux machines
    /** Clic droit sur une machine de l'officine. */
    public static void utiliser(EntityPlayerMP p, Machine m, BlockPos pos) {
        ItemStack held = p.getHeldItemMainhand();
        if (EnCoursVirus.est(held)) {
            if (!Soins.verifierVirus(p)) return;
            Defs.Preparation x = EnCoursVirus.preparation(held);
            Defs.Etape e = EnCoursVirus.etapeEnCours(held);
            if (x == null || e == null) {
                dire(p, TextFormatting.RED + "Cette préparation n'existe plus dans le formulaire.");
                return;
            }
            Machine att = Machine.de(e.machine);
            if (att != m) {
                dire(p, TextFormatting.GOLD + "Cette étape se fait à : " + e.libelle());
                return;
            }
            if (m == Machine.JARRES) {
                TileJarres j = jarres(p, pos);
                if (j == null) return;
                if (j.libre() < 0) {
                    dire(p, TextFormatting.GOLD + "Les trois jarres sont déjà scellées. " + etatJarres(j));
                    return;
                }
            }
            SeancesVirus.ouvrir(p, SeancesVirus.ETAPE, m.jeu, x.id, x.nom, EnCoursVirus.etape(held), x.etapes.size(),
                    EnCoursVirus.rang(held), m.parametre(e), pos, null, EnumHand.MAIN_HAND);
            return;
        }
        if (m == Machine.JARRES) {
            TileJarres j = jarres(p, pos);
            if (j != null && j.scellees() > 0) {
                ouvrirJarre(p, j, pos);
                return;
            }
        }
        if (m == Machine.TABLE) {
            if (!Soins.verifierVirus(p)) return;
            Network.NET.sendTo(new MsgFormulaire(pos, Math.max(0, rang(p)), formulaire(p)), p);
            p.world.playSound(null, pos, VirusSons.PAGE, SoundCategory.BLOCKS, 0.7f, 1f);
            return;
        }
        dire(p, TextFormatting.GOLD + m.label + TextFormatting.GRAY + " · " + usage(m)
                + ". Commence une préparation à la table de préparation, puis reviens avec elle en main.");
    }

    public static String usage(Machine m) {
        switch (m) {
            case YAGEN: return "broyer finement graines, racines et écorces";
            case HACHOIR: return "hacher plantes, champignons et peaux";
            case CHAUDRON: return "infusions, décoctions, réductions, fondre la cire";
            case ALAMBIC: return "eaux florales, alcools, concentrer un élixir";
            case JARRES: return "macérer et fermenter, en heures réelles";
            case BALANCE: return "doser juste (dose stricte : ne jamais dépasser)";
            case MORTIER: return "piler, réduire en pâte";
            case PILULIER: return "rouler et couper les pilules";
            default: return "le formulaire et les gestes de finition";
        }
    }

    private static TileJarres jarres(EntityPlayerMP p, BlockPos pos) {
        TileEntity te = p.world.getTileEntity(pos);
        return te instanceof TileJarres ? (TileJarres) te : null;
    }

    private static String etatJarres(TileJarres j) {
        long now = System.currentTimeMillis();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < TileJarres.JARRES; i++) {
            ItemStack s = j.contenu(i);
            if (s.isEmpty()) continue;
            Defs.Preparation x = EnCoursVirus.preparation(s);
            if (b.length() > 0) b.append(" · ");
            b.append(x == null ? "?" : x.nom).append(" : ")
                    .append(now >= j.pret(i) ? "prête" : "encore " + Fraicheur.duree(j.pret(i) - now));
        }
        return b.toString();
    }

    /** Main libre ou autre objet : ouvre la première jarre prête, sinon dit où en sont les macérations. */
    private static void ouvrirJarre(EntityPlayerMP p, TileJarres j, BlockPos pos) {
        int i = j.prete(System.currentTimeMillis());
        if (i < 0) {
            dire(p, TextFormatting.GRAY + "Jarres scellées · " + etatJarres(j));
            return;
        }
        ItemStack s = j.ouvrir(i);
        p.world.playSound(null, pos, VirusSons.VERRE, SoundCategory.BLOCKS, 0.8f, 0.8f);
        Defs.Preparation x = EnCoursVirus.preparation(s);
        if (EnCoursVirus.fini(s)) {
            ItemStack fin = terminer(s);
            donner(p, fin);
            annoncerFin(p, x, s, fin);
        } else {
            ItemHandlerHelper.giveItemToPlayer(p, s);
            Defs.Etape e = EnCoursVirus.etapeEnCours(s);
            dire(p, TextFormatting.GREEN + "Tu descelles la jarre : " + (x == null ? "?" : x.nom) + TextFormatting.GRAY
                    + " · suivante : " + TextFormatting.GOLD + (e == null ? "?" : e.libelle()));
        }
    }

    // ================================================================== lancer, jouer, finir
    /** Formulaire : le Virus lance une préparation (vérifie la table, le rang, les ingrédients). */
    public static void lancer(EntityPlayerMP p, String id, BlockPos pos) {
        Defs.Preparation x = DonneesVirus.preparation(id);
        if (x == null || x.etapes.isEmpty() || !Soins.verifierVirus(p)) return;
        if (p.getDistanceSq(pos) > 64) return;
        Block b = p.world.getBlockState(pos).getBlock();
        if (!(b instanceof BlockMachine) || ((BlockMachine) b).machine != Machine.TABLE) return;
        if (!visible(p, x)) {
            dire(p, TextFormatting.RED + (x.secret ? "Il te faut le parchemin du Second Souffle, et être Virus 3★." : "Cette préparation demande un Virus " + x.rang + "★."));
            return;
        }
        ItemStack en = demarrer(p, x);
        if (en.isEmpty()) {
            dire(p, TextFormatting.RED + "Il te manque des ingrédients (ou ils sont avariés).");
            return;
        }
        ItemHandlerHelper.giveItemToPlayer(p, en);
        p.world.playSound(null, pos, VirusSons.PAGE, SoundCategory.BLOCKS, 0.6f, 1.2f);
        dire(p, TextFormatting.GREEN + "Préparation lancée : " + x.nom + TextFormatting.GRAY + " · première étape : "
                + TextFormatting.GOLD + x.etapes.get(0).libelle());
    }

    /** Une étape vient d'être jouée (note vérifiée par le serveur). */
    static void etapeJouee(EntityPlayerMP p, SeancesVirus.Seance s, double note) {
        ItemStack held = p.getHeldItem(EnumHand.MAIN_HAND);
        Defs.Preparation x = EnCoursVirus.est(held) ? EnCoursVirus.preparation(held) : null;
        if (x == null || !x.id.equals(s.prep) || EnCoursVirus.etape(held) != s.etape) {
            dire(p, TextFormatting.RED + "Garde ta préparation en main pendant le mini-jeu.");
            return;
        }
        Defs.Etape e = EnCoursVirus.etapeEnCours(held);
        Machine m = e == null ? null : Machine.de(e.machine);
        if (m != null) son(p, s.pos, m);
        if (m == Machine.JARRES) {
            TileJarres j = jarres(p, s.pos);
            if (j == null || j.libre() < 0) {
                dire(p, TextFormatting.RED + "Plus de jarre libre : la macération n'a pas commencé.");
                return;
            }
            EnCoursVirus.noter(held, note);
            long pret = System.currentTimeMillis() + (long) (Math.max(0, e.heures) * HEURE * VirusConfig.macerationFacteur);
            j.sceller(held, pret);
            p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            dire(p, TextFormatting.GRAY + "Versé à " + Math.round(note) + " % · la jarre est scellée, prête dans "
                    + TextFormatting.GOLD + Fraicheur.duree(pret - System.currentTimeMillis()) + TextFormatting.GRAY + " (temps réel).");
            p.inventoryContainer.detectAndSendChanges();
            return;
        }
        EnCoursVirus.noter(held, note);
        if (EnCoursVirus.fini(held)) {
            ItemStack fin = terminer(held);
            p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
            donner(p, fin);
            annoncerFin(p, x, held, fin);
        } else {
            Defs.Etape suiv = EnCoursVirus.etapeEnCours(held);
            dire(p, TextFormatting.GRAY + "Étape notée " + Math.round(note) + " % · suivante : " + TextFormatting.GOLD
                    + (suiv == null ? "?" : suiv.libelle()));
        }
        p.inventoryContainer.detectAndSendChanges();
    }

    private static void donner(EntityPlayerMP p, ItemStack s) {
        if (s.isEmpty()) return;
        if (p.getHeldItemMainhand().isEmpty()) p.setHeldItem(EnumHand.MAIN_HAND, s);
        else ItemHandlerHelper.giveItemToPlayer(p, s);
    }

    private static void annoncerFin(EntityPlayerMP p, Defs.Preparation x, ItemStack enCours, ItemStack fin) {
        int note = noteFinale(enCours);
        String nom = x == null ? "?" : x.nom;
        if (fin.isEmpty() || fin.getItem() == VirusRegistre.RATEE) {
            dire(p, TextFormatting.RED + nom + " ratée · " + note + " %" + TextFormatting.GRAY + " (il faut " + VirusConfig.seuilReussite + " % : inutilisable)");
            p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.2f);
            return;
        }
        TextFormatting c = note >= VirusConfig.seuilBonus ? TextFormatting.AQUA : TextFormatting.GREEN;
        dire(p, c + nom + " prête · " + note + " %" + (note >= VirusConfig.seuilBonus ? " · préparation d'exception !" : "")
                + (fin.getCount() > 1 ? TextFormatting.GRAY + " (×" + fin.getCount() + ")" : ""));
        p.world.playSound(null, p.posX, p.posY, p.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.4f, 1.6f);
    }

    private static void son(EntityPlayerMP p, BlockPos pos, Machine m) {
        SoundEvent e;
        switch (m) {
            case YAGEN: e = VirusSons.MEULE; break;
            case HACHOIR: case PILULIER: e = VirusSons.LAME; break;
            case CHAUDRON: e = VirusSons.BOUILLON; break;
            case MORTIER: e = VirusSons.PILON; break;
            case TABLE: e = VirusSons.PAGE; break;
            default: e = VirusSons.VERRE; break;
        }
        if (e != null) p.world.playSound(null, pos, e, SoundCategory.BLOCKS, 0.8f, 1f);
    }

    public static int rang(EntityPlayer p) {
        SanteData d = Sante.get(p);
        return d == null ? -1 : d.rangVirus;
    }

    /** Une préparation est visible au formulaire à ce rang (le Second Souffle : seulement avec le parchemin, à 3★). */
    public static boolean visible(EntityPlayer p, Defs.Preparation x) {
        int r = rang(p);
        if (r < 0 || x.rang > r) return false;
        if (x.secret) return r >= 3 && porte(p, "parchemin_du_second_souffle");
        return true;
    }

    public static boolean porte(EntityPlayer p, String id) {
        Item i = Objets.item(id);
        if (i == null) return false;
        for (int k = 0; k < p.inventory.getSizeInventory(); k++) if (p.inventory.getStackInSlot(k).getItem() == i) return true;
        return false;
    }

    /** Nombre d'exemplaires de chaque ingrédient demandés par la recette. */
    public static Map<String, Integer> besoins(Defs.Preparation x) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String id : x.ingredients) m.merge("eau".equals(id) ? "bouteille_d_eau" : id, 1, Integer::sum);
        return m;
    }

    /** Une pile utilisable : le bon objet, pas avarié. */
    private static boolean utilisable(ItemStack s, Item i, long now) {
        if (s.isEmpty() || s.getItem() != i) return false;
        return !(s.getItem() instanceof IFoodItem) || !Fraicheur.rotten(s, now);
    }

    public static int compter(EntityPlayer p, String id, long now) {
        Item i = Objets.item(id);
        if (i == null) return 0;
        int n = 0;
        for (int k = 0; k < p.inventory.getSizeInventory(); k++) {
            ItemStack s = p.inventory.getStackInSlot(k);
            if (utilisable(s, i, now)) n += s.getCount();
        }
        return n;
    }

    public static List<String> manquants(EntityPlayer p, Defs.Preparation x) {
        long now = System.currentTimeMillis();
        List<String> l = new ArrayList<>();
        for (Map.Entry<String, Integer> e : besoins(x).entrySet())
            if (compter(p, e.getKey(), now) < e.getValue()) l.add(e.getKey());
        return l;
    }

    /** Le formulaire de la table de préparation : tout ce que le Virus sait préparer, et ce qui lui manque. */
    public static List<MsgFormulaire.Ligne> formulaire(EntityPlayer p) {
        List<MsgFormulaire.Ligne> out = new ArrayList<>();
        for (Defs.Preparation x : DonneesVirus.PREPARATIONS.values()) {
            if (!visible(p, x)) continue;
            MsgFormulaire.Ligne l = new MsgFormulaire.Ligne();
            l.id = x.id;
            l.manquants.addAll(manquants(p, x));
            l.ok = l.manquants.isEmpty();
            out.add(l);
        }
        return out;
    }

    /**
     * Consomme les ingrédients et rend la préparation en cours. La fraîcheur retenue est celle de l'ingrédient
     * le plus abîmé. Renvoie une pile vide si quelque chose manque.
     */
    public static ItemStack demarrer(EntityPlayer p, Defs.Preparation x) {
        if (!manquants(p, x).isEmpty()) return ItemStack.EMPTY;
        long now = System.currentTimeMillis();
        double pire = 1;
        for (Map.Entry<String, Integer> e : besoins(x).entrySet()) {
            Item i = Objets.item(e.getKey());
            int reste = e.getValue();
            for (int k = 0; k < p.inventory.getSizeInventory() && reste > 0; k++) {
                ItemStack s = p.inventory.getStackInSlot(k);
                if (!utilisable(s, i, now)) continue;
                if (s.getItem() instanceof IFoodItem && Fraicheur.perishable(s) && Fraicheur.stamped(s))
                    pire = Math.min(pire, Fraicheur.fraction(s, now));
                int n = Math.min(reste, s.getCount());
                p.inventory.decrStackSize(k, n);
                reste -= n;
            }
        }
        p.inventory.markDirty();
        return EnCoursVirus.creer(x, pire, Math.max(0, rang(p)));
    }

    /** Note finale d'une préparation dont toutes les étapes sont jouées. */
    public static int noteFinale(ItemStack enCours) {
        Defs.Preparation x = EnCoursVirus.preparation(enCours);
        double[] n = EnCoursVirus.notes(enCours);
        if (x == null || n.length == 0) return 0;
        double somme = 0;
        for (double v : n) somme += v;
        int[] b = VirusConfig.bonusEtoiles;
        int r = EnCoursVirus.rang(enCours);
        somme += b == null || b.length == 0 ? 0 : b[Math.min(b.length - 1, r)];
        double note = Math.min(100, somme / n.length) - Fraicheur.penalty(EnCoursVirus.fraicheur(enCours));
        return (int) Math.round(Math.max(0, Math.min(100, note)));
    }

    /** Le produit fini : la préparation notée, ou une préparation ratée sous le seuil. */
    public static ItemStack terminer(ItemStack enCours) {
        Defs.Preparation x = EnCoursVirus.preparation(enCours);
        if (x == null) return ItemStack.EMPTY;
        int note = noteFinale(enCours);
        if (note < VirusConfig.seuilReussite) {
            ItemStack r = new ItemStack(VirusRegistre.RATEE);
            NBTTagCompound t = new NBTTagCompound();
            t.setString("v_rate", x.nom);
            t.setInteger(Qualites.NOTE, note);
            r.setTagCompound(t);
            return r;
        }
        Item i = Objets.item(x.produit());
        if (i == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(i, Math.max(1, x.quantite));
        if (i instanceof ItemPreparation) Qualites.noter(out, note, EnCoursVirus.rang(enCours));
        else Fraicheur.stamp(out, System.currentTimeMillis());
        return out;
    }
}
