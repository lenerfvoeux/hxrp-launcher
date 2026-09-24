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
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Le carnet de recettes du plan de travail : un livre ouvert en cuir.
 * Page de gauche : les recettes du rang (réalisables d'abord), avec leur icône.
 * Page de droite : la fiche de la recette choisie (ingrédients présents ou manquants,
 * étapes et stations, valeurs nutritives, effet) et le bouton pour lancer la préparation.
 */
public class GuiCarnet extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/carnet.png");
    private static final int W = 316, H = 196, LIGNES = 7, HL = 18;
    private static final int ENCRE = 0x3A2418, ENCRE_CLAIRE = 0x7A6450, ROUGE = 0xA8281C, VERT = 0x2E7A1E, OR = 0x9A6A10;

    private final List<MsgRecettes.Ligne> toutes;
    private final List<MsgRecettes.Ligne> faisables = new ArrayList<>();
    private final MsgRecettes msg;
    private boolean ongletToutes;
    private int defil, choix;
    private int x0, y0;

    public GuiCarnet(MsgRecettes msg) {
        this.msg = msg;
        this.toutes = msg.lignes;
        for (MsgRecettes.Ligne l : toutes) if (l.ok) faisables.add(l);
        ongletToutes = faisables.isEmpty();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    private List<MsgRecettes.Ligne> liste() {
        return ongletToutes ? toutes : faisables;
    }

    private MsgRecettes.Ligne choisie() {
        List<MsgRecettes.Ligne> l = liste();
        return choix >= 0 && choix < l.size() ? l.get(choix) : null;
    }

    private static ItemStack pile(String id) {
        Item i = ModRegistry.FOOD.get(id);
        return i == null ? ItemStack.EMPTY : new ItemStack(i);
    }

    // ------------------------------------------------------------------ dessin
    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(TEX);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x0, y0, 0, 0, W, H, 512, 256);
        // onglets
        onglet(x0 + 14, y0 + 24, "Réalisables " + faisables.size(), !ongletToutes);
        onglet(x0 + 84, y0 + 24, "Toutes " + toutes.size(), ongletToutes);
        List<MsgRecettes.Ligne> l = liste();
        List<String> bulle = null;
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= l.size()) break;
            int ry = y0 + 42 + i * HL;
            boolean survol = dans(mx, my, x0 + 12, ry, 140, HL);
            if (idx == choix || survol) {
                mc.getTextureManager().bindTexture(TEX);
                Gui.drawModalRectWithCustomSizedTexture(x0 + 12, ry, 320, idx == choix ? 32 : 52, 136, HL, 512, 256);
            }
        }
        // ascenseur
        if (l.size() > LIGNES) {
            mc.getTextureManager().bindTexture(TEX);
            Gui.drawModalRectWithCustomSizedTexture(x0 + 146, y0 + 42, 320, 140, 6, LIGNES * HL, 512, 256);
            int course = LIGNES * HL - 16;
            int ty = y0 + 42 + course * defil / Math.max(1, l.size() - LIGNES);
            Gui.drawModalRectWithCustomSizedTexture(x0 + 146, ty, 330, 140, 6, 16, 512, 256);
        }
        MsgRecettes.Ligne sel = choisie();
        FoodEntry r = sel == null ? null : FoodDatabase.get(sel.id);
        // étoiles de rang
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= l.size()) break;
            FoodEntry e = FoodDatabase.get(l.get(idx).id);
            if (e != null) etoiles(x0 + 122, y0 + 42 + i * HL + 5, e.rank, 3);
        }
        if (r != null) {
            etoiles(x0 + 212, y0 + 38, r.rank, 3);
            boutonCuisiner(mx, my, sel.ok);
            // coches des ingrédients
            for (int k = 0; k < r.ingredients.size() && k < 8; k++) {
                int[] p = caseIngredient(k);
                boolean manque = sel.manquants.contains(r.ingredients.get(k));
                mc.getTextureManager().bindTexture(TEX);
                Gui.drawModalRectWithCustomSizedTexture(p[0] + 10, p[1] + 9, manque ? 412 : 400, 72, 9, 9, 512, 256);
            }
        }
        // icônes
        RenderHelper.enableGUIStandardItemLighting();
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= l.size()) break;
            itemRender.renderItemAndEffectIntoGUI(pile(l.get(idx).id), x0 + 14, y0 + 43 + i * HL);
        }
        if (r != null) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x0 + 172, y0 + 12, 0);
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
        // textes
        fontRenderer.drawString("Carnet de recettes", x0 + 16, y0 + 11, ENCRE);
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx >= l.size()) break;
            MsgRecettes.Ligne li = l.get(idx);
            FoodEntry e = FoodDatabase.get(li.id);
            String nom = e == null ? li.id : e.name;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(nom, 86), x0 + 33, y0 + 47 + i * HL, li.ok ? ENCRE : ENCRE_CLAIRE);
            if (dans(mx, my, x0 + 12, y0 + 42 + i * HL, 110, HL) && e != null && fontRenderer.getStringWidth(nom) > 86)
                bulle = new ArrayList<>(Arrays.asList(nom));
        }
        if (l.isEmpty())
            fontRenderer.drawSplitString("Aucune recette possible avec ce que tu portes. Regarde l'onglet « Toutes ».", x0 + 18, y0 + 60, 124, ENCRE_CLAIRE);
        fontRenderer.drawString("molette : défiler", x0 + 18, y0 + 172, ENCRE_CLAIRE);
        if (r != null) {
            List<String> titre = fontRenderer.listFormattedStringToWidth(r.name, 92);
            for (int k = 0; k < Math.min(2, titre.size()); k++) fontRenderer.drawString(titre.get(k), x0 + 210, y0 + 13 + k * 10, ENCRE);
            String cat = "preparation".equals(r.kind) ? "Préparation" : r.cat;
            fontRenderer.drawString(fontRenderer.trimStringToWidth(cat == null ? "" : cat, 64), x0 + 236, y0 + 38, ENCRE_CLAIRE);
            fontRenderer.drawString("Ingrédients", x0 + 170, y0 + 50, OR);
            for (int k = 0; k < r.ingredients.size() && k < 8; k++) {
                int[] p = caseIngredient(k);
                FoodEntry ing = FoodDatabase.get(r.ingredients.get(k));
                boolean manque = sel.manquants.contains(r.ingredients.get(k));
                String n = ing == null ? r.ingredients.get(k) : ing.name;
                fontRenderer.drawString(fontRenderer.trimStringToWidth(n, 46), p[0] + 20, p[1] + 4, manque ? ROUGE : ENCRE);
                if (dans(mx, my, p[0], p[1], 68, 17))
                    bulle = new ArrayList<>(Arrays.asList(n, manque ? "§cIl t'en manque (ou il est avarié)" : "§aTu en as sur toi"));
            }
            StringBuilder etapes = new StringBuilder();
            for (int k = 0; k < r.steps.size(); k++) {
                if (k > 0) etapes.append(" › ");
                etapes.append(r.steps.get(k));
            }
            fontRenderer.drawString("Étapes", x0 + 170, y0 + 128, OR);
            fontRenderer.drawSplitString(etapes.toString(), x0 + 170, y0 + 138, 134, ENCRE);
            if (dans(mx, my, x0 + 168, y0 + 126, 136, 28)) {
                bulle = new ArrayList<>();
                for (int k = 0; k < r.steps.size(); k++)
                    bulle.add((k + 1) + ". " + r.steps.get(k) + " §7· " + Station.forGeste(r.steps.get(k)).label);
            }
            String val = r.isDish() ? "Faim +" + r.faim + (r.soif > 0 ? "  Soif +" + r.soif : "") : "Préparation de base";
            fontRenderer.drawString(val, x0 + 170, y0 + 158, VERT);
            if (r.isDish() && r.buff != null) fontRenderer.drawString(fontRenderer.trimStringToWidth(r.buff, 60), x0 + 170, y0 + 168, 0x2A5A8A);
            int bx = x0 + 232, by = y0 + 162;
            String lib = sel.ok ? "Cuisiner" : "Il manque...";
            fontRenderer.drawString(lib, bx + 36 - fontRenderer.getStringWidth(lib) / 2, by + 6, sel.ok ? 0x2A1508 : 0x4A3A2A);
        }
        if (bulle != null) drawHoveringText(bulle, mx, my);
        super.drawScreen(mx, my, pt);
    }

    private int[] caseIngredient(int k) {
        return new int[]{x0 + 170 + (k % 2) * 68, y0 + 60 + (k / 2) * 17};
    }

    private void onglet(int x, int y, String texte, boolean actif) {
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 320, actif ? 0 : 16, 68, 14, 512, 256);
        fontRenderer.drawString(fontRenderer.trimStringToWidth(texte, 62), x + 34 - Math.min(62, fontRenderer.getStringWidth(texte)) / 2, y + 3, actif ? ENCRE : ENCRE_CLAIRE);
    }

    private void etoiles(int x, int y, int n, int max) {
        mc.getTextureManager().bindTexture(TEX);
        for (int i = 0; i < max; i++) Gui.drawModalRectWithCustomSizedTexture(x + i * 8, y, i < n ? 424 : 432, 72, 7, 7, 512, 256);
    }

    private void boutonCuisiner(int mx, int my, boolean actif) {
        int bx = x0 + 232, by = y0 + 162;
        boolean survol = actif && dans(mx, my, bx, by, 72, 20);
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(bx, by, 320, actif ? (survol ? 94 : 72) : 116, 72, 20, 512, 256);
    }

    private static boolean dans(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    // ------------------------------------------------------------------ entrées
    @Override
    protected void mouseClicked(int mx, int my, int bouton) throws IOException {
        super.mouseClicked(mx, my, bouton);
        if (bouton != 0) return;
        if (dans(mx, my, x0 + 14, y0 + 24, 68, 14)) changerOnglet(false);
        else if (dans(mx, my, x0 + 84, y0 + 24, 68, 14)) changerOnglet(true);
        for (int i = 0; i < LIGNES; i++) {
            int idx = defil + i;
            if (idx < liste().size() && dans(mx, my, x0 + 12, y0 + 42 + i * HL, 140, HL)) {
                choix = idx;
                clic();
            }
        }
        MsgRecettes.Ligne sel = choisie();
        if (sel != null && sel.ok && dans(mx, my, x0 + 232, y0 + 162, 72, 20)) cuisiner(sel);
    }

    private void changerOnglet(boolean toutes) {
        if (ongletToutes == toutes) return;
        ongletToutes = toutes;
        defil = 0;
        choix = 0;
        clic();
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
        if (d != 0) defil = Math.max(0, Math.min(Math.max(0, liste().size() - LIGNES), defil + (d > 0 ? -1 : 1)));
    }

    @Override
    protected void keyTyped(char c, int k) throws IOException {
        super.keyTyped(c, k);
        int n = liste().size();
        if (n == 0) return;
        if (k == Keyboard.KEY_DOWN) choix = Math.min(n - 1, choix + 1);
        else if (k == Keyboard.KEY_UP) choix = Math.max(0, choix - 1);
        else if (k == Keyboard.KEY_RETURN) {
            MsgRecettes.Ligne sel = choisie();
            if (sel != null && sel.ok) cuisiner(sel);
            return;
        } else return;
        if (choix < defil) defil = choix;
        if (choix >= defil + LIGNES) defil = choix - LIGNES + 1;
    }
}
