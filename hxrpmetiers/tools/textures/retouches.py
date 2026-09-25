"""Icônes redessinées après essai en jeu (plus lisibles, sans pixels parasites)."""
from parts import *
from ingredients import raw_meat
from plats import soup, shrimp_small


def _seed_dot(c, x, y, pit='#b88a18', dot='#f8e070'):
    c.fill(rect(x, y, x, y), pit)
    c.fill(rect(x, y - 1, x, y - 1), dot)


@item('fraise')
def fraise(c):
    c.shadow_under(16, 29, 8, 1.6)
    m = ellipse(16, 15, 11, 7) | polygon([(5.5, 15), (26.5, 15), (22, 24), (16, 30), (10, 24)])
    m &= YY >= 9
    c.shape(m, R('#e3202c', light=1.25), L_sphere(12, 13, 13, 14, 0.15), hl=4)
    # akènes en quinconce, dans des petits creux
    inner = m & ~edge(m) & ~shift(edge(m), 0, 1)
    for row, y in enumerate(range(13, 28, 3)):
        for x in range(7 + (row % 2) * 2, 26, 4):
            if inner[y, x] and inner[y - 1, x]:
                _seed_dot(c, x, y)
    # collerette : cinq sépales en étoile + pédoncule
    for (x1, y1) in [(6, 12), (10, 14), (16, 13), (22, 14), (26, 12)]:
        leaf(c, 16, 9, x1, y1, 3.6, '#3fa02e', vein=False)
    c.shape(circle(16, 9, 2.2), R('#358a26'), L_sphere(15, 8, 2.2, 2.2))
    c.shape(capsule(16, 8, 18, 2, 1.1, 0.8), R('#4a7a2a'), L_const(0.55))
    pal('fraise', 'Rond', '#e0202a', '#f86a6a', '#fbe0e0', 3)


@item('noisette')
def noisette(c):
    def nut(x, y, s=1.0, seed=0):
        m = ellipse(x, y + 1 * s, 5.6 * s, 5.2 * s) | polygon([(x - 4.5 * s, y - 1 * s), (x, y - 7.5 * s), (x + 4.5 * s, y - 1 * s)])
        c.shape(m, R('#a8662e'), L_sphere(x - 2, y - 3, 6 * s, 7 * s, 0.1), hl=2)
        # stries verticales de la coque
        for dx in (-3, -1, 1, 3):
            c.fill(line_mask(x + dx * s * 0.8, y - 4 * s, x + dx * s, y + 4 * s) & m & ~edge(m), '#8a4e22')
        # base mate plus claire (point d'attache)
        c.shape(ellipse(x, y + 4.2 * s, 4.2 * s, 2 * s) & m, R('#d8b882'), L_const(0.65), edge=False)
        c.speckle(ellipse(x, y + 4.2 * s, 4.2 * s, 2 * s) & m, '#b89060', 0.25, seed=seed)
        return m
    c.shadow_under(16, 28, 13, 2)
    nut(10, 20, 1.0, 1)
    nut(22, 21, 0.95, 2)
    # une noisette encore dans sa collerette verte déchiquetée
    nut(16, 13, 0.85, 3)
    husk = polygon([(9, 13), (11, 9), (13, 11), (14, 6), (16, 9), (18, 5), (19, 10), (22, 8), (21, 13), (23, 15), (16, 17), (9, 15)])
    husk &= ~ellipse(16, 11, 3.2, 3.5)
    c.shape(husk, R('#7aa83a'), L_sphere(14, 9, 8, 6, 0.3))
    pal('noisette', 'Rond', '#9a5a2a', '#f4e8d0', '#6a3a1a', 0)


@item('citron')
def citron(c):
    c.shadow_under(16, 27, 11, 2)
    m = ellipse(16, 18, 11.5, 8) | ellipse(4.5, 18, 2.2, 1.6) | ellipse(27.5, 18, 2.2, 1.6)
    c.shape(m, R('#f2d21e', light=1.2), L_sphere(12, 14, 12, 9, 0.1), hl=4)
    # pores de l'écorce, discrets
    inner = m & ~edge(m)
    for (x, y) in rng_pts(11, inner, 30):
        c.fill(rect(x, y, x, y), '#dcb612')
    for (x, y) in rng_pts(12, inner & (YY < 17) & (XX < 16), 8):
        c.fill(rect(x, y, x, y), '#faec80')
    c.shape(capsule(20, 10, 21, 7, 0.9), R('#6a5a2a'), L_const(0.5), edge=False)
    leaf(c, 21, 8, 29, 4, 4.6, '#3a8a2a')
    pal('citron', 'Rond', '#f4d824', '#f8ec80', '#fbf8d8', 4)


