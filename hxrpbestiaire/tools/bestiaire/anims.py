"""
Les animations « à créer » du bestiaire, écrites ici image-clé par image-clé sur les os des modèles Blockbench
(conventions des fichiers GeckoLib : degrés, pixels ; x positif penche l'os vers l'avant). Elles s'ajoutent aux
animations converties par bbconv.py ; gen.py les écrit dans les .animation.json.

Les mouvements « de fond » (battements d'ailes de la reine, balancement du nécromancien…) sont repris des
animations existantes du modèle (repos en boucle), et les gestes nouveaux posés par-dessus.
"""
import copy


def anim(longueur, boucle=False):
    return {'loop': boucle, 'animation_length': round(longueur, 4), 'bones': {}}


def cles(a, os_, canal, points):
    """points : [(temps, (x, y, z)), …] ; remplace le canal de l'os."""
    a['bones'].setdefault(os_, {})[canal] = {'%.4f' % t: [round(float(v), 3) for v in val] for t, val in points}


def fond(a, source, sauf=()):
    """Reprend une animation en boucle (repos) sur toute la longueur, pour les os non listés dans sauf."""
    L = source['animation_length'] or 1.0
    n = max(1, int(-(-a['animation_length'] // L)))
    for os_, canaux in source['bones'].items():
        if os_ in sauf:
            continue
        for canal, kf in canaux.items():
            if not isinstance(kf, dict):
                a['bones'].setdefault(os_, {})[canal] = copy.deepcopy(kf)
                continue
            out = {}
            for k in range(n):
                for t, v in kf.items():
                    tt = float(t) + k * L
                    if tt <= a['animation_length'] + 1e-6:
                        out['%.4f' % tt] = copy.deepcopy(v)
            a['bones'].setdefault(os_, {})[canal] = out


def valeur_repos(source, os_, canal, defaut=(0, 0, 0)):
    """Première valeur d'un canal dans une animation (la pose de repos), pour partir de là."""
    kf = source['bones'].get(os_, {}).get(canal)
    if kf is None:
        return list(defaut)
    if not isinstance(kf, dict):
        return list(kf)
    t0 = min(kf, key=float)
    v = kf[t0]
    v = v['vector'] if isinstance(v, dict) else v
    return [x if isinstance(x, (int, float)) else 0 for x in v]


def plus(a, b):
    return [a[i] + b[i] for i in range(3)]


# ============================================================================ Reine-guêpe
def reine_guepe(A):
    idle = A['idle']
    ab0 = valeur_repos(idle, 'abdomen', 'rotation')
    nouvelles = {}

    # rafale de dards : elle recule un peu, l'abdomen passe sous le corps, dard pointé vers l'avant, et tire
    a = anim(1.6)
    fond(a, idle, sauf=('abdomen', 'warp_boss', 'arm1', 'arm2'))
    cles(a, 'warp_boss', 'rotation', [(0, (0, 5, 0)), (0.35, (-12, 0, 0)), (0.75, (-14, 0, 0)), (0.9, (-6, 0, 0)), (1.3, (-10, 0, 0)), (1.6, (0, 5, 0))])
    cles(a, 'warp_boss', 'position', [(0, (0, 0, 0)), (0.35, (0, 4, 6)), (0.75, (0, 4, 6)), (0.9, (0, 3, -2)), (1.3, (0, 2, 2)), (1.6, (0, 0, 0))])
    # x négatif : l'abdomen se recourbe vers l'avant sous le corps (à -80 le dard pointe droit devant)
    cles(a, 'abdomen', 'rotation', [(0, ab0), (0.35, plus(ab0, (-55, 0, 0))), (0.75, plus(ab0, (-65, 0, 0))), (0.85, plus(ab0, (-40, 0, 0))),
                                    (0.95, plus(ab0, (-65, 0, 0))), (1.3, plus(ab0, (-55, 0, 0))), (1.6, ab0)])
    cles(a, 'arm1', 'rotation', [(0, (25, 0, 27.5)), (0.4, (-20, 0, 45)), (1.2, (-20, 0, 45)), (1.6, (25, 0, 27.5))])
    cles(a, 'arm2', 'rotation', [(0, (25, 0, -27.5)), (0.4, (-20, 0, -45)), (1.2, (-20, 0, -45)), (1.6, (25, 0, -27.5))])
    nouvelles['tir_dards'] = a

    # piqué : elle monte, se cabre, puis plonge tête la première (x positif sur la racine : le nez pique), ailes battant
    # vite, dard recourbé en avant ; se reprend à la fin
    a = anim(3.5)
    fond(a, idle, sauf=('warp_boss', 'abdomen', 'arm1', 'arm2', 'wing1', 'wing2', 'lower_wing1', 'lower_wing2'))
    w1, w2 = valeur_repos(idle, 'wing1', 'rotation'), valeur_repos(idle, 'wing2', 'rotation')
    l1, l2 = valeur_repos(idle, 'lower_wing1', 'rotation'), valeur_repos(idle, 'lower_wing2', 'rotation')
    bat = lambda base, amp, t0, t1, pas=0.08: [(t0 + i * pas, plus(base, (0, 0, amp if i % 2 else -amp))) for i in range(int((t1 - t0) / pas) + 1)]
    cles(a, 'wing1', 'rotation', bat(w1, 22, 0, 1.1) + bat(w1, 30, 1.15, 2.6, 0.05) + bat(w1, 22, 2.65, 3.5))
    cles(a, 'wing2', 'rotation', bat(w2, -22, 0, 1.1) + bat(w2, -30, 1.15, 2.6, 0.05) + bat(w2, -22, 2.65, 3.5))
    cles(a, 'lower_wing1', 'rotation', bat(l1, 18, 0, 1.1) + bat(l1, 25, 1.15, 2.6, 0.05) + bat(l1, 18, 2.65, 3.5))
    cles(a, 'lower_wing2', 'rotation', bat(l2, -18, 0, 1.1) + bat(l2, -25, 1.15, 2.6, 0.05) + bat(l2, -18, 2.65, 3.5))
    cles(a, 'warp_boss', 'rotation', [(0, (0, 5, 0)), (0.8, (-25, 0, 0)), (1.1, (-30, 0, 0)), (1.35, (55, 0, 0)), (2.6, (60, 0, 0)), (3.0, (10, 0, 0)), (3.5, (0, 5, 0))])
    cles(a, 'abdomen', 'rotation', [(0, ab0), (1.1, plus(ab0, (15, 0, 0))), (1.35, plus(ab0, (-55, 0, 0))), (2.6, plus(ab0, (-60, 0, 0))), (3.5, ab0)])
    cles(a, 'arm1', 'rotation', [(0, (25, 0, 27.5)), (1.2, (-60, 0, 20)), (2.6, (-60, 0, 20)), (3.5, (25, 0, 27.5))])
    cles(a, 'arm2', 'rotation', [(0, (25, 0, -27.5)), (1.2, (-60, 0, -20)), (2.6, (-60, 0, -20)), (3.5, (25, 0, -27.5))])
    nouvelles['pique'] = a
    return nouvelles


# ============================================================================ Abomination
def abomination(A):
    idle, spawn = A['idle'], A['spawn']
    nouvelles = {}
    tete0 = valeur_repos(idle, 'head', 'rotation', (-22.5, 0, 17.5))
    b0 = valeur_repos(A['meleeattack'], 'arm0', 'rotation', (22.1, -6.7, 47.4))
    b1 = valeur_repos(A['meleeattack'], 'arm1', 'rotation', (22.1, 6.7, -47.4))

    # bond : elle se ramasse, jaillit bras en avant, retombe accroupie et se redresse
    a = anim(2.0)
    cles(a, 'abomination', 'position', [(0, (0, -1.7, 0)), (0.35, (0, -9, 0)), (0.5, (0, 2, 0)), (1.2, (0, 4, 0)), (1.35, (0, -10, 0)), (1.7, (0, -4, 0)), (2.0, (0, -1.7, 0))])
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.35, (45, 0, 0)), (0.5, (-15, 0, 0)), (1.1, (25, 0, 0)), (1.35, (55, 0, 0)), (1.7, (25, 0, 0)), (2.0, (0, 0, 0))])
    cles(a, 'head', 'rotation', [(0, tete0), (0.35, plus(tete0, (-30, 0, 0))), (0.6, plus(tete0, (-10, 0, 0))), (1.35, plus(tete0, (20, 0, 0))), (2.0, tete0)])
    cles(a, 'arm0', 'rotation', [(0, b0), (0.35, plus(b0, (40, 0, -20))), (0.55, (-110, 0, 30)), (1.15, (-90, 0, 35)), (1.4, plus(b0, (30, 0, 10))), (2.0, b0)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.35, plus(b1, (40, 0, 20))), (0.55, (-110, 0, -30)), (1.15, (-90, 0, -35)), (1.4, plus(b1, (30, 0, -10))), (2.0, b1)])
    for jambe, s in (('leg0', 1), ('leg1', -1)):
        cles(a, jambe, 'rotation', [(0, (0, 0, 0)), (0.35, (-45, 0, 10 * s)), (0.5, (30, 0, 0)), (1.1, (-50, 0, 15 * s)), (1.35, (-50, 0, 20 * s)), (2.0, (0, 0, 0))])
        cles(a, 'sub_' + jambe, 'rotation', [(0, (0, 0, 0)), (0.35, (80, 0, 0)), (0.5, (0, 0, 0)), (1.1, (90, 0, 0)), (1.35, (95, 0, 0)), (2.0, (0, 0, 0))])
    nouvelles['bond'] = a

    # enfouissement : l'apparition jouée à l'envers (elle s'enfonce), griffes qui creusent
    a = anim(1.25)
    L = spawn['animation_length']
    for os_, canaux in spawn['bones'].items():
        for canal, kf in canaux.items():
            if isinstance(kf, dict):
                a['bones'].setdefault(os_, {})[canal] = {'%.4f' % (L - float(t)): copy.deepcopy(v) for t, v in kf.items()}
    pts = []
    for i in range(11):
        t = i * 0.12
        pts.append((t, (-70 if i % 2 else -20, 0, 40)))
    cles(a, 'arm0', 'rotation', pts)
    cles(a, 'arm1', 'rotation', [(t, (v[0], 0, -40)) for t, v in pts])
    nouvelles['enfouissement'] = a

    # vomissure : elle se cabre, gonfle, puis se plie en avant, tête tendue vers la cible
    a = anim(1.5)
    cles(a, 'abomination', 'position', [(0, (0, -1.7, 0)), (0.4, (0, 0, 0)), (0.75, (0, -4, 0)), (1.5, (0, -1.7, 0))])
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.4, (-25, 0, 0)), (0.6, (45, 0, 0)), (1.05, (40, 0, 0)), (1.5, (0, 0, 0))])
    cles(a, 'body', 'scale', [(0, (1, 1, 1)), (0.4, (1.12, 1.05, 1.12)), (0.6, (0.95, 1, 0.95)), (1.5, (1, 1, 1))])
    cles(a, 'head', 'rotation', [(0, tete0), (0.4, plus(tete0, (-35, 0, 0))), (0.6, plus(tete0, (45, 0, -15))), (1.05, plus(tete0, (40, 0, -15))), (1.5, tete0)])
    cles(a, 'arm0', 'rotation', [(0, b0), (0.4, plus(b0, (-30, 0, 20))), (0.6, plus(b0, (50, 0, 10))), (1.5, b0)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.4, plus(b1, (-30, 0, -20))), (0.6, plus(b1, (50, 0, -10))), (1.5, b1)])
    nouvelles['vomissure'] = a
    return nouvelles


