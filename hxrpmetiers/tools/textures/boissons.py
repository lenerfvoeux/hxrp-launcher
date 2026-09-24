"""Les 28 boissons."""
from parts import *
from plats import lemon_wedge, drizzle


def rim_slice(c, x, y, skin, flesh, r=4):
    """Rondelle d'agrume plantée sur le bord du verre."""
    m = circle(x, y, r)
    c.shape(m, R(skin), L_const(0.6))
    inner = circle(x, y, r - 1)
    c.shape(inner, R(flesh), L_const(0.8), edge=False)
    for a in range(0, 360, 60):
        c.fill(line_mask(x, y, x + math.cos(math.radians(a)) * (r - 1.2), y + math.sin(math.radians(a)) * (r - 1.2)),
               mix(flesh, '#ffffff', 0.5), clip=inner)
    c.erase(rect(int(x) - 1, int(y) + 1, int(x), int(y) + r) & m)


def juice(c, color, garnish=None, straw_col=None, ice=True, level=0.78):
    body, ly = glass(c, 8, 22, 6, 29, color, level=level, taper=1.8, ice=ice)
    if straw_col:
        straw(c, 17, 13, 24, 1, straw_col)
    if garnish:
        garnish(c)
    return body, ly


def mint(c, x, y, n=2):
    for k in range(n):
        leaf(c, x, y, x - 4 + k * 7, y - 4 + k, 3.4, '#3ab04a', vein=False)


def whipped(c, cx, y, rx=7):
    for (yy, r) in [(y, rx), (y - 2.5, rx - 2.2), (y - 4.5, rx - 4.5)]:
        m = ellipse(cx, yy, max(1.5, r), 2.2)
        c.shape(m, R('#fbfaf6'), L_sphere(cx - 1, yy - 1, r, 2.4), edge=True)


def tall(c, color, level=0.85):
    return glass(c, 9, 22, 4, 29, color, level=level, taper=1.2)


@item('jus_d_orange')
def jus_d_orange(c):
    juice(c, '#f8a020', lambda c: rim_slice(c, 22, 7, '#e8781a', '#f8b040'))


@item('jus_de_pomme')
def jus_de_pomme(c):
    def g(c):
        m = ellipse(23, 8, 4, 3.5) & (YY <= 9)
        c.shape(m, R('#d8242a'), L_const(0.6))
        c.shape(ellipse(23, 9, 3.4, 1.4), R('#f8f0c8'), L_const(0.8), edge=False)
    juice(c, '#f0c040', g)


@item('jus_de_raisin')
def jus_de_raisin(c):
    def g(c):
        for (x, y) in [(22, 6), (25, 6), (23.5, 8.5), (26.5, 8.5), (25, 11)]:
            c.shape(circle(x, y, 1.7), R('#6a2a7a'), L_sphere(x - 0.5, y - 0.5, 1.7, 1.7), edge=True)
        c.fill(line_mask(24, 4, 26, 1), '#6a5a2a')
    juice(c, '#6a1a4a', g)


@item('jus_d_ananas')
def jus_d_ananas(c):
    def g(c):
        m = polygon([(18, 6), (27, 2), (28, 8), (20, 9)])
        c.shape(m, R('#f4d040'), L_const(0.8))
        c.fill(line_mask(19, 7, 27, 3), '#c8a020')
        for (x1, y1) in [(26, 0), (30, 1)]:
            leaf(c, 27, 3, x1, y1, 2, '#3a8a3a', vein=False)
    juice(c, '#f4d850', g)


@item('jus_de_pasteque')
def jus_de_pasteque(c):
    def g(c):
        m = polygon([(18, 7), (28, 7), (23, 14)])
        c.shape(m, R('#2a7a2a'), L_const(0.5))
        c.shape(polygon([(19, 6.5), (27, 6.5), (23, 12)]), R('#e8303a'), L_const(0.8), edge=False)
        c.fill(points([(21, 8), (24, 8), (23, 10)]), '#1a1010')
        mint(c, 12, 7, 1)
    juice(c, '#f05060', g)


@item('eau_de_coco')
def eau_de_coco(c):
    m = circle(15, 19, 12)
    c.shape(m, R('#7a5030'), L_sphere(12, 15, 12, 12), hl=1)
    for (x, y) in rng_pts(3, m, 40):
        c.fill(rect(x, y, x + 1, y), '#9a7048')
    top = ellipse(15, 11, 8, 3)
    c.shape(top, R('#fbfaf0'), L_const(0.85))
    c.shape(ellipse(15, 11, 5.5, 1.8), R('#e8f4f8'), L_const(0.8), edge=False)
    straw(c, 16, 11, 24, 1, '#f0a0c0')
    m = polygon([(18, 3), (28, 0), (30, 5)])
    c.shape(m, R('#e83a8a'), L_const(0.7))
    c.fill(line_mask(24, 3, 22, 9), '#c8a070')


