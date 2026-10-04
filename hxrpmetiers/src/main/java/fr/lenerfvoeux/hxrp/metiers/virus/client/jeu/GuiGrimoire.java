package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.client.ClientVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Le Grimoire des maladies : un livre de cuir aux pages qui se tournent. Toutes les maladies y sont dès le départ,
 * une double page chacune (description et sang à gauche, traitement à droite). Un sommaire et deux index
 * (par symptôme, par valeur sanguine), sans correspondance automatique : c'est au Virus de chercher.
 * Au-dessus du rang du lecteur, le traitement est écrit à l'encre délavée. Le Second Souffle n'y figure pas :
 * sa page est sur un parchemin à part (même présentation).
 */
@SideOnly(Side.CLIENT)
public class GuiGrimoire extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/virus/grimoire.png");
    private static final int W = 400, H = 240, TEX_W = 512, TEX_H = 512;
    private static final int PAGE_L = 18, PAGE_R = 214, PAGE_W = 168, HAUT = 16, BAS = 214, LH = 10;
    private static final int ENCRE = 0x2A1A10, ENCRE_CLAIRE = 0x7A6450, ROUGE = 0x8A1E14, OR = 0x8A5A10, BLEU = 0x2A4A7A, DELAVE = 0xD6C9A8;
    private static final long TOURNE_MS = 260;

    /** Une ligne de texte posée sur une page ; cible : double page où mène un clic (-1 : aucune). */
    private static final class Ligne {
        final String texte;
        final int couleur, cible;
        final boolean titre;

        Ligne(String texte, int couleur, int cible, boolean titre) {
            this.texte = texte;
            this.couleur = couleur;
            this.cible = cible;
            this.titre = titre;
        }
    }

    private final boolean parchemin;
    private final List<List<Ligne>> pages = new ArrayList<>();
    private int sommaire, indexSymptomes, indexSang;
    /** Double page où commence chaque maladie (calculée à la première passe de mise en pages). */
    private final Map<String, Integer> doublePageDe = new LinkedHashMap<>();
    private int spread, ancienSpread = -1;
    private long tourne;
    private boolean versAvant;
    private int x0, y0;

    public GuiGrimoire(boolean parchemin) {
        this.parchemin = parchemin;
        construire();
    }

    // ================================================================== mise en pages
    private int rangLecteur() {
        return ClientVirus.rangVirus;
    }

    private List<String> couper(String s, int largeur) {
        List<String> l = new ArrayList<>();
        if (s == null || s.isEmpty()) return l;
        l.addAll(Minecraft().fontRenderer.listFormattedStringToWidth(s, largeur));
        return l;
    }

    private static net.minecraft.client.Minecraft Minecraft() {
        return net.minecraft.client.Minecraft.getMinecraft();
    }

    /** Ajoute des lignes à la suite, en ouvrant une nouvelle page quand la courante est pleine. */
    private final class Ecrivain {
        List<Ligne> page;

        Ecrivain() {
            nouvelle();
        }

        void nouvelle() {
            page = new ArrayList<>();
            pages.add(page);
        }

        void ligne(String t, int c, int cible, boolean titre) {
            if (page.size() >= (BAS - HAUT) / LH) nouvelle();
            page.add(new Ligne(t, c, cible, titre));
        }

        void texte(String t, int c) {
            for (String s : couper(t, PAGE_W)) ligne(s, c, -1, false);
        }

        void titre(String t, int c) {
            for (String s : couper(t, PAGE_W)) ligne(s, c, -1, true);
        }

        void blanc() {
            if (!page.isEmpty()) ligne("", 0, -1, false);
        }

        /** Termine la page courante et reprend sur la prochaine page de gauche. */
        void doublePage() {
            if (!page.isEmpty()) nouvelle();
            if ((pages.size() - 1) % 2 == 1) nouvelle();
        }
    }

    private void construire() {
        pages.clear();
        if (parchemin) {
            pageSecondSouffle();
            return;
        }
        List<Defs.Maladie> maladies = new ArrayList<>(DonneesVirus.MALADIES.values());
        maladies.sort(Comparator.comparingInt(m -> m.numero));
        // pages fixes : titre, sommaire, index — puis une double page par maladie
        // première passe pour connaître la place des index (les numéros de double page en dépendent)
        for (int passe = 0; passe < 2; passe++) {
            pages.clear();
            Ecrivain e = new Ecrivain();
            e.blanc();
            e.titre(TextFormatting.BOLD + "Grimoire des maladies", OR);
            e.blanc();
            e.texte(TextFormatting.ITALIC + "Recueil de l'officine des Hunters Virus. On y trouve chaque mal connu, ses stades, "
                    + "ce que le sang en dit et la façon de le soigner.", ENCRE);
            e.blanc();
            e.texte("Les traitements au-delà de ton rang sont écrits à l'encre délavée.", ENCRE_CLAIRE);
            e.blanc();
            e.texte("Rang du lecteur : " + (rangLecteur() < 0 ? "pas Virus" : rangLecteur() + "★"), BLEU);
            e.nouvelle();
            sommaire = pages.size() - 1;
            e.titre("Sommaire", OR);
            for (Defs.Maladie m : maladies)
                e.ligne(String.format("%2d. %s", m.numero, m.nom), m.rang > Math.max(-1, rangLecteur()) ? ENCRE_CLAIRE : ENCRE,
                        doublePageDe.getOrDefault(m.id, -1), false);
            e.doublePage();
            indexSymptomes = pages.size() - 1;
            e.titre("Index des symptômes", OR);
            e.texte(TextFormatting.ITALIC + "Clic sur un mal pour l'ouvrir.", ENCRE_CLAIRE);
            for (Map.Entry<String, List<Defs.Maladie>> x : parSymptome(maladies).entrySet()) {
                e.ligne(TextFormatting.BOLD + DonneesVirus.nomSymptome(x.getKey()), ROUGE, -1, false);
                for (Defs.Maladie m : x.getValue()) e.ligne("   " + m.numero + ". " + m.nom, ENCRE, doublePageDe.getOrDefault(m.id, -1), false);
            }
            e.doublePage();
            indexSang = pages.size() - 1;
            e.titre("Index du sang", OR);
            e.texte(TextFormatting.ITALIC + "Ce que montre le microscope, et chez qui.", ENCRE_CLAIRE);
            for (Map.Entry<String, List<Defs.Maladie>> x : parSang(maladies).entrySet()) {
                e.texte(TextFormatting.BOLD + x.getKey(), BLEU);
                for (Defs.Maladie m : x.getValue()) e.ligne("   " + m.numero + ". " + m.nom, ENCRE, doublePageDe.getOrDefault(m.id, -1), false);
            }
            e.doublePage();
            for (Defs.Maladie m : maladies) {
                doublePageDe.put(m.id, (pages.size() - 1) / 2);
                pageMaladie(e, m);
                e.doublePage();
            }
            // retire la double page vide de la fin
            while (pages.size() > 2 && pages.get(pages.size() - 1).isEmpty() && pages.get(pages.size() - 2).isEmpty()) {
                pages.remove(pages.size() - 1);
                pages.remove(pages.size() - 1);
            }
        }
        if (pages.size() % 2 == 1) pages.add(new ArrayList<>());
    }

    private static Map<String, List<Defs.Maladie>> parSymptome(List<Defs.Maladie> maladies) {
        Map<String, List<Defs.Maladie>> m = new LinkedHashMap<>();
        for (Defs.Symptome s : DonneesVirus.SYMPTOMES) m.put(s.id, new ArrayList<>());
        for (Defs.Maladie x : maladies)
            for (Defs.Stade st : x.stades) for (String s : st.symptomes) {
                List<Defs.Maladie> l = m.computeIfAbsent(s, k -> new ArrayList<>());
                if (!l.contains(x)) l.add(x);
            }
        m.values().removeIf(List::isEmpty);
        return m;
    }

    private static Map<String, List<Defs.Maladie>> parSang(List<Defs.Maladie> maladies) {
        Map<String, List<Defs.Maladie>> m = new TreeMap<>();
        for (Defs.Maladie x : maladies)
            for (Map.Entry<String, String> v : x.sang.entrySet()) {
                String cle = "MR".equals(v.getKey()) ? "Marqueur rare : " + v.getValue() : Diagnostic.nom(v.getKey()) + " " + Diagnostic.libelle(v.getKey(), v.getValue());
                m.computeIfAbsent(cle, k -> new ArrayList<>()).add(x);
            }
        return m;
    }

    private void pageMaladie(Ecrivain e, Defs.Maladie m) {
        e.titre(TextFormatting.BOLD + "" + m.numero + ". " + m.nom, ROUGE);
        e.ligne(etoiles(m.rang), OR, -1, false);
        e.texte("Touche : " + (m.toucheTexte == null || m.toucheTexte.isEmpty() ? "tout le monde" : m.toucheTexte), ENCRE_CLAIRE);
        if (m.premierMessage != null) e.texte(TextFormatting.ITALIC + "« " + m.premierMessage + " »", ENCRE);
        e.blanc();
        e.ligne(TextFormatting.BOLD + "Évolution", OR, -1, false);
        for (int i = 0; i < m.stades.size(); i++) {
            Defs.Stade s = m.stades.get(i);
            List<String> noms = new ArrayList<>();
            for (String x : s.symptomes) noms.add(DonneesVirus.nomSymptome(x));
            e.texte((s.vie ? "À vie : " : "Stade " + (i + 1) + " : ") + String.join(", ", noms), s.vie ? ROUGE : ENCRE);
        }
        e.texte("Dernier stade en " + m.jours + " jours sans soin.", ENCRE_CLAIRE);
        e.blanc();
        e.ligne(TextFormatting.BOLD + "Sang", OR, -1, false);
        if (m.sang.isEmpty()) e.texte("Rien d'anormal.", ENCRE);
        for (Map.Entry<String, String> v : m.sang.entrySet())
            e.texte(Diagnostic.nom(v.getKey()) + " : " + Diagnostic.libelle(v.getKey(), v.getValue()), BLEU);
        // traitement sur la page de droite
        if (pages.size() % 2 == 1) e.nouvelle();
        boolean lisible = rangLecteur() >= m.rang;
        e.titre(TextFormatting.BOLD + "Traitement", OR);
        if (!lisible) {
            e.texte(TextFormatting.ITALIC + "L'encre est délavée. Il faut être Virus " + m.rang + "★ pour la déchiffrer.", ROUGE);
            e.blanc();
        }
        int c = lisible ? ENCRE : DELAVE;
        String voile = lisible ? "" : TextFormatting.OBFUSCATED.toString();
        Defs.Preparation r = DonneesVirus.preparation(m.remede);
        e.texte(voile + "Remède : " + (r == null ? m.remede : r.nom + " (" + ItemPreparation.forme(r.forme).toLowerCase() + ")"), c);
        if (!m.accompagnements.isEmpty()) {
            List<String> a = new ArrayList<>();
            for (String id : m.accompagnements) {
                Defs.Preparation p = DonneesVirus.preparation(id);
                a.add(p == null ? id : p.nom);
            }
            e.texte(voile + "Avec : " + String.join(", ", a), c);
        }
        e.blanc();
        if (m.traitementTexte != null) for (String s : m.traitementTexte.split(" · ")) e.texte(voile + s, c);
        if (m.consigneTexte != null && !m.consigneTexte.isEmpty() && !"aucune".equals(m.consigneTexte)) {
            e.blanc();
            e.texte(voile + "Consigne : " + m.consigneTexte, lisible ? ROUGE : DELAVE);
        }
        if (lisible && r != null) {
            e.blanc();
            e.texte(TextFormatting.ITALIC + r.texte, ENCRE_CLAIRE);
        }
    }

    /** La page du parchemin : le Remède du Second Souffle et ses six étapes. */
    private void pageSecondSouffle() {
        Ecrivain e = new Ecrivain();
        boolean lisible = rangLecteur() >= 3;
        e.titre(TextFormatting.BOLD + "Remède du Second Souffle", ROUGE);
        e.ligne(etoiles(3), OR, -1, false);
        e.blanc();
        e.texte(TextFormatting.ITALIC + "Une page qui manque à tous les Grimoires.", ENCRE_CLAIRE);
        e.blanc();
        e.texte("Effet : efface toutes les séquelles à vie que les maladies ont laissées dans le corps du patient.", ENCRE);
        e.blanc();
        e.texte("Ingrédients : un exemplaire de chacun des 84 ingrédients de l'officine, et une plume de phénix.", ENCRE);
        e.blanc();
        e.texte("Seuil : 80 %. Ratée, tout est perdu, plume comprise.", ROUGE);
        if (pages.size() % 2 == 1) e.nouvelle();
        e.titre(TextFormatting.BOLD + "Préparation", OR);
        if (!lisible) {
            e.texte(TextFormatting.ITALIC + "Les lignes se brouillent. Seul un Virus 3★ sait les lire.", ROUGE);
            e.blanc();
        }
        String[] ordre = {"poudre_des_trente_quatre_plantes", "teinture_des_vingt_champignons", "essence_des_quinze_raretes",
                "baume_des_quinze_ressources", "cendre_doree", "remede_du_second_souffle"};
        int k = 1;
        for (String id : ordre) {
            Defs.Preparation p = DonneesVirus.preparation(id);
            if (p == null) continue;
            e.texte((lisible ? "" : TextFormatting.OBFUSCATED.toString()) + (k++) + ") " + p.nom + " : " + p.texte, lisible ? ENCRE : DELAVE);
        }
        if (pages.size() % 2 == 1) pages.add(new ArrayList<>());
    }

    private static String etoiles(int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 3; i++) b.append(i < n ? "★" : "☆");
        return b.toString();
    }

    // ================================================================== affichage
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        Keyboard.enableRepeatEvents(true);
    }

    private int spreads() {
        return Math.max(1, (pages.size() + 1) / 2);
    }

    private void tex(int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, TEX_W, TEX_H);
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        tex(x0, y0, 0, 0, W, H);
        Ligne survol = null;
        survol = page(2 * spread, x0 + PAGE_L, mx, my, survol);
        survol = page(2 * spread + 1, x0 + PAGE_R, mx, my, survol);
        tournerPage();
        // flèches et onglets
        GlStateManager.color(1, 1, 1, 1);
        if (spread > 0) tex(x0 + 18, y0 + H - 22, dans(mx, my, x0 + 18, y0 + H - 22, 18, 12) ? 20 : 0, 256, 18, 12);
        if (spread < spreads() - 1) tex(x0 + W - 36, y0 + H - 22, dans(mx, my, x0 + W - 36, y0 + H - 22, 18, 12) ? 60 : 40, 256, 18, 12);
        if (!parchemin) {
            onglet(0, "Sommaire", sommaire / 2, mx, my);
            onglet(1, "Symptômes", indexSymptomes / 2, mx, my);
            onglet(2, "Sang", indexSang / 2, mx, my);
        }
        String num = (spread + 1) + " / " + spreads();
        fontRenderer.drawString(num, x0 + W / 2 - fontRenderer.getStringWidth(num) / 2, y0 + H - 18, ENCRE_CLAIRE);
        if (survol != null && survol.cible >= 0) drawHoveringText("Ouvrir la page", mx, my);
        super.drawScreen(mx, my, pt);
    }

    private Ligne page(int index, int x, int mx, int my, Ligne survol) {
        if (index < 0 || index >= pages.size()) return survol;
        List<Ligne> p = pages.get(index);
        for (int i = 0; i < p.size(); i++) {
            Ligne l = p.get(i);
            int y = y0 + HAUT + i * LH;
            boolean dessus = l.cible >= 0 && dans(mx, my, x, y - 1, PAGE_W, LH);
            if (dessus) {
                drawRect(x - 2, y - 1, x + PAGE_W, y + LH - 1, 0x30A0702A);
                survol = l;
            }
            fontRenderer.drawString(l.texte, x, y, dessus ? ROUGE : l.couleur);
        }
        return survol;
    }

    /** La page qui se tourne : une feuille qui pivote autour de la reliure pendant un quart de seconde. */
    private void tournerPage() {
        long age = System.currentTimeMillis() - tourne;
        if (ancienSpread < 0 || age > TOURNE_MS) return;
        double k = age / (double) TOURNE_MS;
        double cos = Math.cos(k * Math.PI);
        int milieu = x0 + W / 2;
        int largeur = (int) Math.abs(cos * (W / 2 - 10));
        boolean droite = versAvant ? cos > 0 : cos < 0;
        int x = droite ? milieu : milieu - largeur;
        int l = Math.max(1, largeur);
        drawRect(x, y0 + 7, x + l, y0 + H - 9, 0xFFF2E4C2);
        drawGradientRect(x, y0 + 7, x + l, y0 + H - 9, 0x10000000, 0x30000000);
        drawRect(x, y0 + 7, x + l, y0 + H - 9, (int) ((1 - Math.abs(cos)) * 110) << 24);
        drawRect(droite ? x + l - 1 : x, y0 + 7, droite ? x + l : x + 1, y0 + H - 9, 0x60000000);
    }

    private void onglet(int k, String texte, int cible, int mx, int my) {
        int x = x0 + W - 4, y = y0 + 28 + k * 30;
        boolean actif = spread == cible;
        tex(x, y, 80, actif ? 256 : 284, 26, 26);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 9, y + 3, 0);
        GlStateManager.rotate(90, 0, 0, 1);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(texte, 22), 0, -6, actif ? 0xFFF4E0 : 0xE8D0A0);
        GlStateManager.popMatrix();
        if (dans(mx, my, x, y, 26, 26)) drawHoveringText(texte, mx, my);
    }

    private static boolean dans(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void aller(int s) {
        s = Math.max(0, Math.min(spreads() - 1, s));
        if (s == spread) return;
        versAvant = s > spread;
        ancienSpread = spread;
        spread = s;
        tourne = System.currentTimeMillis();
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(VirusSons.PAGE, 0.9f + (float) Math.random() * 0.2f));
    }

    @Override
    protected void mouseClicked(int mx, int my, int bouton) throws IOException {
        super.mouseClicked(mx, my, bouton);
        if (bouton != 0) return;
        if (spread > 0 && dans(mx, my, x0 + 18, y0 + H - 22, 18, 12)) aller(spread - 1);
        else if (dans(mx, my, x0 + W - 36, y0 + H - 22, 18, 12)) aller(spread + 1);
        if (!parchemin) {
            int[] cibles = {sommaire / 2, indexSymptomes / 2, indexSang / 2};
            for (int k = 0; k < 3; k++) if (dans(mx, my, x0 + W - 4, y0 + 28 + k * 30, 26, 26)) aller(cibles[k]);
        }
        for (int cote = 0; cote < 2; cote++) {
            int index = 2 * spread + cote, x = x0 + (cote == 0 ? PAGE_L : PAGE_R);
            if (index >= pages.size()) continue;
            List<Ligne> p = pages.get(index);
            for (int i = 0; i < p.size(); i++)
                if (p.get(i).cible >= 0 && dans(mx, my, x, y0 + HAUT + i * LH - 1, PAGE_W, LH)) {
                    aller(p.get(i).cible);
                    return;
                }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) aller(spread + (d > 0 ? -1 : 1));
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == Keyboard.KEY_LEFT || k == Keyboard.KEY_PRIOR) aller(spread - 1);
        else if (k == Keyboard.KEY_RIGHT || k == Keyboard.KEY_NEXT) aller(spread + 1);
        else if (k == Keyboard.KEY_HOME) aller(0);
        else super.keyTyped(c, k);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }
}
