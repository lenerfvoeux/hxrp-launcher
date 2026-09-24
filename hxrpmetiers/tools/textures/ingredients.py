"""Les 104 ingrédients bruts et les 20 épices/condiments."""
from parts import *


# ============================================================================ céréales
@item('ble')
def ble(c):
    for k, (bx, by, tx, ty) in enumerate([(12, 30, 9, 7), (16, 30, 17, 5), (20, 30, 24, 8)]):
        stem(c, [(bx, by), (tx, ty + 8)], '#c8a040')
        for j in range(6):
            t = j / 6
            x = tx + (bx - tx) * t * 0.35
            y = ty + j * 1.6
            for s in (-1, 1):
                m = ellipse(x + s * 1.3, y, 1.3, 1.1)
                c.shape(m, R('#e8b848'), L_sphere(x + s, y - 1, 1.5, 1.3), edge=True)
        c.fill(line_mask(tx, ty, tx - 2, ty - 5), '#d8b060')
        c.fill(line_mask(tx + 1, ty, tx + 3, ty - 5), '#d8b060')
    c.shape(rect(10, 23, 22, 24), R('#b8402a'), L_const(0.6))
    pal('ble', 'Long', '#e8b848', '#f4e2a8', '#c8a040', 0)


@item('riz')
def riz(c):
    sack(c, '#cdb68a', '#f6f3ea', grains_col='#fbfaf4', n=30)
    c.shape(rect(9, 19, 22, 24), R('#b8402a'), L_const(0.6))
    for (x, y) in [(3, 28), (27, 27), (25, 29)]:
        c.fill(rect(x, y, x + 1, y), '#f6f3ea')
    pal('riz', 'Rond', '#f6f3ea', '#ffffff', '#cdb68a', 0)


@item('mais')
def mais(c):
    cob = capsule(9, 25, 22, 7, 4.6, 3.2)
    c.shape(cob, R('#f2c224'), L_cyl(9, 25, 22, 7, 4.6), hl=2)
    inner = cob & ~edge(cob)
    for (x, y) in np.argwhere(inner)[:, ::-1]:
        if (x + y) % 2 == 0:
            c.fill(rect(x, y, x, y), '#f8dc5a')
        if (x * 3 + y) % 5 == 0:
            c.fill(rect(x, y, x, y), '#c8901a')
    leaf(c, 11, 28, 4, 11, 5, '#8ab84a')
    leaf(c, 12, 29, 20, 20, 5, '#7aa83a')
    leaf(c, 10, 29, 3, 22, 4, '#9ac85a')
    stem(c, [(22, 7), (25, 3)], '#d8c890')
    pal('mais', 'Long', '#f2c224', '#f8dc5a', '#8ab84a', 3)


@item('avoine')
def avoine(c):
    stem(c, [(8, 30), (6, 3)], '#c8b070')
    for j in range(4):
        y = 6 + j * 4.5
        x = 6 + j * 0.5
        for s_ in (-1, 1):
            ex, ey = x + s_ * 3, y + 3
            c.fill(line_mask(x, y, ex, ey - 1), '#b8a060')
            c.shape(ellipse(ex, ey, 1.3, 2.3), R('#ecdca8'), L_sphere(ex - 0.5, ey - 1, 1.3, 2.3), edge=True)
    rs = np.random.RandomState(4)
    for k in range(13):
        x = 13 + rs.randint(0, 14)
        y = 17 + rs.randint(0, 10) - max(0, 3 - abs(x - 20)) * 1.2
        m = ellipse(x, y, 2.6, 1.8)
        c.shape(m, R('#e8d4a0'), L_sphere(x - 1, y - 1, 2.6, 1.8, 0.5), edge=True)
        c.fill(rect(int(x) - 1, int(y) - 1, int(x), int(y) - 1), '#f8ecc8')
    pal('avoine', 'Rond', '#e6d49a', '#f4ead0', '#c8b070', 0)

@item('sarrasin')
def sarrasin(c):
    sp = spoon(c, 27, 6, 13, 20, '#b98a52', bowl_r=7, content='#6a4a2c')
    for (x, y) in rng_pts(11, sp & ~edge(sp), 12):
        m = polygon([(x, y + 2), (x + 1, y), (x + 2, y + 2)])
        c.fill(m, '#4a3018')
        c.fill(rect(x + 1, y + 1, x + 1, y + 1), '#9a7048')
    for (x, y) in [(5, 27), (9, 29), (20, 28)]:
        c.shape(polygon([(x, y + 2.5), (x + 1.5, y), (x + 3, y + 2.5)]), R('#6a4a2c'), L_const(0.6))
    pal('sarrasin', 'Rond', '#6a4a2c', '#c8b08a', '#4a3018', 0)


