package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.network.MsgLancer;
import fr.lenerfvoeux.hxrp.metiers.network.MsgRecettes;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.List;

/** Carnet de recettes du plan de travail : ce que le joueur peut cuisiner ici et maintenant. */
public class GuiCarnet extends GuiScreen {
    private static final int OL = 0xFF5C2A08, BOIS = 0xFF8A5530, BOIS2 = 0xFF9C6438, LAITON = 0xFFE8871E;
    private final List<MsgRecettes.Ligne> lignes;
    private int scroll = 0;
    private final int visible = 9;

    public GuiCarnet(MsgRecettes msg) { this.lignes = msg.lignes; }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        buttonList.clear();
        int x = width / 2 - 150, y = height / 2 - 80;
        for (int i = 0; i < visible; i++) buttonList.add(new GuiButton(i, x + 6, y + 30 + i * 21, 288, 20, ""));
        buttonList.add(new GuiButton(100, x + 6, y + 226, 140, 20, "Fermer"));
        maj();
    }

    private void maj() {
        for (int i = 0; i < visible; i++) {
            GuiButton b = buttonList.get(i);
            int idx = scroll + i;
            boolean ok = idx < lignes.size();
            b.visible = ok;
            b.enabled = ok;
            if (ok) {
                MsgRecettes.Ligne l = lignes.get(idx);
                b.displayString = l.rang + "\u2605  " + l.nom + "  \u00a77" + l.etapes;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        if (b.id == 100) { mc.displayGuiScreen(null); return; }
        int idx = scroll + b.id;
        if (idx < lignes.size()) {
            Network.NET.sendToServer(new MsgLancer(lignes.get(idx).id));
            mc.displayGuiScreen(null);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int d = Mouse.getEventDWheel();
        if (d != 0) {
            scroll = Math.max(0, Math.min(Math.max(0, lignes.size() - visible), scroll + (d > 0 ? -1 : 1)));
            maj();
        }
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        int x = width / 2 - 150, y = height / 2 - 80;
        Gui.drawRect(x - 2, y - 2, x + 302, y + 250, OL);
        Gui.drawRect(x, y, x + 300, y + 248, BOIS);
        Gui.drawRect(x, y, x + 300, y + 1, BOIS2);
        Gui.drawRect(x, y + 22, x + 300, y + 24, LAITON);
        drawCenteredString(fontRenderer, "\u00a7eCarnet de recettes", width / 2, y + 8, 0xFFFFFF);
        if (lignes.isEmpty())
            drawCenteredString(fontRenderer, "\u00a77Aucune recette possible avec ce que tu portes", width / 2, y + 110, 0xFFFFFF);
        drawString(fontRenderer, "\u00a78" + lignes.size() + " recette(s) \u00b7 molette pour faire d\u00e9filer", x + 8, y + 232, 0xFFFFFF);
        super.drawScreen(mx, my, pt);
    }
}
