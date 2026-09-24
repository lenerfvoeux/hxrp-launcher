package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgResultat;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

/**
 * Mini-jeu de cuisine : toute la scène est redessinée pixel par pixel à chaque image,
 * comme dans les prototypes (toile dynamique), puis affichée à l'échelle du pixel art.
 */
public class GuiMiniJeu extends GuiScreen {
    private static final int W = 320, H = 184;
    private static int[] BOIS, FARINE, ACIER;

    private final String geste, recette;
    private final int rang, etape, total;
    private final Gestes g;
    private Toile t;
    private int sc = 3, ox, oy, mxL, myL;
    private long t0, lastFrame;
    private boolean envoye;
    private double note;

    public GuiMiniJeu(MsgMiniJeu m) {
        geste = m.geste; recette = m.recette; rang = m.rang; etape = m.etape; total = m.total;
        g = GestesImpl.creer(m.geste, m.rang);
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        sc = Math.max(2, Math.min(4, Math.min(width / W, height / H)));
        ox = (width - W * sc) / 2;
        oy = (height - H * sc) / 2;
        if (t == null) t = new Toile("hxrp_minijeu", W, H);
        if (BOIS == null) {
            BOIS = Decor.bois(W, H, false, 172, 98);
            FARINE = Decor.bois(W, H, true, 172, 98);
            ACIER = Decor.acier(W, H);
        }
        t0 = lastFrame = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        long now = System.currentTimeMillis();
        double dt = Math.min(0.1, (now - lastFrame) / 1000.0);
        lastFrame = now;
        mxL = (mx - ox) / sc;
        myL = (my - oy) / sc;
        if (g.commence && !envoye) g.tick(dt);
        double reste = Math.max(0, g.duree() - (g.commence ? (now - t0) / 1000.0 : 0));
        if (!envoye && g.note >= 0) terminer(g.note);
        if (!envoye && g.commence && reste <= 0) terminer(g.noteFinale());

        boolean chaud = g instanceof GestesImpl.Cuisson || g instanceof GestesImpl.Maintien || g instanceof GestesImpl.Griller;
        boolean farine = g instanceof GestesImpl.Etaler || g instanceof GestesImpl.Petrir;
        t.copie(chaud ? ACIER : farine ? FARINE : BOIS);

        if (g instanceof GestesImpl.Couper) couper((GestesImpl.Couper) g);
        else if (g instanceof GestesImpl.Etaler) etaler((GestesImpl.Etaler) g);
        else if (g instanceof GestesImpl.Rotation) bol((GestesImpl.Rotation) g);
        else if (g instanceof GestesImpl.Griller) grill((GestesImpl.Griller) g);
        else simple();

        if (g instanceof GestesImpl.Cuisson) feu(((GestesImpl.Cuisson) g).feu);
        if (g instanceof GestesImpl.Maintien) {
            GestesImpl.Maintien m = (GestesImpl.Maintien) g;
            feu(m.feu);
            if (m.alerte) {
                Decor.plaque(t, 246, 40, 13, 15);
                t.rect(245, 44, 1, 5, 0xFFFFFFFF);
                t.rect(245, 51, 1, 1, 0xFFFFFFFF);
                t.rect(200, 34, 56, 5, 0xFF2A1508);
                t.rect(201, 35, (int) (54 * Math.min(1, m.tour / (Math.PI * 2))), 3, Decor.G);
            }
        }

        Decor.jauge(t, 8, 32, 140, g.valeur, g.zoneLo, g.zoneHi);
        for (int i = 0; i < 3; i++) Decor.etoile(t, W - 34 + i * 9, 20, i < rang);
        if (!g.commence) Decor.plaque(t, W / 2, 62, Math.max(80, fontRenderer.getStringWidth(g.consigne()) / sc + 16), 20);
        if (envoye) Decor.plaque(t, W / 2, 62, 110, 20);

        t.upload();
        t.dessine(ox, oy, sc);

        String tps = String.format("%.1f", reste) + "s";
        drawString(fontRenderer, "\u00a7fTEMPS " + tps, ox + (W - 6) * sc - fontRenderer.getStringWidth("TEMPS " + tps), oy + 6 * sc, 0xFFFFFF);
        drawString(fontRenderer, "\u00a77" + g.stat, ox + (W - 6) * sc - fontRenderer.getStringWidth(g.stat), oy + 32 * sc, 0xFFFFFF);
        drawString(fontRenderer, "\u00a76" + recette + "\u00a77 \u00b7 " + geste + " \u00b7 \u00e9tape " + etape + "/" + total, ox + 6 * sc, oy + 6 * sc, 0xFFFFFF);
        if (!g.commence) drawCenteredString(fontRenderer, "\u00a7f" + g.consigne(), ox + W * sc / 2, oy + 69 * sc, 0xFFFFFF);
        if (envoye) drawCenteredString(fontRenderer, "\u00a7f\u00c9TAPE TERMIN\u00c9E \u00a7e" + Math.round(note) + " %", ox + W * sc / 2, oy + 69 * sc, 0xFFFFFF);
        super.drawScreen(mx, my, pt);
    }

