package fr.lenerfvoeux.hxrp.metiers.client;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Contexte;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Rendu;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.ToileGL;
import fr.lenerfvoeux.hxrp.metiers.data.FoodDatabase;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgDebut;
import fr.lenerfvoeux.hxrp.metiers.network.MsgMiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.MsgResultat;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * L'écran d'un mini-jeu. La scène est entièrement redessinée à chaque image sur une toile de pixels
 * (fond transparent, posée sur le monde). Chaque entrée est appliquée à la simulation au temps simulé
 * courant et consignée dans le journal ; à la fin, le journal part au serveur qui rejoue la partie.
 */
public class GuiMiniJeu extends GuiScreen {
    private static final Map<String, int[]> ICONES = new HashMap<>();
    private static ToileGL toile;

    private final MsgMiniJeu msg;
    private final MiniJeu jeu;
    private final Contexte ctx;
    private final Journal journal = new Journal();
    private int etat = Rendu.INTRO;
    private long debutNano, finMs;
    private boolean envoye;
    private int sc = 3, ox, oy;
    private int vus, dernierTemps = -1;

    public GuiMiniJeu(MsgMiniJeu m) {
        this.msg = m;
        this.jeu = Jeux.creer(m.geste, m.rang, m.graine, m.param);
        this.ctx = Contexte.construire(FoodDatabase.get(m.recette), m.geste, m.etape + 1, m.rang, FoodDatabase::get, GuiMiniJeu::icone);
        this.ctx.etapes = Math.max(1, m.total);
    }

