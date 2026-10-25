package fr.lenerfvoeux.hxrp.bestiaire.client;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import fr.lenerfvoeux.hxrp.bestiaire.entite.Bestiole;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

/** Rendu GeckoLib commun ; un sbire « boss en petit » est dessiné à l'échelle de son espèce. */
public class RenduBestiaire<T extends EntityLiving & Bestiole> extends GeoEntityRenderer<T> {
    public RenduBestiaire(RenderManager rm, Espece e) {
        super(rm, new ModeleBestiaire<T>(e));
        shadowSize = Math.min(2.5F, e.largeur * 0.6F);
    }

    @Override
    protected void applyRotations(T e, float age, float lacet, float pt) {
        super.applyRotations(e, age, lacet, pt);
        float s = e.echelleRendu();
        if (s != 1F) GlStateManager.scale(s, s, s);
    }

    /** Pas de bascule sur le flanc à la mort : l'animation de mort s'en charge. */
    @Override
    protected float getDeathMaxRotation(T e) {
        return 0F;
    }

    /**
     * Le cube de GeckoLib 3.0.31, à une différence près : une face absente est sautée. Un cube texturé face par face
     * n'a que les faces qui ont une texture (plans très fins des yeux, des ailes, de la toile : seulement l'avant et
     * l'arrière) ; GeckoLib 3.0.31 laisse les autres vides et plantait en les dessinant (NullPointerException dans
     * IGeoRenderer.renderCube, « Rendering entity in world »).
     */
    @Override
    public void renderCube(BufferBuilder builder, GeoCube cube, float red, float green, float blue, float alpha) {
        MATRIX_STACK.moveToPivot(cube);
        MATRIX_STACK.rotate(cube);
        MATRIX_STACK.moveBackFromPivot(cube);
        for (GeoQuad quad : cube.quads) {
            if (quad == null) continue;
            Vector3f normal = new Vector3f(quad.normal.getX(), quad.normal.getY(), quad.normal.getZ());
            MATRIX_STACK.getNormalMatrix().transform(normal);
            // cubes plats : normales retournées vers l'extérieur (ombrage, shaders d'OptiFine), comme GeckoLib
            if ((cube.size.y == 0 || cube.size.z == 0) && normal.getX() < 0) normal.x *= -1;
            if ((cube.size.x == 0 || cube.size.z == 0) && normal.getY() < 0) normal.y *= -1;
            if ((cube.size.x == 0 || cube.size.y == 0) && normal.getZ() < 0) normal.z *= -1;
            for (GeoVertex vertex : quad.vertices) {
                Vector4f v = new Vector4f(vertex.position.getX(), vertex.position.getY(), vertex.position.getZ(), 1.0F);
                MATRIX_STACK.getModelMatrix().transform(v);
                builder.pos(v.getX(), v.getY(), v.getZ()).tex(vertex.textureU, vertex.textureV).color(red, green, blue, alpha)
                        .normal(normal.getX(), normal.getY(), normal.getZ()).endVertex();
            }
        }
    }
}