@item('huitre')
def huitre(c):
    c.shadow_under(16, 28, 14, 2.2)
    # coquille creuse, irrégulière, feuilletée
    shell = polygon([(2, 15), (4, 9), (10, 5), (17, 4), (24, 6), (29, 10), (30, 16), (27, 22), (20, 27), (12, 27), (5, 23)])
    c.shape(shell, R('#7e7462'), L_sphere(10, 8, 17, 15, 0.2))
    rng = np.random.RandomState(3)
    for k in range(5):
        rx, ry = 14.5 - k * 1.3, 11.5 - k * 1.1
        band = arc_band(16, 16, rx, ry, 1, 0, 360) & shell & ~edge(shell)
        c.fill(band & (rng.rand(32, 32) < 0.8), ['#a49a84', '#6a6252', '#b4aa94', '#665e50', '#a49a84'][k])
    # nacre irisée
    nac = ellipse(16, 15.5, 10.5, 7.5)
    c.shape(nac, ['#aab0bc', '#cfd4dc', '#e8ecf2', '#fafbfd'], L_sphere(12, 12, 11, 8), edge=False)
    c.fill(arc_band(16, 15.5, 10.5, 7.5, 1) & nac, '#7c8290')
    c.fill(arc_band(15, 15, 9, 6, 1, 150, 230) & nac, '#e0d4ec')
    # chair grise, bord frangé foncé
    body = ellipse(16.5, 16.5, 7.5, 5)
    c.shape(body, ['#9a907e', '#b8ae9a', '#d0c8b6', '#e4dccc'], L_sphere(14, 14, 8, 5, 0.1), edge=False, hl=2)
    frange = arc_band(16.5, 16.5, 7.5, 5, 1.2) & body
    c.fill(frange, '#5e5648')
    for a in range(10, 360, 36):
        x = 16.5 + 8.2 * math.cos(math.radians(a))
        y = 16.5 + 5.6 * math.sin(math.radians(a))
        c.fill(rect(int(x), int(y), int(x), int(y)) & nac, '#5e5648')
    c.shape(ellipse(19, 17.5, 2, 1.5), ['#8a7e6a', '#a09078'], L_const(0.5), edge=False)
    pal('huitre', 'Carapace', '#8a8a7a', '#c8c0b0', '#e8e4dc', 0)

@item('petit_pois')
def petit_pois(c):
    c.shadow_under(16, 28, 13, 1.8)
    # cosse ouverte en diagonale : dos, intérieur clair, pois, lèvre avant
    back = capsule(4, 25, 26, 9, 5.6, 4)
    c.shape(back, R('#3f8e2a'), L_cyl(4, 25, 26, 9, 5.6))
    inner = capsule(6, 23, 24, 10.5, 4, 2.8)
    c.shape(inner, ['#9ccc70', '#b4dc8c', '#c8e8a4'], L_const(0.6), edge=False)
    for k in range(5):
        t = k / 4
        x, y = 8.5 + t * 14, 20.5 - t * 9.5
        r = 3.1 - abs(t - 0.5) * 0.9
        c.shape(circle(x, y, r), ['#3e8a24', '#5cb032', '#7cd046', '#9ee064', '#c8f498'], L_sphere(x - 1.2, y - 1.2, r, r), hl=1)
    lip = capsule(5, 28, 27, 12, 2, 1.4) & ~capsule(6, 23, 24, 10.5, 4.2, 3)
    c.shape(lip, R('#4ea236'), L_const(0.62))
    c.shape(capsule(26, 9, 29, 4, 1, 0.8), R('#7a8a3a'), L_const(0.5))
    c.shape(capsule(4, 26, 2, 29, 1, 0.7), R('#8ab04a'), L_const(0.6))
    for (x, y) in [(27, 24), (23, 28)]:
        c.shape(circle(x, y, 2.3), R('#6ec43c', light=1.28), L_sphere(x - 0.9, y - 0.9, 2.3, 2.3), hl=1)
    pal('petit_pois', 'Rond', '#6cc03a', '#a8e080', '#3a8a2a', 0)