    /** Icône 32x32 d'un aliment, lue dans les ressources du mod (mise en cache). */
    static int[] icone(String id) {
        if (id == null) return null;
        return ICONES.computeIfAbsent(id, k -> {
            try (InputStream in = net.minecraft.client.Minecraft.getMinecraft().getResourceManager()
                    .getResource(new ResourceLocation(HxrpMetiers.MODID, "textures/items/" + k + ".png")).getInputStream()) {
                BufferedImage im = ImageIO.read(in);
                if (im == null || im.getWidth() < 32 || im.getHeight() < 32) return null;
                int[] px = new int[32 * 32];
                im.getRGB(0, 0, 32, 32, px, 0, 32);
                return px;
            } catch (Exception e) {
                return null;
            }
        });
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void initGui() {
        if (jeu == null) {
            mc.displayGuiScreen(null);
            return;
        }
        if (toile == null) toile = new ToileGL("hxrp_minijeu", Jeux.W, Jeux.H);
        sc = Math.max(1, Math.min(width / Jeux.W, height / Jeux.H));
        ox = (width - Jeux.W * sc) / 2;
        oy = (height - Jeux.H * sc) / 2;
        Keyboard.enableRepeatEvents(false);
    }

    // ------------------------------------------------------------------ temps
    private int tempsReel() {
        return (int) ((System.nanoTime() - debutNano) / 1_000_000L);
    }

    /** Avance la simulation jusqu'au temps réel, par pas fixes. */
    private void rattraper() {
        if (etat != Rendu.JEU) return;
        int cible = tempsReel();
        while (!jeu.fini && jeu.t + MiniJeu.PAS <= cible) jeu.avancer();
        if (jeu.fini) terminer(false);
    }

    private void commencer() {
        etat = Rendu.JEU;
        debutNano = System.nanoTime();
        Network.NET.sendToServer(new MsgDebut());
        son(SoundEvents.UI_BUTTON_CLICK, 1.0f);
    }

    private void entree(int type, int a, int b) {
        if (etat == Rendu.INTRO) {
            if (type == MiniJeu.CLIC || (type == MiniJeu.TOUCHE && a == MiniJeu.ESPACE)) commencer();
            return;
        }
        if (etat != Rendu.JEU) return;
        rattraper();
        if (jeu.fini) return;
        journal.jouer(jeu, type, a, b);
        if (jeu.fini) terminer(false);
    }

    private void terminer(boolean abandon) {
        if (envoye) return;
        envoye = true;
        if (abandon) jeu.abandonner();
        etat = Rendu.FIN;
        finMs = System.currentTimeMillis();
        Network.NET.sendToServer(new MsgResultat(false, jeu.t, (float) jeu.noteFinale(), journal));
        double n = jeu.noteFinale();
        son(n >= 90 ? SoundEvents.ENTITY_PLAYER_LEVELUP : n >= 60 ? SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP : SoundEvents.BLOCK_NOTE_BASS, n >= 60 ? 1.2f : 0.6f);
    }

    // ------------------------------------------------------------------ rendu
    @Override
    public void drawScreen(int mx, int my, float pt) {
        if (jeu == null) return;
        rattraper();
        sons();
        if (etat == Rendu.FIN && System.currentTimeMillis() - finMs > 1700) {
            mc.displayGuiScreen(null);
            return;
        }
        drawGradientRect(0, 0, width, height, 0x50000000, 0x70000000);
        double tms = etat == Rendu.JEU ? Math.min(tempsReel(), jeu.t + MiniJeu.PAS) : jeu.t;
        Rendu.image(toile.toile, jeu, ctx, tms, etat, (mx - ox) / sc, (my - oy) / sc, jeu.noteFinale());
        toile.envoyer();
        toile.dessiner(ox, oy, sc);
        super.drawScreen(mx, my, pt);
    }

    /** Retours sonores : un son par retour, et le tic du métronome. */
    private void sons() {
        while (vus < jeu.retours.size() && etat != Rendu.INTRO) {
            MiniJeu.Retour r = jeu.retours.get(vus++);
            if (r.qualite == MiniJeu.PARFAIT) son(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
            else if (r.qualite == MiniJeu.BIEN) son(SoundEvents.BLOCK_NOTE_PLING, 1.2f);
            else if (r.qualite == MiniJeu.RATE) son(SoundEvents.BLOCK_NOTE_BASS, 0.7f);
            else son(SoundEvents.UI_BUTTON_CLICK, 1.4f);
        }
        if (vus > jeu.retours.size()) vus = jeu.retours.size();
        if (jeu instanceof Jeux.Rythme && etat == Rendu.JEU) {
            Jeux.Rythme r = (Jeux.Rythme) jeu;
            int k = (jeu.t - (r.piler ? r.avance : 0)) / r.periode;
            if (!r.piler && jeu.t >= 0 && k != dernierTemps) {
                dernierTemps = k;
                son(SoundEvents.BLOCK_NOTE_HAT, k * r.periode >= r.avance ? 1.2f : 0.9f);
            }
        }
    }

    private void son(SoundEvent e, float pitch) {
        mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(e, pitch));
    }

    // ------------------------------------------------------------------ entrées
    private int sx(int x) {
        return (x - ox) / sc;
    }

    private int sy(int y) {
        return (y - oy) / sc;
    }

    @Override
    public void handleMouseInput() throws IOException {
        int x = Mouse.getEventX() * width / mc.displayWidth;
        int y = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        int bouton = Mouse.getEventButton();
        int molette = Mouse.getEventDWheel();
        if (molette != 0) entree(MiniJeu.MOLETTE, molette > 0 ? 1 : -1, 0);
        if (bouton == 0 || bouton == 1) {
            boolean appui = Mouse.getEventButtonState();
            int type = bouton == 0 ? (appui ? MiniJeu.CLIC : MiniJeu.RELACHE) : (appui ? MiniJeu.CLIC_DROIT : MiniJeu.RELACHE_DROIT);
            entree(type, sx(x), sy(y));
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        int k = Keyboard.getEventKey();
        char c = Character.toLowerCase(Keyboard.getEventCharacter());
        boolean appui = Keyboard.getEventKeyState();
        if (appui && Keyboard.isRepeatEvent()) return;
        if (k == Keyboard.KEY_ESCAPE && appui) {
            if (etat == Rendu.JEU) terminer(true);
            else mc.displayGuiScreen(null);
            return;
        }
        if (k == Keyboard.KEY_SPACE) {
            entree(appui ? MiniJeu.TOUCHE : MiniJeu.TOUCHE_RELACHE, MiniJeu.ESPACE, 0);
            return;
        }
        if (!appui) return;
        int dir = -1;
        if (k == Keyboard.KEY_UP || c == 'z' || c == 'w') dir = MiniJeu.HAUT;
        else if (k == Keyboard.KEY_LEFT || c == 'q' || c == 'a') dir = MiniJeu.GAUCHE;
        else if (k == Keyboard.KEY_DOWN || c == 's') dir = MiniJeu.BAS;
        else if (k == Keyboard.KEY_RIGHT || c == 'd') dir = MiniJeu.DROITE;
        if (dir >= 0) entree(MiniJeu.TOUCHE, dir, 0);
        else if (c >= '1' && c <= '9') entree(MiniJeu.TOUCHE, MiniJeu.CHIFFRE + (c - '0'), 0);
        mc.dispatchKeypresses();
    }

    @Override
    public void onGuiClosed() {
        if (jeu == null) return;
        if (etat == Rendu.INTRO) Network.NET.sendToServer(new MsgResultat(true, 0, 0, journal));
        else if (!envoye) terminer(true);
    }
}