    // --------------------------------------------------------------- scènes
    private void couper(GestesImpl.Couper c) {
        Decor.planche(t, 99, 49, 142, 98);
        Decor.carotte(t, 118, 98, 94);
        boolean epais = rang < 2;
        for (int i = 0; i < 5; i++) {
            if (c.fait[i]) {
                t.rect(c.xs[i], 86, 1, 26, c.notes[i] > 0 ? 0xFF7A3A08 : 0xFF4A1C0C);
                t.rect(c.xs[i] + 1, 88, 1, 22, 0xFFFFC07A);
            } else for (int y = 78; y < 120; y += 4) t.rect(c.xs[i] - (epais ? 1 : 0), y, epais ? 2 : 1, 2, 0xFFFFFFFF);
        }
        if (mxL > 0 && mxL < W && myL > 0 && myL < H) Decor.couteau(t, mxL, myL);
    }

    private void etaler(GestesImpl.Etaler e) {
        int cx = 172;
        int w0 = (int) (100 + 70 * Math.min(1, e.rows[0]));
        for (int j = 45; j < 153; j++) for (int i = cx + 3 - w0 / 2; i < cx + 3 + w0 / 2; i++) t.ombre(i, j, 0.72);
        for (int i = 0; i < 12; i++) {
            double f = Math.min(1, e.rows[i]);
            int w = (int) (100 + 70 * f), x = cx - w / 2, y = 42 + i * 9;
            int base = Toile.melange(0xFFDEBC80, 0xFFF6E4BE, f);
            t.rect(x, y, w, 9, base);
            t.rect(x + 2, y + 1, w - 4, 2, Toile.melange(base, 0xFFFFFFFF, 0.25));
            t.rect(x, y, 1, 9, Toile.sombre(base, 0.8));
            t.rect(x + w - 1, y, 1, 9, Toile.sombre(base, 0.8));
            for (int k = 0; k < 7; k++) t.set(x + 4 + k * (w - 8) / 7, y + 2 + (k % 3) * 2, Toile.sombre(base, 0.88));
        }
        int y = (int) e.pinY;
        for (int j = y + 4; j < y + 7; j++) for (int i = 60; i < 264; i++) t.ombre(i, j, 0.7);
        int[] corps = {0xFF3A1E0A, 0xFFF2CB92, 0xFFDDA66A, 0xFFCF955A, 0xFFBF8349, 0xFFAC733F, 0xFF915E31, 0xFF3A1E0A};
        for (int i = 0; i < corps.length; i++) t.rect(60, y - 4 + i, 201, 1, corps[i]);
        int[] gr = {80, 104, 131, 158, 186, 207, 236};
        for (int i = 0; i < gr.length; i++) t.rect(gr[i], y - 1 + (i % 3), 4, 1, 0xFFA06935);
        int[][] poignees = {{46, 59}, {262, 275}};
        int[] pc = {0xFF3A1E0A, 0xFF9A6334, 0xFF7A4A26, 0xFF633C1E, 0xFF3A1E0A};
        for (int[] p : poignees) {
            for (int i = 0; i < pc.length; i++) t.rect(p[0], y - 2 + i, p[1] - p[0], 1, pc[i]);
            t.rect(p[0] + 2, y - 1, 4, 1, 0xFFC08A52);
        }
    }

