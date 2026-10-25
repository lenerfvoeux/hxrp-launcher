package fr.lenerfvoeux.hxrp.bestiaire.entite;

import fr.lenerfvoeux.hxrp.bestiaire.Espece;
import software.bernie.geckolib3.core.IAnimatable;

/** Ce que le rendu GeckoLib commun demande à un boss ou à un sbire. */
public interface Bestiole extends IAnimatable {
    Espece espece();

    /** Animation ponctuelle en cours (sans préfixe), ou chaîne vide. */
    String animationCourante();

    /** Essai du rendu : joue une animation ponctuelle (sans préfixe), sur ce client seulement. */
    void jouerAnimation(String court);

    /** Échelle du rendu (un sbire « boss en petit » réduit le modèle de son boss). */
    default float echelleRendu() {
        return espece().echelle;
    }
}
