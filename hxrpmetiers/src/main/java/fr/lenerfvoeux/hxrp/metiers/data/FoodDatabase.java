package fr.lenerfvoeux.hxrp.metiers.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Charge assets/hxrpmetiers/data/food.json (généré depuis le tableur). */
public final class FoodDatabase {
    public static final Map<String, FoodEntry> ALL = new LinkedHashMap<>();
    public static final List<FoodEntry> INGREDIENTS = new ArrayList<>();
    public static final List<FoodEntry> EPICES = new ArrayList<>();
    public static final List<FoodEntry> PREPARATIONS = new ArrayList<>();
    public static final List<FoodEntry> PLATS = new ArrayList<>();
    public static final List<FoodEntry> BOISSONS = new ArrayList<>();

    private FoodDatabase() {}

    public static void load() {
        if (!ALL.isEmpty()) return;
        try (InputStream in = FoodDatabase.class.getResourceAsStream("/assets/" + HxrpMetiers.MODID + "/data/food.json")) {
            if (in == null) throw new IllegalStateException("food.json introuvable dans le jar");
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            Type t = new TypeToken<List<FoodEntry>>() {}.getType();
            add(gson.fromJson(root.get("ingredients"), t), INGREDIENTS);
            add(gson.fromJson(root.get("epices"), t), EPICES);
            add(gson.fromJson(root.get("preparations"), t), PREPARATIONS);
            add(gson.fromJson(root.get("plats"), t), PLATS);
            add(gson.fromJson(root.get("boissons"), t), BOISSONS);
            HxrpMetiers.LOG.info("Gourmet : {} ingredients, {} epices, {} preparations, {} plats, {} boissons",
                    INGREDIENTS.size(), EPICES.size(), PREPARATIONS.size(), PLATS.size(), BOISSONS.size());
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de charger food.json", ex);
        }
    }

    private static void add(List<FoodEntry> src, List<FoodEntry> dst) {
        if (src == null) return;
        for (FoodEntry e : src) {
            if (e.ingredients == null) e.ingredients = Collections.emptyList();
            if (e.steps == null) e.steps = Collections.emptyList();
            dst.add(e);
            ALL.put(e.id, e);
        }
    }

    public static FoodEntry get(String id) { return ALL.get(id); }
}
