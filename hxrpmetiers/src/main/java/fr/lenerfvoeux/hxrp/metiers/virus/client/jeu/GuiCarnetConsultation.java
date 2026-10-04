package fr.lenerfvoeux.hxrp.metiers.virus.client.jeu;

import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import fr.lenerfvoeux.hxrp.metiers.network.Network;
import fr.lenerfvoeux.hxrp.metiers.virus.client.ClientVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.Defs;
import fr.lenerfvoeux.hxrp.metiers.virus.donnees.DonneesVirus;
import fr.lenerfvoeux.hxrp.metiers.virus.network.MsgCarnet;
import fr.lenerfvoeux.hxrp.metiers.virus.officine.Diagnostic;
import fr.lenerfvoeux.hxrp.metiers.virus.sante.Identite;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Le carnet de consultation d'un patient. À gauche : identité, groupe sanguin et valeurs lues au microscope.
 * À droite : les symptômes qu'il raconte, que le Virus coche (sans correspondance automatique avec le Grimoire).
 */
@SideOnly(Side.CLIENT)
public class GuiCarnetConsultation extends GuiScreen {
    private static final ResourceLocation TEX = new ResourceLocation(HxrpMetiers.MODID, "textures/gui/virus/carnet_consultation.png");
    private static final int W = 400, H = 240, TEX_W = 512, TEX_H = 512;
    private static final int ENCRE = 0x23304A, ENCRE_CLAIRE = 0x6A7488, ROUGE = 0x9A2A1E, VERT = 0x2E6A1E;

    private final ItemStack carnet;
    private final NBTTagCompound t;
    private final Set<String> coches = new LinkedHashSet<>();
    private final boolean virus;
    private boolean modifie;
    private int x0, y0;

    public GuiCarnetConsultation(ItemStack s) {
        this.carnet = s;
        this.t = s.hasTagCompound() ? s.getTagCompound() : new NBTTagCompound();
        NBTTagList l = t.getTagList(Diagnostic.SYMPTOMES, 8);
        for (int i = 0; i < l.tagCount(); i++) coches.add(l.getStringTagAt(i));
        this.virus = ClientVirus.rangVirus >= 0;
    }

    @Override public boolean doesGuiPauseGame() { return false; }

    @Override
    public void initGui() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
    }

    private void tex(int x, int y, int u, int v, int w, int h) {
        mc.getTextureManager().bindTexture(TEX);
        Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, w, h, TEX_W, TEX_H);
    }

    private int colonne(int i) { return i < 17 ? 0 : 1; }

    private int[] caseSymptome(int i) {
        return new int[]{x0 + 214 + colonne(i) * 88, y0 + 40 + (i % 17) * 11};
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        drawDefaultBackground();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.enableBlend();
        tex(x0, y0, 0, 0, W, H);
        List<Defs.Symptome> symptomes = DonneesVirus.SYMPTOMES;
        for (int i = 0; i < symptomes.size() && i < 34; i++) {
            int[] c = caseSymptome(i);
            tex(c[0], c[1], coches.contains(symptomes.get(i).id) ? 10 : 0, 256, 9, 9);
        }
        // page de gauche : le patient et son sang
        String patient = t.getString(Diagnostic.PATIENT);
        fontRenderer.drawString("Carnet de consultation", x0 + 18, y0 + 12, ENCRE_CLAIRE);
        fontRenderer.drawString(patient.isEmpty() ? "Patient inconnu" : patient, x0 + 18, y0 + 26, ENCRE);
        String sexe = Identite.libelleSexe(t.getString(Diagnostic.SEXE));
        NBTTagCompound sang = t.getCompoundTag(Diagnostic.SANG);
        String groupe = sang.hasKey("GS") ? sang.getString("GS") : "?";
        fontRenderer.drawString((sexe == null || sexe.isEmpty() ? "" : sexe + " · ") + "groupe " + groupe, x0 + 18, y0 + 38, ENCRE_CLAIRE);
        if (t.hasKey(Diagnostic.DATE))
            fontRenderer.drawString("Prise de sang : " + Diagnostic.date(t.getLong(Diagnostic.DATE)), x0 + 18, y0 + 50, ENCRE_CLAIRE);
        if (t.hasKey(Diagnostic.ANALYSTE))
            fontRenderer.drawString("Analyse : " + t.getString(Diagnostic.ANALYSTE), x0 + 18, y0 + 60, ENCRE_CLAIRE);
        fontRenderer.drawString("Valeurs sanguines", x0 + 18, y0 + 78, ROUGE);
        int y = y0 + 90;
        int lues = 0;
        for (String k : Diagnostic.CLES) {
            if ("GS".equals(k) || !sang.hasKey(k)) continue;
            lues++;
            String v = sang.getString(k);
            int c = Diagnostic.ILLISIBLE.equals(v) ? ENCRE_CLAIRE : "normal".equals(v) || "aucun".equals(v) ? ENCRE : ROUGE;
            List<String> l = fontRenderer.listFormattedStringToWidth(Diagnostic.nom(k) + " : " + Diagnostic.libelle(k, v), 168);
            for (String s : l) {
                fontRenderer.drawString(s, x0 + 18, y, c);
                y += 10;
            }
        }
        if (lues == 0) fontRenderer.drawSplitString("Aucune valeur lue pour l'instant : prise de sang, puis microscope.", x0 + 18, y, 168, ENCRE_CLAIRE);
        // page de droite : symptômes
        fontRenderer.drawString("Symptômes racontés", x0 + 214, y0 + 12, ROUGE);
        fontRenderer.drawString(virus ? "Clic pour cocher" : "Lecture seule", x0 + 214, y0 + 24, ENCRE_CLAIRE);
        for (int i = 0; i < symptomes.size() && i < 34; i++) {
            int[] c = caseSymptome(i);
            boolean coche = coches.contains(symptomes.get(i).id);
            fontRenderer.drawString(fontRenderer.trimStringToWidth(symptomes.get(i).nom, 74), c[0] + 12, c[1] + 1, coche ? VERT : ENCRE);
        }
        super.drawScreen(mx, my, pt);
    }

    @Override
    protected void mouseClicked(int mx, int my, int bouton) throws IOException {
        super.mouseClicked(mx, my, bouton);
        if (bouton != 0 || !virus) return;
        List<Defs.Symptome> symptomes = DonneesVirus.SYMPTOMES;
        for (int i = 0; i < symptomes.size() && i < 34; i++) {
            int[] c = caseSymptome(i);
            if (mx >= c[0] && mx < c[0] + 86 && my >= c[1] - 1 && my < c[1] + 10) {
                String id = symptomes.get(i).id;
                if (!coches.remove(id)) coches.add(id);
                modifie = true;
                mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.3f));
            }
        }
    }

    /** À la fermeture, le serveur inscrit les coches dans le carnet (s'il est toujours dans l'inventaire). */
    @Override
    public void onGuiClosed() {
        if (!modifie || mc.player == null) return;
        int slot = -1;
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++)
            if (mc.player.inventory.getStackInSlot(i) == carnet) slot = i;
        if (slot >= 0) Network.NET.sendToServer(new MsgCarnet(slot, new ArrayList<>(coches)));
    }
}