@item('quinoa')
def quinoa(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#c89a5a')
    m = ellipse(16, 14, 11, 4.5)
    c.shape(m & op | ellipse(16, 13, 9, 3.5), R('#e8d8a8'), L_sphere(14, 11, 11, 5), edge=False)
    grains(c, ellipse(16, 13.5, 10, 4), '#f4e8c0', 30, seed=2, size=(1, 1))
    grains(c, ellipse(16, 13.5, 10, 4), '#b84a30', 10, seed=4, size=(1, 1))
    grains(c, ellipse(16, 13.5, 10, 4), '#4a3028', 6, seed=6, size=(1, 1))
    bowl_rim(c, 16, 15, 13, 5, '#c89a5a')
    pal('quinoa', 'Rond', '#e8d8a8', '#f4e8c0', '#b84a30', 0)


# ============================================================================ légumes
@item('carotte')
def carotte(c):
    m = capsule(23, 10, 6, 27, 4.8, 1.0)
    c.shape(m, R('#f07a1a'), L_cyl(23, 10, 6, 27, 4.8), hl=2)
    for k in range(5):
        t = 0.15 + k * 0.17
        x, y = 23 - 17 * t, 10 + 17 * t
        w = 3.5 * (1 - t) + 1
        c.fill(line_mask(x - w * 0.4, y - w * 0.4, x + w * 0.2, y + w * 0.2), '#c8540e', clip=m & ~edge(m))
    for (dx, dy, w) in [(6, -8, 3.2), (1, -9, 3.2), (8, -3, 3)]:
        leaf(c, 24, 9, 24 + dx, 9 + dy, w, '#48a82a')
    c.shape(ellipse(24.5, 9.5, 2.5, 2), R('#3a8a22'), L_const(0.5))
    pal('carotte', 'Long', '#f07a1a', '#f8a040', '#48a82a', 1)


@item('pomme_de_terre')
def pomme_de_terre(c):
    m = ellipse(16, 18, 12, 9) | ellipse(12, 16, 8, 8)
    c.shape(m, R('#c8965a'), L_sphere(13, 15, 13, 10, 0.2), hl=2)
    c.speckle(m, '#9a6a38', 0.08, seed=2)
    for (x, y) in [(10, 13), (20, 19), (15, 22), (22, 13)]:
        c.fill(rect(x, y, x + 1, y), '#6a4020')
        c.fill(rect(x, y - 1, x + 1, y - 1), '#e0b880')
    pal('pomme_de_terre', 'Rond', '#c8965a', '#f4e4a8', '#9a6a38', 0)


@item('oignon')
def oignon(c):
    m = ellipse(16, 19, 11, 10) | polygon([(12, 11), (16, 3), (20, 11)])
    c.shape(m, R('#c8782c'), L_sphere(13, 15, 12, 11, 0.2), hl=2)
    for x in (10, 14, 18, 22):
        c.fill(arc_band(16, 19, abs(x - 16) + 1.5, 9.5, 1, 200, 340) & m & ~edge(m), '#9a5418')
    for k in range(4):
        c.fill(line_mask(14 + k, 29, 13 + k * 2, 31), '#e8d0a0')
    c.shape(polygon([(15, 5), (16, 1), (17, 5)]), R('#8a6a3a'), L_const(0.5))
    pal('oignon', 'Rond', '#c8782c', '#f4ecd8', '#e8dcc0', 2)


@item('echalote')
def echalote(c):
    for (cx, cy, s) in [(12, 18, 1.0), (20, 19, 0.9)]:
        m = ellipse(cx, cy, 6 * s, 9 * s) | polygon([(cx - 2, cy - 7 * s), (cx, cy - 13 * s), (cx + 2, cy - 7 * s)])
        c.shape(m, R('#c06a4a'), L_sphere(cx - 2, cy - 3, 7, 10, 0.2), hl=1)
        c.fill(line_mask(cx, cy - 8 * s, cx, cy + 8 * s) & m & ~edge(m), '#8a4030')
        c.fill(line_mask(cx - 2, cy + 8.5 * s, cx + 2, cy + 8.5 * s), '#e8d0a0')
    pal('echalote', 'Rond', '#c06a4a', '#e8c8d8', '#f4e4ec', 2)


@item('ail')
def ail(c):
    m = ellipse(16, 20, 11, 9) | polygon([(13, 13), (16, 4), (19, 13)])
    c.shape(m, R('#f0e8e0'), L_sphere(13, 16, 12, 10, 0.3), hl=2)
    for x in (10, 13, 19, 22):
        c.fill(arc_band(16, 20, abs(x - 16) + 0.5, 8.5, 1, 190, 350) & m & ~edge(m), '#c8b8b0')
    c.fill(line_mask(16, 8, 16, 27) & m & ~edge(m), '#d0c0c0')
    c.blend(ellipse(16, 25, 10, 4) & m, '#a878a0', 0.35)
    for k in range(3):
        c.fill(line_mask(14 + k * 2, 29, 13 + k * 3, 31), '#e0d0b0')
    pal('ail', 'Rond', '#f0e8e0', '#fbf6e8', '#c8b8b0', 0)


@item('tomate')
def tomate(c):
    body = ellipse(16, 18, 12, 10.5)
    c.shape(body, R('#e0321f'), L_sphere(14, 15, 12, 10.5), hl=4)
    c.fill(arc_band(16, 18, 6, 9.5, 1, 200, 250) & body & ~edge(body), '#b82414')
    star = polygon([(16, 6), (18.5, 10), (24, 9), (19.5, 12), (21.5, 15.5), (16, 12.5), (10.5, 15.5), (12.5, 12), (8, 9), (13.5, 10)])
    c.shape(star, R('#3f9a2a'), L_const(0.6))
    c.shape(rect(15, 3, 16, 7), R('#3f7a22'), L_const(0.5), edge=False)
    pal('tomate', 'Rond', '#e0321f', '#f05a40', '#f6e8a0', 3)


@item('poivron')
def poivron(c):
    for (cx, rx) in [(10, 6), (22, 6), (16, 7)]:
        m = ellipse(cx, 19, rx, 10)
        c.shape(m, R('#d8281c'), L_sphere(cx - 2, 15, rx, 10, 0.2), hl=2)
    c.shape(ellipse(16, 9.5, 5, 2.5), R('#2a7a1a'), L_const(0.6))
    c.shape(capsule(16, 9, 18, 3, 1.4, 1.1), R('#3f8a22'), L_cyl(16, 9, 18, 3, 1.4))
    pal('poivron', 'Rond', '#d8281c', '#e84a3a', '#f4f0e0', 3)


@item('courgette')
def courgette(c):
    m = capsule(5, 24, 26, 8, 4.3, 3.8)
    c.shape(m, R('#3a8a2a'), L_cyl(5, 24, 26, 8, 4.3), hl=2)
    c.speckle(m, '#8ac85a', 0.12, seed=4)
    for k in range(3):
        c.fill(line_mask(8 + k * 5, 19 - k * 4, 12 + k * 5, 16 - k * 4), '#6ab04a', clip=m & ~edge(m))
    c.shape(capsule(26, 8, 29, 5, 1.5, 1.2), R('#7a8a4a'), L_const(0.5))
    c.shape(ellipse(5, 24, 2, 2), R('#c8c070'), L_const(0.6), edge=False)
    pal('courgette', 'Long', '#3a8a2a', '#e8f0b0', '#6ab04a', 1)


@item('aubergine')
def aubergine(c):
    m = capsule(8, 23, 22, 12, 6.5, 4.2) | ellipse(9, 23, 7, 6.5)
    c.shape(m, R('#5a2a6a'), L_cyl(8, 23, 22, 12, 6.5), hl=4)
    cal = polygon([(19, 9), (24, 11), (27, 8), (26, 13), (23, 16), (20, 15), (17, 13)])
    c.shape(cal, R('#3f7a2a'), L_const(0.6))
    c.shape(capsule(25, 10, 28, 5, 1.4, 1), R('#5a7a3a'), L_const(0.5))
    pal('aubergine', 'Long', '#5a2a6a', '#f0ecc8', '#3f7a2a', 3)


@item('chou')
def chou(c):
    m = ellipse(16, 18, 13.5, 11.5)
    c.shape(m, R('#4a9a3a'), L_sphere(12, 14, 14, 12, 0.2), hl=1)
    for (cx, cy, rx, ry, cc) in [(9, 18, 7, 9, '#3a8a2e'), (23, 18, 7, 9, '#3a8a2e'), (16, 22, 9, 6, '#4a9a3a')]:
        leafm = ellipse(cx, cy, rx, ry) & m
        c.shape(leafm, R(cc), L_sphere(cx - 2, cy - 3, rx, ry, 0.3), edge=True)
        c.fill(line_mask(cx, cy + ry - 2, cx + (16 - cx) * 0.4, cy - ry + 3) & leafm & ~edge(leafm), '#a8d880')
    head = ellipse(16, 14, 7.5, 7)
    c.shape(head, R('#a8d870'), L_sphere(14, 11, 7.5, 7, 0.2), hl=2)
    c.fill(arc_band(18, 15, 5, 5.5, 1, 100, 250) & head & ~edge(head), '#7ab04a')
    c.fill(arc_band(13, 13, 4, 4, 1, 280, 60) & head & ~edge(head), '#c8f090')
    pal('chou', 'Rond', '#5aa83a', '#e8f0c0', '#a8d870', 2)

@item('chou_fleur')
def chou_fleur(c):
    for (x0, y0, x1, y1) in [(5, 26, 1, 14), (27, 26, 31, 14), (9, 29, 4, 22), (23, 29, 28, 22)]:
        leaf(c, x0, y0, x1, y1, 7, '#4a9a3a')
    for (x, y, r) in [(11, 16, 5), (21, 16, 5), (16, 12, 5.5), (16, 19, 6), (10, 21, 4), (22, 21, 4)]:
        m = circle(x, y, r)
        c.shape(m, R('#f4ecd8'), L_sphere(x - 1, y - 1.5, r, r), edge=True)
    pal('chou_fleur', 'Rond', '#f4ecd8', '#fbf6e8', '#4a9a3a', 0)


@item('brocoli')
def brocoli(c):
    c.shape(polygon([(13, 30), (19, 30), (18, 17), (14, 17)]), R('#a8c878'), L_horiz(13, 19))
    c.fill(line_mask(16, 22, 11, 17), '#a8c878')
    c.fill(line_mask(16, 22, 22, 17), '#a8c878')
    for (x, y, r) in [(10, 14, 5), (22, 14, 5), (16, 10, 6), (16, 16, 5), (7, 18, 3.5), (25, 18, 3.5)]:
        m = circle(x, y, r)
        c.shape(m, R('#2e7a2a'), L_sphere(x - 1, y - 1.5, r, r), edge=True)
        c.speckle(m, '#5ab04a', 0.3, seed=int(x * y))
    pal('brocoli', 'Rond', '#2e7a2a', '#a8c878', '#5ab04a', 8)


@item('laitue')
def laitue(c):
    for (x0, y0, x1, y1, w) in [(16, 28, 2, 14, 11), (16, 28, 30, 14, 11), (16, 28, 7, 6, 11), (16, 28, 25, 6, 11)]:
        m = leaf(c, x0, y0, x1, y1, w, '#7ac84a')
        c.fill(edge(m) & (YY < 26), '#a8e070')
    m = leaf(c, 16, 28, 16, 8, 12, '#a8e070')
    c.shape(ellipse(16, 22, 6, 6), R('#c8f090'), L_sphere(15, 20, 6, 6), edge=True)
    pal('laitue', 'Herbes', '#7ac84a', '#c8f090', '#4a9a2a', 8)


@item('poireau')
def poireau(c):
    m = capsule(5, 28, 17, 13, 3.3)
    c.shape(m, R('#f4f0dc'), L_cyl(5, 28, 17, 13, 3.3), hl=1)
    g = capsule(14, 17, 19, 10, 3.4)
    c.shape(g, R('#9ac85a'), L_cyl(14, 17, 19, 10, 3.4))
    for (x1, y1) in [(28, 2), (30, 9), (24, 1)]:
        leaf(c, 18, 11, x1, y1, 5, '#3a8a3a')
    for k in range(4):
        c.fill(line_mask(4, 29, 1 + k, 31), '#e0d8b0')
    pal('poireau', 'Long', '#f4f0dc', '#fbfaee', '#3a8a3a', 2)


@item('concombre')
def concombre(c):
    m = capsule(5, 25, 27, 8, 4.5)
    c.shape(m, R('#2a6a22'), L_cyl(5, 25, 27, 8, 4.5), hl=2)
    for k in range(4):
        c.fill(line_mask(6 + k * 5, 23 - k * 4, 11 + k * 5, 19 - k * 4), '#5a9a3a', clip=m & ~edge(m))
    c.speckle(m, '#8ac86a', 0.1, seed=9)
    pal('concombre', 'Long', '#2a6a22', '#d8f0b0', '#f4f8e0', 3)


@item('champignon')
def champignon(c):
    c.shape(rrect(12, 16, 20, 29, 2), R('#efe6d4'), L_horiz(12, 20))
    cap = ellipse(16, 15, 13, 9) & (YY <= 18)
    c.shape(cap, R('#c89a6a'), L_sphere(12, 10, 13, 9, 0.3), hl=3)
    c.fill(rect(5, 18, 27, 18) & ellipse(16, 15, 13, 9), '#8a6040')
    c.speckle(cap, '#e0c098', 0.06, seed=3)
    pal('champignon', 'Rond', '#c89a6a', '#f4ecdc', '#8a6040', 0)


@item('epinards')
def epinards(c):
    for (x0, y0, x1, y1, w, cc) in [(16, 30, 4, 10, 10, '#2a6a22'), (16, 30, 28, 10, 10, '#2e7426'), (16, 30, 16, 4, 11, '#3a8a2a'),
                                    (16, 30, 8, 20, 8, '#357f28')]:
        stem(c, [(x0, y0), ((x0 + x1) / 2, (y0 + y1) / 2 + 3)], '#6aa04a')
        leaf(c, (x0 + x1) / 2, (y0 + y1) / 2 + 3, x1, y1, w, cc)
    pal('epinards', 'Herbes', '#2e7426', '#4a9a3a', '#6aa04a', 8)


@item('haricot_vert')
def haricot_vert(c):
    for k, (x0, y0, x1, y1) in enumerate([(4, 24, 26, 10), (5, 27, 27, 14), (3, 20, 24, 6), (7, 29, 29, 19)]):
        m = capsule(x0, y0, x1, y1, 1.6, 1.2)
        c.shape(m, R('#4aa032'), L_cyl(x0, y0, x1, y1, 1.6))
        for t in (0.3, 0.5, 0.7):
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            c.fill(rect(int(x), int(y), int(x), int(y)), '#7ac85a')
        c.fill(line_mask(x1, y1, x1 + 2, y1 - 2), '#8a9a4a')
    pal('haricot_vert', 'Long', '#4aa032', '#a8e080', '#7ac85a', 3)


@item('petit_pois')
def petit_pois(c):
    back = capsule(4, 23, 27, 10, 5.2, 3.8)
    c.shape(back, R('#3a8a2a'), L_cyl(4, 23, 27, 10, 5.2))
    inner = capsule(6, 21.5, 25, 11, 3.8, 2.8)
    c.shape(inner, R('#c8e8a0'), L_const(0.75), edge=False)
    for k in range(5):
        x, y = 8 + k * 4, 20.5 - k * 2.3
        m = circle(x, y, 2.3)
        c.shape(m, R('#6cc03a'), L_sphere(x - 0.8, y - 0.8, 2.3, 2.3), hl=1)
    lip = capsule(4, 25, 27, 12, 2.2, 1.6) & ~capsule(4, 22.5, 27, 10, 3.8, 2.8)
    c.shape(lip, R('#4aa032'), L_const(0.6))
    c.fill(line_mask(27, 10, 30, 6), '#6a8a3a')
    c.shape(capsule(3, 25, 1, 28, 1), R('#8ab04a'), L_const(0.6))
    pal('petit_pois', 'Rond', '#6cc03a', '#a8e080', '#3a8a2a', 0)

@item('potiron')
def potiron(c):
    for (cx, rx) in [(8, 7), (24, 7), (12, 7), (20, 7), (16, 7)]:
        m = ellipse(cx, 19, rx, 10)
        c.shape(m, R('#e8781a'), L_sphere(cx - 2, 14, rx + 3, 11, 0.2), hl=1)
    c.shape(capsule(16, 10, 18, 3, 2.2, 1.6), R('#6a7a3a'), L_cyl(16, 10, 18, 3, 2.2))
    c.fill(line_mask(18, 5, 23, 5), '#5a8a2a')
    pal('potiron', 'Rond', '#e8781a', '#f8a848', '#f4e4b0', 3)


@item('radis')
def radis(c):
    for (cx, cy, s) in [(11, 19, 1.0), (21, 21, 0.9)]:
        stem(c, [(cx, cy - 5), (cx - 3, 3)], '#6aa04a')
        leaf(c, cx - 1, cy - 6, cx - 5 + s * 3, 2, 5, '#4a9a3a')
        m = ellipse(cx, cy, 6 * s, 6 * s)
        c.shape(m, R('#d8284a'), L_sphere(cx - 2, cy - 2, 6, 6), hl=2)
        c.shape(ellipse(cx, cy + 5 * s, 3 * s, 1.8), R('#f4ecec'), L_const(0.7), edge=False)
        c.fill(line_mask(cx, cy + 6, cx + 1, cy + 10), '#f0e0e0')
    pal('radis', 'Rond', '#d8284a', '#fbf4f4', '#4a9a3a', 0)


@item('betterave')
def betterave(c):
    for (x1, y1) in [(8, 2), (15, 1), (22, 3)]:
        stem(c, [(16, 12), ((16 + x1) / 2, (12 + y1) / 2)], '#b8203a')
        leaf(c, (16 + x1) / 2, (12 + y1) / 2, x1, y1, 6, '#3a7a2a')
    m = ellipse(16, 20, 10, 9) | polygon([(12, 26), (16, 31), (20, 26)])
    c.shape(m, R('#7a1a3a'), L_sphere(13, 17, 10, 10), hl=2)
    c.fill(line_mask(16, 29, 18, 31), '#5a1028')
    pal('betterave', 'Rond', '#7a1a3a', '#b82a5a', '#d85a88', 2)


@item('patate_douce')
def patate_douce(c):
    m = capsule(5, 22, 26, 12, 3, 3) | ellipse(15, 17, 9, 6.5)
    c.shape(m, R('#a8483a'), L_sphere(13, 14, 11, 8, 0.2), hl=2)
    for (x, y) in [(10, 17), (18, 14), (20, 19)]:
        c.fill(rect(x, y, x + 2, y), '#7a2a22')
    c.fill(line_mask(26, 12, 29, 10), '#8a4a3a')
    pal('patate_douce', 'Long', '#a8483a', '#f0a050', '#7a2a22', 0)


@item('navet')
def navet(c):
    for (x1, y1) in [(10, 2), (16, 1), (23, 3)]:
        leaf(c, 16, 11, x1, y1, 5, '#4a9a3a')
    m = ellipse(16, 19, 10, 9) | polygon([(13, 25), (16, 31), (19, 25)])
    c.shape(m, R('#f4f0e8'), L_sphere(13, 16, 10, 10), hl=2)
    c.shape(m & (YY < 18 - (XX - 16) * 0.2), R('#9a3a8a'), L_sphere(13, 16, 10, 10), edge=False)
    c.fill(edge(m) & (YY < 18), '#6a2a5a')
    pal('navet', 'Rond', '#f4f0e8', '#fbfaf4', '#9a3a8a', 0)


@item('celeri')
def celeri(c):
    for k, (x0, x1) in enumerate([(9, 6), (15, 15), (21, 24)]):
        m = capsule(x0 + 2, 30, x1 + 2, 10, 2.8, 2.2)
        c.shape(m, R('#a8d078'), L_cyl(x0 + 2, 30, x1 + 2, 10, 2.8))
        c.fill(line_mask(x0 + 2, 29, x1 + 2, 12) & m & ~edge(m), '#c8e89a')
        for (dx, dy) in [(-4, -6), (3, -7), (0, -8)]:
            leaf(c, x1 + 2, 11, x1 + 2 + dx, 11 + dy, 4, '#4a9a3a', vein=False)
    pal('celeri', 'Long', '#a8d078', '#d8f0b0', '#4a9a3a', 1)


@item('asperge')
def asperge(c):
    for (x0, x1) in [(10, 7), (16, 16), (22, 25)]:
        m = capsule(x0, 30, x1, 6, 2, 1.8)
        c.shape(m, R('#6aa03a'), L_cyl(x0, 30, x1, 6, 2))
        tip = ellipse(x1, 5, 2.4, 3.2)
        c.shape(tip, R('#6a6a3a'), L_sphere(x1 - 1, 4, 2.4, 3), edge=True)
        for yy in (12, 18, 24):
            xx = x0 + (x1 - x0) * (30 - yy) / 24
            c.fill(line_mask(xx - 1, yy, xx + 1, yy - 1), '#4a7a2a')
    c.shape(rect(7, 21, 25, 23), R('#c83a2a'), L_const(0.6))
    pal('asperge', 'Long', '#6aa03a', '#e0f0c0', '#6a6a3a', 1)


@item('artichaut')
def artichaut(c):
    c.shape(capsule(16, 24, 16, 31, 2.2), R('#7a9a4a'), L_cyl(16, 24, 16, 31, 2.2))
    rows = [(24, [-6, 0, 6], 6.5), (18, [-9, -3, 3, 9], 6), (12, [-6, 0, 6], 5.5), (7, [-2.5, 2.5], 4.5), (3.5, [0], 3.5)]
    for ri, (y, xs, w) in enumerate(rows):
        for k, dx in enumerate(xs):
            x = 16 + dx
            sc = polygon([(x - w / 2 - 0.5, y + 1), (x - w / 2 + 0.5, y - w * 0.6), (x, y - w * 1.05), (x + w / 2 - 0.5, y - w * 0.6),
                          (x + w / 2 + 0.5, y + 1), (x, y + 2.5)])
            cc = '#6a8a4a' if (k + ri) % 2 else '#7a9a54'
            c.shape(sc, R(cc), L_sphere(x - 1, y - 3, w, w, 0.4), edge=True)
            c.fill(rect(int(x), int(y - w * 0.95), int(x), int(y - w * 0.8)), '#9a5a8a')
    pal('artichaut', 'Noyau', '#6a8a4a', '#d8e0a8', '#9a5a8a', 6)

@item('avocat')
def avocat(c):
    back = ellipse(20, 15, 8, 11)
    c.shape(back, R('#2a4a1a'), L_sphere(18, 12, 8, 11), hl=1)
    c.speckle(back, '#3a5a2a', 0.2, seed=2)
    half = ellipse(12, 19, 8.5, 10.5)
    c.shape(half, R('#3a5a22'), L_const(0.4))
    flesh = ellipse(12, 19, 7.2, 9.2)
    c.shape(flesh, R('#d8e070'), L_sphere(11, 18, 8, 9, 0.8), edge=False)
    c.fill(arc_band(12, 19, 7.2, 9.2, 1.4), '#a8c848')
    c.shape(circle(12, 21, 3.8), R('#8a5a2a'), L_sphere(11, 20, 3.8, 3.8), hl=1)
    pal('avocat', 'Noyau', '#2a4a1a', '#d8e070', '#8a5a2a', 6)


# ============================================================================ fruits
@item('pomme')
def pomme(c):
    m = ellipse(11, 18, 8.5, 10) | ellipse(21, 18, 8.5, 10) | ellipse(16, 22, 10, 7.5)
    c.shape(m, R('#d8242a'), L_sphere(13, 15, 13, 11), hl=3)
    c.fill(arc_band(16, 9, 3, 2, 1, 0, 180), '#a81820')
    c.speckle(m, '#f06058', 0.05, seed=5)
    c.shape(capsule(16, 9, 17, 3, 1), R('#6a4a2a'), L_const(0.5))
    leaf(c, 17, 5, 25, 3, 4, '#4aa032')
    pal('pomme', 'Noyau', '#d8242a', '#f8f0c8', '#6a4a2a', 6)


@item('poire')
def poire(c):
    m = ellipse(16, 21, 9.5, 8.5) | ellipse(16, 12, 5.5, 7)
    c.shape(m, R('#c8c83a'), L_sphere(13, 15, 11, 13, 0.2), hl=2)
    c.speckle(m, '#9a8a2a', 0.06, seed=4)
    c.blend(ellipse(20, 20, 5, 6) & m, '#d86a3a', 0.3)
    c.shape(capsule(16, 6, 18, 1.5, 1), R('#6a4a2a'), L_const(0.5))
    leaf(c, 17, 3, 23, 2, 3.5, '#4aa032', vein=False)
    pal('poire', 'Noyau', '#c8c83a', '#f8f4d8', '#6a4a2a', 6)


@item('orange')
def orange(c):
    m = circle(16, 18, 11.5)
    c.shape(m, R('#f08a14'), L_sphere(14, 15, 11.5, 11.5), hl=3)
    for (x, y) in rng_pts(8, m & ~edge(m), 30):
        c.fill(rect(x, y, x, y), '#d8700c')
    c.shape(circle(16, 7.5, 1.4), R('#6a7a2a'), L_const(0.5), edge=False)
    leaf(c, 17, 7, 25, 4, 4.5, '#3a8a2a')
    pal('orange', 'Rond', '#f08a14', '#f8a830', '#fbe8b8', 4)


@item('citron')
def citron(c):
    m = ellipse(16, 17, 12, 8.5) | polygon([(2, 17), (5, 15), (5, 19)]) | polygon([(30, 17), (27, 15), (27, 19)])
    c.shape(m, R('#f4d824'), L_sphere(13, 14, 12, 9), hl=3)
    for (x, y) in rng_pts(6, m & ~edge(m), 24):
        c.fill(rect(x, y, x, y), '#d8b814')
    leaf(c, 18, 10, 25, 4, 4, '#3a8a2a')
    pal('citron', 'Rond', '#f4d824', '#f8ec80', '#fbf8d8', 4)


@item('citron_vert')
def citron_vert(c):
    m = circle(12, 15, 9)
    c.shape(m, R('#5aa822'), L_sphere(10, 12, 9, 9), hl=2)
    c.speckle(m, '#3a8a1a', 0.08, seed=2)
    slice_round(c, 21, 22, 7.5, '#4a9a22', '#b8e05a', motif='citrus')
    pal('citron_vert', 'Rond', '#5aa822', '#b8e05a', '#e8f8c0', 4)


@item('fraise')
def fraise(c):
    m = polygon([(5, 11), (27, 11), (24, 20), (16, 30), (8, 20)]) | ellipse(16, 13, 11, 5)
    c.shape(m, R('#e0202a'), L_sphere(13, 14, 12, 12, 0.2), hl=3)
    for (x, y) in np.argwhere(m & ~edge(m))[:, ::-1]:
        if (x * 3 + y * 5) % 11 == 0 and y > 12:
            c.fill(rect(x, y, x, y), '#f8e080')
    cal = polygon([(8, 10), (12, 8), (16, 5), (20, 8), (24, 10), (20, 11), (16, 13), (12, 11)])
    c.shape(cal, R('#3a9a2a'), L_const(0.6))
    c.shape(capsule(16, 7, 17, 2, 1), R('#3a7a22'), L_const(0.5), edge=False)
    pal('fraise', 'Rond', '#e0202a', '#f86a6a', '#fbe0e0', 3)


@item('framboise')
def framboise(c):
    for (x, y) in [(16, 10), (11, 13), (21, 13), (13, 17), (19, 17), (16, 14), (10, 20), (22, 20), (16, 21), (13, 24), (19, 24), (16, 27)]:
        m = circle(x, y, 3)
        c.shape(m, R('#d02050'), L_sphere(x - 0.8, y - 0.8, 3, 3), edge=True, hl=1)
    leaf(c, 16, 7, 9, 3, 4, '#3a8a2a', vein=False)
    leaf(c, 16, 7, 23, 3, 4, '#3a8a2a', vein=False)
    pal('framboise', 'Rond', '#d02050', '#e85078', '#f8a0b8', 0)


@item('myrtille')
def myrtille(c):
    for (x, y, r) in [(11, 20, 6), (21, 21, 6), (16, 12, 6)]:
        m = circle(x, y, r)
        c.shape(m, R('#3a3a8a'), L_sphere(x - 2, y - 2, r, r), hl=2)
        c.speckle(m, '#6a70b0', 0.15, seed=x)
        c.shape(polygon([(x - 1.5, y - r + 1), (x, y - r + 3), (x + 1.5, y - r + 1), (x, y - r + 1.5)]), R('#2a2a5a'), L_const(0.4), edge=False)
    pal('myrtille', 'Rond', '#3a3a8a', '#6a4a9a', '#9a9ad0', 0)


@item('banane')
def banane(c):
    m = arc_band(27, 4, 22, 22, 7, 100, 175)
    c.shape(m, R('#f4d02a'), L_sphere(14, 16, 18, 18, 0.4), hl=3)
    c.fill(arc_band(27, 4, 18.5, 18.5, 1, 110, 170) & m & ~edge(m), '#d8b020')
    c.shape(capsule(26, 26, 29, 25, 1.6), R('#6a5a2a'), L_const(0.5))
    c.shape(capsule(5, 6, 4, 3, 1.5, 1.2), R('#7a8a3a'), L_const(0.5))
    pal('banane', 'Long', '#f4d02a', '#f8f0c8', '#6a5a2a', 0)


@item('ananas')
def ananas(c):
    for (x1, y1, w) in [(10, 1, 4), (16, 0, 4), (22, 1, 4), (7, 6, 4), (25, 6, 4)]:
        leaf(c, 16, 11, x1, y1, w, '#3a8a3a', vein=False)
    m = ellipse(16, 20, 9, 10.5)
    c.shape(m, R('#d8a02a'), L_sphere(13, 17, 9, 10.5), hl=2)
    for k in range(-4, 5):
        c.fill(line_mask(16 + k * 4 - 8, 10, 16 + k * 4 + 8, 31) & m & ~edge(m), '#9a6a1a')
        c.fill(line_mask(16 + k * 4 + 8, 10, 16 + k * 4 - 8, 31) & m & ~edge(m), '#9a6a1a')
    pal('ananas', 'Noyau', '#d8a02a', '#f8e060', '#3a8a3a', 1)


@item('mangue')
def mangue(c):
    m = ellipse(16, 17, 12, 10) | ellipse(12, 20, 9, 8)
    c.shape(m, R('#e8a02a'), L_sphere(13, 14, 13, 11), hl=3)
    c.blend(ellipse(21, 12, 8, 6) & m, '#d83a2a', 0.45)
    c.blend(ellipse(8, 23, 6, 5) & m, '#7ab03a', 0.4)
    c.fill(line_mask(26, 10, 28, 8), '#6a4a2a')
    pal('mangue', 'Noyau', '#e8a02a', '#f8c030', '#f4e8b0', 6)


@item('peche')
def peche(c):
    m = circle(16, 18, 11.5)
    c.shape(m, R('#f0a060'), L_sphere(14, 15, 12, 12), hl=2)
    c.blend(ellipse(20, 20, 8, 8) & m, '#e04a4a', 0.45)
    c.fill(arc_band(10, 18, 7, 11, 1, 290, 70) & m & ~edge(m), '#c8603a')
    leaf(c, 16, 7, 24, 3, 4.5, '#3a8a2a')
    pal('peche', 'Noyau', '#f0a060', '#f8c878', '#a8503a', 6)


@item('abricot')
def abricot(c):
    for (cx, cy, r) in [(20, 19, 8), (11, 17, 8.5)]:
        m = circle(cx, cy, r)
        c.shape(m, R('#f09a2a'), L_sphere(cx - 2, cy - 2, r, r), hl=2)
        c.blend(ellipse(cx + 3, cy + 2, 4, 4) & m, '#e0503a', 0.35)
        c.fill(arc_band(cx - 4, cy, 5, r, 1, 290, 70) & m & ~edge(m), '#c8701a')
    leaf(c, 11, 9, 5, 4, 3.5, '#3a8a2a', vein=False)
    pal('abricot', 'Noyau', '#f09a2a', '#f8b848', '#9a5a2a', 6)


@item('cerise')
def cerise(c):
    stem(c, [(10, 20), (16, 4)], '#5a7a2a')
    stem(c, [(22, 21), (16, 4)], '#5a7a2a')
    leaf(c, 16, 4, 25, 3, 4, '#3a8a2a')
    for (x, y) in [(9, 22), (22, 23)]:
        m = circle(x, y, 6)
        c.shape(m, R('#b8101e'), L_sphere(x - 2, y - 2, 6, 6), hl=3)
    pal('cerise', 'Noyau', '#b8101e', '#d83040', '#e8c8a0', 6)


@item('raisin')
def raisin(c):
    stem(c, [(16, 7), (18, 2)], '#6a5a2a')
    leaf(c, 18, 4, 26, 3, 4.5, '#4a8a2a')
    for (x, y) in [(11, 10), (16, 10), (21, 10), (8, 14), (13, 14), (18, 14), (23, 14), (10, 18), (15, 18), (20, 18),
                   (12, 22), (17, 22), (15, 26)]:
        m = circle(x, y, 2.9)
        c.shape(m, R('#6a2a7a'), L_sphere(x - 1, y - 1, 3, 3), edge=True, hl=1)
    pal('raisin', 'Rond', '#6a2a7a', '#9a5aa0', '#c8a0d0', 0)


@item('kiwi')
def kiwi(c):
    back = ellipse(20, 14, 9, 7.5)
    c.shape(back, R('#8a6a3a'), L_sphere(18, 12, 9, 8), hl=1)
    c.speckle(back, '#6a4a28', 0.2, seed=3)
    m = circle(12, 20, 9)
    c.shape(m, R('#7a5a30'), L_const(0.4))
    fl = circle(12, 20, 7.8)
    c.shape(fl, R('#7ac03a'), L_sphere(11, 19, 8, 8, 0.8), edge=False)
    c.shape(circle(12, 20, 2.8), R('#f4f0c8'), L_const(0.7), edge=False)
    for a in range(0, 360, 30):
        x = 12 + math.cos(math.radians(a)) * 4.2
        y = 20 + math.sin(math.radians(a)) * 4.2
        c.fill(rect(int(x), int(y), int(x), int(y)), '#1a1a10')
    pal('kiwi', 'Rond', '#8a6a3a', '#7ac03a', '#f4f0c8', 3)


@item('figue')
def figue(c):
    m = ellipse(16, 19, 11, 10) | polygon([(12, 12), (16, 3), (20, 12)])
    c.shape(m, R('#5a2a5a'), L_sphere(13, 15, 12, 12), hl=2)
    for x in (11, 16, 21):
        c.fill(line_mask(16, 6, x, 28) & m & ~edge(m), '#7a4a7a')
    c.shape(capsule(16, 4, 17, 1, 1), R('#6a7a3a'), L_const(0.5))
    pal('figue', 'Rond', '#5a2a5a', '#d84a6a', '#f4d0a0', 3)


@item('melon')
def melon(c):
    m = circle(13, 14, 11)
    c.shape(m, R('#b8b86a'), L_sphere(11, 11, 11, 11), hl=1)
    for k in range(-3, 4):
        c.fill(arc_band(13 + k * 3, 14, 3 + abs(k), 10.5, 1, 250, 110) & m & ~edge(m), '#8a9a4a')
    sl = polygon([(10, 27), (29, 27), (27, 21), (20, 17), (12, 20)])
    c.shape(sl, R('#f09a3a'), L_vert(17, 27, 0.9, 0.5), hl=1)
    c.shape(rect(10, 26, 29, 28), R('#8aa84a'), L_const(0.5))
    for x in (15, 19, 23):
        c.fill(rect(x, 23, x + 1, 23), '#f4e0a0')
    pal('melon', 'Rond', '#b8b86a', '#f09a3a', '#f4e0a0', 3)


@item('pasteque')
def pasteque(c):
    rind = polygon([(1, 12), (31, 12), (16, 30)])
    c.shape(rind, R('#2a7a2a'), L_const(0.5))
    c.shape(polygon([(2.5, 12), (29.5, 12), (16, 27.5)]), R('#e8f0c0'), L_const(0.8), edge=False)
    flesh = polygon([(3.5, 11), (28.5, 11), (16, 26)])
    c.shape(flesh, R('#e8303a'), L_vert(8, 26, 0.9, 0.5), edge=False, hl=2)
    for (x, y) in [(10, 14), (16, 13), (22, 14), (13, 18), (19, 18), (16, 22)]:
        c.fill(rect(x, y, x, y + 1), '#1a1010')
    c.shape(ellipse(16, 11, 13, 2) & (YY <= 11), R('#f06058'), L_const(0.8))
    pal('pasteque', 'Rond', '#2a7a2a', '#e8303a', '#1a1010', 3)


@item('noix_de_coco')
def noix_de_coco(c):
    back = circle(19, 13, 9)
    c.shape(back, R('#6a4428'), L_sphere(17, 10, 9, 9), hl=1)
    for (x, y) in rng_pts(4, back, 40):
        c.fill(rect(x, y, x + 1, y), '#8a6440')
    for (x, y) in [(17, 9), (21, 9), (19, 12)]:
        c.fill(rect(x, y, x, y), '#2a1808')
    h = ellipse(12, 21, 10, 8)
    c.shape(h, R('#6a4428'), L_const(0.4))
    c.shape(ellipse(12, 20, 8.5, 6), R('#fbf8f0'), L_sphere(10, 18, 9, 7, 0.8), edge=False)
    c.shape(ellipse(12, 20, 6, 4), R('#e8f0f4'), L_const(0.8), edge=False)
    pal('noix_de_coco', 'Rond', '#6a4428', '#fbf8f0', '#e8f0f4', 1)


# ============================================================================ viandes
def raw_meat(c, m, color='#c8323a', fat='#f4e4dc', seed=1, marbling=10):
    c.shape(m, R(color), L_sphere(13, 13, 14, 12, 0.6), hl=2)
    rs = np.random.RandomState(seed)
    inner = m & ~edge(m)
    for (x, y) in rng_pts(seed, inner, marbling):
        L = rs.randint(2, 5)
        c.fill(line_mask(x, y, x + L, y + rs.randint(-1, 2)), fat, clip=inner)


@item('boeuf')
def boeuf(c):
    m = ellipse(16, 17, 13, 10) | ellipse(10, 20, 8, 8)
    c.shape(m, R('#f0e0d0'), L_const(0.7))
    meat = ellipse(17, 17, 11, 8.3) | ellipse(11, 19.5, 6.5, 6.5)
    raw_meat(c, meat, '#b8202a', '#f4d8d0', seed=3, marbling=12)
    c.shape(ellipse(7, 13, 2.5, 2.5), R('#f4f0e8'), L_const(0.8))
    pal('boeuf', 'Pavé', '#b8202a', '#d84048', '#f4d8d0', 5)


@item('veau')
def veau(c):
    bone = capsule(4, 24, 11, 19, 1.8)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    m = ellipse(18, 16, 11, 9)
    c.shape(m, R('#f0e0d8'), L_const(0.7))
    raw_meat(c, ellipse(18, 16.3, 9.5, 7.6), '#e0909a', '#f8e4e4', seed=5, marbling=5)
    pal('veau', 'Pavé', '#e0909a', '#f0b0b0', '#f8e4e4', 5)


@item('porc')
def porc(c):
    bone = capsule(4, 25, 10, 20, 2)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    m = ellipse(17, 16, 12, 9.5) | ellipse(10, 19, 5, 5)
    c.shape(m, R('#f8ece4'), L_sphere(14, 13, 13, 10, 0.8))
    raw_meat(c, ellipse(18, 15.5, 9.5, 7), '#e8888a', '#f8e0e0', seed=7, marbling=4)
    pal('porc', 'Pavé', '#e8888a', '#f4b0b0', '#f8ece4', 5)


@item('poulet')
def poulet(c):
    bone = capsule(21, 9, 27, 3, 2)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    c.shape(circle(28, 3, 2) | circle(26, 1.8, 1.6), R('#f4ecdc'), L_const(0.8))
    m = capsule(11, 20, 21, 10, 8.5, 3.2)
    c.shape(m, R('#f0c0a0'), L_sphere(10, 15, 11, 11, 0.4), hl=2)
    c.speckle(m, '#e0a888', 0.1, seed=3)
    pal('poulet', 'Pavé', '#f0c0a0', '#f8d8c8', '#f4ecdc', 0)


@item('dinde')
def dinde(c):
    bone = capsule(22, 8, 28, 2, 2.2)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    c.shape(circle(29, 2, 2.2) | circle(27, 1, 1.6), R('#f4ecdc'), L_const(0.8))
    m = capsule(11, 20, 22, 9, 10, 3.5)
    c.shape(m, R('#d8a080'), L_sphere(9, 14, 12, 12, 0.4), hl=2)
    c.speckle(m, '#b88060', 0.12, seed=4)
    pal('dinde', 'Pavé', '#d8a080', '#f0d0c0', '#f4ecdc', 0)


@item('mouton')
def mouton(c):
    for k in range(4):
        x = 9 + k * 5
        c.shape(capsule(x, 12, x + 3, 2, 1.4), R('#f4ecdc'), L_const(0.8))
    m = rrect(4, 11, 28, 28, 4)
    c.shape(m, R('#f4e8dc'), L_vert(11, 28, 0.9, 0.5))
    raw_meat(c, rrect(6, 15, 26, 26, 3), '#a82a3a', '#f0d0d0', seed=9, marbling=6)
    for k in range(3):
        c.fill(line_mask(11 + k * 5, 15, 11 + k * 5, 26), '#8a1a2a')
    pal('mouton', 'Pavé', '#a82a3a', '#c84a58', '#f4e8dc', 5)


@item('lapin')
def lapin(c):
    bone = capsule(21, 10, 27, 4, 1.6)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    m = capsule(9, 21, 21, 11, 6.5, 2.8) | ellipse(10, 21, 7, 6)
    c.shape(m, R('#e8b0a8'), L_sphere(9, 17, 9, 8, 0.5), hl=2)
    c.fill(arc_band(10, 21, 5, 4, 1, 200, 330) & m & ~edge(m), '#c8888a')
    stem(c, [(4, 7), (8, 12)], '#6a8a3a')
    leaf(c, 5, 8, 2, 4, 3, '#4a8a3a', vein=False)
    pal('lapin', 'Pavé', '#e8b0a8', '#f0c8c0', '#c8888a', 0)


@item('canard')
def canard(c):
    flesh = polygon([(3, 18), (8, 12), (26, 11), (30, 16), (26, 24), (7, 25)])
    c.shape(flesh, R('#a8202e'), L_sphere(14, 14, 15, 9, 0.5), hl=1)
    c.speckle(flesh, '#c84048', 0.08, seed=3)
    skin = polygon([(4, 16), (9, 10), (25, 9), (29, 14), (25, 17), (8, 18)])
    c.shape(skin, R('#f0d8a8'), L_sphere(13, 10, 14, 6, 0.6), hl=2)
    for k in range(-2, 5):
        c.fill(line_mask(6 + k * 4, 17, 11 + k * 4, 10) & skin & ~edge(skin), '#d0a870')
        c.fill(line_mask(6 + k * 4, 10, 11 + k * 4, 17) & skin & ~edge(skin), '#d0a870')
    c.fill(line_mask(5, 18, 28, 16), '#f4e8d0')
    pal('canard', 'Pavé', '#9a1a2a', '#b83040', '#f0dcc0', 5)

@item('cerf')
def cerf(c):
    m = ellipse(15, 19, 12, 8.5)
    c.shape(m, R('#6a1420'), L_const(0.45))
    top = ellipse(15, 17, 11, 7)
    raw_meat(c, top, '#7a1a28', '#a84a50', seed=11, marbling=6)
    for (x, y) in [(22, 5), (25, 8), (27, 4)]:
        c.shape(circle(x, y, 1.6), R('#3a3a6a'), L_sphere(x - 0.5, y - 0.5, 1.6, 1.6), edge=True)
    stem(c, [(20, 10), (27, 3)], '#4a6a2a')
    leaf(c, 23, 7, 21, 2, 3, '#3a6a3a', vein=False)
    pal('cerf', 'Pavé', '#7a1a28', '#9a2a38', '#a84a50', 5)


@item('sanglier')
def sanglier(c):
    bone = capsule(22, 9, 28, 3, 2) | circle(29, 2.5, 2) | circle(27.5, 1.5, 1.4)
    c.shape(bone, R('#f4ecdc'), L_const(0.8))
    m = capsule(12, 19, 22, 10, 10, 3.6)
    c.shape(m, R('#7a2224'), L_sphere(10, 14, 12, 12, 0.5), hl=1)
    c.speckle(m, '#a84040', 0.1, seed=4)
    rind = m & (YY < 15 - (XX - 12) * 0.6)
    c.shape(rind, R('#4a3020'), L_const(0.5), edge=False)
    for (x, y) in np.argwhere(rind & ~edge(rind))[:, ::-1]:
        if (x * 3 + y) % 4 == 0:
            c.fill(line_mask(x, y, x - 1, y - 2) & m, '#2a1a10')
    c.fill(edge(rind) & ~edge(m), '#f0d8c8')
    pal('sanglier', 'Pavé', '#8a2a2a', '#a84040', '#3a2418', 5)

@item('saumon')
def saumon(c):
    m = polygon([(3, 14), (26, 7), (30, 11), (29, 20), (7, 27), (2, 22)])
    c.shape(m, R('#f07a50'), L_sphere(14, 13, 16, 12, 0.6), hl=3)
    for k in range(6):
        x = 6 + k * 4
        c.fill(line_mask(x, 24 - k * 0.7, x + 4, 11 - k * 0.6) & m & ~edge(m), '#f8c8a8')
    c.shape(polygon([(7, 27), (29, 20), (29, 22), (8, 29)]), R('#8a8a90'), L_const(0.5))
    pal('saumon', 'Poisson', '#9aa0a8', '#f07a50', '#f8c8a8', 5)


@item('thon')
def thon(c):
    fish_body(c, 3, 19, 26, 14, 13, '#1a2a5a', '#d8dce4', fin='#e8c020', seed=2)
    for k in range(4):
        c.fill(rect(15 + k * 3, 11 - k * 0, 15 + k * 3, 11), '#e8c020')
    c.shape(polygon([(12, 12), (17, 7), (19, 12)]), R('#2a3a6a'), L_const(0.5))
    pal('thon', 'Poisson', '#1a2a5a', '#c8303a', '#d8dce4', 0)


@item('cabillaud')
def cabillaud(c):
    fish_body(c, 3, 18, 26, 15, 11, '#7a7a5a', '#e8e4dc', fin='#8a8a6a', spots='#5a5a3a', stripe='#f4f0e8', seed=3)
    c.fill(line_mask(4, 20, 3, 23), '#6a6a4a')
    pal('cabillaud', 'Poisson', '#7a7a5a', '#f4f0ec', '#e8e4dc', 0)


@item('truite')
def truite(c):
    fish_body(c, 3, 18, 26, 14, 11, '#5a7a4a', '#e8dcd0', fin='#6a8a5a', spots='#2a2a1a', stripe='#e87a8a', seed=5)
    pal('truite', 'Poisson', '#5a7a4a', '#f08a78', '#e87a8a', 0)


@item('sardine')
def sardine(c):
    fish_body(c, 4, 13, 27, 9, 6.5, '#3a5a8a', '#e8ecf0', fin='#6a8aa8', seed=6)
    fish_body(c, 5, 24, 28, 21, 6.5, '#3a5a8a', '#e8ecf0', fin='#6a8aa8', seed=7)
    pal('sardine', 'Poisson', '#3a5a8a', '#b86a6a', '#e8ecf0', 0)


@item('maquereau')
def maquereau(c):
    b = fish_body(c, 3, 18, 26, 14, 10, '#2a6a6a', '#e8ecec', fin='#3a7a7a', seed=8)
    for k in range(7):
        x = 8 + k * 2.6
        c.fill(line_mask(x, 12, x + 1.5, 16.5) & b & ~edge(b), '#102a2a')
    pal('maquereau', 'Poisson', '#2a6a6a', '#c88878', '#e8ecec', 0)


@item('anchois')
def anchois(c):
    for k, y in enumerate((9, 16, 23)):
        fish_body(c, 5 + k, y, 26 + k, y - 3, 4.5, '#5a7a9a', '#e8eef4', fin='#7a9ab0', seed=9 + k)
    pal('anchois', 'Poisson', '#5a7a9a', '#b87a7a', '#e8eef4', 0)


@item('crevette')
def crevette(c):
    for k in range(6):
        a = math.radians(200 - k * 32)
        x, y = 16 + math.cos(a) * 8, 16 + math.sin(a) * 8
        r = 4.3 - k * 0.45
        m = circle(x, y, r)
        c.shape(m, R('#f07a5a'), L_sphere(x - 1, y - 1, r, r), edge=True, hl=1)
        c.fill(arc_band(x, y, r, r, 1, 200, 300) & m, '#f8b8a0')
    c.shape(polygon([(22, 23), (27, 27), (25, 21)]), R('#e85a3a'), L_const(0.6))
    c.shape(circle(8, 12, 3), R('#e86a4a'), L_sphere(7, 11, 3, 3))
    c.fill(rect(7, 11, 7, 11), '#101010')
    c.fill(line_mask(6, 10, 1, 2), '#c84a2a')
    c.fill(line_mask(8, 9, 12, 1), '#c84a2a')
    for k in range(3):
        c.fill(line_mask(10 + k * 3, 20, 9 + k * 3, 24), '#c84a2a')
    pal('crevette', 'Carapace', '#f07a5a', '#f8c8b8', '#c84a2a', 0)


@item('calamar')
def calamar(c):
    for k in range(5):
        x = 8 + k * 2
        c.fill(line_mask(x, 20, x - 3 + k, 30), '#e8b0b8')
        c.fill(line_mask(x + 1, 20, x - 2 + k, 30), '#d890a0')
    m = capsule(12, 19, 26, 5, 5.5, 3.5)
    c.shape(m, R('#f0d0d0'), L_cyl(12, 19, 26, 5, 5.5), hl=2)
    c.speckle(m, '#c87a9a', 0.12, seed=4)
    c.shape(polygon([(22, 3), (30, 2), (27, 10)]), R('#e8c0c8'), L_const(0.6))
    c.fill(rect(14, 16, 14, 16), '#101010')
    pal('calamar', 'Long', '#f0d0d0', '#fbf4f0', '#c87a9a', 2)


@item('moule')
def moule(c):
    for (cx, cy, rot) in [(12, 16, -1), (20, 18, 1)]:
        m = polygon([(cx - rot * 1, cy - 10), (cx + 5, cy - 3), (cx + 4, cy + 8), (cx - 3, cy + 9), (cx - 5, cy)])
        c.shape(m, R('#2a2a4a'), L_sphere(cx - 2, cy - 3, 7, 10), hl=2)
        for k in range(3):
            c.fill(arc_band(cx - rot, cy - 9, 5 + k * 5, 5 + k * 5, 1, 30, 150) & m & ~edge(m), '#4a4a7a')
    fl = ellipse(20, 19, 3, 5)
    c.shape(fl, R('#f09a4a'), L_sphere(19, 17, 3, 5), edge=True)
    pal('moule', 'Carapace', '#2a2a4a', '#f09a4a', '#4a4a7a', 0)


@item('huitre')
def huitre(c):
    m = polygon([(3, 16), (10, 7), (22, 6), (30, 12), (28, 22), (18, 28), (7, 25)])
    c.shape(m, R('#8a8a7a'), L_sphere(14, 12, 15, 12), hl=1)
    for k in range(4):
        c.fill(arc_band(16, 17, 12 - k * 2.5, 9 - k * 2, 1, 0, 360) & m & ~edge(m), '#6a6a5a')
    inner = ellipse(16, 17, 10, 7)
    c.shape(inner, R('#e8e4dc'), L_const(0.8), edge=False)
    fl = ellipse(15, 17, 7, 4.8)
    c.shape(fl, R('#c8c0b0'), L_sphere(13, 15, 7, 5), edge=True, hl=2)
    c.fill(arc_band(15, 17, 7, 4.8, 1), '#6a6a6a')
    pal('huitre', 'Carapace', '#8a8a7a', '#c8c0b0', '#e8e4dc', 0)


@item('crabe')
def crabe(c):
    for s in (-1, 1):
        for k in range(3):
            c.fill(line_mask(16 + s * 6, 20 + k * 2, 16 + s * 12, 24 + k * 2.5, 1), '#c83a22')
        cl = circle(16 + s * 11, 9, 3.6)
        c.shape(cl, R('#e04a2a'), L_sphere(16 + s * 11 - 1, 8, 3.6, 3.6), hl=1)
        c.erase(polygon([(16 + s * 11, 9), (16 + s * 13, 5), (16 + s * 9, 5)]))
        c.fill(line_mask(16 + s * 5, 16, 16 + s * 10, 12, 2), '#c83a22')
    body = ellipse(16, 18, 10, 7)
    c.shape(body, R('#e04a2a'), L_sphere(14, 15, 10, 7), hl=2)
    for x in (13, 19):
        c.fill(rect(x, 10, x, 12), '#8a2a1a')
        c.shape(circle(x, 10, 1.3), R('#101010'), L_const(0.2), edge=False)
    pal('crabe', 'Carapace', '#e04a2a', '#f8ece4', '#c83a22', 0)


# ============================================================================ produits animaux
@item('oeuf')
def oeuf(c):
    m = ellipse(16, 18, 9, 11.5) & (YY > 6.5) | ellipse(16, 14, 7.5, 8)
    c.shape(m, R('#e8c89a'), L_sphere(13, 13, 10, 12), hl=4)
    c.speckle(m, '#c8a070', 0.05, seed=2)
    pal('oeuf', 'Rond', '#e8c89a', '#fbf8f0', '#f4b020', 0)


@item('lait')
def lait(c):
    bottle(c, 16, 3, 29, 12, 5, 9, '#f4f4f0', liquid='#fbfbf8', cap='#2a6ad0', label='#3a8ae0')
    c.fill(rect(12, 19, 20, 19), '#ffffff')
    pal('lait', 'Rond', '#f4f4f0', '#ffffff', '#2a6ad0', 0)


@item('lait_de_chevre')
def lait_de_chevre(c):
    body = ellipse(15, 20, 9, 9.5) | rect(9, 8, 21, 20)
    c.shape(body, R('#d8b890'), L_horiz(6, 24, 0.95, 0.3), hl=1)
    c.shape(arc_band(24, 17, 4.5, 5.5, 2, 270, 90), R('#c8a880'), L_const(0.6))
    top = ellipse(15, 8, 6, 2)
    c.shape(top, R('#fbfbf8'), L_const(0.8), edge=False)
    c.shape(polygon([(8, 7), (10, 5), (11, 8)]), R('#d8b890'), L_const(0.8))
    c.shape(rect(9, 16, 21, 22), R('#4a8a3a'), L_const(0.6))
    c.fill(points([(13, 18), (14, 18), (15, 18), (16, 18), (17, 17), (12, 17), (13, 20), (16, 20), (11, 16)]), '#fbfbf0')
    pal('lait_de_chevre', 'Rond', '#d8b890', '#fbfbf8', '#4a8a3a', 0)


@item('miel')
def miel(c):
    jar(c, 6, 24, 9, 29, content='#e8a018', lid='#c8302a', level=0.9, glass_col='#f0d890')
    c.shape(rect(5, 9, 25, 12) & ~rrect(6, 9, 24, 12, 1) | rrect(5, 12, 10, 16, 1), R('#e8a018'), L_const(0.7))
    stem(c, [(27, 1), (20, 10)], '#b88a50', width=2)
    c.shape(rrect(17, 9, 22, 14, 1), R('#d8a030'), L_const(0.7))
    pal('miel', 'Rond', '#e8a018', '#f8c848', '#c8302a', 0)


# ============================================================================ fruits secs & légumineuses
def nut(c, x, y, rx, ry, color, ridge=True, seed=0):
    m = ellipse(x, y, rx, ry)
    c.shape(m, R(color), L_sphere(x - 1, y - 1, rx, ry), hl=1)
    if ridge:
        c.fill(line_mask(x - rx * 0.5, y - ry * 0.4, x + rx * 0.4, y + ry * 0.5) & m & ~edge(m), R(color)[1])
    return m


@item('amande')
def amande(c):
    for (x, y, a) in [(10, 13, 0), (21, 12, 1), (15, 22, 2)]:
        m = ellipse(x, y, 5, 7.5) & (YY > y - 7) | polygon([(x - 3, y - 4), (x, y - 9), (x + 3, y - 4)])
        c.shape(m, R('#b8784a'), L_sphere(x - 1, y - 2, 5, 8), hl=1)
        c.speckle(m, '#8a5030', 0.15, seed=int(x))
    pal('amande', 'Rond', '#b8784a', '#f4ead8', '#8a5030', 0)


@item('noisette')
def noisette(c):
    for (x, y) in [(11, 18), (21, 19)]:
        m = circle(x, y, 6.5)
        c.shape(m, R('#9a5a2a'), L_sphere(x - 2, y - 2, 6.5, 6.5), hl=2)
        c.fill(line_mask(x - 3, y + 3, x + 3, y + 3) & m, '#6a3a1a')
        cap = ellipse(x, y - 5, 5.5, 3)
        c.shape(cap, R('#d8c890'), L_const(0.7))
    leaf(c, 16, 10, 12, 2, 5, '#6a9a3a')
    pal('noisette', 'Rond', '#9a5a2a', '#f4e8d0', '#6a3a1a', 0)


@item('noix')
def noix(c):
    m = ellipse(13, 15, 10, 9)
    c.shape(m, R('#b88a5a'), L_sphere(10, 12, 10, 9), hl=1)
    for (x0, y0, x1, y1) in [(6, 12, 11, 9), (8, 18, 14, 16), (14, 11, 19, 14), (16, 19, 20, 16), (13, 6, 13, 24)]:
        c.fill(line_mask(x0, y0, x1, y1) & m & ~edge(m), '#8a5a30')
    k = ellipse(22, 23, 7, 5.5)
    c.shape(k, R('#9a6a3a'), L_const(0.4))
    c.shape(ellipse(22, 22.5, 5.5, 4), R('#d8a868'), L_sphere(21, 21, 6, 4), edge=False)
    c.fill(line_mask(22, 19, 22, 26), '#8a5a30')
    pal('noix', 'Rond', '#b88a5a', '#d8a868', '#8a5a30', 0)


@item('pistache')
def pistache(c):
    for (x, y) in [(10, 13), (21, 12), (16, 22)]:
        m = ellipse(x, y, 5.5, 7)
        c.shape(m, R('#e0cca0'), L_sphere(x - 1, y - 2, 5.5, 7), hl=1)
        g = ellipse(x + 0.5, y, 2.5, 5)
        c.shape(g, R('#7ab03a'), L_sphere(x, y - 1, 2.5, 5), edge=True)
        c.blend(ellipse(x + 1, y + 2, 1.6, 2) & g, '#8a3a6a', 0.4)
    pal('pistache', 'Rond', '#e0cca0', '#7ab03a', '#8a3a6a', 0)


@item('cacahuete')
def cacahuete(c):
    for (cx, cy, a) in [(12, 14, -30), (20, 20, 20)]:
        dx, dy = math.cos(math.radians(a)) * 5, math.sin(math.radians(a)) * 5
        m = circle(cx - dx, cy - dy, 4.8) | circle(cx + dx, cy + dy, 4.8) | capsule(cx - dx, cy - dy, cx + dx, cy + dy, 3.6)
        c.shape(m, R('#d8b07a'), L_sphere(cx - 2, cy - 2, 9, 6, 0.2), hl=1)
        for (x, y) in np.argwhere(m & ~edge(m))[:, ::-1]:
            if (x + y * 2) % 4 == 0:
                c.fill(rect(x, y, x, y), '#b88a58')
    pal('cacahuete', 'Rond', '#d8b07a', '#e8a878', '#b88a58', 0)


def legume_pile(c, color, shape='round', seed=1, pouch='#b89a6a'):
    body = sack(c, pouch, color, x0=6, x1=26, y0=10, y1=29)
    top = ellipse(16, 13, 8, 2.8) | ellipse(16, 12, 5, 2.5)
    rs = np.random.RandomState(seed)
    for (x, y) in rng_pts(seed, top, 14):
        if shape == 'round':
            m = circle(x, y, 1.5)
        elif shape == 'kidney':
            m = ellipse(x, y, 2, 1.2)
        else:
            m = ellipse(x, y, 1.6, 1.0)
        c.shape(m, R(color), L_sphere(x - 0.5, y - 0.5, 2, 1.6), edge=True)
    for (x, y) in [(3, 28), (28, 27), (26, 30)]:
        if shape == 'kidney':
            m = ellipse(x, y, 2.2, 1.3)
        else:
            m = circle(x, y, 1.6)
        c.shape(m, R(color), L_sphere(x - 0.5, y - 0.5, 2, 1.6), edge=True)


@item('lentilles')
def lentilles(c):
    legume_pile(c, '#7a8a3a', 'flat', seed=2)
    pal('lentilles', 'Rond', '#7a8a3a', '#c8b060', '#5a6a2a', 0)


@item('pois_chiche')
def pois_chiche(c):
    legume_pile(c, '#e0c088', 'round', seed=3)
    pal('pois_chiche', 'Rond', '#e0c088', '#f0dcb0', '#b89060', 0)


@item('haricot_rouge')
def haricot_rouge(c):
    legume_pile(c, '#8a1a24', 'kidney', seed=4)
    pal('haricot_rouge', 'Rond', '#8a1a24', '#e8d0b8', '#5a1018', 0)


@item('haricot_blanc')
def haricot_blanc(c):
    legume_pile(c, '#f0ecdc', 'kidney', seed=5)
    pal('haricot_blanc', 'Rond', '#f0ecdc', '#fbf8f0', '#c8c0a8', 0)


@item('soja')
def soja(c):
    for (x0, y0, x1, y1) in [(4, 22, 24, 8), (8, 28, 28, 15)]:
        m = capsule(x0, y0, x1, y1, 3.6)
        c.shape(m, R('#6aa83a'), L_cyl(x0, y0, x1, y1, 3.6), hl=1)
        for t in (0.2, 0.5, 0.8):
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            c.shape(circle(x, y, 2.4), R('#7ab84a'), L_sphere(x - 0.5, y - 0.5, 2.4, 2.4), edge=True)
        c.speckle(m, '#9ad06a', 0.15, seed=int(x0))
    pal('soja', 'Rond', '#6aa83a', '#e8e0a0', '#9ad06a', 0)


# ============================================================================ herbes fraîches
def bunch_tie(c, x, y, color='#c83a2a'):
    c.shape(rect(x - 3, y, x + 3, y + 1), R(color), L_const(0.6))


@item('basilic')
def basilic(c):
    stem(c, [(16, 31), (16, 8)], '#5a8a2a', width=2)
    for (x0, y0, x1, y1, w) in [(16, 26, 5, 22, 8), (16, 25, 27, 21, 8), (16, 18, 6, 12, 8), (16, 17, 26, 11, 8), (16, 10, 16, 1, 7)]:
        leaf(c, x0, y0, x1, y1, w, '#3aa02a')
    pal('basilic', 'Herbes', '#3aa02a', '#6ac84a', '#5a8a2a', 8)


@item('persil')
def persil(c):
    for (x1, y1) in [(8, 6), (16, 3), (24, 6), (12, 10), (21, 10)]:
        stem(c, [(16, 30), (x1, y1 + 3)], '#5a9a2a')
        for (dx, dy) in [(-2.5, -1), (2.5, -1), (0, -3)]:
            m = circle(x1 + dx, y1 + dy + 2, 2.4)
            c.shape(m, R('#2e8a2a'), L_sphere(x1 + dx - 0.5, y1 + dy + 1.5, 2.4, 2.4), edge=True)
            c.fill(rect(int(x1 + dx), int(y1 + dy + 2), int(x1 + dx), int(y1 + dy + 2)), '#1a5a1a')
    bunch_tie(c, 16, 25)
    pal('persil', 'Herbes', '#2e8a2a', '#5ab04a', '#1a5a1a', 8)


@item('menthe')
def menthe(c):
    stem(c, [(16, 31), (16, 6)], '#6a9a3a', width=2)
    for (x0, y0, x1, y1, w) in [(16, 27, 5, 25, 7), (16, 27, 27, 25, 7), (16, 20, 6, 16, 7), (16, 20, 26, 16, 7), (16, 12, 9, 6, 6),
                                (16, 12, 23, 6, 6), (16, 7, 16, 1, 5)]:
        m = leaf(c, x0, y0, x1, y1, w, '#4ab04a')
        for (x, y) in np.argwhere(edge(m))[:, ::-1]:
            if (x + y) % 2 == 0:
                c.fill(rect(x, y, x, y), '#8ad07a')
    pal('menthe', 'Herbes', '#4ab04a', '#8ad07a', '#6a9a3a', 8)


@item('coriandre')
def coriandre(c):
    for (x1, y1) in [(7, 8), (16, 4), (25, 8), (10, 16), (22, 15)]:
        stem(c, [(16, 30), (x1, y1 + 2)], '#7aa84a')
        m = circle(x1, y1, 3.6)
        c.shape(m, R('#5ab83a'), L_sphere(x1 - 1, y1 - 1, 3.6, 3.6), edge=True)
        for a in (0, 120, 240):
            xx = x1 + math.cos(math.radians(a - 90)) * 2.5
            yy = y1 + math.sin(math.radians(a - 90)) * 2.5
            c.erase(rect(int(xx), int(yy), int(xx), int(yy)) & edge(m))
        c.fill(line_mask(x1, y1 + 2, x1, y1 - 2) & m, '#3a8a2a')
    bunch_tie(c, 16, 25, '#e0a020')
    pal('coriandre', 'Herbes', '#5ab83a', '#8ad06a', '#3a8a2a', 8)


@item('thym')
def thym(c):
    for (x0, x1) in [(12, 6), (16, 16), (20, 26)]:
        stem(c, [(x0, 30), (x1, 3)], '#7a5a3a')
        for j in range(12):
            t = j / 12
            x = x0 + (x1 - x0) * t
            y = 29 - 26 * t
            for s in (-1, 1):
                c.fill(rect(int(x + s * 1.5), int(y), int(x + s * 1.5), int(y)), '#6a8a5a' if j % 2 else '#8aa87a')
    pal('thym', 'Herbes', '#6a8a5a', '#8aa87a', '#7a5a3a', 8)


@item('romarin')
def romarin(c):
    for (x0, y0, x1, y1) in [(8, 30, 20, 3), (14, 30, 27, 8)]:
        stem(c, [(x0, y0), (x1, y1)], '#6a5a3a')
        n = 11
        for j in range(n):
            t = 0.08 + j / n * 0.9
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            for s in (-1, 1):
                c.fill(line_mask(x, y, x + s * 3, y - 1.5), '#3a6a5a' if j % 2 else '#5a8a7a')
    for (x, y) in [(19, 5), (26, 10)]:
        c.fill(rect(x, y, x + 1, y), '#9a7ac8')
    pal('romarin', 'Herbes', '#3a6a5a', '#5a8a7a', '#9a7ac8', 8)


@item('ciboulette')
def ciboulette(c):
    for k in range(7):
        x0 = 12 + k
        x1 = 5 + k * 3.5
        c.shape(line_mask(x0, 30, x1, 5, 1), R('#3a9a2a'), L_const(0.6), edge=False)
        c.fill(line_mask(x0, 30, x1, 5), '#4ab03a' if k % 2 else '#2a7a22')
    bunch_tie(c, 15, 24, '#c8a040')
    for (x, y) in [(6, 5), (26, 6)]:
        m = circle(x, y, 2.6)
        c.shape(m, R('#b87ad8'), L_sphere(x - 0.5, y - 0.5, 2.6, 2.6), edge=True)
    pal('ciboulette', 'Herbes', '#3a9a2a', '#6ac84a', '#b87ad8', 8)


@item('aneth')
def aneth(c):
    for (x1, y1) in [(8, 4), (17, 2), (25, 6)]:
        stem(c, [(16, 30), (x1, y1 + 4)], '#6a9a3a')
        for j in range(5):
            t = 0.35 + j * 0.13
            x, y = 16 + (x1 - 16) * t, 30 + (y1 + 4 - 30) * t
            for s in (-1, 1):
                c.fill(line_mask(x, y, x + s * 4, y - 3), '#6ab04a')
                c.fill(line_mask(x, y - 1, x + s * 2.5, y - 4), '#8ac86a')
    pal('aneth', 'Herbes', '#6ab04a', '#8ac86a', '#6a9a3a', 8)


@item('estragon')
def estragon(c):
    for (x0, x1) in [(13, 8), (18, 22)]:
        stem(c, [(x0, 31), (x1, 3)], '#5a8a3a')
        for j in range(5):
            t = 0.2 + j * 0.17
            x, y = x0 + (x1 - x0) * t, 31 - 28 * t
            s = 1 if j % 2 else -1
            leaf(c, x, y, x + s * 6, y - 5, 2.6, '#3a8a3a', vein=False)
    pal('estragon', 'Herbes', '#3a8a3a', '#6ab04a', '#5a8a3a', 8)


# ============================================================================ bases de boissons
@item('grains_de_cafe')
def grains_de_cafe(c):
    for (x, y, a) in [(10, 11, -20), (21, 12, 25), (11, 22, 30), (21, 23, -15), (16, 17, 0)]:
        m = ellipse(x, y, 4.5, 5.5)
        c.shape(m, R('#6a3a1a'), L_sphere(x - 1, y - 1.5, 4.5, 5.5), hl=1)
        c.fill(line_mask(x - 0.5, y - 4, x + 0.5, y + 4) & m & ~edge(m), '#2a1408')
    pal('grains_de_cafe', 'Rond', '#6a3a1a', '#8a5a2a', '#2a1408', 0)


@item('feuilles_de_the')
def feuilles_de_the(c):
    stem(c, [(16, 31), (16, 4)], '#6a7a3a')
    for (x0, y0, x1, y1, w) in [(16, 24, 4, 18, 8), (16, 20, 28, 13, 8), (16, 13, 7, 6, 7), (16, 8, 16, 1, 5)]:
        leaf(c, x0, y0, x1, y1, w, '#2a6a2a')
    c.shape(ellipse(16, 3, 1.6, 3), R('#8ac86a'), L_const(0.7))
    pal('feuilles_de_the', 'Herbes', '#2a6a2a', '#4a8a3a', '#6a7a3a', 8)


@item('feves_de_cacao')
def feves_de_cacao(c):
    m = ellipse(14, 14, 9, 12)
    c.shape(m, R('#c8781a'), L_sphere(11, 10, 9, 12), hl=2)
    for x in (9, 12, 15, 18):
        c.fill(line_mask(x, 4, x, 24) & m & ~edge(m), '#9a5a14')
    for (x, y) in [(21, 25), (26, 22), (25, 28)]:
        mm = ellipse(x, y, 3, 2.2)
        c.shape(mm, R('#5a2a1a'), L_sphere(x - 0.5, y - 0.5, 3, 2.2), edge=True)
    pal('feves_de_cacao', 'Rond', '#c8781a', '#f4ecd8', '#5a2a1a', 0)


@item('canne_a_sucre')
def canne_a_sucre(c):
    for (x0, x1, cc) in [(8, 14, '#9ab84a'), (18, 22, '#b8b050')]:
        m = capsule(x0, 30, x1, 3, 2.6)
        c.shape(m, R(cc), L_cyl(x0, 30, x1, 3, 2.6), hl=1)
        for k in range(4):
            t = 0.15 + k * 0.23
            x, y = x0 + (x1 - x0) * t, 30 - 27 * t
            c.fill(line_mask(x - 2.5, y, x + 2.5, y - 0.5) & m, R(cc)[0])
    leaf(c, 14, 4, 5, 1, 3, '#5a9a3a', vein=False)
    leaf(c, 22, 4, 29, 1, 3, '#5a9a3a', vein=False)
    pal('canne_a_sucre', 'Long', '#9ab84a', '#f4f0d8', '#5a9a3a', 1)


@item('bouteille_d_eau')
def bouteille_d_eau(c):
    bottle(c, 16, 2, 30, 12, 5, 8, '#9ad0f0', liquid='#b8e4fa', cap='#2a6ad0', label='#2a8ae0', cap_h=3)
    for y in (22, 26):
        c.fill(rect(11, y, 20, y), '#d8f0ff')
    pal('bouteille_d_eau', 'Rond', '#9ad0f0', '#b8e4fa', '#2a6ad0', 0)


# ============================================================================ épices & condiments
def spice_jar(c, powder, label, lid='#3a3a3a', grain=None, seed=1):
    jar(c, 8, 23, 8, 29, content=powder, lid=lid, label=label, level=0.8, lid_h=5)
    if grain:
        grains(c, rect(10, 16, 21, 27) & ~rect(9, 19, 22, 23), grain, 14, seed=seed, size=(1, 1))


@item('sel')
def sel(c):
    body = polygon([(9, 12), (22, 12), (23, 29), (8, 29)])
    c.shape(body, R('#d8e8ee'), L_horiz(8, 23, 0.9, 0.3))
    c.shape(body & (YY > 15) & ~edge(body), R('#fbfbfb'), L_horiz(8, 23, 0.95, 0.5), edge=False)
    c.speckle(body & (YY > 16), '#e0e8ec', 0.2, seed=2)
    cap = ellipse(15.5, 9, 7, 4) & (YY < 12) | rect(9, 9, 22, 12)
    c.shape(cap, R('#c0c4cc'), L_sphere(13, 6, 8, 5, 0.3), hl=2)
    for (x, y) in [(13, 8), (16, 7), (19, 8), (15, 10), (18, 10)]:
        c.fill(rect(x, y, x, y), '#4a4a50')
    for (x, y) in [(26, 27), (27, 29), (4, 28)]:
        c.fill(rect(x, y, x + 1, y + 1), '#ffffff')
    pal('sel', 'Rond', '#ffffff', '#ffffff', '#c0c4cc', 0)


@item('poivre_noir')
def poivre_noir(c):
    body = rrect(10, 9, 21, 29, 3) | ellipse(15.5, 18, 7, 5)
    c.shape(body, R('#5a3420'), L_horiz(9, 22, 0.95, 0.3), hl=2)
    c.shape(rect(10, 12, 21, 13), R('#c8a040'), L_const(0.7))
    c.shape(ellipse(15.5, 7, 4, 3) | rect(14, 3, 17, 7), R('#5a3420'), L_sphere(14, 5, 4, 4))
    c.shape(circle(15.5, 2.5, 1.8), R('#c8a040'), L_const(0.7))
    for (x, y) in [(4, 24), (26, 23), (6, 28), (27, 28)]:
        m = circle(x, y, 1.8)
        c.shape(m, R('#2a2420'), L_sphere(x - 0.5, y - 0.5, 1.8, 1.8), edge=True)
    pal('poivre_noir', 'Rond', '#2a2420', '#4a4038', '#5a3420', 0)


@item('sucre')
def sucre(c):
    for (x, y) in [(5, 18), (15, 18), (10, 9)]:
        top = polygon([(x, y + 3), (x + 5, y), (x + 11, y + 2), (x + 6, y + 5)])
        front = polygon([(x, y + 3), (x + 6, y + 5), (x + 6, y + 12), (x, y + 10)])
        side = polygon([(x + 6, y + 5), (x + 11, y + 2), (x + 11, y + 9), (x + 6, y + 12)])
        c.shape(front, R('#e8e8e4'), L_const(0.6))
        c.shape(side, R('#d0d0cc'), L_const(0.4))
        c.shape(top, R('#ffffff'), L_const(0.9))
        c.speckle(front | side, '#f8f8f8', 0.25, seed=x)
    pal('sucre', 'Rond', '#ffffff', '#ffffff', '#e8e8e4', 0)


@item('paprika')
def paprika(c):
    spice_jar(c, '#c8321a', '#f4e8c8', lid='#c8302a')
    c.fill(points([(12, 20), (13, 21), (14, 20), (15, 21), (16, 20), (17, 21), (18, 20), (19, 21)]), '#c8321a')
    pal('paprika', 'Rond', '#c8321a', '#e04a2a', '#f4e8c8', 0)


@item('cumin')
def cumin(c):
    spoon(c, 28, 4, 14, 17, '#b98a52', bowl_r=8, content='#9a7038')
    grains(c, ellipse(14, 16, 7, 4), '#6a4a20', 16, seed=3, size=(2, 1))
    pile(c, 9, 27, 6, 3, '#9a7038', grain='#6a4a20', n=6)
    pal('cumin', 'Rond', '#9a7038', '#b88a48', '#6a4a20', 0)


@item('curcuma')
def curcuma(c):
    pile(c, 18, 23, 10, 5.5, '#f0a818', grain='#d88a10', n=10)
    r = capsule(3, 13, 14, 8, 3, 2.5) | capsule(9, 11, 12, 4, 2)
    c.shape(r, R('#b8844a'), L_sphere(7, 8, 7, 5, 0.3), hl=1)
    c.fill(line_mask(6, 13, 7, 9) & r, '#8a5a30')
    c.shape(circle(14, 8, 2.4), R('#f0901a'), L_const(0.7))
    pal('curcuma', 'Rond', '#b8844a', '#f0a818', '#8a5a30', 1)


@item('cannelle')
def cannelle(c):
    for (x0, y0, x1, y1) in [(4, 24, 26, 10), (6, 28, 28, 15), (3, 19, 22, 6)]:
        m = capsule(x0, y0, x1, y1, 2.4)
        c.shape(m, R('#9a5028'), L_cyl(x0, y0, x1, y1, 2.4), hl=1)
        c.fill(line_mask(x0 + 1, y0, x1, y1 + 1) & m & ~edge(m), '#6a3014')
        c.shape(circle(x1, y1, 2.2), R('#b86a38'), L_const(0.7))
        c.fill(arc_band(x1, y1, 1.5, 1.5, 1, 0, 270), '#6a3014')
    pal('cannelle', 'Long', '#9a5028', '#b86a38', '#6a3014', 2)


@item('muscade')
def muscade(c):
    m = ellipse(13, 17, 9, 10.5)
    c.shape(m, R('#8a5a36'), L_sphere(10, 13, 9, 11), hl=2)
    for k in range(-3, 4):
        c.fill(arc_band(13 + k * 3, 17, 4, 10, 1, 250, 110) & m & ~edge(m), '#6a4024')
    h = ellipse(23, 22, 6, 7)
    c.shape(h, R('#8a5a36'), L_const(0.4))
    c.shape(ellipse(23, 21.5, 4.6, 5.5), R('#c89a6a'), L_const(0.7), edge=False)
    for (x0, y0, x1, y1) in [(21, 19, 25, 24), (24, 18, 21, 25)]:
        c.fill(line_mask(x0, y0, x1, y1) & h, '#8a5a36')
    pal('muscade', 'Rond', '#8a5a36', '#c89a6a', '#6a4024', 0)


@item('piment')
def piment(c):
    m = capsule(9, 11, 21, 22, 3.8, 1.2) | ellipse(10, 11, 4, 3.5)
    c.shape(m, R('#d8141a'), L_cyl(9, 11, 21, 22, 3.8), hl=3)
    tip = capsule(21, 22, 26, 29, 1.3, 0.6)
    c.shape(tip, R('#d8141a'), L_const(0.6))
    c.shape(ellipse(9, 9, 4, 2.6), R('#3a8a2a'), L_const(0.6))
    c.shape(capsule(8, 8, 5, 2, 1.2), R('#4a8a2a'), L_const(0.6))
    pal('piment', 'Long', '#d8141a', '#f04a3a', '#f8e0a0', 3)


@item('gingembre')
def gingembre(c):
    m = capsule(4, 20, 24, 16, 4) | capsule(10, 18, 8, 8, 3) | capsule(18, 16, 22, 7, 3) | capsule(22, 18, 29, 24, 3)
    c.shape(m, R('#d8b078'), L_sphere(12, 12, 14, 10, 0.2), hl=2)
    for (x0, y0, x1, y1) in [(7, 17, 7, 23), (14, 15, 14, 21), (20, 13, 20, 19)]:
        c.fill(line_mask(x0, y0, x1 + 1, y1) & m & ~edge(m), '#a8804a')
    c.shape(circle(29, 25, 2.3), R('#f4e0a0'), L_const(0.7))
    pal('gingembre', 'Long', '#d8b078', '#f4e0a0', '#a8804a', 1)


@item('vanille')
def vanille(c):
    for (x0, y0, x1, y1) in [(5, 28, 25, 4), (9, 29, 29, 8)]:
        m = capsule(x0, y0, x1, y1, 1.5, 1.0)
        c.shape(m, R('#3a2418'), L_cyl(x0, y0, x1, y1, 1.5), hl=1)
    for a in range(0, 360, 72):
        x = 8 + math.cos(math.radians(a)) * 3.5
        y = 9 + math.sin(math.radians(a)) * 3.5
        c.shape(ellipse(x, y, 2.5, 2.5), R('#f4ecb0'), L_const(0.8))
    c.shape(circle(8, 9, 1.6), R('#e8c030'), L_const(0.7), edge=False)
    pal('vanille', 'Long', '#3a2418', '#5a3a28', '#f4ecb0', 0)


@item('safran')
def safran(c):
    jar(c, 9, 22, 13, 29, content='#f4e8d0', lid='#c89a2a', level=0.5, lid_h=4)
    for k in range(7):
        x = 11 + k * 1.6
        c.fill(line_mask(x, 26, x + (k % 3) - 1, 21), '#d82a14')
    for (x0, y0, x1, y1) in [(6, 9, 10, 3), (14, 8, 17, 2), (20, 9, 26, 4), (10, 10, 12, 4)]:
        c.fill(line_mask(x0, y0, x1, y1, 1), '#e8321a')
        c.fill(rect(x1, y1, x1, y1), '#f0a020')
    pal('safran', 'Rond', '#d82a14', '#e8321a', '#f0a020', 0)


@item('clou_de_girofle')
def clou_de_girofle(c):
    for (x, y, a) in [(9, 9, 20), (20, 8, -15), (14, 18, 5), (24, 19, -30), (8, 22, 35)]:
        ex, ey = x + math.sin(math.radians(a)) * 8, y + math.cos(math.radians(a)) * 8
        c.shape(capsule(x, y, ex, ey, 1.4, 0.8), R('#5a3420'), L_cyl(x, y, ex, ey, 1.4))
        c.shape(circle(x, y, 2.4), R('#6a3a22'), L_sphere(x - 0.5, y - 0.5, 2.4, 2.4), edge=True)
    pal('clou_de_girofle', 'Rond', '#5a3420', '#6a3a22', '#2a1a10', 0)


@item('laurier')
def laurier(c):
    stem(c, [(8, 29), (22, 6)], '#6a5a3a')
    leaf(c, 12, 22, 3, 7, 7, '#5a7a3a')
    leaf(c, 14, 19, 28, 10, 7, '#6a8a44')
    leaf(c, 18, 12, 20, 1, 6, '#5a7a3a')
    pal('laurier', 'Herbes', '#5a7a3a', '#6a8a44', '#6a5a3a', 8)


@item('graines_de_moutarde')
def graines_de_moutarde(c):
    inner = spoon(c, 28, 4, 14, 17, '#b98a52', bowl_r=8, content='#d8b030')
    for (x, y) in rng_pts(4, inner & ~edge(inner), 14):
        c.fill(rect(x, y, x, y), '#f0d060')
        c.fill(rect(x + 1, y + 1, x + 1, y + 1) & inner, '#9a7a18')
    for (x, y) in [(6, 26), (9, 28), (5, 29), (20, 28)]:
        m = circle(x, y, 1.4)
        c.shape(m, R('#d8b030'), L_const(0.7), edge=True)
    pal('graines_de_moutarde', 'Rond', '#d8b030', '#f0d060', '#9a7a18', 0)


@item('sesame')
def sesame(c):
    op = bowl(c, 16, 16, 12, 4.5, 8, '#3a3a3a')
    top = ellipse(16, 15, 10, 4) | ellipse(16, 14, 7, 3.4)
    c.shape(top, R('#f0e0b8'), L_sphere(14, 12, 10, 5), edge=False)
    for (x, y) in rng_pts(3, top, 30):
        c.fill(rect(x, y, x + 1, y), '#fbf4e0' if (x + y) % 2 else '#c8b088')
    bowl_rim(c, 16, 16, 12, 4.5, '#3a3a3a')
    pal('sesame', 'Rond', '#f0e0b8', '#fbf4e0', '#c8b088', 0)


@item('levure')
def levure(c):
    top = polygon([(4, 14), (16, 9), (28, 13), (16, 18)])
    front = polygon([(4, 14), (16, 18), (16, 27), (4, 22)])
    side = polygon([(16, 18), (28, 13), (28, 22), (16, 27)])
    c.shape(front, R('#e8d8a0'), L_const(0.6))
    c.shape(side, R('#d0c088'), L_const(0.4))
    c.shape(top, R('#f4e8b8'), L_const(0.9))
    c.speckle(front | side | top, '#c8b070', 0.12, seed=3)
    wrap = polygon([(4, 18), (16, 22), (16, 27), (4, 22)]) | polygon([(16, 22), (28, 17), (28, 22), (16, 27)])
    c.shape(wrap, R('#c8d8e8'), L_const(0.7), edge=True)
    c.fill(line_mask(6, 20, 14, 23), '#2a6ad0')
    pal('levure', 'Rond', '#e8d8a0', '#f4e8b8', '#c8b070', 0)


@item('huile_d_olive')
def huile_d_olive(c):
    bottle(c, 14, 2, 30, 11, 4, 11, '#6a7a1a', liquid='#b8a820', cap='#3a2a1a', label='#f4ecd0', cap_h=4)
    c.fill(points([(11, 19), (12, 20), (13, 19), (14, 20), (15, 19), (16, 20)]), '#5a7a2a')
    for (x, y) in [(25, 25), (27, 21)]:
        m = ellipse(x, y, 2.6, 3.2)
        c.shape(m, R('#5a6a1a'), L_sphere(x - 1, y - 1, 2.6, 3.2), hl=1)
    leaf(c, 26, 19, 30, 14, 2.6, '#6a8a4a', vein=False)
    pal('huile_d_olive', 'Rond', '#b8a820', '#d8c840', '#6a7a1a', 0)


@item('vinaigre')
def vinaigre(c):
    bottle(c, 16, 2, 30, 10, 3.5, 12, '#6a1a2a', liquid='#8a1a2a', cap='#c8a040', label='#f4ecd0', cap_h=4)
    c.fill(points([(13, 20), (14, 21), (15, 20), (16, 21), (17, 20), (18, 21)]), '#6a1a2a')
    pal('vinaigre', 'Rond', '#8a1a2a', '#a82a3a', '#c8a040', 0)


@item('sauce_soja')
def sauce_soja(c):
    body = polygon([(10, 12), (22, 12), (24, 29), (8, 29)])
    c.shape(body, R('#dce8ee'), L_horiz(8, 24, 0.9, 0.3))
    c.shape(body & (YY > 14) & ~edge(body), R('#3a1a0a'), L_horiz(8, 24, 0.8, 0.3), edge=False)
    c.shape(rect(9, 19, 23, 24), R('#f4ecd8'), L_const(0.7))
    c.fill(points([(13, 21), (14, 21), (15, 22), (17, 21), (18, 22), (19, 21)]), '#c8201a')
    cap = rrect(10, 7, 22, 12, 2) | polygon([(22, 8), (27, 6), (27, 8), (22, 10)])
    c.shape(cap, R('#d8201a'), L_vert(6, 12, 0.9, 0.4), hl=1)
    pal('sauce_soja', 'Rond', '#3a1a0a', '#5a2a14', '#d8201a', 0)
