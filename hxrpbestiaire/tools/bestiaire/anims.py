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


def enchainer(a1, a2, pause=0.0, boucle=False):
    """a1, puis (après une pause figée sur sa dernière pose) a2 : une seule animation."""
    L1 = float(a1['animation_length'])
    a = anim(L1 + pause + float(a2['animation_length']), boucle)
    for source, decalage in ((a1, 0.0), (a2, L1 + pause)):
        for os_, canaux in source['bones'].items():
            for canal, kf in canaux.items():
                dest = a['bones'].setdefault(os_, {}).setdefault(canal, {})
                if not isinstance(kf, dict):
                    kf = {'0.0': kf}
                for t, v in kf.items():
                    dest['%.4f' % (float(t) + decalage)] = copy.deepcopy(v)
    # la pose de fin de a1 tenue pendant la pause, pour chaque os qu'a1 anime
    for os_, canaux in a1['bones'].items():
        for canal, kf in canaux.items():
            if isinstance(kf, dict) and kf:
                fin = kf[max(kf, key=float)]
                a['bones'][os_][canal]['%.4f' % (L1 + pause - 0.001)] = copy.deepcopy(fin)
    for os_ in a['bones']:
        for canal in a['bones'][os_]:
            a['bones'][os_][canal] = dict(sorted(a['bones'][os_][canal].items(), key=lambda kv: float(kv[0])))
    return a


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


# ============================================================================ Xerra
PATTES_XERRA = ('leg1', 'leg4', 'leg7', 'leg10', 'leg13', 'leg16', 'leg19', 'leg22')


def xerra(A):
    idle = A['idle']
    nouvelles = {}
    b1, b2 = valeur_repos(idle, 'arm_1', 'rotation'), valeur_repos(idle, 'arm_2', 'rotation')

    # jet de toile : elle se cabre, l'abdomen passe sous le corps (filières pointées devant), la femme tend les mains
    a = anim(1.5)
    fond(a, idle, sauf=('xerra', 'abdomen', 'arm_1', 'arm_2', 'woman'))
    cles(a, 'xerra', 'rotation', [(0, (0, 0, 0)), (0.4, (-14, 0, 0)), (0.7, (-16, 0, 0)), (0.8, (4, 0, 0)), (1.1, (2, 0, 0)), (1.5, (0, 0, 0))])
    cles(a, 'xerra', 'position', [(0, (0, 0, 0)), (0.4, (0, 2, 3)), (0.7, (0, 2, 3)), (0.8, (0, -1, -2)), (1.5, (0, 0, 0))])
    cles(a, 'abdomen', 'rotation', [(0, (0, 0, 0)), (0.4, (-45, 0, 0)), (0.7, (-60, 0, 0)), (0.8, (-40, 0, 0)), (1.1, (-35, 0, 0)), (1.5, (0, 0, 0))])
    cles(a, 'woman', 'rotation', [(0, (0, 0, 0)), (0.4, (-10, 0, 0)), (0.8, (15, 0, 0)), (1.5, (0, 0, 0))])
    cles(a, 'arm_1', 'rotation', [(0, b1), (0.4, (-40, 20, -60)), (0.75, (-100, 0, -10)), (1.1, (-90, 0, -15)), (1.5, b1)])
    cles(a, 'arm_2', 'rotation', [(0, b2), (0.4, (-40, -20, 60)), (0.75, (-100, 0, 10)), (1.1, (-90, 0, 15)), (1.5, b2)])
    nouvelles['jet_toile'] = a

    # bond : elle se ramasse, pattes pliées, jaillit (le saut lui-même est physique), pattes repliées en l'air, retombe
    a = anim(2.0)
    fond(a, idle, sauf=('xerra', 'abdomen', 'claw1', 'claw2') + PATTES_XERRA)
    cles(a, 'xerra', 'position', [(0, (0, 0, 0)), (0.4, (0, -7, 0)), (0.55, (0, 3, 0)), (1.3, (0, 2, 0)), (1.45, (0, -8, 0)), (1.75, (0, -3, 0)), (2.0, (0, 0, 0))])
    cles(a, 'xerra', 'rotation', [(0, (0, 0, 0)), (0.4, (8, 0, 0)), (0.55, (-18, 0, 0)), (1.1, (10, 0, 0)), (1.45, (12, 0, 0)), (2.0, (0, 0, 0))])
    cles(a, 'abdomen', 'rotation', [(0, (0, 0, 0)), (0.4, (12, 0, 0)), (0.6, (-20, 0, 0)), (1.3, (-10, 0, 0)), (1.5, (15, 0, 0)), (2.0, (0, 0, 0))])
    for pince in ('claw1', 'claw2'):
        cles(a, pince, 'rotation', [(0, (0, 0, 0)), (0.4, (15, 0, 0)), (0.6, (-45, 0, 0)), (1.3, (-30, 0, 0)), (1.5, (25, 0, 0)), (2.0, (0, 0, 0))])
    for patte in PATTES_XERRA:
        # z vers le corps : patte repliée (côté gauche x<0 : z négatif la relève)
        s_ = -1 if patte in ('leg1', 'leg4', 'leg7', 'leg10') else 1
        cles(a, patte, 'rotation', [(0, (0, 0, 0)), (0.4, (0, 0, 18 * s_)), (0.55, (0, 0, -20 * s_)), (0.8, (0, 0, 35 * s_)), (1.3, (0, 0, 30 * s_)),
                                    (1.45, (0, 0, -15 * s_)), (2.0, (0, 0, 0))])
    nouvelles['bond'] = a

    # renaissance : elle s'effondre (xerra_death), reste à terre, puis se relève (xerra_revive)
    nouvelles['renaissance'] = enchainer(A['xerra_death'], A['xerra_revive'], 2.3)
    return nouvelles


