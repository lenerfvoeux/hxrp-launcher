"""Les 37 préparations intermédiaires + la « préparation en cours »."""
from parts import *


def flour_sack(c, color, symbol):
    body = sack(c, '#efe8d8', color, x0=5, x1=26, y0=7, y1=29)
    c.shape(ellipse(15.5, 20, 6, 5), R('#f8f4ea'), L_const(0.8), edge=True)
    symbol(c)
    pile(c, 26, 27, 4.5, 2.5, color)


@item('farine')
def farine(c):
    def sym(c):
        stem(c, [(15, 24), (16, 16)], '#c8a040')
        for k in range(3):
            c.fill(rect(14, 17 + k * 2, 14, 17 + k * 2), '#d8a830')
            c.fill(rect(17, 17 + k * 2, 17, 17 + k * 2), '#d8a830')
    flour_sack(c, '#fbfaf4', sym)
    pal('farine', 'Rond', '#fbfaf4', '#ffffff', '#efe8d8', 0)


@item('farine_de_mais')
def farine_de_mais(c):
    def sym(c):
        m = ellipse(15.5, 20, 2.2, 3.5)
        c.shape(m, R('#f2c224'), L_const(0.7))
        leaf(c, 14, 24, 11, 18, 2.5, '#7aa83a', vein=False)
    flour_sack(c, '#f4d870', sym)
    pal('farine_de_mais', 'Rond', '#f4d870', '#f8e8a0', '#efe8d8', 0)


