package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusRegistre;
import fr.lenerfvoeux.hxrp.metiers.virus.block.TilePresentoir;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgFormulaire;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgJeuVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.SeancesVirus;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Écrans du Virus ouverts sur ordre du serveur (mini-jeux, formulaire de l'officine) ou par un objet (lectures). */
@SideOnly(Side.CLIENT)
public final class ClientJeux {
    private static final Map<String, int[]> ICONES = new HashMap<>();

    private ClientJeux() {}

    public static void recevoir(IMessage m) {
        Minecraft mc = Minecraft.getMinecraft();
        if (m instanceof MsgJeuVirus) mc.displayGuiScreen(new GuiJeuVirus((MsgJeuVirus) m));
        else if (m instanceof MsgFormulaire) mc.displayGuiScreen(new GuiFormulaire((MsgFormulaire) m));
    }

    public static void modeles() {
        ClientRegistry.bindTileEntitySpecialRenderer(TilePresentoir.class, new RenduPresentoir());
    }

    /** Lire une ordonnance, un carnet de consultation ou le parchemin du Second Souffle. */
    public static void lire(ItemStack s) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (s.getItem() == VirusRegistre.CARNET) mc.displayGuiScreen(new GuiCarnetConsultation(s));
            else if (s.getItem() == VirusRegistre.PARCHEMIN) mc.displayGuiScreen(new GuiGrimoire(true));
            else if (s.getItem() == VirusRegistre.ORDONNANCE) mc.displayGuiScreen(new GuiOrdonnance(s));
        });
    }

    public static void grimoire() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiGrimoire(false)));
    }

    // ================================================================== contexte des scènes
    static ContexteVirus contexte(MsgJeuVirus m) {
        ContexteVirus c = new ContexteVirus();
        c.type = m.type;
        c.rang = m.rang;
        c.param = m.param;
        c.sujet = m.sujet;
        Defs.Preparation p = DonneesVirus.preparation(m.prep);
        switch (m.type) {
            case SeancesVirus.ETAPE:
                c.titre = p == null ? m.prep : p.nom;
                c.etape = m.etape + 1;
                c.etapes = Math.max(1, m.total);
                break;
            case SeancesVirus.ADMIN:
                c.titre = (p == null ? m.prep : p.nom) + " · " + m.sujet;
                break;
            case SeancesVirus.SANG:
                c.titre = "Prise de sang · " + m.sujet;
                break;
            default:
                c.titre = "Analyse de sang · " + m.sujet;
                break;
        }
        if (p != null) {
            c.forme = p.forme == null ? "" : p.forme;
            Set<String> vus = new LinkedHashSet<>(p.ingredients);
            for (String id : vus) {
                if ("seringue_vide".equals(id)) continue;
                int[] ic = icone(id);
                if (ic != null) c.icones.add(ic);
            }
            c.iconePrep = icone(p.produit());
            int coul = m.type == SeancesVirus.ADMIN ? couleurVive(c.iconePrep) : 0;
            if (coul == 0 && !c.icones.isEmpty()) coul = couleurVive(c.icones.get(0));
            if (coul == 0 && !c.icones.isEmpty()) coul = ContexteVirus.moyenne(c.icones.get(0));
            if (coul != 0) c.couleur = coul;
        }
        return c;
    }

    /** Couleur moyenne des pixels bien colorés d'une icône (le liquide d'une fiole plutôt que son verre). */
    static int couleurVive(int[] ic) {
        if (ic == null) return 0;
        long r = 0, g = 0, b = 0, n = 0;
        for (int px : ic) {
            if ((px >>> 24) < 255) continue;
            int rr = (px >> 16) & 255, gg = (px >> 8) & 255, bb = px & 255;
            int max = Math.max(rr, Math.max(gg, bb)), min = Math.min(rr, Math.min(gg, bb));
            if (max < 60 || (max - min) < max * 0.3) continue;
            r += rr;
            g += gg;
            b += bb;
            n++;
        }
        if (n < 6) return 0;
        return 0xFF000000 | (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    /** Icône 32x32 d'un objet du Virus (textures/items/virus/) ou du Gourmet (textures/items/), mise en cache. */
    static int[] icone(String id) {
        if (id == null || id.indexOf(':') >= 0) return null;
        String cle = "eau".equals(id) ? "bouteille_d_eau" : id;
        return ICONES.computeIfAbsent(cle, k -> {
            for (String dossier : new String[]{"textures/items/virus/", "textures/items/"}) {
                try (InputStream in = Minecraft.getMinecraft().getResourceManager()
                        .getResource(new ResourceLocation(HxrpMetiers.MODID, dossier + k + ".png")).getInputStream()) {
                    BufferedImage im = ImageIO.read(in);
                    if (im == null || im.getWidth() < 32 || im.getHeight() < 32) continue;
                    int[] px = new int[32 * 32];
                    im.getRGB(0, 0, 32, 32, px, 0, 32);
                    return px;
                } catch (Exception ignored) {
                    // essayer le dossier suivant
                }
            }
            return null;
        });
    }
}