@item('citronnade')
def citronnade(c):
    def g(c):
        rim_slice(c, 22, 7, '#e8c820', '#f8ec80')
        mint(c, 12, 8, 1)
    juice(c, '#f8f4b0', g)


@item('smoothie_fraise_banane')
def smoothie_fraise_banane(c):
    body, ly = tall(c, '#f08a9a')
    c.fill(rect(10, int(ly) + 1, 21, int(ly) + 2) & body & ~edge(body), '#f8b8c0')
    straw(c, 17, 10, 24, 0, '#fbfaf4')
    m = polygon([(6, 3), (13, 3), (11, 7), (9.5, 10), (8, 7)]) | ellipse(9.5, 4, 3.5, 2)
    c.shape(m, R('#e0202a'), L_sphere(8, 4, 3.5, 4), hl=1)
    c.shape(polygon([(7, 2), (9.5, 0), (12, 2), (9.5, 3)]), R('#3a9a2a'), L_const(0.6))


@item('lait_a_la_fraise')
def lait_a_la_fraise(c):
    bottle(c, 15, 3, 29, 12, 5, 9, '#f4d8e0', liquid='#f8c0d0', cap='#e03a5a', label='#fbfaf4', cap_h=3)
    m = polygon([(13, 20), (18, 20), (17, 23), (15.5, 25), (14, 23)]) | ellipse(15.5, 20.5, 2.6, 1.4)
    c.shape(m, R('#e0202a'), L_const(0.7))
    c.fill(rect(15, 19, 16, 19), '#3a9a2a')


@item('lait_chaud_au_miel')
def lait_chaud_au_miel(c):
    top = mug(c, color='#f0d8a0', liquid='#fbf8ec')
    drizzle(c, [(10, 10), (13, 11), (16, 10)], '#e8a018')
    stem(c, [(22, 2), (16, 10)], '#b88a50', width=2)
    c.shape(rrect(20, 1, 25, 5, 1), R('#d8a030'), L_const(0.7))
    for k in range(2):
        c.fill(line_mask(11 + k * 5, 6, 12 + k * 5, 2), '#f0f4f8')


@item('chocolat_chaud')
def chocolat_chaud(c):
    top = mug(c, color='#c83a3a', liquid='#5a2a14')
    whipped(c, 14, 9, 6)
    c.speckle(ellipse(14, 7, 5, 3), '#6a3a1e', 0.2, seed=3)
    for (x, y) in [(10, 8), (18, 8)]:
        c.shape(rrect(x - 1, y - 1, x + 1, y + 1, 0), R('#fbf0f0'), L_const(0.8))


@item('cafe_noir')
def cafe_noir(c):
    c.shape(ellipse(15, 25, 12, 3.4), R('#f2eee6'), L_vert(22, 28, 0.9, 0.5))
    top = mug(c, 8, 21, 14, 25, color='#f2eee6', liquid='#2a140a', saucer=False)
    c.shape(ellipse(14.5, 14, 4, 1), R('#8a5a2a'), L_const(0.7), edge=False)
    for k in range(3):
        c.fill(line_mask(10 + k * 4, 10, 11 + k * 4, 5), '#e8ecf0')
    for (x, y) in [(25, 26), (4, 26)]:
        c.shape(ellipse(x, y, 1.6, 1.2), R('#4a2a14'), L_const(0.6))


@item('cafe_au_lait')
def cafe_au_lait(c):
    op = bowl(c, 16, 15, 13, 5, 10, '#f2eee6')
    m = fill_bowl(c, op, '#b8804a', 0.9, 16, 15, 13, 5)
    c.shape(ellipse(16, 14.5, 7, 2.2), R('#f0dcb8'), L_const(0.8), edge=False)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')
    for k in range(3):
        c.fill(line_mask(10 + k * 5, 8, 11 + k * 5, 3), '#f0f4f8')


@item('the_vert')
def the_vert(c):
    body, ly = glass(c, 7, 23, 11, 27, '#a8c850', level=0.8, taper=1)
    c.shape(arc_band(24, 19, 4, 4, 1.8, 270, 90), R('#dcecf2'), L_const(0.6))
    leaf(c, 15, int(ly) + 2, 19, int(ly) + 6, 3, '#4a8a2a', vein=False)
    for k in range(3):
        c.fill(line_mask(10 + k * 5, 8, 11 + k * 5, 3), '#f0f4f8')