# ============================================================================ Nécromancien
TUNIQUE = ('tunic_front', 'tunic_back', 'tunic_back2', 'tunic_left', 'tunic_left2', 'tunic_right')


def necromancien(A):
    idle = A['idle']
    nouvelles = {}
    d0 = valeur_repos(idle, 'rightarm1', 'rotation', (36.7, -5, 25.8))
    g0 = valeur_repos(idle, 'leftarm1', 'rotation', (9.5, -4.7, -16.7))
    racine = valeur_repos(idle, 'necromancer', 'rotation', (0, 22.5, 0))

    # flotte : il glisse au-dessus du sol, la tunique traîne derrière lui
    a = anim(2.0, boucle=True)
    fond(a, idle, sauf=('necromancer',) + TUNIQUE)
    cles(a, 'necromancer', 'position', [(0, (0, 3, 0)), (0.5, (0, 5, 0)), (1.0, (0, 3, 0)), (1.5, (0, 1.5, 0)), (2.0, (0, 3, 0))])
    cles(a, 'necromancer', 'rotation', [(0, plus(racine, (6, 0, 0))), (1.0, plus(racine, (8, 0, 2))), (2.0, plus(racine, (6, 0, 0)))])
    for i, t in enumerate(TUNIQUE):
        ph = i * 0.25
        cles(a, t, 'rotation', [(0, (-14, 0, 0)), ((0.5 + ph) % 2.0, (-20, 0, 3)), ((1.0 + ph) % 2.0, (-12, 0, 0)), ((1.5 + ph) % 2.0, (-18, 0, -3)),
                               (2.0, (-14, 0, 0))])
        a['bones'][t]['rotation'] = dict(sorted(a['bones'][t]['rotation'].items(), key=lambda kv: float(kv[0])))
    nouvelles['flotte'] = a

    # invocation : il lève les bras et le bâton vers le ciel, puis frappe l'air
    a = anim(2.0)
    fond(a, idle, sauf=('rightarm1', 'leftarm1', 'necromancer', 'head'))
    cles(a, 'necromancer', 'rotation', [(0, racine), (0.6, plus(racine, (-12, -22.5, 0))), (1.2, plus(racine, (-15, -22.5, 0))), (1.4, plus(racine, (8, -22.5, 0))),
                                        (2.0, racine)])
    cles(a, 'necromancer', 'position', [(0, (0, 0, 0)), (0.6, (0, 6, 0)), (1.2, (0, 8, 0)), (1.4, (0, 2, 0)), (2.0, (0, 0, 0))])
    cles(a, 'rightarm1', 'rotation', [(0, d0), (0.6, (-160, 0, 20)), (1.2, (-170, 0, 15)), (1.4, (-100, 0, 20)), (2.0, d0)])
    cles(a, 'leftarm1', 'rotation', [(0, g0), (0.6, (-160, 0, -20)), (1.2, (-170, 0, -15)), (1.4, (-100, 0, -20)), (2.0, g0)])
    cles(a, 'head', 'rotation', [(0, (0, 0, 0)), (0.6, (-30, 0, 0)), (1.2, (-35, 0, 0)), (1.5, (10, 0, 0)), (2.0, (0, 0, 0))])
    nouvelles['invocation'] = a

    # boule d'âme : le bâton pointé devant lui, trois âmes partent
    a = anim(1.6)
    fond(a, idle, sauf=('rightarm1', 'leftarm1', 'necromancer'))
    cles(a, 'necromancer', 'rotation', [(0, racine), (0.5, plus(racine, (-6, 10, 0))), (0.9, plus(racine, (10, -15, 0))), (1.6, racine)])
    cles(a, 'rightarm1', 'rotation', [(0, d0), (0.5, (-40, 20, 60)), (0.9, (-95, -10, 10)), (1.25, (-90, -10, 10)), (1.6, d0)])
    cles(a, 'leftarm1', 'rotation', [(0, g0), (0.5, (-60, 0, -30)), (0.9, (-80, 0, -10)), (1.6, g0)])
    nouvelles['boule_ame'] = a

    # téléportation : il tourne sur lui-même et se réduit à rien, puis reparaît en grandissant
    a = anim(1.6)
    fond(a, idle, sauf=('necromancer', 'chest'))
    cles(a, 'necromancer', 'rotation', [(0, racine), (0.8, plus(racine, (0, 540, 0))), (1.6, plus(racine, (0, 1080, 0)))])
    cles(a, 'necromancer', 'scale', [(0, (1, 1, 1)), (0.6, (0.6, 1.2, 0.6)), (0.8, (0.02, 0.02, 0.02)), (1.0, (0.4, 1.3, 0.4)), (1.6, (1, 1, 1))])
    nouvelles['teleportation'] = a

    # apparition : il se matérialise au-dessus du cercueil, en tournoyant, et descend
    a = anim(2.5)
    fond(a, idle, sauf=('necromancer',))
    cles(a, 'necromancer', 'scale', [(0, (0.02, 0.02, 0.02)), (1.2, (0.7, 1.1, 0.7)), (2.0, (1, 1, 1)), (2.5, (1, 1, 1))])
    cles(a, 'necromancer', 'position', [(0, (0, 24, 0)), (1.2, (0, 14, 0)), (2.0, (0, 3, 0)), (2.5, (0, 0, 0))])
    cles(a, 'necromancer', 'rotation', [(0, plus(racine, (0, -720, 0))), (2.0, racine), (2.5, racine)])
    nouvelles['apparition'] = a

    # mort : il se tord, lève le bâton une dernière fois et s'enfonce en se dissipant
    a = anim(3.0)
    cles(a, 'necromancer', 'rotation', [(0, racine), (0.6, plus(racine, (-20, 0, 8))), (1.4, plus(racine, (25, 0, -5))), (3.0, plus(racine, (40, 0, -10)))])
    cles(a, 'necromancer', 'position', [(0, (0, 0, 0)), (0.6, (0, 4, 0)), (1.4, (0, -6, 0)), (3.0, (0, -60, 0))])
    cles(a, 'necromancer', 'scale', [(0, (1, 1, 1)), (2.0, (0.9, 0.8, 0.9)), (3.0, (0.3, 0.2, 0.3))])
    cles(a, 'rightarm1', 'rotation', [(0, d0), (0.6, (-150, 0, 30)), (1.4, (-60, 0, 40)), (3.0, (-30, 0, 60))])
    cles(a, 'leftarm1', 'rotation', [(0, g0), (0.6, (-120, 0, -40)), (1.4, (-40, 0, -50)), (3.0, (-10, 0, -70))])
    cles(a, 'head', 'rotation', [(0, (0, 0, 0)), (0.6, (-40, 0, 0)), (1.4, (30, 0, 0)), (3.0, (45, 0, 10))])
    nouvelles['mort'] = a
    return nouvelles