    private void bol(GestesImpl.Rotation r) {
        int cx = 172, cy = 108;
        for (int j = cy - 64; j <= cy + 70; j++)
            for (int i = cx - 64; i <= cx + 70; i++) {
                double dx = i - cx, dy = j - cy, d = Math.sqrt(dx * dx + dy * dy);
                if (d >= 59.5) { if (Math.hypot(dx - 4, dy - 5) < 60) t.ombre(i, j, 0.55); continue; }
                if (d >= 58.5) t.set(i, j, 0xFF34302C);
                else if (d >= 51) {
                    double l = (-dx - dy * 1.2) / d;
                    t.set(i, j, d < 52.2 ? 0xFF84807A : l > 0.7 ? 0xFFFFFFFF : l > 0.2 ? 0xFFECECE8 : l < -0.6 ? 0xFF96948E : 0xFFC4C2BC);
                } else {
                    double v = Math.sin(2 * Math.atan2(dy, dx) + Math.log(d + 2) * 3.2 - r.acc * 1.3);
                    int c = v > 0.45 ? 0xFFF7ECCD : v < -0.45 ? 0xFFD6BC8A : 0xFFECD9B0;
                    if (d > 48) c = Toile.sombre(c, 0.9);
                    if (((i * 7 + j * 13) % 89) == 0) c = 0xFFFFFCEE;
                    t.set(i, j, c);
                }
            }
        int X = r.mx, Y = r.my;
        for (int k = 0; k < 18; k++) { t.rect(X + 3 + k, Y - 11 - k, 3, 1, 0xFF3A1E0A); t.set(X + 4 + k, Y - 11 - k, k % 4 != 0 ? 0xFF8A5530 : 0xFFB07A45); }
        for (int o = -1; o <= 1; o++)
            for (double a = 0; a < 6.28; a += 0.18)
                t.set((int) (X + Math.cos(a + o * 0.9) * (3 + Math.abs(o) * 2)), (int) (Y + Math.sin(a) * 9), o == 0 ? 0xFFF2F2F2 : 0xFFA8A8A8);
    }

    private void grill(GestesImpl.Griller gr) {
        t.rect(70, 40, 200, 124, 0xFF0E0E10);
        for (int y = 46; y < 160; y += 8) { t.rect(72, y, 196, 3, 0xFF2A2C30); t.rect(72, y, 196, 1, 0xFF6E747C); }
        for (int i = 0; i < gr.cuisson.length; i++) {
            int x = 138 + (i % 2) * 68, y = 72 + (i / 2) * 34;
            boolean fini = gr.faces[i] >= 2;
            double c = Math.min(1.12, gr.cuisson[i]);
            int col = fini ? 0xFF6E3218 : c < 0.72 ? Toile.melange(0xFFE8A0A0, 0xFFA04822, c / 0.72) : Toile.melange(0xFFA04822, 0xFF2A1408, (c - 0.72) / 0.4);
            t.ell(x, y, 25, 8, 0xFF2A1408);
            t.ell(x, y, 24, 7, col);
            t.rect(x - 16, y - 16, 32, 5, 0xFF2A1508);
            if (!fini) t.rect(x - 15, y - 15, (int) (30 * c / 1.12), 3, Decor.G);
            else { t.rect(x - 4, y - 16, 8, 5, Decor.GD); t.rect(x - 3, y - 15, 6, 3, Decor.G); }
        }
    }

    private void simple() {
        int cx = 172, cy = 106;
        t.disc(cx, cy, 58, 0xFF343230);
        t.disc(cx, cy, 57, 0xFF96948E);
        t.disc(cx - 1, cy - 1, 56, 0xFFDCDAD4);
        t.disc(cx, cy, 50, 0xFF84807A);
        double v = Math.max(0, Math.min(1, g.valeur));
        t.disc(cx, cy, 48 * Math.max(0.15, v), Toile.melange(0xFFE0C08A, 0xFFF8EAC4, v));
    }

    private void feu(double f) {
        t.rect(109, 163, 142, 11, Decor.OL);
        t.rect(110, 164, 140, 9, 0xFF2A1508);
        int fw = (int) (138 * f);
        t.rect(111, 165, fw, 7, 0xFFB8321A);
        t.rect(111, 165, fw, 4, 0xFFE0641E);
        t.rect(111, 165, fw, 1, 0xFFFFB03A);
        int kx = 110 + (int) (140 * f);
        t.rect(kx - 3, 159, 7, 19, Decor.OL);
        t.rect(kx - 2, 160, 5, 17, Decor.OR);
        t.rect(kx - 2, 160, 5, 1, Decor.HL);
    }

    // --------------------------------------------------------------- entrées
    @Override protected void mouseClicked(int mx, int my, int btn) { if (!envoye && btn == 0) g.down((mx - ox) / sc, (my - oy) / sc); }
    @Override protected void mouseClickMove(int mx, int my, int btn, long since) { if (!envoye) g.drag((mx - ox) / sc, (my - oy) / sc); }
    @Override protected void mouseReleased(int mx, int my, int btn) { if (!envoye && btn == 0) g.up((mx - ox) / sc, (my - oy) / sc); }

    private void terminer(double n) {
        if (envoye) return;
        envoye = true;
        note = Math.max(0, Math.min(100, n));
        Network.NET.sendToServer(new MsgResultat((float) note, (int) (System.currentTimeMillis() - t0)));
        new Thread(() -> {
            try { Thread.sleep(1300); } catch (InterruptedException ignored) {}
            mc.addScheduledTask(() -> { if (mc.currentScreen == this) mc.displayGuiScreen(null); });
        }).start();
    }
}