@item('the_a_la_menthe')
def the_a_la_menthe(c):
    body, ly = glass(c, 9, 22, 10, 29, '#c8a030', level=0.8, taper=0.8)
    for y in (15, 22):
        c.fill(line_mask(9, y, 22, y) & body, '#e0b030')
        c.fill(line_mask(9, y + 1, 22, y + 1) & body, '#3a8ad0')
    mint(c, 15, 8, 2)
    c.shape(capsule(15, 9, 16, 12, 0.8), R('#4a8a2a'), L_const(0.6))
    for k in range(2):
        c.fill(line_mask(12 + k * 6, 5, 13 + k * 6, 1), '#f0f4f8')


@item('jus_multivitamine')
def jus_multivitamine(c):
    body, ly = glass(c, 8, 22, 6, 29, '#f8a020', level=0.8, taper=1.8, ice=True)
    c.fill(rect(9, 23, 21, 28) & body & ~edge(body), '#f86020')
    c.fill(rect(9, 20, 21, 22) & body & ~edge(body), '#f88020')
    straw(c, 17, 12, 24, 1, '#3ab04a')
    rim_slice(c, 9, 7, '#e8c820', '#f8ec80', r=3.4)


@item('smoothie_mangue_coco')
def smoothie_mangue_coco(c):
    body, ly = tall(c, '#f8b030')
    c.speckle(rect(10, int(ly), 21, int(ly) + 1) & body, '#fbfaf2', 0.5, seed=3)
    straw(c, 17, 10, 24, 0, '#2a8ad0')
    m = rrect(5, 2, 10, 7, 1)
    c.shape(m, R('#f8b030'), L_const(0.8))
    c.fill(line_mask(6, 3, 9, 6), '#e89020')


@item('smoothie_vert')
def smoothie_vert(c):
    body, ly = tall(c, '#7ac040')
    straw(c, 17, 10, 24, 0, '#f4f0e8')
    rim_slice(c, 9, 5, '#8a6a3a', '#7ac03a', r=3.6)
    c.fill(circle(9, 5, 1), '#f4f0c8')
    mint(c, 16, 3, 1)


def milkshake(c, color, top_col=None):
    body, ly = glass(c, 9, 22, 8, 29, color, level=0.9, taper=2.2)
    c.fill(line_mask(10, 20, 21, 20) & body & ~edge(body), R(color)[1])
    whipped(c, 15.5, 7, 7.5)
    c.shape(circle(16, 1.8, 1.8), R('#c8102a'), L_sphere(15.5, 1.3, 1.8, 1.8), hl=1)
    c.fill(line_mask(16, 1, 18, -1), '#4a6a2a')
    straw(c, 20, 6, 26, 0, '#e03a5a')
    if top_col:
        drizzle(c, [(10, 6), (13, 5), (16, 7), (20, 5)], top_col)


@item('milkshake_vanille')
def milkshake_vanille(c):
    milkshake(c, '#f8ecc8')
    c.speckle(rect(10, 12, 21, 27), '#3a2418', 0.03, seed=3)


@item('milkshake_chocolat')
def milkshake_chocolat(c):
    milkshake(c, '#8a5030', top_col='#4a2414')


@item('cappuccino')
def cappuccino(c):
    top = mug(c, 7, 21, 12, 26, color='#f2eee6', liquid='#e8d0a8')
    heart = circle(12.5, 12, 1.4) | circle(15, 12, 1.4) | polygon([(11.2, 12.3), (16.3, 12.3), (13.75, 14)])
    c.fill(heart & top, '#fbf6ec')
    c.fill(arc_band(14, 12, 6, 1.5, 1) & top, '#a86a3a')
    c.speckle(top, '#6a3a1e', 0.1, seed=4)


@item('cafe_glace')
def cafe_glace(c):
    body, ly = glass(c, 8, 22, 5, 29, '#8a5a30', level=0.85, taper=1.5, ice=True)
    c.fill(rect(9, 22, 21, 28) & body & ~edge(body), '#6a3a1e')
    for k in range(3):
        c.fill(arc_band(15, 14 + k * 3, 4, 1.2, 1, 0, 180) & body, '#f0dcc0')
    straw(c, 17, 11, 24, 0, '#2a2a2a', '#6a6a6a')


