package fr.lenerfvoeux.hxrp.metiers.virus.sante;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusConfig;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgEtatVirus;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Le cœur du Hunter Virus côté serveur, appelé une fois par seconde pour chaque joueur : groupe sanguin,
 * maladies tirées au hasard, avancée des stades, épisodes de symptômes, blessures, traitement, puis application
 * des effets (potions sans particules, cœurs max, faim et soif du Gourmet, saignements) et envoi au client des
 * effets qu'il doit dessiner.
 */
public final class MoteurSante {
    public static final DamageSource MAL = new DamageSource("hxrp_mal").setDamageBypassesArmor();
    private static final UUID COEURS = UUID.fromString("6a1c2e4f-5b3d-4e8a-9f10-2b3c4d5e6f71");
    private static final UUID LENTEUR = UUID.fromString("7b2d3f5a-6c4e-4f9b-8a21-3c4d5e6f7a82");
    private static final Random RNG = new Random();

    private MoteurSante() {}

    public static void tick(EntityPlayerMP p) {
        SanteData d = Sante.get(p);
        if (d == null) return;
        if (p.ticksExisted % 20 != 0) return;
        try {
            seconde(p, d, System.currentTimeMillis());
        } catch (RuntimeException ex) {
            HxrpMetiers.LOG.error("Virus : erreur dans le suivi de santé de " + p.getName(), ex);
        }
    }

    static void seconde(EntityPlayerMP p, SanteData d, long now) {
        if (d.groupe.isEmpty() || p.ticksExisted % 600 == 0) identite(p, d);
        if (!p.isEntityAlive() || p.isSpectator()) return;
        Maladies.hasard(p, d, now);
        progression(p, d, now);
        episodes(p, d, now);
        boolean auRepos = !p.isSprinting() && now - d.dernierCombat > 5000;
        Blessures.seconde(p, d, now, auRepos);
        Traitements.seconde(p, d, now);
        appliquer(p, d, now);
        synchroniser(p, d, now, false);
    }

    // ================================================================== groupe sanguin
    private static void identite(EntityPlayerMP p, SanteData d) {
        long c = Identite.creation(p);
        if (d.groupe.isEmpty()) {
            d.groupe = SanteData.GROUPES[RNG.nextInt(SanteData.GROUPES.length)];
            d.identiteRef = c;
            d.dirty = true;
            return;
        }
        if (c != 0 && d.identiteRef == 0) {
            d.identiteRef = c;
            d.dirty = true;
        } else if (c != 0 && c != d.identiteRef && VirusConfig.nouveauGroupeParPersonnage) {
            d.groupe = SanteData.GROUPES[RNG.nextInt(SanteData.GROUPES.length)];
            d.identiteRef = c;
            d.dirty = true;
        }
    }