@item('mouton')
def mouton(c):
    """Carré d'agneau : quatre côtes manchonnées, noix de viande rouge, couche de gras."""
    c.shadow_under(16, 28, 14, 2)
    for k in range(4):
        x = 8 + k * 5.5
        bone = capsule(x, 15, x + 2.5, 4, 1.5, 1.2)
        c.shape(bone, R('#f4ecdc'), L_cyl(x, 15, x + 2.5, 4, 1.5))
        c.shape(circle(x + 2.5, 3.8, 1.6), R('#fbf6ec'), L_sphere(x + 2, 3.2, 1.6, 1.6))
    meat = polygon([(3, 17), (6, 13), (27, 12), (30, 16), (29, 25), (4, 26)])
    c.shape(meat, R('#f0dccc'), L_sphere(12, 14, 16, 10, 0.3))
    loin = ellipse(16, 21, 11, 4)
    raw_meat(c, loin, '#b02838', '#f0c8c8', seed=9, marbling=5)
    c.fill(arc_band(16, 21, 11, 4, 1) & loin, '#8a1a28')
    for k in range(4):
        x = 8.5 + k * 5.5
        c.fill(line_mask(x, 14, x - 0.5, 16.5) & meat & ~edge(meat), '#d8b8a4')
    pal('mouton', 'Pavé', '#a82a3a', '#c84a58', '#f4e8dc', 5)

@item('calamar')
def calamar(c):
    """Encornet entier, en diagonale : manteau tacheté, nageoires, yeux, tentacules."""
    c.shadow_under(16, 29, 13, 1.8)
    # tentacules (dessous), deux longs
    for k in range(6):
        x = 8 + k * 1.8
        c.fill(line_mask(x + 3, 19, x - 3, 28 - (k % 2), 1), '#e2a2ae')
    c.fill(line_mask(10, 20, 2, 30, 1), '#d8909e')
    c.fill(line_mask(14, 21, 11, 31, 1), '#d8909e')
    # manteau
    mantle = capsule(13, 18, 25, 5, 5, 3)
    c.shape(mantle, R('#f2dcdc', light=1.12), L_cyl(13, 18, 25, 5, 5), hl=2)
    for (x, y) in rng_pts(21, mantle & ~edge(mantle), 26):
        c.fill(rect(x, y, x, y), '#b86a8a' if (x + y) % 3 else '#9a4a72')
    # nageoires en losange au bout
    fins = polygon([(21, 4), (25, 0), (30, 3), (28, 8)])
    c.shape(fins, R('#ecc8d0'), L_const(0.65))
    # tête et yeux
    head = ellipse(11.5, 19.5, 4, 3.4)
    c.shape(head, R('#ecc6cc'), L_sphere(10, 18, 4, 3.4))
    c.fill(rect(10, 18, 11, 19), '#101018')
    c.fill(rect(10, 18, 10, 18), '#e8f0ff')
    pal('calamar', 'Long', '#f0d0d0', '#fbf4f0', '#c87a9a', 2)


@item('radis')
def radis(c):
    c.shadow_under(16, 29, 12, 1.6)
    for (cx, cy, lean) in [(10, 19, -1), (21, 21, 1)]:
        for (dx, top) in [(-2, 2), (1, 1), (3, 4)]:
            stem(c, [(cx, cy - 5), (cx + lean * 2 + dx, cy - 10)], '#5a9a3a')
            leaf(c, cx + lean * 2 + dx, cy - 9, cx + lean * 4 + dx * 1.6, top, 4.5, '#4a9a3a', vein=True)
        m = circle(cx, cy, 5.8)
        c.shape(m, R('#d8244a', light=1.25), L_sphere(cx - 2, cy - 2, 5.8, 5.8), hl=2)
        low = m & (YY > cy + 2)
        c.shape(low, ['#d8c8cc', '#efe4e6', '#fbf6f6'], L_sphere(cx - 2, cy, 5.8, 5.8), edge=False)
        c.fill(edge(m) & low, '#c0a8ae')
        c.fill(line_mask(cx, cy + 5.8, cx + lean, cy + 9.5), '#e8dada')
    pal('radis', 'Rond', '#d8284a', '#fbf4f4', '#4a9a3a', 0)


