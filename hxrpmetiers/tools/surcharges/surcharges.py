"""
Textures retouchées à la main, qui priment sur les dessins générés par tools/textures/gen_items.py (Gourmet)
et tools/virus/gen_virus.py (Hunter Virus). Elles sont rangées ici comme dans assets/hxrpmetiers/ :

    tools/surcharges/textures/items/<id>.png          icônes du Gourmet
    tools/surcharges/textures/items/virus/<id>.png    icônes du Hunter Virus

Pour retoucher une icône : déposer le PNG 32x32 ici, puis relancer le générateur concerné.
Une surcharge qui ne correspond plus à aucun objet fait échouer la génération (verifier_inutilisees).
"""
import glob
import os

from PIL import Image

ICI = os.path.dirname(os.path.abspath(__file__))
_utilisees = set()


def image(rel):
    """La texture fournie pour ce chemin (relatif à assets/hxrpmetiers/), ou None."""
    p = os.path.join(ICI, rel)
    if not os.path.isfile(p):
        return None
    im = Image.open(p).convert('RGBA')
    if im.size != (32, 32):
        raise SystemExit('Surcharge %s : %dx%d au lieu de 32x32' % (rel, im.size[0], im.size[1]))
    _utilisees.add(os.path.normpath(rel))
    return im


def verifier_inutilisees(dossier):
    """Erreur si une texture de tools/surcharges/<dossier>/ (sans les sous-dossiers) n'a remplacé aucune icône."""
    restes = sorted(os.path.relpath(p, ICI) for p in glob.glob(os.path.join(ICI, dossier, '*.png'))
                    if os.path.normpath(os.path.relpath(p, ICI)) not in _utilisees)
    if restes:
        raise SystemExit('Surcharges sans objet correspondant : ' + ', '.join(restes))
