package fr.lenerfvoeux.hxrp.metiers.client.entite;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.lenerfvoeux.hxrp.metiers.HxrpMetiers;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Modèle d'animal lu dans data/modeles/<id>.json (généré par tools/monde/entites.py).
 * Chaque partie peut porter un rôle qui l'anime : tête qui suit le regard, pattes qui marchent,
 * ailes qui battent, queue de poisson qui ondule, pinces qui s'ouvrent…
 */
public class ModeleJson extends ModelBase {
    private static final class Partie {
        final ModelRenderer r;
        final String role;
        final float bx, by, bz;
        final int index;

        Partie(ModelRenderer r, String role, float bx, float by, float bz, int index) {
            this.r = r; this.role = role; this.bx = bx; this.by = by; this.bz = bz; this.index = index;
        }
    }

    public final float echelle, ombre;
    private final List<ModelRenderer> racines = new ArrayList<>();
    private final List<Partie> parties = new ArrayList<>();

    public static ModeleJson charger(String id) {
        try (InputStream in = ModeleJson.class.getResourceAsStream("/assets/" + HxrpMetiers.MODID + "/data/modeles/" + id + ".json")) {
            if (in == null) throw new IllegalStateException("modèle introuvable : " + id);
            return new ModeleJson(new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class));
        } catch (Exception ex) {
            throw new RuntimeException("Impossible de charger le modèle de " + id, ex);
        }
    }

    private ModeleJson(JsonObject o) {
        JsonArray t = o.getAsJsonArray("texture");
        textureWidth = t.get(0).getAsInt();
        textureHeight = t.get(1).getAsInt();
        echelle = o.get("echelle").getAsFloat();
        ombre = o.get("ombre").getAsFloat();
        for (JsonElement e : o.getAsJsonArray("parties")) racines.add(construire(e.getAsJsonObject()));
    }

    private ModelRenderer construire(JsonObject p) {
        ModelRenderer r = new ModelRenderer(this, p.get("nom").getAsString());
        JsonArray pv = p.getAsJsonArray("pivot"), rot = p.getAsJsonArray("rot");
        r.setRotationPoint(pv.get(0).getAsFloat(), pv.get(1).getAsFloat(), pv.get(2).getAsFloat());
        r.rotateAngleX = rot.get(0).getAsFloat();
        r.rotateAngleY = rot.get(1).getAsFloat();
        r.rotateAngleZ = rot.get(2).getAsFloat();
        for (JsonElement be : p.getAsJsonArray("boites")) {
            JsonObject b = be.getAsJsonObject();
            JsonArray uv = b.getAsJsonArray("uv"), pos = b.getAsJsonArray("pos"), ta = b.getAsJsonArray("taille");
            r.setTextureOffset(uv.get(0).getAsInt(), uv.get(1).getAsInt());
            r.addBox(pos.get(0).getAsFloat(), pos.get(1).getAsFloat(), pos.get(2).getAsFloat(),
                    ta.get(0).getAsInt(), ta.get(1).getAsInt(), ta.get(2).getAsInt(), b.get("gonfle").getAsFloat());
        }
        String role = p.has("role") ? p.get("role").getAsString() : null;
        parties.add(new Partie(r, role, r.rotateAngleX, r.rotateAngleY, r.rotateAngleZ, parties.size()));
        if (p.has("enfants")) for (JsonElement e : p.getAsJsonArray("enfants")) r.addChild(construire(e.getAsJsonObject()));
        return r;
    }

    @Override
    public void render(Entity e, float limbSwing, float amount, float age, float headYaw, float headPitch, float scale) {
        setRotationAngles(limbSwing, amount, age, headYaw, headPitch, scale, e);
        for (ModelRenderer r : racines) r.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float amount, float age, float headYaw, float headPitch, float scale, Entity e) {
        boolean eau = e != null && e.isInWater();
        boolean sol = e == null || e.onGround;
        for (Partie p : parties) {
            ModelRenderer r = p.r;
            r.rotateAngleX = p.bx;
            r.rotateAngleY = p.by;
            r.rotateAngleZ = p.bz;
            if (p.role == null) continue;
            switch (p.role) {
                case "tete":
                    r.rotateAngleX += headPitch * 0.017453292f;
                    r.rotateAngleY += headYaw * 0.017453292f;
                    break;
                case "patte_ag":
                case "patte_pd":
                    r.rotateAngleX += MathHelper.cos(limbSwing * 0.6662f) * 1.4f * amount;
                    break;
                case "patte_ad":
                case "patte_pg":
                    r.rotateAngleX += MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * amount;
                    break;
                case "aile_g":
                case "aile_d": {
                    float battement = sol ? MathHelper.sin(age * 0.08f) * 0.05f : 0.6f + MathHelper.sin(age * 1.3f) * 0.6f;
                    r.rotateAngleZ += p.role.equals("aile_g") ? battement : -battement;
                    break;
                }
                case "queue":
                    r.rotateAngleY += MathHelper.sin(age * (eau ? 0.45f : 1.2f)) * (eau ? 0.3f + amount * 0.4f : 0.6f);
                    break;
                case "queue_oiseau":
                    r.rotateAngleX += MathHelper.sin(age * 0.05f) * 0.06f;
                    break;
                case "antenne":
                    r.rotateAngleX += MathHelper.sin(age * 0.2f) * 0.08f;
                    break;
                case "pince_g":
                    r.rotateAngleY += MathHelper.sin(age * 0.25f) * 0.15f;
                    break;
                case "pince_d":
                    r.rotateAngleY -= MathHelper.sin(age * 0.25f) * 0.15f;
                    break;
                case "patte_crabe":
                    r.rotateAngleZ += MathHelper.sin(limbSwing * 1.4f + p.index * 1.7f) * 0.45f * amount;
                    break;
                default:
                    break;
            }
        }
    }
}
