package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.ModRegistry;
import fr.lenerfvoeux.hxrp.metiers.cuisine.Station;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.data.FoodEntry;
import fr.lenerfvoeux.hxrp.metiers.network.MsgLancer;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Le carnet de recettes du plan de travail : un livre ouvert en cuir.
 * Page de gauche : recherche, onglets, puis les recettes du rang, de 0 à 3 étoiles.
 * Page de droite : la fiche de la recette choisie (ingrédients présents ou manquants,
 * étapes et stations, valeurs nutritives, effet) et le bouton pour lancer la préparation.
 */
public class GuiCarnet extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/carnet.png");
    private static final int W = 400, H = 240, LIGNES = 8, HL = 18, TEX_W = 512, TEX_H = 512;
    private static final int LISTE_Y = 58, NOM_MAX = 124;
    private static final int ENCRE = 0x3A2418, ENCRE_CLAIRE = 0x7A6450, ROUGE = 0xA8281C, VERT = 0x2E7A1E, OR = 0x9A6A10, BLEU = 0x2A5A8A;
    /** Ordre d'affichage : 0 étoile d'abord, puis 1, 2, 3 ; à rang égal par ordre alphabétique. */
    private static final Comparator<MsgRecettes.Ligne> PAR_RANG = Comparator
            .comparingInt((MsgRecettes.Ligne l) -> rang(l.id))
            .thenComparing(l -> nom(l.id), String.CASE_INSENSITIVE_ORDER);

    private final List<MsgRecettes.Ligne> toutes = new ArrayList<>();
    private final List<MsgRecettes.Ligne> faisables = new ArrayList<>();
    private final List<MsgRecettes.Ligne> visibles = new ArrayList<>();
    private final MsgRecettes msg;
    private GuiTextField recherche;
    private String filtre = "";
    private boolean ongletToutes;
    private int defil, choix;
    private int x0, y0;

    public GuiCarnet(MsgRecettes msg) {
        this.msg = msg;
        toutes.addAll(msg.lignes);
        toutes.sort(PAR_RANG);
        for (MsgRecettes.Ligne l : toutes) if (l.ok) faisables.add(l);
        ongletToutes = faisables.isEmpty();
        filtrer();
    }

    private static int rang(String id) {
        FoodEntry e = FoodDatabase.get(id);
        return e == null ? 9 : e.rank;
    }

    private static String nom(String id) {
        FoodEntry e = FoodDatabase.get(id);
        return e == null ? id : e.name;
    }

    /** Minuscules sans accents, pour chercher « pates » et trouver « Pâtes ». */
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

    /**
     * Recettes de l'onglet courant qui correspondent à la recherche : par le nom du plat,
     * ou par un de ses ingrédients (« tomate » trouve aussi la salade de tomates et la pizza).
     */
    private void filtrer() {
        MsgRecettes.Ligne avant = choisie();
        visibles.clear();
        String f = simple(filtre);
        List<MsgRecettes.Ligne> parIngredient = new ArrayList<>();
        for (MsgRecettes.Ligne l : ongletToutes ? toutes : faisables) {
            if (f.isEmpty() || simple(nom(l.id)).contains(f)) {
                visibles.add(l);
                continue;
            }
            FoodEntry e = FoodDatabase.get(l.id);
            if (e != null) for (String ing : e.ingredients) {
                if (simple(nom(ing)).contains(f)) {
                    parIngredient.add(l);
                    break;
                }
            }
        }
        visibles.addAll(parIngredient);
        choix = Math.max(0, avant == null ? 0 : visibles.indexOf(avant));
        defil = Math.max(0, Math.min(defil, visibles.size() - LIGNES));
        if (choix < defil || choix >= defil + LIGNES) defil = Math.max(0, Math.min(choix, visibles.size() - LIGNES));
    }

    private MsgRecettes.Ligne choisie() {
        return choix >= 0 && choix < visibles.size() ? visibles.get(choix) : null;
    }

    private static ItemStack pile(String id) {
        Item i = ModRegistry.FOOD.get(id);
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    private void tex(int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, TEX_W, TEX_H);
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
            FoodEntry e = FoodDatabase.get(visibles.get(idx).id);
            if (e != null) etoiles(x0 + 150, ry + 5, e.rank);
        }
        if (visibles.size() > LIGNES) {
            tex(x0 + 186, y0 + LISTE_Y, 160, 256, 6, LIGNES * HL);
            int course = LIGNES * HL - 16;
            tex(x0 + 186, y0 + LISTE_Y + course * defil / Math.max(1, visibles.size() - LIGNES), 170, 256, 6, 16);
        }
        MsgRecettes.Ligne sel = choisie();
        FoodEntry r = sel == null ? null : FoodDatabase.get(sel.id);
        if (r != null) {
            etoiles(x0 + 250, y0 + 36, r.rank);
            boutonCuisiner(mx, my, sel.ok);
            for (int k = 0; k < r.ingredients.size() && k < 8; k++) {
                int[] p = caseIngredient(k);
                tex(p[0] + 10, p[1] + 9, sel.manquants.contains(r.ingredients.get(k)) ? 112 : 100, 256, 9, 9);
            }
        }
        // icônes
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= visibles.size()) break;
            itemRender.renderItemAndEffectIntoGUI(pile(visibles.get(idx).id), x0 + 14, y0 + LISTE_Y + 1 + i * HL);
        }
        if (r != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x0 + 212, y0 + 12, 0);
            GlStateManager.scale(2, 2, 1);
            itemRender.renderItemAndEffectIntoGUI(pile(r.id), 0, 0);
            GlStateManager.popMatrix();
            for (int k = 0; k < r.ingredients.size() && k < 8; k++) {
                int[] p = caseIngredient(k);
                itemRender.renderItemAndEffectIntoGUI(pile(r.ingredients.get(k)), p[0], p[1]);
            }
        }
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        // textes, page de gauche
        fontRenderer.drawString("Carnet de recettes", x0 + 16, y0 + 10, ENCRE);
        recherche.drawTextBox();
        if (recherche.getText().isEmpty() && !recherche.isFocused())
            fontRenderer.drawString("Rechercher une recette…", x0 + 28, y0 + 25, ENCRE_CLAIRE);
        else if (recherche.getText().isEmpty())
            fontRenderer.drawString("Rechercher (nom ou ingrédient)", x0 + 34, y0 + 25, 0xB8A888);
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= visibles.size()) break;
            MsgRecettes.Ligne li = visibles.get(idx);
            String n = nom(li.id);
            int ry = y0 + LISTE_Y + i * HL;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(n, NOM_MAX - 12), x0 + 33, ry + 5, li.ok ? ENCRE : ENCRE_CLAIRE);
            if (dans(mx, my, x0 + 12, ry, 170, HL) && fontRenderer.getStringWidth(n) > NOM_MAX - 12)
                bulle = new ArrayList<>(Arrays.asList(n));
        }
        if (visibles.isEmpty()) {
            String vide = !filtre.isEmpty() ? "Aucune recette ne correspond à « " + filtre + " »."
                    : ongletToutes ? "Aucune recette à ton rang pour l'instant."
                    : "Aucune recette possible avec ce que tu portes. Regarde l'onglet « Toutes ».";
            fontRenderer.drawSplitString(vide, x0 + 18, y0 + LISTE_Y + 8, 164, ENCRE_CLAIRE);
        }
        fontRenderer.drawString(visibles.size() + " recette" + (visibles.size() > 1 ? "s" : "") + " · molette : défiler", x0 + 18, y0 + 214, ENCRE_CLAIRE);
        // textes, page de droite
        if (r != null) {
            List<String> titre = fontRenderer.listFormattedStringToWidth(r.name, 128);
            for (int k = 0; k < Math.min(2, titre.size()); k++) fontRenderer.drawString(titre.get(k), x0 + 250, y0 + 12 + k * 10, ENCRE);
            String cat = r.isPreparation() ? "Préparation" : r.cat;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(cat == null ? "" : cat, 96), x0 + 284, y0 + 37, ENCRE_CLAIRE);
            fontRenderer.drawString("Ingrédients", x0 + 212, y0 + 52, OR);
            for (int k = 0; k < r.ingredients.size() && k < 8; k++) {
                int[] p = caseIngredient(k);
                FoodEntry ing = FoodDatabase.get(r.ingredients.get(k));
                boolean manque = sel.manquants.contains(r.ingredients.get(k));
                String n = ing == null ? r.ingredients.get(k) : ing.name;
                fontRenderer.drawString(fontRenderer.trimStringToWidth(n, 68), p[0] + 20, p[1] + 4, manque ? ROUGE : ENCRE);
                if (dans(mx, my, p[0], p[1], 88, 17))
                    bulle = new ArrayList<>(Arrays.asList(n, manque ? "§cIl t'en manque (ou il est avarié)" : "§aTu en as sur toi"));
            }
            StringBuilder etapes = new StringBuilder();
            for (int k = 0; k < r.steps.size(); k++) {
                if (k > 0) etapes.append(" › ");
                etapes.append(r.steps.get(k));
            }
            fontRenderer.drawString("Étapes", x0 + 212, y0 + 138, OR);
            fontRenderer.drawSplitString(etapes.toString(), x0 + 212, y0 + 148, 176, ENCRE);
            if (dans(mx, my, x0 + 210, y0 + 136, 178, 32)) {
                bulle = new ArrayList<>();
                for (int k = 0; k < r.steps.size(); k++)
                    bulle.add((k + 1) + ". " + r.steps.get(k) + " §7· " + Station.forGeste(r.steps.get(k)).label);
            }
            if (r.isDish()) {
                fontRenderer.drawString("Faim +" + r.faim + (r.soif > 0 ? "   Soif +" + r.soif : ""), x0 + 212, y0 + 176, VERT);
                if (r.buff != null && !r.buff.isEmpty()) {
                    List<String> effet = fontRenderer.listFormattedStringToWidth(r.buff, 90);
                    for (int k = 0; k < Math.min(2, effet.size()); k++) fontRenderer.drawString(effet.get(k), x0 + 212, y0 + 188 + k * 10, BLEU);
                    if (effet.size() > 2 && dans(mx, my, x0 + 210, y0 + 186, 92, 22)) bulle = new ArrayList<>(Arrays.asList(r.buff));
                }
            } else {
                fontRenderer.drawString("Préparation de base", x0 + 212, y0 + 176, VERT);
                fontRenderer.drawSplitString("Sa note comptera dans les plats où tu l'utilises.", x0 + 212, y0 + 188, 90, ENCRE_CLAIRE);
            }
            int bx = x0 + 310, by = y0 + 202;
            String lib = sel.ok ? "Cuisiner" : "Il manque…";
            fontRenderer.drawString(lib, bx + 36 - fontRenderer.getStringWidth(lib) / 2, by + 6, sel.ok ? 0x2A1508 : 0x4A3A2A);
        }
        if (bulle != null) drawHoveringText(bulle, mx, my);
        super.drawScreen(mx, my, pt);
    }

    private int[] caseIngredient(int k) {
        return new int[]{x0 + 212 + (k % 2) * 88, y0 + 62 + (k / 2) * 18};
    }

    private void onglet(int x, int y, String texte, boolean actif) {
        tex(x, y, 0, actif ? 256 : 272, 84, 14);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(texte, 78), x + 42 - Math.min(78, fontRenderer.getStringWidth(texte)) / 2, y + 3, actif ? ENCRE : ENCRE_CLAIRE);
    }

    /** Trois étoiles de rang : dorées pour le rang de la recette, pâles pour le reste. */
    private void etoiles(int x, int y, int n) {
        for (int i = 0; i < 3; i++) tex(x + i * 10, y, i < n ? 124 : 136, 256, 9, 9);
    }

    private void boutonCuisiner(int mx, int my, boolean actif) {
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
                clic();
            }
        }
        MsgRecettes.Ligne sel = choisie();
        if (sel != null && sel.ok && dans(mx, my, x0 + 310, y0 + 202, 72, 20)) cuisiner(sel);
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

    private void cuisiner(MsgRecettes.Ligne sel) {
        Network.NET.sendToServer(new MsgLancer(sel.id, msg.pos));
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
        if (d != 0) defil = Math.max(0, Math.min(Math.max(0, visibles.size() - LIGNES), defil + (d > 0 ? -1 : 1)));
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
            if (choix < defil) defil = choix;
            if (choix >= defil + LIGNES) defil = choix - LIGNES + 1;
            return;
        }
        if (k == Keyboard.KEY_RETURN || k == Keyboard.KEY_NUMPADENTER) {
            MsgRecettes.Ligne sel = choisie();
            if (sel != null && sel.ok) cuisiner(sel);
            return;
        }
        // tout le reste va dans la recherche (la touche d'inventaire ne ferme pas le carnet pendant qu'on tape)
        if (recherche.textboxKeyTyped(c, k)) majFiltre();
    }
}