def _petit_poisson(c, x0, y0, x1, y1, h, back, belly, band, seed, spots=False):
    b = fish_body(c, x0, y0, x1, y1, h, back, belly, fin=mix(back, '#ffffff', 0.25), seed=seed)
    dx, dy = x1 - x0, y1 - y0
    Lg = math.hypot(dx, dy)
    ux, uy = dx / Lg, dy / Lg
    # ligne argentée le long du flanc
    c.fill(line_mask(x0 + ux * 5, y0 + uy * 5 - 0.3, x1 - ux * 2, y1 - uy * 2 - 0.3) & b & ~edge(b), band)
    if spots:
        for k in range(4):
            px, py = x0 + ux * (7 + k * 3.2) + uy * h * 0.18, y0 + uy * (7 + k * 3.2) - ux * h * 0.18
            c.fill(rect(int(px), int(py), int(px), int(py)) & b, '#1a2a3a')
    return b


@item('sardine')
def sardine(c):
    c.shadow_under(16, 28, 13, 1.6)
    _petit_poisson(c, 3, 12, 26, 8, 7, '#2e5e86', '#eef2f6', '#b8d0e4', 6, spots=True)
    _petit_poisson(c, 5, 23, 28, 19, 7, '#2e5e86', '#eef2f6', '#b8d0e4', 7, spots=True)
    pal('sardine', 'Poisson', '#3a5a8a', '#b86a6a', '#e8ecf0', 0)


@item('anchois')
def anchois(c):
    c.shadow_under(16, 28, 12, 1.6)
    for k, (y0, y1) in enumerate([(8, 5), (15, 13), (22, 21)]):
        _petit_poisson(c, 4 + k, y0 + 1, 27 + k, y1, 4.6, '#4a6e8e', '#f0f4f8', '#dfe8f2', 9 + k)
    pal('anchois', 'Poisson', '#5a7a9a', '#b87a7a', '#e8eef4', 0)


@item('lapin')
def lapin(c):
    """Lapin dépouillé : corps fin, longues cuisses arrière, petite tête pointue."""
    c.shadow_under(16, 28, 13, 1.8)
    # pattes arrière longues, repliées vers l'arrière
    c.shape(capsule(7, 22, 3, 28, 2.2, 1.2) | capsule(9, 23, 7, 29, 2.2, 1.2), R('#dca09c'), L_const(0.6))
    # pattes avant fines
    c.shape(capsule(22, 13, 26, 18, 1.2, 0.9) | capsule(20, 14, 23, 19, 1.2, 0.9), R('#dca09c'), L_const(0.6))
    body = capsule(9, 21, 22, 12, 4.6, 3.2)
    c.shape(body, R('#e8b4b0', light=1.12), L_cyl(9, 21, 22, 12, 4.6), hl=2)
    # râble (dos) plus clair, côtes marquées
    c.fill(line_mask(8, 19, 21, 10) & body & ~edge(body), '#f4d0cc')
    for k in range(4):
        x = 15 + k * 2
        c.fill(line_mask(x, 19 - k * 1.4, x + 2, 21 - k * 1.4) & body & ~edge(body), '#c8868c')
    head = polygon([(21, 10), (25, 7), (30, 7), (27, 11), (23, 13)])
    c.shape(head, R('#e0a8a4'), L_sphere(25, 8, 4, 3))
    c.fill(rect(26, 8, 26, 8), '#6a2a30')
    pal('lapin', 'Pavé', '#e8b0a8', '#f0c8c0', '#c8888a', 0)

@item('banane')
def banane(c):
    c.shadow_under(16, 27, 12, 1.8)
    for k, (dx, dy, col_) in enumerate([(2, 1, '#e0bc1c'), (0, -1, '#f4d42a')]):
        outer = circle(20 + dx, 3 + dy, 21) & (YY > 6 + dy)
        crescent = outer & ~circle(23 + dx, -2 + dy, 20.5)
        crescent &= (XX > 3 + dx) & (XX < 29 + dx)
        c.shape(crescent, R(col_, light=1.2), L_sphere(10 + dx, 15 + dy, 16, 14, 0.4), hl=3 - k)
        c.fill(arc_band(21 + dx, 1 + dy, 21.5, 21.5, 1, 100, 150) & crescent & ~edge(crescent), R(col_)[1])
        c.shape(circle(4 + dx, 13 + dy, 1.4), R('#4a3a1e'), L_const(0.4))
    c.shape(capsule(27, 22, 30, 18, 1.4, 1.1), R('#7a7a38'), L_const(0.55))
    c.shape(circle(30, 18, 1.2), R('#4a3a1e'), L_const(0.4))
    pal('banane', 'Long', '#f4d02a', '#f8f0c8', '#6a5a2a', 0)