# ============================================================================ Archer squelette
def archer_squelette(A):
    nouvelles = {}
    # tir : il lève l'arc droit devant lui (bras de l'arc tendu, x -90), bande, lâche (vers 45 %), et revient
    a = anim(1.25)
    tete = valeur_repos(A['idle'], 'head', 'rotation', (-22.5, 0, 17.5))
    cles(a, 'skeleton_archer', 'position', [(0, (0, -1.7, 0)), (1.25, (0, -1.7, 0))])
    cles(a, 'head', 'rotation', [(0, tete), (0.3, (-5, 0, 0)), (0.9, (-5, 0, 0)), (1.25, tete)])
    cles(a, 'arm0', 'rotation', [(0, (0, 0, 50)), (0.3, (-90, 0, 0)), (0.55, (-90, 0, 0)), (0.62, (-96, 0, 0)), (0.9, (-88, 0, 0)), (1.25, (0, 0, 50))])
    cles(a, 'arm1', 'rotation', [(0, (0, 0, -50)), (0.3, (-90, 0, -15)), (0.5, (-80, 35, -25)), (0.56, (-100, 0, -5)), (0.9, (-70, 0, -30)),
                                 (1.25, (0, 0, -50))])
    nouvelles['tir'] = a
    return nouvelles


CREATIONS = {
    'reine_guepe': reine_guepe,
    'abomination': abomination,
    'necromancien': necromancien,
    'archer_squelette': archer_squelette,
}
