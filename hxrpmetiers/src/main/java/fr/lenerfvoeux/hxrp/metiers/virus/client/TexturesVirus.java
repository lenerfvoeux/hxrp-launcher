package fr.lenerfvoeux.hxrp.metiers.virus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * Textures des surcouches d'effets, calculées au premier usage (pas de fichier, pas de shader) :
 * vignette radiale, bruit de brouillage, dégradé de paupière, silhouette d'hallucination.
 */
@SideOnly(Side.CLIENT)
public final class TexturesVirus {
    private static ResourceLocation vignette, bruit, paupiere, silhouette;

    private TexturesVirus() {}

    private static ResourceLocation creer(String nom, int w, int h, int[] px) {
        DynamicTexture t = new DynamicTexture(w, h);
        System.arraycopy(px, 0, t.getTextureData(), 0, px.length);
        t.updateDynamicTexture();
        return Minecraft.getMinecraft().getTextureManager().getDynamicTextureLocation(nom, t);
    }

    /** Blanc, opaque sur les bords, transparent au centre (on la teinte à l'usage). */
    public static ResourceLocation vignette() {
        if (vignette == null) {
            int n = 128;
            int[] px = new int[n * n];
            for (int y = 0; y < n; y++)
                for (int x = 0; x < n; x++) {
                    double dx = (x + 0.5) / n * 2 - 1, dy = (y + 0.5) / n * 2 - 1;
                    double r = Math.sqrt(dx * dx * 0.85 + dy * dy * 1.1);
                    double a = lisser((r - 0.55) / 0.75);
                    px[y * n + x] = ((int) (a * 255) << 24) | 0xFFFFFF;
                }
            vignette = creer("hxrp_virus_vignette", n, n, px);
        }
        return vignette;
    }

    /** Neige grise granuleuse, en mosaïque. */
    public static ResourceLocation bruit() {
        if (bruit == null) {
            int n = 128;
            int[] px = new int[n * n];
            Random r = new Random(77);
            for (int i = 0; i < px.length; i++) {
                int v = 170 + r.nextInt(86);
                int a = 40 + r.nextInt(150);
                px[i] = a << 24 | v << 16 | v << 8 | v;
            }
            bruit = creer("hxrp_virus_bruit", n, n, px);
        }
        return bruit;
    }

    /** Dégradé vertical : opaque en haut, fondu vers le bas (paupière supérieure ; retournée pour l'inférieure). */
    public static ResourceLocation paupiere() {
        if (paupiere == null) {
            int w = 4, h = 64;
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++) {
                double a = y < h * 0.7 ? 1 : 1 - lisser((y - h * 0.7) / (h * 0.3));
                for (int x = 0; x < w; x++) px[y * w + x] = ((int) (a * 255) << 24) | 0x0A0606;
            }
            paupiere = creer("hxrp_virus_paupiere", w, h, px);
        }
        return paupiere;
    }

    /** Une silhouette humaine sombre, floue sur les bords (aperçue du coin de l'œil). */
    public static ResourceLocation silhouette() {
        if (silhouette == null) {
            int w = 32, h = 64;
            int[] px = new int[w * h];
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    double cx = x + 0.5 - w / 2.0;
                    double d;
                    if (y < 14) d = Math.hypot(cx, (y + 0.5 - 8) * 1.1) - 6;           // tête
                    else if (y < 40) d = Math.abs(cx) - (9 - (y - 14) * 0.08);         // buste
                    else d = Math.min(Math.abs(cx - 4), Math.abs(cx + 4)) - 3.2;        // jambes
                    if (y >= 14 && y < 34) d = Math.min(d, Math.abs(Math.abs(cx) - 11) - 2.2); // bras
                    double a = 1 - lisser((d + 1.5) / 3.0);
                    a *= 0.55 + 0.45 * Math.sin(y * 0.4 + x * 0.3) * 0.3 + 0.45;
                    a = Math.max(0, Math.min(1, a));
                    px[y * w + x] = ((int) (a * 220) << 24) | 0x080608;
                }
            silhouette = creer("hxrp_virus_silhouette", w, h, px);
        }
        return silhouette;
    }

    private static double lisser(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