@item('the_glace_a_la_peche')
def the_glace_a_la_peche(c):
    body, ly = juice(c, '#d8801a', straw_col='#f08a50')
    m = ellipse(10, 7, 4, 3.5) & (YY <= 8)
    c.shape(m, R('#f0a060'), L_const(0.6))
    c.shape(ellipse(10, 8, 3.4, 1.3), R('#f8d098'), L_const(0.8), edge=False)
    leaf(c, 13, 5, 16, 2, 2.4, '#3a8a2a', vein=False)


@item('lassi_a_la_mangue')
def lassi_a_la_mangue(c):
    body, ly = glass(c, 9, 22, 7, 29, '#f8c040', level=0.88, taper=1.5)
    c.fill(rect(10, int(ly) + 1, 21, int(ly) + 1) & body, '#fbe890')
    c.speckle(ellipse(15.5, ly, 5, 1), '#e0a020', 0.4, seed=3)
    m = rrect(20, 3, 25, 8, 1)
    c.shape(m, R('#f8b030'), L_const(0.8))
    leaf(c, 16, 6, 12, 2, 2.4, '#3ab04a', vein=False)


@item('limonade_au_gingembre')
def limonade_au_gingembre(c):
    body, ly = juice(c, '#f4ecb0', straw_col='#f8f4e8')
    for (x, y) in [(11, 22), (14, 18), (18, 24), (12, 26), (19, 15)]:
        c.fill(rect(x, y, x, y), '#ffffff')
    g = capsule(21, 7, 27, 5, 1.8) | capsule(24, 6, 25, 2, 1.3)
    c.shape(g, R('#d8b078'), L_const(0.7))
    rim_slice(c, 9, 7, '#e8c820', '#f8ec80', r=3.4)


@item('chai_latte')
def chai_latte(c):
    top = mug(c, 7, 21, 11, 26, color='#3a6a8a', liquid='#d8b080')
    c.speckle(top, '#8a5a2a', 0.2, seed=3)
    c.shape(capsule(16, 11, 24, 2, 1.2), R('#9a5028'), L_cyl(16, 11, 24, 2, 1.2))
    star = polygon([(6, 3), (7, 5), (9, 5), (7.5, 6.5), (8, 9), (6, 7.5), (4, 9), (4.5, 6.5), (3, 5), (5, 5)])
    c.shape(star, R('#6a3a1e'), L_const(0.6))


@item('mojito_sans_alcool')
def mojito_sans_alcool(c):
    body, ly = glass(c, 8, 22, 5, 29, '#e8f8d0', level=0.8, taper=0.6, ice=True)
    for (x, y) in [(11, 20), (17, 23), (13, 26), (18, 17)]:
        leaf(c, x, y, x + 3, y - 2, 2.4, '#3ab04a', vein=False)
    for (x, y) in [(12, 23), (16, 19)]:
        c.shape(ellipse(x, y, 2, 1.2), R('#7ac03a'), L_const(0.7), edge=False)
    straw(c, 17, 10, 24, 0, '#3ab04a')
    mint(c, 11, 6, 2)
    lemon_wedge(c, 22, 6, '#6ab02a')


@item('cocktail_exotique')
def cocktail_exotique(c):
    body = polygon([(7, 8), (24, 8), (19, 21), (12, 21)]) | ellipse(15.5, 8, 8.5, 1.8)
    c.shape(body, R('#dcecf2'), L_horiz(7, 24, 0.9, 0.3))
    liq = polygon([(8.5, 9), (22.5, 9), (18.5, 20), (12.5, 20)]) & ~edge(body)
    c.shape(liq & (YY < 13), R('#f8c020'), L_const(0.7), edge=False)
    c.shape(liq & (YY >= 13) & (YY < 17), R('#f88020'), L_const(0.7), edge=False)
    c.shape(liq & (YY >= 17), R('#e0303a'), L_const(0.7), edge=False)
    c.shape(rect(15, 21, 16, 27) | ellipse(15.5, 28, 6, 1.6), R('#dcecf2'), L_const(0.6))
    m = polygon([(20, 7), (28, 3), (29, 9), (22, 10)])
    c.shape(m, R('#f4d040'), L_const(0.8))
    for (x1, y1) in [(27, 0), (31, 2)]:
        leaf(c, 28, 4, x1, y1, 2, '#3a8a3a', vein=False)
    um = polygon([(2, 6), (8, 1), (13, 5)])
    c.shape(um, R('#e83a8a'), L_const(0.7))
    c.fill(line_mask(8, 4, 11, 10), '#c8a070')
    straw(c, 14, 8, 18, 0, '#2a8ad0')
