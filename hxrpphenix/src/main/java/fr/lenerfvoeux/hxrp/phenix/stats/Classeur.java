package fr.lenerfvoeux.hxrp.phenix.stats;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Lecture d'un classeur Excel (.xlsx) sans bibliothèque : chaque feuille devient une liste de lignes, chaque ligne une
 * table « en-tête de colonne → valeur » (texte, nombre tel qu'écrit, ou valeur calculée d'une formule).
 * Les en-têtes sont comparés sans accents ni majuscules (voir {@link #cle}).
 */
public final class Classeur {
    private final Map<String, List<Map<String, String>>> feuilles = new LinkedHashMap<>();

    private Classeur() {}

    public static Classeur lire(File f) throws IOException {
        try (InputStream in = new java.io.FileInputStream(f)) {
            return lire(in);
        }
    }

    /** Lit un classeur depuis un flux (un fichier, ou la copie par défaut rangée dans le jar). */
    public static Classeur lire(InputStream flux) throws IOException {
        Map<String, byte[]> z = new HashMap<>();
        try (ZipInputStream zin = new ZipInputStream(flux)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (!e.isDirectory()) z.put(e.getName(), octets(zin));
            }
        }
        try {
            Classeur c = new Classeur();
            List<String> partages = new ArrayList<>();
            Document ss = xml(z, "xl/sharedStrings.xml");
            if (ss != null) {
                NodeList si = ss.getElementsByTagName("si");
                for (int i = 0; i < si.getLength(); i++) partages.add(texte((Element) si.item(i)));
            }
            Document wb = xml(z, "xl/workbook.xml");
            Document rels = xml(z, "xl/_rels/workbook.xml.rels");
            if (wb == null || rels == null) throw new IOException("ce n'est pas un classeur .xlsx");
            Map<String, String> cibles = new HashMap<>();
            NodeList rl = rels.getElementsByTagName("Relationship");
            for (int i = 0; i < rl.getLength(); i++) {
                Element r = (Element) rl.item(i);
                cibles.put(r.getAttribute("Id"), r.getAttribute("Target"));
            }
            NodeList sh = wb.getElementsByTagName("sheet");
            for (int i = 0; i < sh.getLength(); i++) {
                Element s = (Element) sh.item(i);
                String cible = cibles.get(s.getAttribute("r:id"));
                if (cible == null) continue;
                String chemin = cible.startsWith("/") ? cible.substring(1) : "xl/" + cible;
                Document d = xml(z, chemin);
                if (d != null) c.feuilles.put(cle(s.getAttribute("name")), lignes(d, partages));
            }
            return c;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("classeur illisible : " + e, e);
        }
    }

    /** Les lignes d'une feuille (après la ligne d'en-têtes), ou une liste vide. */
    public List<Map<String, String>> feuille(String nom) {
        List<Map<String, String>> l = feuilles.get(cle(nom));
        return l == null ? new ArrayList<>() : l;
    }

    public boolean contient(String nom) {
        return feuilles.containsKey(cle(nom));
    }

    /** En-tête ou nom de feuille normalisé : minuscules, sans accents ni espaces superflus. */
    public static String cle(String s) {
        String n = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[’']", "'").replaceAll("\\s+", " ").trim();
    }

    // ================================================================== XML
    private static byte[] octets(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static Document xml(Map<String, byte[]> z, String chemin) throws Exception {
        byte[] contenu = z.get(chemin);
        if (contenu == null) return null;
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(false);
        f.setExpandEntityReferences(false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder b = f.newDocumentBuilder();
        return b.parse(new java.io.ByteArrayInputStream(contenu));
    }

    /** Tout le texte des balises <t> d'un élément (texte riche compris). */
    private static String texte(Element e) {
        StringBuilder b = new StringBuilder();
        NodeList t = e.getElementsByTagName("t");
        for (int i = 0; i < t.getLength(); i++) b.append(t.item(i).getTextContent());
        return b.toString();
    }

    private static List<Map<String, String>> lignes(Document d, List<String> partages) {
        List<Map<String, String>> out = new ArrayList<>();
        Map<Integer, String> entetes = null;
        NodeList rows = d.getElementsByTagName("row");
        for (int i = 0; i < rows.getLength(); i++) {
            Map<Integer, String> valeurs = new HashMap<>();
            NodeList cs = ((Element) rows.item(i)).getElementsByTagName("c");
            int auto = 0;
            for (int k = 0; k < cs.getLength(); k++) {
                Element c = (Element) cs.item(k);
                int col = c.hasAttribute("r") ? colonne(c.getAttribute("r")) : auto;
                auto = col + 1;
                String v = valeur(c, partages);
                if (v != null && !v.isEmpty()) valeurs.put(col, v);
            }
            if (valeurs.isEmpty()) continue;
            if (entetes == null) {
                entetes = valeurs;
                continue;
            }
            Map<String, String> ligne = new LinkedHashMap<>();
            for (Map.Entry<Integer, String> e : valeurs.entrySet()) {
                String h = entetes.get(e.getKey());
                if (h != null) ligne.put(cle(h), e.getValue());
            }
            out.add(ligne);
        }
        return out;
    }

    private static String valeur(Element c, List<String> partages) {
        String t = c.getAttribute("t");
        if ("inlineStr".equals(t)) return texte(c);
        Node v = premier(c, "v");
        if (v == null) return null;
        String s = v.getTextContent();
        if ("s".equals(t)) {
            try {
                int i = Integer.parseInt(s.trim());
                return i >= 0 && i < partages.size() ? partages.get(i) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return s;
    }

    private static Node premier(Element e, String nom) {
        NodeList l = e.getElementsByTagName(nom);
        return l.getLength() == 0 ? null : l.item(0);
    }

    /** « AB12 » → 27 (colonnes numérotées à partir de 0). */
    static int colonne(String ref) {
        int n = 0;
        for (int i = 0; i < ref.length(); i++) {
            char ch = ref.charAt(i);
            if (ch < 'A' || ch > 'Z') break;
            n = n * 26 + (ch - 'A' + 1);
        }
        return n - 1;
    }
}