# ============================================================================ Roi des glaces
def roi_des_glaces(A):
    idle = A['idle']
    nouvelles = {}
    b1, b2 = valeur_repos(idle, 'arm1', 'rotation'), valeur_repos(idle, 'arm2', 'rotation')
    s1, s2 = valeur_repos(idle, 'sub_arm1', 'rotation'), valeur_repos(idle, 'sub_arm2', 'rotation')
    tete = valeur_repos(idle, 'head', 'rotation')
    mand = valeur_repos(idle, 'mandible', 'rotation')

    # appel de l'escorte : il se redresse, lève les deux bras au ciel, mâchoire ouverte, et les abat
    a = anim(2.4)
    fond(a, idle, sauf=('body', 'head', 'mandible', 'arm1', 'arm2', 'sub_arm1', 'sub_arm2'))
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.6, (-18, 0, 0)), (1.4, (-22, 0, 0)), (1.7, (12, 0, 0)), (2.4, (0, 0, 0))])
    cles(a, 'head', 'rotation', [(0, tete), (0.6, (-15, 0, 0)), (1.4, (-20, 0, 0)), (1.7, (25, 0, 0)), (2.4, tete)])
    cles(a, 'mandible', 'rotation', [(0, mand), (0.6, (35, 0, 0)), (1.4, (40, 0, 0)), (1.7, (10, 0, 0)), (2.4, mand)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.6, (-165, -10, 20)), (1.4, (-170, -10, 25)), (1.7, (-60, 0, 30)), (2.4, b1)])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.6, (-165, 10, -20)), (1.4, (-170, 10, -25)), (1.7, (-60, 0, -30)), (2.4, b2)])
    cles(a, 'sub_arm1', 'rotation', [(0, s1), (0.6, (-10, 0, 0)), (1.7, (-30, 0, 0)), (2.4, s1)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (0.6, (-10, 0, 0)), (1.7, (-30, 0, 0)), (2.4, s2)])
    nouvelles['invocation_bras_leves'] = a

    # lance de glace : la main libre (arm2) se charge derrière l'épaule, puis lance vers l'avant (vers 50 %)
    a = anim(1.6)
    fond(a, idle, sauf=('body', 'arm2', 'sub_arm2'))
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.6, (-8, -25, 0)), (0.8, (12, 20, 0)), (1.6, (0, 0, 0))])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.6, (-150, 20, -40)), (0.8, (-70, -10, -15)), (1.0, (-40, -10, -18)), (1.6, b2)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (0.6, (-80, 0, 0)), (0.8, (-10, 0, 0)), (1.6, s2)])
    nouvelles['lance_glace'] = a

    # pluie de stalactites : il brandit le bâton au ciel à deux mains, puis le plante au sol (vers 60 %)
    a = anim(2.2)
    fond(a, idle, sauf=('ice_king', 'body', 'head', 'arm1', 'arm2', 'sub_arm1', 'sub_arm2'))
    cles(a, 'ice_king', 'position', [(0, (0, -2, 0)), (0.9, (0, 1, 0)), (1.3, (0, -7, 0)), (1.7, (0, -5, 0)), (2.2, (0, -2, 0))])
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.9, (-20, 0, 0)), (1.3, (30, 0, 0)), (1.7, (25, 0, 0)), (2.2, (0, 0, 0))])
    cles(a, 'head', 'rotation', [(0, tete), (0.9, (-25, 0, 0)), (1.3, (20, 0, 0)), (2.2, tete)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.9, (-170, 0, 5)), (1.3, (-60, 0, 10)), (1.7, (-55, 0, 10)), (2.2, b1)])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.9, (-170, 0, -5)), (1.3, (-60, 0, -10)), (1.7, (-55, 0, -10)), (2.2, b2)])
    cles(a, 'sub_arm1', 'rotation', [(0, s1), (0.9, (0, 0, 0)), (1.3, (-20, 0, 0)), (2.2, s1)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (0.9, (0, 0, 0)), (1.3, (-20, 0, 0)), (2.2, s2)])
    nouvelles['stalactites'] = a

    # couronne de givre (renaissance) : il s'effondre à genoux, les bras croisés, figé et tremblant dans la glace,
    # puis se libère d'un coup, bras écartés
    a = anim(5.0)
    tremble = [(1.0 + i * 0.1, (0, 0, (1.5 if i % 2 else -1.5))) for i in range(26)]
    cles(a, 'ice_king', 'position', [(0, (0, -2, 0)), (0.8, (0, -12, 0)), (3.6, (0, -12, 0)), (3.9, (0, 2, 0)), (5.0, (0, -2, 0))])
    cles(a, 'ice_king', 'rotation', [(0, (0, 0, 0)), (0.8, (0, 0, 0))] + tremble + [(3.7, (0, 0, 0)), (5.0, (0, 0, 0))])
    cles(a, 'body', 'rotation', [(0, (0, 0, 0)), (0.8, (28, 0, 0)), (3.6, (30, 0, 0)), (3.9, (-25, 0, 0)), (4.4, (-15, 0, 0)), (5.0, (0, 0, 0))])
    cles(a, 'head', 'rotation', [(0, tete), (0.8, (45, 0, 0)), (3.6, (45, 0, 0)), (3.9, (-30, 0, 0)), (4.4, (-25, 0, 0)), (5.0, tete)])
    cles(a, 'mandible', 'rotation', [(0, mand), (3.6, mand), (3.9, (40, 0, 0)), (4.4, (35, 0, 0)), (5.0, mand)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.8, (-70, 30, -25)), (3.6, (-70, 30, -25)), (3.9, (-30, 0, 80)), (4.4, (-30, 0, 70)), (5.0, b1)])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.8, (-70, -30, 25)), (3.6, (-70, -30, 25)), (3.9, (-30, 0, -80)), (4.4, (-30, 0, -70)), (5.0, b2)])
    for jambe, sj in (('leg1', 'sub_leg1'), ('leg2', 'sub_leg2')):
        j0, sj0 = valeur_repos(idle, jambe, 'rotation'), valeur_repos(idle, sj, 'rotation')
        cles(a, jambe, 'rotation', [(0, j0), (0.8, plus(j0, (-55, 0, 0))), (3.6, plus(j0, (-55, 0, 0))), (3.9, j0), (5.0, j0)])
        cles(a, sj, 'rotation', [(0, sj0), (0.8, (95, 0, 0)), (3.6, (95, 0, 0)), (3.9, sj0), (5.0, sj0)])
    nouvelles['couronne_de_givre'] = a
    return nouvelles