@item('semoule')
def semoule(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#3a6ab0')
    top = ellipse(16, 14, 11, 4.3) | ellipse(16, 12.5, 8, 3.5)
    c.shape(top, R('#f0d070'), L_sphere(14, 11, 11, 5, 0.2), edge=False)
    c.speckle(top, '#d8b050', 0.25, seed=4)
    bowl_rim(c, 16, 15, 13, 5, '#3a6ab0')
    pal('semoule', 'Rond', '#f0d070', '#f8e8a0', '#d8b050', 0)


@item('pate_a_pain')
def pate_a_pain(c):
    board(c, 2, 17, 29, 29, '#c89a5a')
    m = ellipse(16, 17, 11, 8)
    c.shape(m, R('#f0dcb0'), L_sphere(13, 13, 11, 8, 0.3), hl=3)
    c.fill(arc_band(16, 17, 6, 4, 1, 200, 330) & m, '#d8c090')
    c.speckle(ellipse(16, 25, 12, 3) & ~m, '#fbfaf4', 0.3, seed=2)
    pal('pate_a_pain', 'Rond', '#f0dcb0', '#f8ecd0', '#d8c090', 7)


@item('pate_a_pizza')
def pate_a_pizza(c):
    m = ellipse(16, 17, 14, 10)
    c.shape(m, R('#e8c890'), L_sphere(13, 14, 14, 10, 0.6))
    c.shape(ellipse(16, 17, 11.5, 7.8), R('#f4e0b0'), L_sphere(14, 15, 12, 8, 0.8), edge=False)
    for (x, y) in [(12, 15), (19, 18), (15, 21), (21, 14)]:
        c.fill(rect(x, y, x + 1, y), '#dcc088')
    c.speckle(m, '#fbfaf4', 0.08, seed=3)
    pal('pate_a_pizza', 'Rond', '#e8c890', '#f4e0b0', '#dcc088', 0)


@item('pate_brisee')
def pate_brisee(c):
    c.shape(ellipse(16, 19, 14, 9.5), R('#9a9aa0'), L_const(0.5))
    m = ellipse(16, 18, 13, 8.5)
    c.shape(m, R('#f0d8a0'), L_sphere(13, 15, 13, 9, 0.6))
    c.shape(ellipse(16, 18, 10.5, 6.5), R('#f4e4b8'), L_const(0.7), edge=False)
    for a in range(0, 360, 18):
        x = 16 + math.cos(math.radians(a)) * 12
        y = 18 + math.sin(math.radians(a)) * 7.8
        c.fill(rect(int(x), int(y), int(x), int(y)), '#d8b878')
    for (x, y) in [(12, 17), (16, 19), (20, 17), (14, 21), (18, 15)]:
        c.fill(rect(x, y, x, y), '#c8a870')
    pal('pate_brisee', 'Rond', '#f0d8a0', '#f4e4b8', '#d8b878', 0)


@item('pate_feuilletee')
def pate_feuilletee(c):
    top = polygon([(3, 13), (19, 7), (29, 12), (13, 19)])
    front = polygon([(3, 13), (13, 19), (13, 27), (3, 21)])
    side = polygon([(13, 19), (29, 12), (29, 20), (13, 27)])
    c.shape(front, R('#f0dcaa'), L_const(0.6))
    c.shape(side, R('#e0c890'), L_const(0.45))
    for k in range(1, 4):
        c.fill(line_mask(3, 13 + k * 2, 13, 19 + k * 2), '#f8ecc8')
        c.fill(line_mask(13, 19 + k * 2, 29, 12 + k * 2), '#f0dcaa')
    c.shape(top, R('#f8e8c0'), L_const(0.85))
    c.speckle(top, '#fbfaf4', 0.1, seed=4)
    pal('pate_feuilletee', 'Pavé', '#f0dcaa', '#f8ecc8', '#e0c890', 2)


@item('pate_a_crepe')
def pate_a_crepe(c):
    op = bowl(c, 15, 15, 13, 5, 10, '#f2eee6')
    fill_bowl(c, op, '#f4e0a0', 0.8, 15, 15, 13, 5)
    c.fill(arc_band(15, 15, 6, 2, 1, 180, 360), '#fbf0c8')
    stem(c, [(25, 1), (20, 13)], '#c0c4cc', width=2)
    c.shape(ellipse(20, 14, 3, 1.6), R('#c0c4cc'), L_const(0.6))
    bowl_rim(c, 15, 15, 13, 5, '#f2eee6')
    pal('pate_a_crepe', 'Rond', '#f4e0a0', '#fbf0c8', '#f2eee6', 0)


@item('pates_fraiches')
def pates_fraiches(c):
    for k in range(9):
        a0 = k * 40
        c.shape(arc_band(16, 17, 12 - (k % 3) * 2.5, 9 - (k % 3) * 2, 2.2, a0, a0 + 200), R('#f0d070'), L_const(0.55 + (k % 3) * 0.1))
    c.shape(ellipse(16, 17, 4, 3), R('#e0b850'), L_const(0.3))
    c.speckle(ellipse(16, 17, 13, 10), '#fbfaf4', 0.03, seed=6)
    pal('pates_fraiches', 'Long', '#f0d070', '#f8e8a0', '#e0b850', 0)


@item('tortilla')
def tortilla(c):
    for k, y in enumerate((22, 19, 16)):
        m = ellipse(16, y, 13, 7)
        c.shape(m, R('#f0d898'), L_sphere(13, y - 3, 13, 7, 0.7))
        for (x, yy) in rng_pts(k + 3, m & ~edge(m) & (YY < y + 2), 7):
            c.fill(rect(x, yy, x + 1, yy), '#b8803a')
    pal('tortilla', 'Rond', '#f0d898', '#f8e8c0', '#b8803a', 0)


@item('pain')
def pain(c):
    m = capsule(4, 24, 27, 8, 5.5, 5)
    c.shape(m, R('#d8903a'), L_cyl(4, 24, 27, 8, 5.5), hl=3)
    for k in range(4):
        x, y = 8 + k * 5, 21 - k * 3.6
        cut = line_mask(x - 1.5, y + 1.5, x + 3, y - 3.5)
        c.fill(cut & m & ~edge(m), '#f4dca0')
        c.fill(shift(cut, 1, 1) & m & ~edge(m), '#a8601a')
    pal('pain', 'Long', '#d8903a', '#f8ecc8', '#a8601a', 7)


@item('pain_burger')
def pain_burger(c):
    bun_bottom(c, 16, 22, 12, '#d89040')
    c.shape(rect(5, 21, 27, 21), R('#f8e8c0'), L_const(0.8), edge=False)
    bun_top(c, 16, 20, 12, 10, '#d88a38')
    pal('pain_burger', 'Rond', '#d88a38', '#f8e8c0', '#f8ecc8', 7)


@item('pain_de_mie')
def pain_de_mie(c):
    loaf = rrect(4, 8, 22, 27, 3) | ellipse(13, 9, 9, 4)
    c.shape(loaf, R('#c8803a'), L_horiz(4, 22, 0.9, 0.4), hl=2)
    s = rrect(15, 12, 29, 29, 3) | ellipse(22, 13, 7, 3.5)
    c.shape(s, R('#c8803a'), L_const(0.6))
    inner = rrect(16, 13, 28, 28, 2) | ellipse(22, 14, 5.8, 2.8)
    c.shape(inner, R('#f8ecc8'), L_const(0.8), edge=False)
    c.speckle(inner, '#e8d4a0', 0.15, seed=4)
    pal('pain_de_mie', 'Pavé', '#c8803a', '#f8ecc8', '#e8d4a0', 7)


@item('chapelure')
def chapelure(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#8a5a3a')
    top = ellipse(16, 14, 11, 4.3) | ellipse(16, 12.5, 8, 3.5)
    c.shape(top, R('#e0a850'), L_sphere(14, 11, 11, 5, 0.2), edge=False)
    grains(c, top, '#b8782a', 20, seed=2, size=(1, 1))
    grains(c, top, '#f4d890', 12, seed=5, size=(1, 1))
    bowl_rim(c, 16, 15, 13, 5, '#8a5a3a')
    pal('chapelure', 'Rond', '#e0a850', '#f4d890', '#b8782a', 0)


@item('beurre')
def beurre(c):
    c.shape(polygon([(1, 22), (14, 27), (31, 20), (18, 16)]), R('#e8e4dc'), L_const(0.8))
    top = polygon([(4, 16), (17, 11), (28, 15), (15, 20)])
    front = polygon([(4, 16), (15, 20), (15, 25), (4, 21)])
    side = polygon([(15, 20), (28, 15), (28, 20), (15, 25)])
    c.shape(front, R('#f4d860'), L_const(0.6))
    c.shape(side, R('#e8c848'), L_const(0.45))
    c.shape(top, R('#f8e88a'), L_const(0.85), hl=2)
    c.fill(line_mask(8, 14.5, 18, 11), '#fbf4c0')
    pal('beurre', 'Pavé', '#f4d860', '#f8e88a', '#e8c848', 0)


@item('creme')
def creme(c):
    body = polygon([(8, 11), (24, 11), (22, 28), (10, 28)])
    c.shape(body, R('#f4f0e8'), L_horiz(8, 24, 0.95, 0.35))
    c.shape(rect(9, 17, 23, 22) & body, R('#4a8ad0'), L_const(0.6))
    c.fill(points([(12, 19), (13, 20), (14, 19), (18, 19), (19, 20), (20, 19)]), '#fbfbf8')
    top = ellipse(16, 11, 8, 2.8)
    c.shape(top, R('#fbfaf2'), L_const(0.85), edge=False)
    c.shape(ellipse(15, 9.5, 4, 2) | ellipse(17, 8.5, 2, 1.5), R('#ffffff'), L_const(0.9), edge=True)
    c.fill(line_mask(21, 3, 18, 10, 2), '#c0c4cc')
    pal('creme', 'Rond', '#f4f0e8', '#fbfaf2', '#4a8ad0', 0)


@item('fromage')
def fromage(c):
    top = polygon([(3, 15), (22, 7), (29, 13), (10, 20)])
    front = polygon([(3, 15), (10, 20), (10, 28), (3, 23)])
    side = polygon([(10, 20), (29, 13), (29, 21), (10, 28)])
    c.shape(front, R('#f0c030'), L_const(0.6))
    c.shape(side, R('#e8b028'), L_const(0.45))
    c.shape(top, R('#f8d860'), L_const(0.85))
    for (x, y, r) in [(14, 22, 1.6), (20, 19, 2), (25, 17, 1.4), (6, 20, 1.3), (22, 24, 1.2)]:
        c.shape(circle(x, y, r), R('#c89018'), L_const(0.3), edge=False)
    for (x, y, r) in [(15, 12, 1.5), (21, 11, 1.3)]:
        c.shape(ellipse(x, y, r * 1.4, r * 0.8), R('#e0b030'), L_const(0.3), edge=False)
    pal('fromage', 'Pavé', '#f0c030', '#f8d860', '#c89018', 7)


@item('fromage_de_chevre')
def fromage_de_chevre(c):
    m = capsule(5, 22, 22, 12, 5.5)
    c.shape(m, R('#f4f2ea'), L_cyl(5, 22, 22, 12, 5.5), hl=2)
    c.speckle(m, '#b8b8b0', 0.15, seed=2)
    s = ellipse(24, 23, 5, 5.5)
    c.shape(s, R('#d8d8d0'), L_const(0.6))
    c.shape(ellipse(24, 23, 4, 4.5), R('#fbfaf4'), L_const(0.85), edge=False)
    pal('fromage_de_chevre', 'Long', '#f4f2ea', '#fbfaf4', '#b8b8b0', 0)


@item('yaourt')
def yaourt(c):
    body = polygon([(8, 11), (24, 11), (22, 28), (10, 28)])
    c.shape(body, R('#fbfaf6'), L_horiz(8, 24, 0.95, 0.4))
    c.shape(rect(9, 16, 23, 22) & body, R('#f0c0d0'), L_const(0.7))
    c.shape(circle(16, 19, 2.4), R('#e03a4a'), L_const(0.6))
    top = ellipse(16, 11, 8, 2.8)
    c.shape(top, R('#fbfbf8'), L_const(0.85), edge=False)
    lid = polygon([(16, 9), (26, 3), (29, 7), (19, 11)])
    c.shape(lid, R('#d0d4dc'), L_const(0.8), hl=1)
    pal('yaourt', 'Rond', '#fbfaf6', '#ffffff', '#f0c0d0', 0)


@item('lait_de_coco')
def lait_de_coco(c):
    body = rect(7, 8, 24, 28) | ellipse(15.5, 28, 8.5, 2) | ellipse(15.5, 8, 8.5, 2)
    c.shape(body, R('#c0c4cc'), L_horiz(7, 24, 0.95, 0.3), hl=2)
    lab = rect(7, 12, 24, 24)
    c.shape(lab, R('#3a8a4a'), L_horiz(7, 24, 0.9, 0.4), edge=False)
    c.shape(circle(13, 18, 3.5), R('#6a4428'), L_const(0.6))
    c.shape(ellipse(13, 17.5, 2.6, 2.2), R('#fbfaf4'), L_const(0.8), edge=False)
    c.shape(ellipse(15.5, 8, 7.5, 1.5), R('#d8dce4'), L_const(0.8), edge=False)
    pal('lait_de_coco', 'Rond', '#fbfaf4', '#ffffff', '#6a4428', 0)


@item('bouillon')
def bouillon(c):
    op = cocotte(c, '#b8bcc4', content='#e0a840', top=13)
    c.fill(arc_band(16, 13, 6, 2, 1, 180, 360) & op, '#f0c870')
    for (x, y, cc) in [(11, 13, '#f07a1a'), (20, 12, '#3a9a2a'), (16, 14, '#f4ecd0')]:
        c.fill(rect(x, y, x + 1, y), cc)
    for k in range(3):
        c.fill(line_mask(10 + k * 5, 8, 11 + k * 5, 4), '#f0f4f8')
    pal('bouillon', 'Rond', '#e0a840', '#f0c870', '#b8bcc4', 0)


@item('fumet_de_poisson')
def fumet_de_poisson(c):
    op = cocotte(c, '#b8bcc4', content='#e8dcb0', top=13)
    c.fill(line_mask(9, 13, 22, 12), '#f8f4e8')
    for k in range(4):
        c.fill(line_mask(11 + k * 3, 11, 12 + k * 3, 14), '#f8f4e8')
    c.fill(points([(22, 11), (23, 12), (23, 13)]), '#f8f4e8')
    for k in range(3):
        c.fill(line_mask(10 + k * 5, 8, 11 + k * 5, 4), '#f0f4f8')
    pal('fumet_de_poisson', 'Rond', '#e8dcb0', '#f8f4e8', '#b8bcc4', 0)


@item('riz_cuit')
def riz_cuit(c):
    op = bowl(c, 16, 16, 13, 5, 9, '#2a2a3a')
    rice_mound(c, 16, 13, 10, 5)
    bowl_rim(c, 16, 16, 13, 5, '#2a2a3a')
    c.fill(line_mask(22, 2, 26, 14), '#c8a070')
    c.fill(line_mask(24, 2, 27, 14), '#b89060')
    pal('riz_cuit', 'Rond', '#f6f3ea', '#ffffff', '#2a2a3a', 0)


@item('tofu')
def tofu(c):
    for (x, y) in [(3, 15), (14, 19), (13, 9)]:
        top = polygon([(x, y + 3), (x + 7, y), (x + 14, y + 3), (x + 7, y + 6)])
        front = polygon([(x, y + 3), (x + 7, y + 6), (x + 7, y + 12), (x, y + 9)])
        side = polygon([(x + 7, y + 6), (x + 14, y + 3), (x + 14, y + 9), (x + 7, y + 12)])
        c.shape(front, R('#f4f0e0'), L_const(0.6))
        c.shape(side, R('#e4e0cc'), L_const(0.45))
        c.shape(top, R('#fbfaf0'), L_const(0.9))
    pal('tofu', 'Pavé', '#f4f0e0', '#fbfaf0', '#e4e0cc', 0)


@item('caramel')
def caramel(c):
    jar(c, 7, 24, 9, 29, content='#c8701a', lid='#6a3a1a', level=0.85, glass_col='#f0d8a8')
    c.shape(rect(6, 9, 25, 11) | rrect(8, 11, 12, 17, 1) | rrect(18, 11, 21, 15, 1), R('#d88a2a'), L_const(0.7), hl=1)
    pal('caramel', 'Rond', '#c8701a', '#e89a3a', '#6a3a1a', 0)


@item('chocolat')
def chocolat(c):
    bar = polygon([(3, 15), (19, 7), (29, 13), (13, 22)])
    c.shape(polygon([(3, 15), (13, 22), (13, 25), (3, 18)]) | polygon([(13, 22), (29, 13), (29, 16), (13, 25)]), R('#3a1a0e'), L_const(0.4))
    c.shape(bar, R('#5a2a14'), L_const(0.6))
    for k in range(1, 4):
        c.fill(line_mask(3 + k * 4, 15 - k * 2, 13 + k * 4, 22 - k * 2), '#3a1a0e')
    for k in range(1, 3):
        c.fill(line_mask(3 + k * 3.3, 15 + k * 2.3, 19 + k * 3.3, 7 + k * 2), '#3a1a0e')
    c.fill(line_mask(4, 15, 18, 8), '#7a4a2a')
    wrap = polygon([(17, 17), (29, 11), (31, 17), (19, 25)])
    c.shape(wrap, R('#c8302a'), L_const(0.6))
    c.shape(polygon([(17, 17), (29, 11), (29, 12.5), (17, 18.5)]), R('#e8d070'), L_const(0.8))
    pal('chocolat', 'Pavé', '#5a2a14', '#7a4a2a', '#3a1a0e', 0)


@item('creme_patissiere')
def creme_patissiere(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#f2eee6')
    fill_bowl(c, op, '#f4d050', 0.9, 16, 15, 13, 5)
    c.shape(ellipse(15, 13, 6, 2.2), R('#f8e088'), L_const(0.8), edge=False)
    c.shape(capsule(7, 13, 14, 11, 0.8), R('#3a2418'), L_const(0.4), edge=False)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')
    pal('creme_patissiere', 'Rond', '#f4d050', '#f8e088', '#3a2418', 0)


@item('chantilly')
def chantilly(c):
    op = bowl(c, 16, 18, 13, 5, 8, '#f2eee6')
    for (y, rx) in [(15, 10), (11, 7), (7.5, 4)]:
        m = ellipse(16, y, rx, 3.2)
        c.shape(m, R('#fbfaf6'), L_sphere(14, y - 1, rx, 3.4), hl=2)
        c.fill(arc_band(16, y, rx - 1, 2, 1, 20, 160), '#e8e4dc')
    c.shape(polygon([(15, 5), (16, 1), (18, 5)]), R('#fbfaf6'), L_const(0.8))
    bowl_rim(c, 16, 18, 13, 5, '#f2eee6')
    pal('chantilly', 'Rond', '#fbfaf6', '#ffffff', '#e8e4dc', 0)


@item('sauce_tomate')
def sauce_tomate(c):
    jar(c, 7, 24, 9, 29, content='#c82a1a', lid='#3a8a3a', label='#f4ecd0', level=0.9)
    c.fill(points([(12, 20), (13, 19), (14, 20), (15, 21), (16, 20), (17, 19), (18, 20), (19, 21)]), '#c82a1a')
    leaf(c, 20, 8, 27, 2, 4, '#3aa02a')
    pal('sauce_tomate', 'Rond', '#c82a1a', '#e04a2a', '#3aa02a', 0)


@item('mayonnaise')
def mayonnaise(c):
    jar(c, 7, 24, 9, 29, content='#f8e8a8', lid='#2a5ab0', label='#f0f0f0', level=0.9)
    c.fill(points([(12, 20), (13, 21), (14, 20), (18, 20), (19, 21), (20, 20)]), '#e8b020')
    pal('mayonnaise', 'Rond', '#f8e8a8', '#fbf4d0', '#2a5ab0', 0)


@item('ketchup')
def ketchup(c):
    body = rrect(8, 10, 23, 29, 4)
    c.shape(body, R('#d01a1a'), L_horiz(8, 23, 0.95, 0.3), hl=3)
    c.shape(rect(9, 16, 22, 22), R('#f4ecd0'), L_const(0.7))
    c.shape(ellipse(15.5, 19, 3, 2.5), R('#d01a1a'), L_const(0.6))
    leaf(c, 16, 17, 19, 15, 2, '#3a9a2a', vein=False)
    cap = rrect(10, 5, 21, 10, 1) | polygon([(13, 5), (15, 1), (16, 1), (18, 5)])
    c.shape(cap, R('#f4f0e8'), L_vert(1, 10, 0.9, 0.5))
    pal('ketchup', 'Rond', '#d01a1a', '#e03a2a', '#f4ecd0', 0)


@item('moutarde')
def moutarde(c):
    body = polygon([(7, 12), (25, 12), (23, 29), (9, 29)])
    c.shape(body, R('#e8e0d0'), L_horiz(7, 25, 0.95, 0.35))
    c.shape(rect(8, 17, 24, 23) & body, R('#2a3a6a'), L_const(0.6))
    c.fill(points([(12, 20), (13, 19), (14, 20), (18, 20), (19, 19), (20, 20)]), '#e8c030')
    top = ellipse(16, 12, 9, 3)
    c.shape(top, R('#d8b020'), L_const(0.7), edge=False)
    c.shape(ellipse(15, 10.5, 5, 2.2), R('#e8c030'), L_sphere(14, 9, 5, 2.5), edge=True)
    stem(c, [(27, 2), (20, 11)], '#b88a50', width=2)
    pal('moutarde', 'Rond', '#d8b020', '#e8c030', '#2a3a6a', 0)


@item('vinaigrette')
def vinaigrette(c):
    body = ellipse(16, 22, 9, 8) | rect(13, 6, 18, 16)
    c.shape(body, R('#dcecf2'), L_horiz(7, 25, 0.9, 0.3))
    inner = ellipse(16, 22, 7.8, 6.8) & (YY > 16)
    c.shape(inner & (YY <= 21), R('#e0c030'), L_const(0.7), edge=False)
    c.shape(inner & (YY > 21), R('#8a2a2a'), L_const(0.5), edge=False)
    c.fill(points([(12, 19), (18, 20), (15, 25), (19, 24)]), '#3a8a2a')
    c.fill(line_mask(10, 18, 10, 26) & inner, '#ffffff')
    c.shape(rect(12, 3, 19, 6), R('#8a6a3a'), L_const(0.6))
    pal('vinaigrette', 'Rond', '#e0c030', '#8a2a2a', '#3a8a2a', 0)


@item('bechamel')
def bechamel(c):
    c.shape(capsule(24, 17, 31, 13, 1.6), R('#3a3a3a'), L_const(0.5))
    op = cocotte(c, '#b8bcc4', content='#f8f4e4', x0=3, x1=24, top=14)
    c.fill(arc_band(13.5, 14, 5, 1.6, 1, 180, 360), '#fbfaf2')
    c.speckle(op, '#8a7a5a', 0.05, seed=3)
    stem(c, [(20, 2), (14, 13)], '#c0c4cc', width=2)
    c.shape(ellipse(14, 12, 2.5, 3), R('#c0c4cc'), L_const(0.6))
    pal('bechamel', 'Rond', '#f8f4e4', '#fbfaf2', '#b8bcc4', 0)


@item('pesto')
def pesto(c):
    jar(c, 7, 24, 9, 29, content='#4a8a22', lid='#c8a040', label='#f4ecd0', level=0.9)
    c.speckle(rect(9, 14, 22, 18), '#8ab83a', 0.3, seed=2)
    leaf(c, 12, 20, 20, 21, 3, '#3aa02a', vein=False)
    pal('pesto', 'Rond', '#4a8a22', '#6aa83a', '#c8a040', 0)


@item('sauce_barbecue')
def sauce_barbecue(c):
    bottle(c, 16, 2, 30, 12, 5, 9, '#5a1e10', liquid='#6a2210', cap='#2a2a2a', label='#e8a030', cap_h=3)
    c.fill(points([(13, 16), (14, 15), (15, 16), (17, 16), (18, 15), (19, 16)]), '#c8301a')
    c.shape(polygon([(14, 19), (16, 15), (18, 19)]), R('#e8501a'), L_const(0.7), edge=False)
    pal('sauce_barbecue', 'Rond', '#5a1e10', '#8a3018', '#e8a030', 0)


@item('sauce_curry')
def sauce_curry(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#3a3a4a')
    m = fill_bowl(c, op, '#e8a020', 0.9, 16, 15, 13, 5)
    c.fill(arc_band(16, 15, 6, 2, 1, 180, 360) & m, '#f4c050')
    c.speckle(m, '#c86a10', 0.12, seed=5)
    leaf(c, 18, 13, 23, 11, 3, '#3a9a2a', vein=False)
    bowl_rim(c, 16, 15, 13, 5, '#3a3a4a')
    pal('sauce_curry', 'Rond', '#e8a020', '#f4c050', '#c86a10', 0)


@item('preparation_en_cours')
def preparation_en_cours(c):
    op = bowl(c, 14, 16, 12, 4.5, 9, '#c85a2a')
    m = fill_bowl(c, op, '#f0d090', 0.8, 14, 16, 12, 4.5)
    for (x, y, cc) in [(9, 15, '#e0321f'), (13, 16, '#3a9a2a'), (17, 15, '#f07a1a'), (11, 17, '#f4d860')]:
        c.fill(rect(x, y, x + 1, y), cc)
    stem(c, [(22, 2), (16, 15)], '#b98a52', width=2)
    bowl_rim(c, 14, 16, 12, 4.5, '#c85a2a')
    note = polygon([(21, 18), (30, 16), (31, 27), (22, 29)])
    c.shape(note, R('#f8f0d8'), L_const(0.8))
    for k in range(3):
        c.fill(line_mask(23, 20 + k * 2.5, 29, 19 + k * 2.5), '#8a7a6a')
    c.shape(circle(26, 17, 1.2), R('#c83a2a'), L_const(0.6), edge=False)
