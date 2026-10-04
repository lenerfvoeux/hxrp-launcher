package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Contexte;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.Rendu;
import fr.lenerfvoeux.hxrp.metiers.client.minijeu.ToileGL;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Jeux;
import fr.lenerfvoeux.hxrp.metiers.minijeu.Journal;
import fr.lenerfvoeux.hxrp.metiers.minijeu.MiniJeu;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.VirusSons;
import fr.lenerfvoeux.hxrp.metiers.virus.minijeu.JeuxVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgDebutVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgJeuVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgResultatVirus;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * L'écran d'un mini-jeu du Virus, sur le moteur du Gourmet : la scène est redessinée à chaque image sur une toile
 * de pixels posée sur le monde ; chaque entrée est appliquée au temps simulé et consignée dans le journal,
 * que le serveur rejoue à la fin pour calculer la note.
 */
@SideOnly(Side.CLIENT)
public class GuiJeuVirus extends GuiScreen {
    private static ToileGL toile;

    private final MiniJeu jeu;
    private final ContexteVirus ctx;
    private final Contexte hud = new Contexte();
    private final ScenesVirus scene;
    private final Journal journal = new Journal();
    private int etat = Rendu.INTRO;
    private long debutNano, finMs;
    private boolean envoye;
    private int sc = 3, ox, oy;
    private int vus, dernierTemps = -1;

    public GuiJeuVirus(MsgJeuVirus m) {
        this.jeu = Jeux.creer(m.cle, m.rang, m.graine, m.param);
        this.ctx = ClientJeux.contexte(m);
        this.scene = new ScenesVirus(ctx);
        hud.recette = ctx.titre;
        hud.etape = ctx.etape;
        hud.etapes = ctx.etapes;
        hud.rang = m.rang;
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
        if (toile == null) toile = new ToileGL("hxrp_minijeu_virus", JeuxVirus.W, JeuxVirus.H);
        sc = Math.max(1, Math.min(width / JeuxVirus.W, height / JeuxVirus.H));
        ox = (width - JeuxVirus.W * sc) / 2;
        oy = (height - JeuxVirus.H * sc) / 2;
        Keyboard.enableRepeatEvents(false);
    }

    // ------------------------------------------------------------------ temps
    private int tempsReel() {
        return (int) ((System.nanoTime() - debutNano) / 1_000_000L);
    }

    private void rattraper() {
        if (etat != Rendu.JEU) return;
        int cible = tempsReel();
        while (!jeu.fini && jeu.t + MiniJeu.PAS <= cible) jeu.avancer();
        if (jeu.fini) terminer(false);
    }

    private void commencer() {
        etat = Rendu.JEU;
        debutNano = System.nanoTime();
        Network.NET.sendToServer(new MsgDebutVirus());
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
        Network.NET.sendToServer(new MsgResultatVirus(false, jeu.t, (float) jeu.noteFinale(), journal));
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
        Rendu.image(toile.toile, jeu, hud, tms, etat, (mx - ox) / sc, (my - oy) / sc, jeu.noteFinale(), scene);
        toile.envoyer();
        toile.dessiner(ox, oy, sc);
        super.drawScreen(mx, my, pt);
    }

    /** Retours sonores : un son par retour, le bruit de la machine aux temps du rythme. */
    private void sons() {
        while (vus < jeu.retours.size() && etat != Rendu.INTRO) {
            MiniJeu.Retour r = jeu.retours.get(vus++);
            if (r.qualite == MiniJeu.PARFAIT) son(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.5f);
            else if (r.qualite == MiniJeu.BIEN) son(SoundEvents.BLOCK_NOTE_PLING, 1.2f);
            else if (r.qualite == MiniJeu.RATE) son(SoundEvents.BLOCK_NOTE_BASS, 0.7f);
            else son(SoundEvents.UI_BUTTON_CLICK, 1.4f);
            SoundEvent m = machine();
            if (m != null && r.qualite != MiniJeu.INFO) son(m, 1f);
        }
        if (vus > jeu.retours.size()) vus = jeu.retours.size();
        if (jeu instanceof JeuxVirus.Rythme && etat == Rendu.JEU) {
            JeuxVirus.Rythme r = (JeuxVirus.Rythme) jeu;
            if (r.cible < r.total) {
                int k = r.cible;
                int avant = r.temps(k) - jeu.t;
                if (avant <= 0 && k != dernierTemps) {
                    dernierTemps = k;
                    son(SoundEvents.BLOCK_NOTE_HAT, 1.2f);
                }
            }
        }
    }

    private SoundEvent machine() {
        if (jeu instanceof JeuxVirus.Pilon) return VirusSons.PILON;
        if (jeu instanceof JeuxVirus.Yagen) return VirusSons.MEULE;
        if (jeu instanceof JeuxVirus.Hachoir || jeu instanceof JeuxVirus.Pilulier) return VirusSons.LAME;
        if (jeu instanceof JeuxVirus.Injection) return VirusSons.SERINGUE;
        return null;
    }

    private void son(SoundEvent e, float pitch) {
        if (e != null) mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(e, pitch));
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
        if (etat == Rendu.INTRO) Network.NET.sendToServer(new MsgResultatVirus(true, 0, 0, journal));
        else if (!envoye) terminer(true);
    }
}