# ============================================================================ Gobelin archer
def gobelin_archer(A):
    idle = A['idle']
    nouvelles = {}
    g0, d0 = valeur_repos(idle, 'leftArm', 'rotation'), valeur_repos(idle, 'rightArm', 'rotation')
    # tir : l'arc (bras gauche) tendu devant, la main droite bande la corde, lâche vers 50 %, puis revient
    a = anim(1.2)
    fond(a, idle, sauf=('leftArm', 'rightArm', 'ice_goblin_distance', 'head'))
    cles(a, 'ice_goblin_distance', 'rotation', [(0, (0, 8.9, 0)), (0.3, (0, 25, 0)), (0.9, (0, 25, 0)), (1.2, (0, 8.9, 0))])
    cles(a, 'head', 'rotation', [(0, valeur_repos(idle, 'head', 'rotation')), (0.3, (-5, -20, 0)), (0.9, (-5, -20, 0)), (1.2, valeur_repos(idle, 'head', 'rotation'))])
    cles(a, 'leftArm', 'rotation', [(0, g0), (0.3, (-90, -20, 0)), (0.6, (-92, -20, 0)), (0.65, (-98, -20, 0)), (0.9, (-88, -20, 0)), (1.2, g0)])
    cles(a, 'rightArm', 'rotation', [(0, d0), (0.3, (-90, -40, 0)), (0.55, (-85, -70, 0)), (0.6, (-95, -10, 0)), (0.9, (-60, -10, 0)), (1.2, d0)])
    nouvelles['tir'] = a
    return nouvelles


