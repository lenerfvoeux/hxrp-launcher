package fr.lenerfvoeux.hxrp.metiers.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

/**
 * Une toile de pixels redessinée à chaque image, comme le canvas des prototypes :
 * on écrit dans un tableau de pixels, on l'envoie à la carte graphique, on l'affiche à l'échelle.
 */
public class Toile {
    public final int w, h;
    public final int[] px;
    private final DynamicTexture tex;
    private final ResourceLocation rl;

    public Toile(String nom, int w, int h) {
        this.w = w;
        this.h = h;
        this.tex = new DynamicTexture(w, h);
        this.px = tex.getTextureData();
        this.rl = Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(nom, tex);
    }

    // ---------------------------------------------------------------- primitives
    public void copie(int[] src) { System.arraycopy(src, 0, px, 0, px.length); }

    public int get(int x, int y) { return x < 0 || y < 0 || x >= w || y >= h ? 0 : px[y * w + x]; }

    public void set(int x, int y, int c) { if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = c; }

    public void rect(int x, int y, int rw, int rh, int c) {
        for (int j = Math.max(0, y); j < Math.min(h, y + rh); j++)
            for (int i = Math.max(0, x); i < Math.min(w, x + rw); i++) px[j * w + i] = c;
    }

    public void disc(int cx, int cy, double r, int c) {
        int n = (int) Math.floor(r);
        for (int dy = -n; dy <= n; dy++) {
            int hw = (int) Math.round(Math.sqrt(Math.max(0, r * r - dy * dy)));
            rect(cx - hw, cy + dy, 2 * hw + 1, 1, c);
        }
    }

    public void ell(int cx, int cy, double rx, double ry, int c) {
        int n = (int) Math.floor(ry);
        for (int dy = -n; dy <= n; dy++) {
            int hw = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - dy * dy / (ry * ry))));
            rect(cx - hw, cy + dy, 2 * hw + 1, 1, c);
        }
    }

    public void ligne(int x0, int y0, int x1, int y1, int c) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 2 + 1;
        for (int k = 0; k <= n; k++) set(Math.round(x0 + (x1 - x0) * (float) k / n), Math.round(y0 + (y1 - y0) * (float) k / n), c);
    }

    /** Assombrit un pixel existant (ombres portées). */
    public void ombre(int x, int y, double f) {
        int c = get(x, y);
        if (c == 0) return;
        set(x, y, 0xFF000000 | ((int) (((c >> 16) & 255) * f) << 16) | ((int) (((c >> 8) & 255) * f) << 8) | (int) ((c & 255) * f));
    }

    public static int melange(int a, int b, double t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int sombre(int c, double f) { return melange(c, 0xFF000000, 1 - f); }

    // ---------------------------------------------------------------- affichage
    public void upload() { tex.updateDynamicTexture(); }

    public void dessine(int x, int y, int scale) {
        GlStateManager.color(1, 1, 1, 1);
        Minecraft.getMinecraft().getTextureManager().bindTexture(rl);
        Gui.drawScaledCustomSizeModalRect(x, y, 0, 0, w, h, w * scale, h * scale, w, h);
    }
}
