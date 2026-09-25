package fr.lenerfvoeux.hxrp.metiers.monde;

import com.google.gson.Gson;
import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * D'où vient chaque ingrédient dans le monde (généré par tools/monde/gen_monde.py dans data/recolte.json) :
 * cultures à semer, arbres fruitiers, nouveaux animaux, animaux vanilla, blocs du monde.
 */
public final class Recolte {
    public static final class Culture {
        public String id, forme, climat;
        public int min = 1, max = 2;
    }

    public static final class Arbre {
        public String id, fruit, climat, forme, bois;
    }

    public static final class Animal {
        public String id, comportement;
        public List<String> milieux = Collections.emptyList();
        public List<String> donne = Collections.emptyList();
    }

    public static final class Bloc {
        public String id, produit;
    }

    public List<Culture> cultures = new ArrayList<>();
    public List<Arbre> arbres = new ArrayList<>();
    public List<Animal> animaux = new ArrayList<>();
    public Map<String, List<String>> vanilla = Collections.emptyMap();
    public List<Bloc> blocs = new ArrayList<>();
    public List<String> objets = new ArrayList<>();

    private static Recolte instance;

    public static Recolte get() {
        if (instance == null) {
            try (InputStream in = Recolte.class.getResourceAsStream("/assets/" + HxrpMetiers.MODID + "/data/recolte.json")) {
                if (in == null) throw new IllegalStateException("recolte.json introuvable dans le jar");
                instance = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Recolte.class);
                HxrpMetiers.LOG.info("Gourmet : {} cultures, {} arbres fruitiers, {} animaux, {} blocs de récolte",
                        instance.cultures.size(), instance.arbres.size(), instance.animaux.size(), instance.blocs.size());
            } catch (Exception ex) {
                throw new RuntimeException("Impossible de charger recolte.json", ex);
            }
        }
        return instance;
    }

    public Animal animal(String id) {
        for (Animal a : animaux) if (a.id.equals(id)) return a;
        return null;
    }
}