# ============================================================================ Démon noir
def demon_noir(A):
    idle = A['idle']
    nouvelles = {}
    corps = valeur_repos(idle, 'body', 'rotation')
    tete = valeur_repos(idle, 'head', 'rotation')
    b1, b2 = valeur_repos(idle, 'arm1', 'rotation'), valeur_repos(idle, 'arm2', 'rotation')
    s1, s2 = valeur_repos(idle, 'sub_arm1', 'rotation'), valeur_repos(idle, 'sub_arm2', 'rotation')
    jambes = {j: valeur_repos(idle, j, 'rotation') for j in ('leg1', 'sub_leg1', 'leg2', 'sub_leg2')}

    # charge d'épaule : il se ramasse, épaule droite en avant, tête baissée, et court (la course est physique)
    a = anim(2.0)
    fond(a, idle, sauf=('body', 'head', 'arm1', 'arm2', 'sub_arm1', 'sub_arm2', 'leg1', 'leg2', 'sub_leg1', 'sub_leg2', 'body_ppal'))
    cles(a, 'body_ppal', 'position', [(0, (0, 10, 0)), (0.35, (0, 4, 0))] + [(0.5 + i * 0.15, (0, 6 if i % 2 else 2, 0)) for i in range(9)] + [(2.0, (0, 10, 0))])
    cles(a, 'body', 'rotation', [(0, corps), (0.35, (40, -30, 5)), (1.7, (40, -30, 5)), (2.0, corps)])
    cles(a, 'head', 'rotation', [(0, tete), (0.35, (10, 20, 0)), (1.7, (10, 20, 0)), (2.0, tete)])
    cles(a, 'arm1', 'rotation', [(0, b1), (0.35, (-35, 20, 15)), (1.7, (-35, 20, 15)), (2.0, b1)])
    cles(a, 'sub_arm1', 'rotation', [(0, s1), (0.35, (-85, 0, 0)), (1.7, (-85, 0, 0)), (2.0, s1)])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.35, (40, 0, -20)), (1.7, (40, 0, -20)), (2.0, b2)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (0.35, (-60, 0, 0)), (2.0, s2)])
    for i, (j, sj) in enumerate((('leg1', 'sub_leg1'), ('leg2', 'sub_leg2'))):
        pas = [(0.5 + k * 0.3, plus(jambes[j], (35 if (k + i) % 2 else -35, 0, 0))) for k in range(5)]
        cles(a, j, 'rotation', [(0, jambes[j]), (0.35, plus(jambes[j], (-15, 0, 0)))] + pas + [(2.0, jambes[j])])
        cles(a, sj, 'rotation', [(0, jambes[sj]), (0.35, plus(jambes[sj], (15, 0, 0))), (2.0, jambes[sj])])
    nouvelles['charge_epaule'] = a

    # orbe sombre : la main gauche se lève et forme l'orbe au-dessus de l'épaule, puis la projette (vers 50 %)
    a = anim(1.8)
    fond(a, idle, sauf=('body', 'arm2', 'sub_arm2', 'hand2', 'head'))
    cles(a, 'body', 'rotation', [(0, corps), (0.7, plus(corps, (-10, 20, 0))), (0.9, plus(corps, (15, -20, 0))), (1.8, corps)])
    cles(a, 'head', 'rotation', [(0, tete), (0.7, plus(tete, (-10, 0, 0))), (0.9, plus(tete, (10, 0, 0))), (1.8, tete)])
    cles(a, 'arm2', 'rotation', [(0, b2), (0.7, (-150, 0, -30)), (0.9, (-80, 0, -5)), (1.2, (-75, 0, -10)), (1.8, b2)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (0.7, (-70, 0, 0)), (0.9, (-5, 0, 0)), (1.2, (-10, 0, 0)), (1.8, s2)])
    cles(a, 'hand2', 'rotation', [(0, (0, 0, 0)), (0.7, (-30, 0, -20)), (0.9, (20, 0, 0)), (1.8, (0, 0, 0))])
    nouvelles['orbe'] = a

    # second souffle (renaissance) : il tombe à genoux, la tête basse, halète, puis se relève en rugissant
    a = anim(5.0)
    fond(a, idle, sauf=('body_ppal', 'body', 'head', 'arm1', 'arm2', 'sub_arm1', 'sub_arm2', 'leg1', 'leg2', 'sub_leg1', 'sub_leg2'))
    halete = [(1.2 + i * 0.35, (0, -16 if i % 2 else -18, 0)) for i in range(6)]
    cles(a, 'body_ppal', 'position', [(0, (0, 10, 0)), (1.0, (0, -18, 0))] + halete + [(3.8, (0, -6, 0)), (4.3, (0, 12, 0)), (5.0, (0, 10, 0))])
    cles(a, 'body', 'rotation', [(0, corps), (1.0, (45, 0, 0)), (3.4, (42, 0, 0)), (3.9, (10, 0, 0)), (4.3, (-25, 0, 0)), (5.0, corps)])
    cles(a, 'head', 'rotation', [(0, tete), (1.0, (40, 0, 0)), (3.4, (40, 0, 0)), (4.3, (-50, 0, 0)), (5.0, tete)])
    cles(a, 'arm1', 'rotation', [(0, b1), (1.0, (-40, 0, 10)), (3.4, (-40, 0, 10)), (4.3, (-30, 0, 85)), (5.0, b1)])
    cles(a, 'arm2', 'rotation', [(0, b2), (1.0, (-40, 0, -10)), (3.4, (-40, 0, -10)), (4.3, (-30, 0, -85)), (5.0, b2)])
    cles(a, 'sub_arm1', 'rotation', [(0, s1), (1.0, (-10, 0, 0)), (4.3, (-20, 0, 0)), (5.0, s1)])
    cles(a, 'sub_arm2', 'rotation', [(0, s2), (1.0, (-10, 0, 0)), (4.3, (-20, 0, 0)), (5.0, s2)])
    for j, sj, k in (('leg1', 'sub_leg1', 1), ('leg2', 'sub_leg2', -1)):
        cles(a, j, 'rotation', [(0, jambes[j]), (1.0, plus(jambes[j], (-30 if k > 0 else 10, 0, 0))), (3.6, plus(jambes[j], (-30 if k > 0 else 10, 0, 0))),
                                (4.3, jambes[j]), (5.0, jambes[j])])
        cles(a, sj, 'rotation', [(0, jambes[sj]), (1.0, plus(jambes[sj], (25 if k > 0 else -40, 0, 0))), (3.6, plus(jambes[sj], (25 if k > 0 else -40, 0, 0))),
                                 (4.3, jambes[sj]), (5.0, jambes[sj])])
    nouvelles['second_souffle'] = a
    return nouvelles


CREATIONS = {
    'reine_guepe': reine_guepe,
    'abomination': abomination,
    'necromancien': necromancien,
    'archer_squelette': archer_squelette,
    'xerra': xerra,
    'roi_des_glaces': roi_des_glaces,
    'gobelin_archer': gobelin_archer,
    'demon_noir': demon_noir,
}
