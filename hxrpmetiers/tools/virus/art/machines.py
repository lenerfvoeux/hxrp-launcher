"""
Les blocs 3D de l'officine du Hunter Virus : les neuf machines, le meuble à tiroirs, le microscope,
le Grimoire sur son lutrin et le présentoir. Même DSL que les stations du Gourmet (tools/models/stations.py) ;
face avant au NORD, le blockstate la tourne vers le joueur à la pose.
Textures dans textures/blocks/virus/ (matieres.py), modèles dans models/block/virus/.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, '..', '..', 'models'))
sys.path.insert(0, HERE)
import stations  # noqa: E402
from matieres import *  # noqa: E402,F401,F403
import matieres  # noqa: E402

MODELS = {}
BUILDERS = []


def V(t):
    return 'virus/' + t


def modele(name, particle, **kw):
    m = stations.Model(name, 'hxrpmetiers:blocks/virus/' + particle, **kw)
    stations.MODELS.pop(name, None)
    MODELS[name] = m
    return m


def builder(fn):
    BUILDERS.append(fn)
    return fn


def tx(x0, y0, x1, y1):
    """Rectangle d'un modèle (face nord, unités 1/16) -> texels de la texture projetée (inclusifs)."""
    return (int(round(2 * (16 - x1))), int(round(2 * (16 - y1))), int(round(2 * (16 - x0))) - 1, int(round(2 * (16 - y0))) - 1)


# ============================================================================ meuble à tiroirs
TIROIRS_X = (1.0, 15.0)
TIROIRS_Y = (2.2, 14.6)


def grille_tiroirs():
    cols, rows = 4, 5
    w = (TIROIRS_X[1] - TIROIRS_X[0]) / cols
    h = (TIROIRS_Y[1] - TIROIRS_Y[0]) / rows
    for r in range(rows):
        for k in range(cols):
            x0 = TIROIRS_X[0] + k * w
            y0 = TIROIRS_Y[0] + r * h
            yield r, k, x0, y0, x0 + w, y0 + h


@tex('meuble_face')
def meuble_face():
    c = new('#3a100c')
    for r, k, x0, y0, x1, y1 in grille_tiroirs():
        u0, v0, u1, v1 = tx(x0, y0, x1, y1)
        box_bevel(c, u0, v0, u1, v1, '#7a3020', '#2a0a06', fill='#5c1c14')
        c.fill(rect(u0 + 1, v0 + 1, u1 - 1, v0 + 1), '#6a2418')
        # étiquette de papier et deux caractères à l'encre
        lx0, lx1, ly0, ly1 = u0 + 2, u1 - 2, v0 + 1, v0 + 2
        c.fill(rect(lx0, ly0, lx1, ly1), '#efe2c0')
        rs = np.random.RandomState(300 + r * 5 + k)
        for j in range(2):
            gx = lx0 + 1 + j * 2
            c.fill(rect(gx, ly0, gx, ly1 - (rs.randint(0, 2))), '#2a1a12')
    box_bevel(c, 0, 0, 31, 31, '#7a3020', '#1e0604')
    return c


@builder
def meuble():
    m = modele('meuble_a_tiroirs', 'laque')
    for (x, z) in ((1, 2), (13.5, 2), (1, 13.5), (13.5, 13.5)):
        m.box([x, 0, z], [x + 1.5, 1, z + 1.5], V('laque_noire'))
    m.box([0.2, 1, 1.2], [15.8, 1.8, 15.8], V('laque_sombre'))
    m.box([0.5, 1.8, 1.5], [15.5, 15, 15.5], {'n': V('meuble_face'), 's': V('meuble_cote'), 'w': V('meuble_cote'), 'e': V('meuble_cote')},
          faces='nswe', uv='proj')
    m.box([0, 15, 1], [16, 16, 16], {'u': V('meuble_dessus'), '*': V('laque_sombre')}, uv={'u': 'proj', '*': 'size'})
    # anneaux de laiton qui dépassent des tiroirs
    for r, k, x0, y0, x1, y1 in grille_tiroirs():
        cx = (x0 + x1) / 2
        m.box([cx - 0.4, y0 + 0.35, 1.0], [cx + 0.4, y0 + 0.95, 1.5], V('laiton_v'), faces='nweud')
    # pot de porcelaine bleue et son couvercle
    m.disc(4, 11.5, 2.2, 16, 19, V('porcelaine_bleue'), faces='nsewu', uv='size')
    m.disc(4, 11.5, 1.6, 19, 19.6, V('porcelaine_bleue'), faces='nsewu')
    m.box([3.6, 19.6, 11.1], [4.4, 20.2, 11.9], V('laiton_v'), faces='nsewu')
    # plateau de seringues stériles
    m.box([8.5, 16, 3], [14.5, 16.5, 8], V('bois_clair_v'), faces='nsewu')
    for z in (4.2, 5.8):
        m.box([9.2, 16.5, z], [12.6, 17.1, z + 0.6], V('verre_v'), faces='nsewu')
        m.box([12.6, 16.6, z + 0.1], [13.8, 17.0, z + 0.5], V('laiton_v'), faces='nsewu')
        m.box([8.4, 16.7, z + 0.25], [9.2, 16.9, z + 0.35], V('fonte_v'), faces='nsewu')
    # cordelette rouge à pompon sur le coin
    m.box([14.7, 12.4, 0.75], [15.0, 15, 1.05], V('corde_rouge'), faces='nsweu')
    m.box([14.45, 11.2, 0.5], [15.25, 12.4, 1.3], V('corde_rouge'))
    return m


def save_textures(out_dir):
    matieres.save_all(out_dir)
