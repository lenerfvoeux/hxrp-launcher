package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.item.ItemPreparation;
import fr.lenerfvoeux.hxrp.metiers.virus.item.Objets;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgFormulaire;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgPreparer;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Le formulaire de l'officine, sur la table de préparation : un livre relié de laque brun-rouge.
 * Page de gauche : recherche, onglets, préparations du rang (0 à 3 étoiles).
 * Page de droite : la fiche (ingrédients présents ou manquants, étapes et machines, à quoi elle sert) et le bouton « Préparer ».
 */
@SideOnly(Side.CLIENT)
public class GuiFormulaire extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/virus/formulaire.png");
    private static final int W = 400, H = 240, LIGNES = 8, HL = 18, TEX_W = 512, TEX_H = 512;
    private static final int LISTE_Y = 58, NOM_MAX = 124;
    private static final int ENCRE = 0x2E1A12, ENCRE_CLAIRE = 0x7A6450, ROUGE = 0xA8281C, VERT = 0x2E7A1E, OR = 0x8A5A10, BLEU = 0x2A5A8A;
    private static final Comparator<MsgFormulaire.Ligne> PAR_RANG = Comparator
            .comparingInt((MsgFormulaire.Ligne l) -> rang(l.id))
            .thenComparing(l -> nom(l.id), String.CASE_INSENSITIVE_ORDER);

    private final List<MsgFormulaire.Ligne> toutes = new ArrayList<>();
    private final List<MsgFormulaire.Ligne> faisables = new ArrayList<>();
    private final List<MsgFormulaire.Ligne> visibles = new ArrayList<>();
    private final MsgFormulaire msg;
    private GuiTextField recherche;
    private String filtre = "";
    private boolean ongletToutes;
    private int defil, choix, defilFiche;
    private int x0, y0;

    public GuiFormulaire(MsgFormulaire msg) {
        this.msg = msg;
        for (MsgFormulaire.Ligne l : msg.lignes) if (DonneesVirus.preparation(l.id) != null) toutes.add(l);
        toutes.sort(PAR_RANG);
        for (MsgFormulaire.Ligne l : toutes) if (l.ok) faisables.add(l);
        ongletToutes = faisables.isEmpty();
        filtrer();
    }

    private static int rang(String id) {
        Defs.Preparation p = DonneesVirus.preparation(id);
        return p == null ? 9 : p.rang;
    }

    private static String nom(String id) {
        Defs.Preparation p = DonneesVirus.preparation(id);
        return p == null ? id : p.nom;
    }

    private static String simple(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        Keyboard.enableRepeatEvents(true);
        recherche = new GuiTextField(0, fontRenderer, x0 + 28, y0 + 25, 150, 10);
        recherche.setEnableBackgroundDrawing(false);
        recherche.setMaxStringLength(32);
        recherche.setTextColor(ENCRE);
        recherche.setText(filtre);
        recherche.setFocused(true);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        if (recherche != null) recherche.updateCursorCounter();
    }

    /** Par le nom de la préparation, ou par un de ses ingrédients (« lavande » trouve tout ce qui en contient). */
    private void filtrer() {
        MsgFormulaire.Ligne avant = choisie();
        visibles.clear();
        String f = simple(filtre);
        List<MsgFormulaire.Ligne> parIngredient = new ArrayList<>();
        for (MsgFormulaire.Ligne l : ongletToutes ? toutes : faisables) {
            if (f.isEmpty() || simple(nom(l.id)).contains(f)) {
                visibles.add(l);
                continue;
            }
            Defs.Preparation p = DonneesVirus.preparation(l.id);
            if (p != null) for (String ing : p.ingredients) {
                if (simple(Objets.nom(ing)).contains(f)) {
                    parIngredient.add(l);
                    break;
                }
            }
        }
        visibles.addAll(parIngredient);
        choix = Math.max(0, avant == null ? 0 : visibles.indexOf(avant));
        defil = Math.max(0, Math.min(defil, visibles.size() - LIGNES));
        if (choix < defil || choix >= defil + LIGNES) defil = Math.max(0, Math.min(choix, visibles.size() - LIGNES));
        defilFiche = 0;
    }

    private MsgFormulaire.Ligne choisie() {
        return choix >= 0 && choix < visibles.size() ? visibles.get(choix) : null;
    }

    private void tex(int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, TEX_W, TEX_H);
    }

    /** Ingrédients de la recette, regroupés (identifiant → quantité). */
    private static Map<String, Integer> besoins(Defs.Preparation p) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String id : p.ingredients) m.merge("eau".equals(id) ? "bouteille_d_eau" : id, 1, Integer::sum);
        return m;
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        tex(x0, y0, 0, 0, W, H);
        tex(x0 + 14, y0 + 21, 180, 256, 172, 14);
        onglet(x0 + 14, y0 + 40, "Réalisables " + faisables.size(), !ongletToutes);
        onglet(x0 + 100, y0 + 40, "Toutes " + toutes.size(), ongletToutes);
        List<String> bulle = null;
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= visibles.size()) break;
            int ry = y0 + LISTE_Y + i * HL;
            if (idx == choix) tex(x0 + 12, ry, 0, 288, 172, HL);
            else if (dans(mx, my, x0 + 12, ry, 170, HL)) tex(x0 + 12, ry, 0, 308, 172, HL);
            etoiles(x0 + 150, ry + 5, rang(visibles.get(idx).id));
        }
        if (visibles.size() > LIGNES) {
            tex(x0 + 186, y0 + LISTE_Y, 160, 256, 6, LIGNES * HL);
            int course = LIGNES * HL - 16;
            tex(x0 + 186, y0 + LISTE_Y + course * defil / Math.max(1, visibles.size() - LIGNES), 170, 256, 6, 16);
        }
        MsgFormulaire.Ligne sel = choisie();
        Defs.Preparation r = sel == null ? null : DonneesVirus.preparation(sel.id);
        List<Map.Entry<String, Integer>> ings = r == null ? new ArrayList<>() : new ArrayList<>(besoins(r).entrySet());
        int cases = Math.min(8, ings.size());
        int premiers = Math.max(0, Math.min(defilFiche, ings.size() - 8));
        if (r != null) {
            etoiles(x0 + 250, y0 + 36, r.rang);
            bouton(mx, my, sel.ok);
            for (int k = 0; k < cases; k++) {
                int[] p = caseIngredient(k);
                tex(p[0] + 10, p[1] + 9, sel.manquants.contains(ings.get(premiers + k).getKey()) ? 112 : 100, 256, 9, 9);
            }
        }
        // icônes
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= visibles.size()) break;
            Defs.Preparation p = DonneesVirus.preparation(visibles.get(idx).id);
            if (p != null) itemRender.renderItemAndEffectIntoGUI(Objets.pile(p.produit()), x0 + 14, y0 + LISTE_Y + 1 + i * HL);
        }
        if (r != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x0 + 212, y0 + 12, 0);
            GlStateManager.scale(2, 2, 1);
            itemRender.renderItemAndEffectIntoGUI(Objets.pile(r.produit()), 0, 0);
            GlStateManager.popMatrix();
            for (int k = 0; k < cases; k++) {
                int[] p = caseIngredient(k);
                ItemStack s = Objets.pile(ings.get(premiers + k).getKey());
                itemRender.renderItemAndEffectIntoGUI(s, p[0], p[1]);
                int n = ings.get(premiers + k).getValue();
                if (n > 1) itemRender.renderItemOverlayIntoGUI(fontRenderer, s, p[0], p[1], String.valueOf(n));
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        // page de gauche
        fontRenderer.drawString("Formulaire de l'officine", x0 + 16, y0 + 10, ENCRE);
        recherche.drawTextBox();
        if (recherche.getText().isEmpty() && !recherche.isFocused())
            fontRenderer.drawString("Rechercher une préparation…", x0 + 28, y0 + 25, ENCRE_CLAIRE);
        else if (recherche.getText().isEmpty())
            fontRenderer.drawString("Nom ou ingrédient", x0 + 34, y0 + 25, 0xB8A888);
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= visibles.size()) break;
            MsgFormulaire.Ligne li = visibles.get(idx);
            String n = nom(li.id);
            int ry = y0 + LISTE_Y + i * HL;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(n, NOM_MAX - 12), x0 + 33, ry + 5, li.ok ? ENCRE : ENCRE_CLAIRE);
            if (dans(mx, my, x0 + 12, ry, 170, HL) && fontRenderer.getStringWidth(n) > NOM_MAX - 12) bulle = new ArrayList<>(Arrays.asList(n));
        }
        if (visibles.isEmpty()) {
            String vide = !filtre.isEmpty() ? "Aucune préparation ne correspond à « " + filtre + " »."
                    : ongletToutes ? "Aucune préparation à ton rang pour l'instant."
                    : "Rien de possible avec ce que tu portes. Regarde l'onglet « Toutes ».";
            fontRenderer.drawSplitString(vide, x0 + 18, y0 + LISTE_Y + 8, 164, ENCRE_CLAIRE);
        }
        fontRenderer.drawString(visibles.size() + " préparation" + (visibles.size() > 1 ? "s" : "") + " · molette : défiler", x0 + 18, y0 + 214, ENCRE_CLAIRE);
        // page de droite
        if (r != null) {
            List<String> titre = fontRenderer.listFormattedStringToWidth(r.nom, 128);
            for (int k = 0; k < Math.min(2, titre.size()); k++) fontRenderer.drawString(titre.get(k), x0 + 250, y0 + 12 + k * 10, ENCRE);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(ItemPreparation.forme(r.forme), 96), x0 + 284, y0 + 37, ENCRE_CLAIRE);
            fontRenderer.drawString("Ingrédients" + (ings.size() > 8 ? " (" + ings.size() + ", molette)" : ""), x0 + 212, y0 + 52, OR);
            for (int k = 0; k < cases; k++) {
                int[] p = caseIngredient(k);
                String id = ings.get(premiers + k).getKey();
                boolean manque = sel.manquants.contains(id);
                String n = Objets.nom(id);
                fontRenderer.drawString(fontRenderer.trimStringToWidth(n, 68), p[0] + 20, p[1] + 4, manque ? ROUGE : ENCRE);
                if (dans(mx, my, p[0], p[1], 88, 17)) bulle = new ArrayList<>(Arrays.asList(n, manque ? "§cIl t'en manque (ou il est avarié)" : "§aTu en as sur toi"));
            }
            StringBuilder etapes = new StringBuilder();
            for (int k = 0; k < r.etapes.size(); k++) {
                if (k > 0) etapes.append(" › ");
                etapes.append(r.etapes.get(k).libelle());
            }
            fontRenderer.drawString("Étapes", x0 + 212, y0 + 138, OR);
            List<String> lignesEtapes = fontRenderer.listFormattedStringToWidth(etapes.toString(), 176);
            for (int k = 0; k < Math.min(3, lignesEtapes.size()); k++) fontRenderer.drawString(lignesEtapes.get(k), x0 + 212, y0 + 148 + k * 9, ENCRE);
            if (dans(mx, my, x0 + 210, y0 + 136, 178, 40)) {
                bulle = new ArrayList<>();
                for (int k = 0; k < r.etapes.size(); k++) bulle.add((k + 1) + ". " + r.etapes.get(k).libelle());
            }
            List<String> usage = usage(r);
            for (int k = 0; k < Math.min(3, usage.size()); k++)
                fontRenderer.drawString(fontRenderer.trimStringToWidth(usage.get(k), 94), x0 + 212, y0 + 178 + k * 9, k == 0 ? VERT : BLEU);
            if (usage.size() > 1 && dans(mx, my, x0 + 210, y0 + 176, 96, 30)) bulle = usage;
            int bx = x0 + 310, by = y0 + 202;
            String lib = sel.ok ? "Préparer" : "Il manque…";
            fontRenderer.drawString(lib, bx + 36 - fontRenderer.getStringWidth(lib) / 2, by + 6, sel.ok ? 0x2A1508 : 0x4A3A2A);
        }
        if (bulle != null) drawHoveringText(bulle, mx, my);
        super.drawScreen(mx, my, pt);
    }

    /** À quoi sert la préparation : soin d'une blessure, remède de telle maladie, ou base pour l'officine. */
    private static List<String> usage(Defs.Preparation r) {
        List<String> l = new ArrayList<>();
        Defs.Blessure b = r.soin == null ? null : DonneesVirus.blessure(r.soin);
        if (b != null) l.add("Soigne : " + b.nom);
        List<String> maladies = new ArrayList<>();
        for (Defs.Maladie m : DonneesVirus.MALADIES.values()) if (m.preparations().contains(r.id)) maladies.add(m.nom);
        if (!maladies.isEmpty()) {
            l.add(maladies.size() == 1 ? "Traite : " + maladies.get(0) : "Traite " + maladies.size() + " maladies");
            if (maladies.size() > 1) for (String m : maladies) l.add("• " + m);
        }
        if ("remede_du_second_souffle".equals(r.id)) l.add("Efface les séquelles à vie");
        if (l.isEmpty()) l.add(r.administrable() ? "Remède" : "Base de l'officine");
        return l;
    }

    private int[] caseIngredient(int k) {
        return new int[]{x0 + 212 + (k % 2) * 88, y0 + 62 + (k / 2) * 18};
    }

    private void onglet(int x, int y, String texte, boolean actif) {
        tex(x, y, 0, actif ? 256 : 272, 84, 14);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(texte, 78), x + 42 - Math.min(78, fontRenderer.getStringWidth(texte)) / 2, y + 3, actif ? ENCRE : ENCRE_CLAIRE);
    }

    private void etoiles(int x, int y, int n) {
        for (int i = 0; i < 3; i++) tex(x + i * 10, y, i < n ? 124 : 136, 256, 9, 9);
    }

    private void bouton(int mx, int my, boolean actif) {
        int bx = x0 + 310, by = y0 + 202;
        boolean survol = actif && dans(mx, my, bx, by, 72, 20);
        tex(bx, by, 0, actif ? (survol ? 352 : 330) : 374, 72, 20);
    }

    private static boolean dans(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // ------------------------------------------------------------------ entrées
    @Override
    protected void mouseClicked(int mx, int my, int bouton) throws IOException {
        super.mouseClicked(mx, my, bouton);
        recherche.mouseClicked(mx, my, bouton);
        if (bouton == 1 && dans(mx, my, x0 + 14, y0 + 21, 172, 14)) {
            recherche.setText("");
            majFiltre();
        }
        if (bouton != 0) return;
        if (dans(mx, my, x0 + 14, y0 + 21, 172, 14)) recherche.setFocused(true);
        if (dans(mx, my, x0 + 14, y0 + 40, 84, 14)) changerOnglet(false);
        else if (dans(mx, my, x0 + 100, y0 + 40, 84, 14)) changerOnglet(true);
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx < visibles.size() && dans(mx, my, x0 + 12, y0 + LISTE_Y + i * HL, 170, HL)) {
                choix = idx;
                defilFiche = 0;
                clic();
            }
        }
        MsgFormulaire.Ligne sel = choisie();
        if (sel != null && sel.ok && dans(mx, my, x0 + 310, y0 + 202, 72, 20)) preparer(sel);
    }

    private void changerOnglet(boolean t) {
        if (ongletToutes == t) return;
        ongletToutes = t;
        defil = 0;
        choix = 0;
        filtrer();
        choix = 0;
        clic();
    }

    private void majFiltre() {
        if (recherche.getText().equals(filtre)) return;
        filtre = recherche.getText();
        defil = 0;
        filtrer();
    }

    private void preparer(MsgFormulaire.Ligne sel) {
        Network.NET.sendToServer(new MsgPreparer(sel.id, msg.pos));
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f));
        mc.displayGuiScreen(null);
    }

    private void clic() {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        if (mx >= x0 + W / 2) {
            MsgFormulaire.Ligne sel = choisie();
            Defs.Preparation r = sel == null ? null : DonneesVirus.preparation(sel.id);
            int n = r == null ? 0 : besoins(r).size();
            defilFiche = Math.max(0, Math.min(Math.max(0, n - 8), defilFiche + (d > 0 ? -2 : 2)));
        } else defil = Math.max(0, Math.min(Math.max(0, visibles.size() - LIGNES), defil + (d > 0 ? -1 : 1)));
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        if (k == Keyboard.KEY_ESCAPE) {
            super.keyTyped(c, k);
            return;
        }
        int n = visibles.size();
        if (k == Keyboard.KEY_DOWN || k == Keyboard.KEY_UP) {
            if (n == 0) return;
            choix = k == Keyboard.KEY_DOWN ? Math.min(n - 1, choix + 1) : Math.max(0, choix - 1);
            defilFiche = 0;
            if (choix < defil) defil = choix;
            if (choix >= defil + LIGNES) defil = choix - LIGNES + 1;
            return;
        }
        if (k == Keyboard.KEY_RETURN || k == Keyboard.KEY_NUMPADENTER) {
            MsgFormulaire.Ligne sel = choisie();
            if (sel != null && sel.ok) preparer(sel);
            return;
        }
        if (recherche.textboxKeyTyped(c, k)) majFiltre();
    }
}