@item('pasteque')
def pasteque(c):
    c.shadow_under(16, 29, 12, 1.6)
    top_y = 9
    outer = polygon([(1, top_y), (31, top_y), (16, 30)])
    c.shape(outer, R('#2a7a28'), L_vert(top_y, 30, 0.7, 0.3))
    # rayures sombres de l'écorce
    for k in range(-3, 4):
        c.fill(line_mask(16 + k * 4.2, top_y, 16 + k * 0.8, 30, 1) & outer & ~edge(outer), '#1c5a1c')
    white = polygon([(3, top_y), (29, top_y), (16, 27.2)])
    c.shape(white, ['#b8d890', '#dcecc0', '#f0f6e0'], L_vert(top_y, 27, 0.8, 0.4), edge=False)
    flesh = polygon([(4.5, top_y), (27.5, top_y), (16, 25.3)])
    c.shape(flesh, R('#ea3440', light=1.2), L_vert(top_y, 25, 0.95, 0.45), edge=False)
    # pépins en goutte
    for (x, y) in [(9, 12), (16, 11), (23, 12), (12, 16), (20, 16), (16, 20)]:
        c.fill(rect(x, y, x, y + 1), '#1a1010')
        c.fill(rect(x, y, x, y), '#4a3a3a')
    # tranche de dessus, plus claire (épaisseur)
    c.shape(rect(1, top_y - 2, 30, top_y - 1), ['#e84a50', '#f26a6c', '#f8908c'], L_horiz(1, 30, 0.9, 0.5), edge=False)
    c.fill(rect(1, top_y - 2, 2, top_y - 1) | rect(29, top_y - 2, 30, top_y - 1), '#2a7a28')
    pal('pasteque', 'Rond', '#2a7a28', '#e8303a', '#1a1010', 3)


@item('salade_verte')
def salade_verte(c):
    op = bowl(c, 16, 17, 13, 5, 9, '#b8844a')
    greens = ['#4f9a2e', '#6cc03a', '#8ad24c', '#3f8a28', '#7ccc44', '#5aae34']
    heads = [(7, 14, 4.5), (13, 11, 5), (20, 11, 5), (25, 14, 4.5), (10, 16, 4), (17, 15, 4.5), (23, 16, 4)]
    for k, (x, y, r) in enumerate(heads):
        m = ellipse(x, y, r, r * 0.8)
        # bord ondulé de la feuille
        for a in range(0, 360, 45):
            m |= circle(x + r * 0.85 * math.cos(math.radians(a)), y + r * 0.7 * math.sin(math.radians(a)), 1.4)
        col_ = greens[k % len(greens)]
        c.shape(m, R(col_, light=1.25), L_sphere(x - 1.5, y - 1.5, r + 1, r), hl=1)
        c.fill(line_mask(x, y + r * 0.6, x - r * 0.3, y - r * 0.5) & m & ~edge(m), R(col_)[4])
    bowl_rim(c, 16, 17, 13, 5, '#b8844a')

@item('curry_de_crevettes')
def curry_de_crevettes(c):
    """Bol creux de curry orange, crevettes roses, coriandre."""
    def top(c, m):
        c.speckle(m, '#f0a040', 0.12, seed=3)
        for (x, y) in [(10, 13), (17, 12), (22, 15), (13, 16)]:
            shrimp_small(c, x, y, 0.95)
        for (x, y) in [(15, 14), (20, 12), (9, 16)]:
            leaf(c, x, y, x + 2.5, y - 1.5, 2.2, '#3aa02a', vein=False)
        c.fill(arc_band(16, 14, 6, 2, 1, 200, 320) & m, '#f8e0a8')
    soup(c, '#2e3e5a', '#e2741c', top, cy=14, depth=11, foot=True)