    // ================================================================== stades
    private static void progression(EntityPlayerMP p, SanteData d, long now) {
        if (!d.malade()) return;
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m == null) {
            HxrpMetiers.LOG.warn("Virus : {} avait la maladie {} qui n'existe plus, retirée", p.getName(), d.maladie);
            d.maladie = "";
            d.traitement = null;
            d.convalescenceFin = 0;
            d.dirty = true;
            return;
        }
        if (d.convalescenceFin > 0) return;
        int s = Maladies.stade(m, d.maladieDebut, now);
        if (s > d.stadeAnnonce) {
            d.stadeAnnonce = s;
            Defs.Stade st = m.stades.get(s);
            Maladies.message(p, st.message);
            if (st.vie) Maladies.acquerirVie(p, d, m);
            d.prochainEpisode = Math.min(d.prochainEpisode, now + 60_000);
            d.dirty = true;
        }
    }

    public static int stadeActuel(SanteData d) {
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m == null) return -1;
        return Math.max(0, Math.min(m.stades.size() - 1, d.stadeAnnonce));
    }

    // ================================================================== épisodes
    private static void episodes(EntityPlayerMP p, SanteData d, long now) {
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m != null && d.convalescenceFin == 0) {
            if (d.prochainEpisode == 0 || d.prochainEpisode < now - 10 * Maladies.MINUTE) {
                d.prochainEpisode = now + 60_000 + RNG.nextInt(120_000);
            } else if (now >= d.prochainEpisode) {
                Defs.Stade st = m.stades.get(stadeActuel(d));
                episode(p, d, st.effets, st.message, now);
                d.prochainEpisode = now + Maladies.intervalleEpisode();
            }
        }
        // effets à vie « par moments »
        List<Defs.Maladie> vie = new ArrayList<>();
        for (String id : d.aVie) {
            Defs.Maladie v = DonneesVirus.maladie(id);
            if (v != null && aDesEpisodes(v.dernierStade().effets)) vie.add(v);
        }
        if (!vie.isEmpty()) {
            if (d.prochainEpisodeVie == 0 || d.prochainEpisodeVie < now - Maladies.HEURE) {
                d.prochainEpisodeVie = now + delaiVie();
            } else if (now >= d.prochainEpisodeVie) {
                Defs.Maladie v = vie.get(RNG.nextInt(vie.size()));
                episode(p, d, v.dernierStade().effets, v.dernierStade().message, now);
                d.prochainEpisodeVie = now + delaiVie();
            }
        }
        // blessures dont un effet revient par moments (vertige, nausée…)
        List<Defs.EffetSpec> bl = new ArrayList<>();
        String msg = null;
        for (Map.Entry<String, SanteData.Blessure> e : d.blessures.entrySet()) {
            Defs.Blessure def = DonneesVirus.blessure(e.getKey());
            if (def == null || e.getValue().stade <= 0 || e.getValue().reposRequis > 0) continue;
            for (int k = 0; k < e.getValue().stade && k < def.stades.size(); k++)
                for (Defs.EffetSpec s : def.stades.get(k).effets) if (!s.continu()) {
                    bl.add(s);
                    msg = def.stades.get(e.getValue().stade - 1).message;
                }
        }
        if (!bl.isEmpty()) {
            if (d.prochainEpisodeBlessure == 0 || d.prochainEpisodeBlessure < now - Maladies.HEURE) {
                d.prochainEpisodeBlessure = now + 120_000 + RNG.nextInt(120_000);
            } else if (now >= d.prochainEpisodeBlessure) {
                episode(p, d, bl, msg, now);
                d.prochainEpisodeBlessure = now + 180_000 + RNG.nextInt(180_000);
            }
        }
    }

    private static long delaiVie() {
        return (long) (VirusConfig.episodeVieMinutes * (0.5 + RNG.nextDouble()) * Maladies.MINUTE);
    }

    private static boolean aDesEpisodes(List<Defs.EffetSpec> l) {
        for (Defs.EffetSpec e : l) if (!e.continu()) return true;
        return false;
    }

    /** Un épisode : le message RP, puis chaque effet « par épisode » pour sa durée. */
    public static void episode(EntityPlayerMP p, SanteData d, List<Defs.EffetSpec> effets, String message, long now) {
        Maladies.message(p, message);
        for (Defs.EffetSpec s : effets) {
            if (s.continu() || !condition(s.condition, p, d, now)) continue;
            Effet e = Effet.de(s.code);
            if (e == null) continue;
            if (e == Effet.DEGATS) {
                degatsLegers(p, 1 + RNG.nextInt(2));
                continue;
            }
            d.episodes.merge(e.code, now + e.dureeEpisode(RNG), Math::max);
            if (e == Effet.TOUX || e == Effet.ETERNUE) annoncer(p, e);
        }
        d.dirty = true;
    }

    /** Les joueurs proches entendent la toux et voient le message RP. */
    private static void annoncer(EntityPlayerMP p, Effet e) {
        String nom = Identite.nom(p);
        String txt = e == Effet.TOUX ? nom + " est pris d'une quinte de toux." : nom + " éternue bruyamment.";
        for (EntityPlayer o : p.world.playerEntities)
            if (o != p && o.getDistanceSq(p) < 16 * 16) o.sendMessage(new TextComponentString(TextFormatting.GRAY + "" + TextFormatting.ITALIC + "* " + txt));
        p.world.playSound(null, p.posX, p.posY, p.posZ, e == Effet.TOUX ? VirusSons.TOUX : VirusSons.ETERNUEMENT, SoundCategory.PLAYERS,
                0.9f, 0.85f + RNG.nextFloat() * 0.3f);
    }

    public static boolean condition(String c, EntityPlayerMP p, SanteData d, long now) {
        if (c == null || c.isEmpty()) return true;
        switch (c) {
            case "nuit": return Traitements.estNuit(p.world);
            case "jour": return !Traitements.estNuit(p.world);
            case "eau": return p.isInWater();
            case "y100": return p.posY > 100;
            case "combat": return now - d.dernierCombat < 60_000;
            default: return true;
        }
    }

    // ================================================================== effets en cours
    /** Effets actifs et leur fin (Long.MAX_VALUE : tant que la cause dure). */
    public static EnumMap<Effet, Long> actifs(EntityPlayerMP p, SanteData d, long now) {
        EnumMap<Effet, Long> a = new EnumMap<>(Effet.class);
        Defs.Maladie m = DonneesVirus.maladie(d.maladie);
        if (m != null && d.convalescenceFin == 0) continus(a, m.stades.get(stadeActuel(d)).effets, p, d, now);
        for (String id : d.aVie) {
            Defs.Maladie v = DonneesVirus.maladie(id);
            if (v != null) continus(a, v.dernierStade().effets, p, d, now);
        }
        for (Map.Entry<String, SanteData.Blessure> e : d.blessures.entrySet()) {
            Defs.Blessure def = DonneesVirus.blessure(e.getKey());
            if (def == null) continue;
            for (int k = 0; k < e.getValue().stade && k < def.stades.size(); k++) continus(a, def.stades.get(k).effets, p, d, now);
        }
        d.episodes.entrySet().removeIf(e -> e.getValue() <= now);
        for (Map.Entry<String, Long> e : d.episodes.entrySet()) {
            Effet ef = Effet.de(e.getKey());
            if (ef != null) a.merge(ef, e.getValue(), Math::max);
        }
        return a;
    }

    private static void continus(EnumMap<Effet, Long> a, List<Defs.EffetSpec> l, EntityPlayerMP p, SanteData d, long now) {
        for (Defs.EffetSpec s : l) {
            if (!s.continu() || !condition(s.condition, p, d, now)) continue;
            Effet e = Effet.de(s.code);
            if (e != null) a.put(e, Long.MAX_VALUE);
        }
    }

    /** Cœurs max retirés : chaque séquelle compte (−4 et −2 font −6), au plus 8 cœurs. */
    private static int coeursRetires(SanteData d) {
        int n = 0;
        for (String id : d.aVie) {
            Defs.Maladie v = DonneesVirus.maladie(id);
            if (v == null) continue;
            for (Defs.EffetSpec s : v.dernierStade().effets) {
                if ("coeur-1".equals(s.code)) n += 1;
                else if ("coeur-2".equals(s.code)) n += 2;
                else if ("coeur-4".equals(s.code)) n += 4;
            }
        }
        return Math.min(8, n);
    }

    private static void appliquer(EntityPlayerMP p, SanteData d, long now) {
        EnumMap<Effet, Long> a = actifs(p, d, now);
        boolean creatif = p.capabilities.isCreativeMode;
        int aggr = 0;
        for (SanteData.Blessure b : d.blessures.values()) if (b.stade >= 4 && b.reposRequis == 0) aggr = Math.max(aggr, b.aggravation);
        // lenteur, faiblesse, nausée, cécité
        int lent = a.containsKey(Effet.LENTEUR2) ? 1 : a.containsKey(Effet.LENTEUR1) ? 0 : -1;
        if (lent >= 0 && aggr >= 2) lent = Math.min(2, lent + 1);
        int faible = a.containsKey(Effet.FAIBLESSE2) ? 1 : a.containsKey(Effet.FAIBLESSE1) ? 0 : -1;
        if (faible >= 0 && aggr >= 3) faible = Math.min(2, faible + 1);
        if (!creatif) {
            if (lent >= 0) potion(p, MobEffects.SLOWNESS, lent);
            if (faible >= 0) potion(p, MobEffects.WEAKNESS, faible);
            if (a.containsKey(Effet.NAUSEE)) potion(p, MobEffects.NAUSEA, 0);
            if (a.containsKey(Effet.CECITE)) potion(p, MobEffects.BLINDNESS, 0);
        }
        modificateur(p.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED), LENTEUR, "Lenteur légère (Virus)",
                a.containsKey(Effet.LENTEUR0) && lent < 0 && !creatif ? -0.08 : 0, 1);
        // cœurs max
        int coeurs = coeursRetires(d);
        modificateur(p.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH), COEURS, "Séquelles (Virus)", -2.0 * coeurs, 0);
        if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        // métabolisme (lu par le Gourmet)
        d.multFaim = a.containsKey(Effet.FAIM2) ? 2.0 : a.containsKey(Effet.FAIM1) ? 1.5 : 1.0;
        d.multSoif = a.containsKey(Effet.SOIF2) ? 2.0 : a.containsKey(Effet.SOIF1) ? 1.5 : 1.0;
        d.facteurNourriture = a.containsKey(Effet.NOURRITURE25) ? 0.75 : a.containsKey(Effet.NOURRITURE15) ? 0.85 : 1.0;
        d.pasDeRegen = a.containsKey(Effet.NOREGEN);
        d.recupLente = a.containsKey(Effet.RECUP);
        d.lait = a.containsKey(Effet.LAIT);
        // saignements et poison
        double f = 1 + aggr * 0.5;
        if (a.containsKey(Effet.HEMORRAGIE) && now - d.dernierSaignement >= 6000 / f) {
            d.dernierSaignement = now;
            degatsLegers(p, 1);
        } else if (a.containsKey(Effet.SAIGNEMENT) && now - d.dernierSaignement >= 90_000 / f) {
            d.dernierSaignement = now;
            degatsLegers(p, 1);
        }
        if (a.containsKey(Effet.POISON) && now - d.dernierPoison >= 25_000 / f) {
            d.dernierPoison = now;
            degatsLegers(p, 1);
        }
        // course
        if (a.containsKey(Effet.NOSPRINT) && p.isSprinting()) p.setSprinting(false);
        // dégâts en marchant (fracture ouverte)
        double dx = p.posX - d.dernierX, dz = p.posZ - d.dernierZ;
        double bouge = Math.sqrt(dx * dx + dz * dz);
        d.dernierX = p.posX;
        d.dernierY = p.posY;
        d.dernierZ = p.posZ;
        if (a.containsKey(Effet.DEGATS_MARCHE) && p.onGround && bouge > 1.5 && bouge < 20) {
            d.marche += 1;
            if (d.marche >= 4) {
                d.marche = 0;
                degatsLegers(p, 1);
            }
        }
        d.actifs = a;
    }

    private static void potion(EntityPlayerMP p, Potion po, int amp) {
        PotionEffect cur = p.getActivePotionEffect(po);
        int duree = po == MobEffects.NAUSEA ? 120 : 70;
        if (cur == null || cur.getAmplifier() < amp || (cur.getAmplifier() == amp && cur.getDuration() < duree - 30))
            p.addPotionEffect(new PotionEffect(po, duree, amp, true, false));
    }

    private static void modificateur(IAttributeInstance att, UUID id, String nom, double valeur, int operation) {
        if (att == null) return;
        AttributeModifier m = att.getModifier(id);
        if (m != null && Math.abs(m.getAmount() - valeur) < 1e-6) return;
        if (m != null) att.removeModifier(id);
        if (valeur != 0) att.applyModifier(new AttributeModifier(id, nom, valeur, operation).setSaved(false));
    }

    /** Dégâts d'une maladie ou d'une blessure : on n'en meurt jamais (il reste au moins un demi-cœur). */
    public static void degatsLegers(EntityPlayerMP p, float n) {
        if (p.capabilities.isCreativeMode || !p.isEntityAlive()) return;
        if (!VirusConfig.blessuresMortelles) n = Math.min(n, p.getHealth() - 1);
        if (n <= 0) return;
        p.hurtResistantTime = 0;
        p.attackEntityFrom(MAL, n);
    }

    // ================================================================== client
    /** Envoie au client les effets à dessiner et l'état de l'ordonnance, quand ils changent (ou toutes les 10 s). */
    public static void synchroniser(EntityPlayerMP p, SanteData d, long now, boolean force) {
        MsgEtatVirus m = MsgEtatVirus.de(p, d, now);
        String sig = m.signature();
        if (!force && !d.dirty && sig.equals(d.dernierEnvoi) && p.ticksExisted - d.dernierEnvoiTick < 200) return;
        d.dernierEnvoi = sig;
        d.dernierEnvoiTick = p.ticksExisted;
        d.dirty = false;
        Network.NET.sendTo(m, p);
    }
}
