"""Les 171 plats du Gourmet, chacun dessiné pour ressembler au vrai plat."""
from parts import *


# ============================================================================ gabarits
def soup(c, bowl_col, soup_col, top=None, cx=16, cy=14, rx=13, ry=5, depth=10, level=0.85, foot=True):
    op = bowl(c, cx, cy, rx, ry, depth, bowl_col, foot=foot)
    m = fill_bowl(c, op, soup_col, level, cx, cy, rx, ry)
    if top:
        top(c, m)
    bowl_rim(c, cx, cy, rx, ry, bowl_col)
    return m


def heap_in_bowl(c, bowl_col, color, cx=16, cy=15, rx=13, ry=5, depth=9, h=3):
    """Bol rempli en dôme (salades, riz, purée)."""
    op = bowl(c, cx, cy, rx, ry, depth, bowl_col)
    dome = ellipse(cx, cy - 0.5, rx - 1.5, ry - 0.8) | ellipse(cx, cy - h * 0.6, rx - 4, ry - 1 + h * 0.3)
    c.shape(dome, R(color), L_sphere(cx - 3, cy - h, rx - 2, ry + h, 0.3), edge=False)
    return dome


def chunks(c, mask, colors, n=6, seed=3, r=(1.6, 2.4), edge=True):
    rs = np.random.RandomState(seed)
    for i, (x, y) in enumerate(rng_pts(seed, mask & ~edge_of(mask), n)):
        cc = colors[i % len(colors)]
        rr = rs.uniform(*r)
        m = ellipse(x, y, rr, rr * 0.8) & mask
        c.shape(m, R(cc), L_sphere(x - 0.6, y - 0.6, rr, rr), edge=edge)


def cubes(c, mask, colors, n=8, seed=5, s=2):
    rs = np.random.RandomState(seed)
    for i, (x, y) in enumerate(rng_pts(seed, mask & ~edge_of(mask), n)):
        rr = R(colors[i % len(colors)])
        m = rect(x, y, x + s - 1, y + s - 1) & mask
        c.fill(m, rr[3])
        c.fill(rect(x, y + s - 1, x + s - 1, y + s - 1) & mask, rr[1])
        c.fill(rect(x, y, x, y) & mask, rr[4])


def noodles(c, mask, color, seed=2, n=7, dark=None):
    rr = R(color)
    rs = np.random.RandomState(seed)
    ys, xs = np.nonzero(mask)
    if len(xs) == 0:
        return
    x0, x1, y0, y1 = xs.min(), xs.max(), ys.min(), ys.max()
    for k in range(n):
        yb = y0 + (y1 - y0) * (k + 0.5) / n
        ph = rs.rand() * 6
        prev = None
        for x in range(x0, x1 + 1):
            y = yb + math.sin(x * 0.7 + ph) * 1.2
            if prev:
                m = line_mask(prev[0], prev[1], x, y) & mask
                c.fill(m, rr[3 if k % 2 else 2])
                c.fill(shift(m, 0, 1) & mask, dark or rr[1])
            prev = (x, y)


def slices_fan(c, cx, cy, n, outer, inner, w=4, h=6, step=3.2, tilt=0.8, seed=0):
    """Tranches en éventail (rôti, magret, carpaccio)."""
    for k in range(n):
        x = cx - (n - 1) * step / 2 + k * step
        y = cy + (k - (n - 1) / 2) * tilt * 0.3
        m = ellipse(x, y, w, h)
        c.shape(m, R(outer), L_const(0.45))
        c.shape(ellipse(x + 0.3, y, w - 1.2, h - 1.3), R(inner), L_sphere(x - 1, y - 2, w, h, 0.6), edge=False)


def lemon_wedge(c, x, y, color='#f4d824'):
    m = ellipse(x, y, 3.5, 2.5) & (YY >= y - 0.5)
    c.shape(m, R(color), L_const(0.7))
    c.shape(ellipse(x, y, 2.6, 1.6) & (YY >= y), R(mix(color, '#ffffff', 0.5)), L_const(0.8), edge=False)
    c.fill(line_mask(x - 2, y + 0.5, x + 2, y + 0.5), R(color)[1])


def basil(c, x, y, w=3.4, a=-0.6, color='#3aa02a'):
    leaf(c, x, y, x + math.cos(a) * 5, y + math.sin(a) * 5, w, color, vein=False)


def egg_half(c, x, y, s=1.0, yolk='#f4b020', filling=False):
    m = ellipse(x, y, 4 * s, 3 * s)
    c.shape(m, R('#fbfaf2'), L_sphere(x - 1, y - 1, 4 * s, 3 * s, 0.5))
    ym = circle(x + 0.3, y + 0.2, 1.8 * s)
    c.shape(ym, R(yolk), L_sphere(x - 0.3, y - 0.3, 2 * s, 2 * s, 0.4), edge=not filling)
    if filling:
        c.speckle(ym, mix(yolk, '#ffffff', 0.4), 0.4, seed=int(x))


def fried_egg(c, cx, cy, rx=8, ry=5.5):
    m = ellipse(cx, cy, rx, ry) | ellipse(cx - 3, cy + 1, rx * 0.6, ry * 0.8) | ellipse(cx + 3, cy - 1, rx * 0.6, ry * 0.7)
    c.shape(m, R('#fbfaf2'), L_const(0.8))
    c.fill(edge(m), '#e8c898')
    y = circle(cx + 0.5, cy - 0.5, 2.8)
    c.shape(y, R('#f4a818'), L_sphere(cx - 0.5, cy - 1.5, 2.8, 2.8), hl=2)


def drumstick(c, x0, y0, x1, y1, color='#b8641e', r=4.2, bone=True):
    if bone:
        c.shape(capsule(x1, y1, x1 + (x1 - x0) * 0.35, y1 + (y1 - y0) * 0.35, 1.3), R('#f4ecdc'), L_const(0.8))
    m = capsule(x0, y0, x1, y1, r, r * 0.45)
    c.shape(m, R(color), L_sphere(x0 - 1, y0 - 1, r + 2, r + 2, 0.3), hl=2)
    return m


def sausage(c, x0, y0, x1, y1, r=2.3, color='#a8482a'):
    m = capsule(x0, y0, x1, y1, r)
    c.shape(m, R(color), L_cyl(x0, y0, x1, y1, r), hl=2)
    return m


def shrimp_small(c, x, y, s=1.0, color='#f07a5a'):
    m = arc_band(x, y, 3.2 * s, 3 * s, 1.9 * s, 180, 60)
    c.shape(m, R(color), L_const(0.65), edge=True)
    c.fill(arc_band(x, y, 3.2 * s, 3 * s, 1, 190, 300) & m, '#f8c8b8')


def mussel(c, x, y, open_=True):
    m = ellipse(x, y, 3, 2)
    c.shape(m, R('#2a2a4a'), L_sphere(x - 1, y - 1, 3, 2), hl=1)
    if open_:
        c.shape(ellipse(x, y - 0.3, 1.8, 1.1), R('#f09a4a'), L_const(0.7), edge=False)


def bread_slice(c, x0, y0, x1, y1, crust='#c8803a', crumb='#f4dca0', toast=0.0):
    m = rrect(x0, y0, x1, y1, 2)
    c.shape(m, R(crust), L_const(0.55))
    inner = rrect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 1)
    c.shape(inner, R(mix(crumb, '#c8802a', toast)), L_vert(y0, y1, 0.85, 0.5), edge=False)
    c.speckle(inner, mix(crumb, '#a86a2a', 0.3 + toast * 0.5), 0.12, seed=x0 + y0)
    return inner


def croutons(c, mask, n=5, seed=4):
    cubes(c, mask, ['#d89a4a', '#e8b060'], n=n, seed=seed, s=2)


def parmesan(c, mask, n=5, seed=6):
    for (x, y) in rng_pts(seed, mask & ~edge_of(mask), n):
        c.fill(rect(x, y, x + 2, y), '#fbf2c8')
        c.fill(rect(x, y + 1, x + 1, y + 1), '#e8d8a0')


def olives(c, mask, n=3, seed=9, color='#2a2a2a'):
    for (x, y) in rng_pts(seed, mask & ~edge_of(mask), n):
        m = ellipse(x, y, 1.6, 1.3)
        c.shape(m, R(color), L_sphere(x - 0.5, y - 0.5, 1.6, 1.3), edge=True)


def drizzle(c, pts, color, width=1):
    for i in range(len(pts) - 1):
        c.fill(line_mask(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], width), color)


def chopsticks(c, x0=20, y0=2, x1=29, y1=17, color='#c8a070'):
    c.fill(line_mask(x0, y0, x1, y1), R(color)[2])
    c.fill(line_mask(x0 + 2, y0, x1 + 1, y1 - 1), R(color)[3])


def pan(c, cx=15, cy=17, rx=12, ry=7, color='#3a3a3e', handle=True):
    if handle:
        c.shape(capsule(cx + rx - 1, cy + 1, 31, cy - 4, 1.6), R('#2a2a2a'), L_const(0.5))
    body = ellipse(cx, cy + 1.5, rx, ry)
    c.shape(body, R(color), L_const(0.35))
    inner = ellipse(cx, cy, rx - 1.3, ry - 1.3)
    c.shape(inner, R(mix(color, '#5a5a60', 0.5)), L_vert(cy - ry, cy + ry, 0.3, 0.7), edge=False)
    return inner


def wood_board(c, x0=2, y0=12, x1=29, y1=27, color='#b8844a'):
    return board(c, x0, y0, x1, y1, color)


def paper_cone(c, color='#e03a2a', stripe='#ffffff', x0=7, x1=25, top=12, bottom=29):
    body = polygon([(x0, top), (x1, top), (x1 - 3, bottom), (x0 + 3, bottom)])
    c.shape(body, R(color), L_horiz(x0, x1, 0.9, 0.35))
    for k in range(x0 + 3, x1, 4):
        c.fill(polygon([(k, top), (k + 2, top), (k + 1.5, bottom), (k - 0.5, bottom)]) & body & ~edge(body), stripe)
    return body


def glaze(c, mask, color, n=6, seed=2):
    for (x, y) in rng_pts(seed, mask & ~edge_of(mask), n):
        c.fill(rect(x, y, x + 1, y), mix(color, '#ffffff', 0.55))


# ============================================================================ entrées et salades
@item('salade_verte')
def salade_verte(c):
    op = bowl(c, 16, 16, 13, 5, 9, '#b8844a')
    for (x0, y0, x1, y1) in [(6, 16, 3, 7), (12, 15, 10, 4), (18, 15, 21, 4), (24, 16, 28, 8), (16, 16, 15, 6)]:
        leaf(c, x0, y0, x1, y1, 7, '#6ac03a')
    for (x, y) in [(10, 12), (20, 11), (15, 14)]:
        slice_round(c, x, y, 2.3, '#d8284a', '#fbf4f4', motif='ring')
    bowl_rim(c, 16, 16, 13, 5, '#b8844a')


@item('salade_de_tomates')
def salade_de_tomates(c):
    plate(c)
    for (x, y) in [(9, 18), (15, 16), (21, 18), (12, 22), (18, 22)]:
        slice_round(c, x, y, 3.8, '#c8281a', '#e8483a', motif='seeds', ry=3)
    for (x, y) in [(11, 15), (19, 20)]:
        c.fill(arc_band(x, y, 2.5, 1.7, 1), '#f0e0f0')
    basil(c, 16, 19, 3, -0.8)
    basil(c, 20, 15, 3, -2.4)
    for (x, y) in [(7, 21), (24, 16), (14, 25)]:
        c.fill(rect(x, y, x + 1, y), '#e8d040')


@item('carottes_rapees')
def carottes_rapees(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#f2eee6')
    dome = ellipse(16, 14, 11, 4.2) | ellipse(16, 12.5, 8, 3.5)
    c.shape(dome, R('#f08a2a'), L_sphere(14, 11, 11, 5, 0.3), edge=False)
    rs = np.random.RandomState(3)
    for (x, y) in rng_pts(5, dome, 26):
        c.fill(line_mask(x, y, x + rs.randint(-2, 3), y + 1) & dome, '#f8b050' if (x + y) % 2 else '#c8601a')
    herbs(c, 14, 11, seed=2)
    herbs(c, 19, 13, seed=3)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')
    lemon_wedge(c, 25, 11)


@item('salade_de_riz')
def salade_de_riz(c):
    dome = heap_in_bowl(c, '#3a7ab0', '#f6f3ea')
    grains(c, dome, '#ffffff', 16, seed=2)
    cubes(c, dome, ['#f2c224', '#d82a1a', '#e8a8a0', '#3a9a2a'], n=12, seed=4)
    bowl_rim(c, 16, 15, 13, 5, '#3a7ab0')


@item('salade_de_lentilles')
def salade_de_lentilles(c):
    dome = heap_in_bowl(c, '#f2eee6', '#6a7030')
    for (x, y) in rng_pts(4, dome, 40):
        c.fill(rect(x, y, x + 1, y), '#8a8a40' if (x + y) % 2 else '#4a5020')
    cubes(c, dome, ['#f0e0f0', '#e8c8e0'], n=5, seed=6, s=1)
    herbs(c, 13, 12, seed=4)
    herbs(c, 19, 13, seed=5)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')


@item('bruschetta')
def bruschetta(c):
    for (x0, y0) in [(2, 13), (13, 17)]:
        top = bread_slice(c, x0, y0, x0 + 15, y0 + 9, crust='#b86a2a', crumb='#f0c880', toast=0.3)
        cubes(c, top, ['#d82a1a', '#e84a3a', '#c81a14'], n=9, seed=x0, s=2)
        basil(c, x0 + 7, y0 + 4, 2.6, -0.5)
    c.fill(points([(6, 15), (20, 20), (11, 18)]), '#f0e8d0')


@item('oeufs_mimosa')
def oeufs_mimosa(c):
    plate(c)
    for (x, y) in [(10, 17), (21, 17), (15.5, 22)]:
        egg_half(c, x, y, 1.15, yolk='#f4c830', filling=True)
    herbs(c, 10, 16, seed=1, n=2)
    herbs(c, 21, 16, seed=2, n=2)
    herbs(c, 15, 21, seed=3, n=2)


@item('tzatziki')
def tzatziki(c):
    def top(c, m):
        c.speckle(m, '#5aa03a', 0.12, seed=2)
        c.speckle(m, '#9ad07a', 0.1, seed=3)
        c.fill(arc_band(16, 14, 5, 2, 1, 180, 350) & m, '#fbfbf6')
        leaf(c, 17, 13, 22, 10, 3, '#4ab04a', vein=False)
        c.fill(points([(12, 13), (13, 12), (14, 13)]), '#e0d030')
    soup(c, '#3a6ab0', '#f4f2e8', top)


@item('salade_nicoise')
def salade_nicoise(c):
    w = plate(c)
    for (x0, y0, x1, y1) in [(6, 20, 3, 13), (26, 20, 29, 13), (16, 15, 16, 9)]:
        leaf(c, x0, y0, x1, y1, 6, '#7ac84a')
    for (x, y) in [(9, 17), (22, 22)]:
        egg_half(c, x, y, 0.9)
    for (x, y) in [(18, 17), (12, 22)]:
        m = ellipse(x, y, 3, 2.2)
        c.shape(m, R('#d8281a'), L_sphere(x - 1, y - 1, 3, 2.2), hl=1)
    for k in range(3):
        c.shape(capsule(14 + k, 19, 21 + k, 21, 0.8), R('#4aa032'), L_const(0.6), edge=True)
    olives(c, w, 3, seed=4)
    c.shape(capsule(7, 23, 12, 24, 0.9), R('#8a9aa8'), L_const(0.6))
    cubes(c, ellipse(16, 20, 4, 2), ['#e8b0a0'], n=3, seed=2)


@item('salade_cesar')
def salade_cesar(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#f2eee6')
    for (x0, y0, x1, y1) in [(7, 15, 3, 6), (13, 14, 11, 4), (19, 14, 22, 4), (25, 15, 29, 8)]:
        leaf(c, x0, y0, x1, y1, 6, '#5ab03a')
    for (x, y) in [(10, 12), (17, 13), (22, 11)]:
        c.shape(rrect(x, y, x + 4, y + 2, 1), R('#e0a060'), L_const(0.7))
        c.fill(line_mask(x + 1, y + 2, x + 3, y), '#a86030')
    croutons(c, ellipse(16, 13, 10, 3), 4)
    parmesan(c, ellipse(16, 12, 9, 3), 5)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')


@item('taboule')
def taboule(c):
    dome = heap_in_bowl(c, '#f2eee6', '#f0d070')
    c.speckle(dome, '#3a9a2a', 0.25, seed=2)
    c.speckle(dome, '#6ac04a', 0.12, seed=3)
    cubes(c, dome, ['#d82a1a'], n=5, seed=5, s=1)
    leaf(c, 16, 12, 20, 8, 3, '#4ab04a', vein=False)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')
    lemon_wedge(c, 26, 12)


@item('houmous')
def houmous(c):
    def top(c, m):
        c.fill(arc_band(16, 14, 8, 3, 1, 180, 360) & m, '#d8c090')
        c.fill(arc_band(16, 14, 5, 2, 1, 0, 180) & m, '#d8c090')
        c.shape(ellipse(16, 14, 3.5, 1.4), R('#c8b030'), L_const(0.8), edge=False)
        c.speckle(m, '#c83a1a', 0.08, seed=3)
        for (x, y) in [(11, 13), (21, 15), (19, 12)]:
            c.shape(circle(x, y, 1.3), R('#e0c088'), L_const(0.7), edge=True)
    soup(c, '#e8e4dc', '#e8d4a0', top)


@item('guacamole')
def guacamole(c):
    def top(c, m):
        c.speckle(m, '#8ac03a', 0.2, seed=2)
        cubes(c, m, ['#d82a1a', '#f0e0f0'], n=6, seed=4, s=1)
        leaf(c, 14, 13, 19, 10, 3, '#3aa02a', vein=False)
    soup(c, '#4a4a4a', '#9ab83a', top, foot=False)
    for (x, y) in [(3, 8), (28, 7)]:
        c.shape(polygon([(x - 3, y + 4), (x + 3, y + 4), (x, y - 3)]), R('#e8c050'), L_const(0.7))
    lemon_wedge(c, 26, 25, '#6ab02a')


@item('salade_de_betterave')
def salade_de_betterave(c):
    w = plate(c)
    cubes(c, ellipse(16, 19, 9, 5), ['#8a1a3a', '#a82a4a', '#6a1030'], n=16, seed=3, s=2)
    for (x, y) in rng_pts(7, ellipse(16, 19, 8, 4), 5):
        c.shape(ellipse(x, y, 1.6, 1.2), R('#fbfaf4'), L_const(0.85), edge=True)
    for (x, y) in [(10, 16), (22, 21)]:
        c.shape(ellipse(x, y, 2, 1.5), R('#b88a5a'), L_const(0.6))
    herbs(c, 18, 16, seed=2)


@item('salade_de_chevre_chaud')
def salade_de_chevre_chaud(c):
    w = plate(c)
    for (x0, y0, x1, y1) in [(8, 20, 3, 14), (24, 20, 29, 14), (16, 16, 16, 10)]:
        leaf(c, x0, y0, x1, y1, 6, '#6ac03a')
    for (x, y) in [(11, 18), (21, 19)]:
        c.shape(ellipse(x, y + 1, 4.5, 3), R('#c8803a'), L_const(0.5))
        c.shape(ellipse(x, y, 3.6, 2.5), R('#f8f0dc'), L_sphere(x - 1, y - 1, 3.6, 2.5, 0.4), hl=1)
        c.fill(rect(int(x) - 1, int(y) - 1, int(x), int(y) - 1), '#e8b860')
    drizzle(c, [(8, 17), (12, 16), (16, 18), (22, 17), (24, 19)], '#e8a018')
    for (x, y) in [(16, 22), (7, 22)]:
        c.shape(ellipse(x, y, 2, 1.5), R('#b88a5a'), L_const(0.6))


@item('carpaccio_de_boeuf')
def carpaccio_de_boeuf(c):
    w = plate(c)
    for (x, y) in [(10, 17), (16, 15.5), (22, 17), (12, 22), (20, 22), (16, 19.5)]:
        m = ellipse(x, y, 4, 2.6)
        c.shape(m, R('#c8323a'), L_const(0.6), edge=True)
        c.fill(rect(int(x) - 1, int(y), int(x) + 1, int(y)), '#e8a0a0')
    parmesan(c, ellipse(16, 19, 8, 4), 6)
    basil(c, 15, 18, 2.8, -0.6)
    lemon_wedge(c, 24, 13)


@item('tartare_de_saumon')
def tartare_de_saumon(c):
    plate(c)
    cyl = rect(9, 13, 22, 22) | ellipse(15.5, 22, 6.5, 2.5)
    c.shape(cyl, R('#f07a50'), L_horiz(9, 22, 0.9, 0.4))
    c.speckle(cyl, '#f8b090', 0.2, seed=2)
    c.speckle(cyl, '#c85030', 0.12, seed=3)
    av = ellipse(15.5, 13, 6.5, 2.5) | rect(9, 11, 22, 13)
    c.shape(av, R('#a8c848'), L_vert(10, 15, 0.9, 0.5))
    c.shape(ellipse(15.5, 11, 6.5, 2.4), R('#c8e070'), L_const(0.8), edge=True)
    leaf(c, 15, 10, 19, 6, 2.6, '#4a9a3a', vein=False)
    leaf(c, 15, 10, 12, 6, 2.6, '#4a9a3a', vein=False)


@item('huitres_au_citron')
def huitres_au_citron(c):
    plate(c, color='#dce8f0')
    c.speckle(ellipse(16, 19.5, 12, 6.5), '#ffffff', 0.35, seed=2)
    for (x, y) in [(10, 17), (21, 17), (15, 22)]:
        m = ellipse(x, y, 5, 3.4)
        c.shape(m, R('#8a8a7a'), L_const(0.55))
        c.shape(ellipse(x, y, 3.6, 2.3), R('#d8d0c0'), L_sphere(x - 1, y - 1, 3.6, 2.3, 0.4), edge=True, hl=1)
    c.shape(circle(24, 23, 3.5), R('#f4d824'), L_sphere(23, 22, 3.5, 3.5), hl=1)
    c.shape(circle(24, 23, 2.3), R('#f8ec80'), L_const(0.8), edge=False)


# ============================================================================ soupes
@item('soupe_de_legumes')
def soupe_de_legumes(c):
    soup(c, '#c85a2a', '#c8903a', lambda c, m: cubes(c, m, ['#f07a1a', '#f4e4a8', '#6ab04a', '#f07a1a'], n=12, seed=3))


@item('veloute_de_potiron')
def veloute_de_potiron(c):
    def top(c, m):
        c.fill(arc_band(16, 14, 5, 2, 1, 150, 390) & m, '#fbf4e8')
        c.fill(arc_band(17, 14, 2.5, 1, 1) & m, '#fbf4e8')
        for (x, y) in [(10, 13), (21, 15), (12, 16)]:
            c.fill(rect(x, y, x + 1, y), '#5a7a2a')
    soup(c, '#f2eee6', '#f08a1a', top)


@item('bouillon_de_poulet_aux_pates')
def bouillon_de_poulet_aux_pates(c):
    def top(c, m):
        noodles(c, m, '#f4dc80', n=4)
        for (x, y) in [(10, 13), (20, 15), (15, 12)]:
            slice_round(c, x, y, 1.6, '#e8701a', '#f8a040', motif='ring')
        cubes(c, m, ['#f4e8d8'], n=4, seed=4)
        herbs(c, 18, 13, seed=2, n=2)
    soup(c, '#f2eee6', '#e8b848', top)


@item('soupe_a_l_oignon_gratinee')
def soupe_a_l_oignon_gratinee(c):
    op = bowl(c, 16, 15, 12, 5, 10, '#8a4a28')
    for s in (-1, 1):
        c.shape(rrect(16 + s * 13 - 2, 16, 16 + s * 13 + 1, 19, 1), R('#8a4a28'), L_const(0.6))
    dome = ellipse(16, 13.5, 12, 5) | ellipse(16, 12, 9, 4)
    c.shape(dome, R('#e8b040'), L_sphere(13, 10, 12, 6, 0.3), hl=2)
    c.speckle(dome, '#a8601a', 0.2, seed=4)
    c.speckle(dome, '#f8d880', 0.1, seed=5)
    for x in (6, 12, 21, 26):
        c.fill(line_mask(x, 16, x + 0.5, 19), '#e8b040')


@item('gaspacho')
def gaspacho(c):
    body, ly = glass(c, 7, 24, 7, 28, '#d8321a', level=0.82, taper=2)
    cubes(c, rect(8, int(ly) - 1, 23, int(ly)), ['#3a8a2a', '#f0e0a0', '#e8c020'], n=5, seed=3, s=1)
    leaf(c, 16, int(ly) - 1, 21, int(ly) - 5, 3, '#3aa02a', vein=False)
    c.shape(capsule(23, 6, 29, 2, 1.4), R('#5a9a3a'), L_const(0.6))
    c.shape(ellipse(22, 7, 2.6, 1.6), R('#c8e8a0'), L_const(0.8))


@item('soupe_de_lentilles_corail')
def soupe_de_lentilles_corail(c):
    def top(c, m):
        c.fill(arc_band(16, 14, 5, 2, 1, 170, 370) & m, '#fbf4e8')
        c.speckle(m, '#c85a1a', 0.1, seed=3)
        leaf(c, 18, 13, 22, 11, 2.6, '#3aa02a', vein=False)
        leaf(c, 18, 13, 16, 10, 2.6, '#3aa02a', vein=False)
    soup(c, '#3a5a8a', '#e8782a', top)


@item('minestrone')
def minestrone(c):
    def top(c, m):
        cubes(c, m, ['#f07a1a', '#6ab04a', '#f4f0e0', '#e8d8a0'], n=12, seed=6)
        for (x, y) in [(10, 13), (19, 15)]:
            c.shape(ellipse(x, y, 1.8, 1), R('#f4e0a0'), L_const(0.7), edge=True)
        basil(c, 15, 12, 3, -0.4)
    soup(c, '#f2eee6', '#c8481a', top)


@item('soupe_miso_au_tofu')
def soupe_miso_au_tofu(c):
    def top(c, m):
        c.fill(arc_band(16, 14, 7, 2.5, 1) & m, '#d8a860')
        cubes(c, m, ['#fbf8ec'], n=5, seed=3, s=2)
        c.speckle(m, '#4aa03a', 0.12, seed=4)
        c.shape(polygon([(18, 12), (23, 11), (22, 14), (18, 14)]), R('#1a3a1a'), L_const(0.4), edge=False)
    soup(c, '#1a1a1a', '#c8904a', top)
    c.fill(arc_band(16, 14, 13, 5, 1) & (YY > 14), '#a82a1a')


@item('soupe_de_poisson')
def soupe_de_poisson(c):
    def top(c, m):
        for (x, y) in [(11, 13), (21, 14)]:
            c.shape(ellipse(x, y, 3, 1.8), R('#e8b060'), L_const(0.7))
            c.shape(ellipse(x, y - 0.8, 2, 0.9), R('#f08a3a'), L_const(0.8), edge=False)
        chunks(c, m, ['#f8f0e4'], n=3, seed=5, r=(1.3, 1.8))
        herbs(c, 16, 13, seed=2)
    soup(c, '#f2eee6', '#c8501a', top)


@item('pho_au_boeuf')
def pho_au_boeuf(c):
    def top(c, m):
        noodles(c, m, '#f4f0e0', n=4)
        for (x, y) in [(10, 13), (16, 12), (21, 14)]:
            c.shape(ellipse(x, y, 2.6, 1.6), R('#8a4a3a'), L_const(0.6))
        leaf(c, 12, 15, 8, 12, 3, '#3aa02a', vein=False)
        leaf(c, 20, 12, 24, 10, 3, '#4ab04a', vein=False)
        c.fill(points([(14, 15), (18, 16)]), '#d8141a')
    soup(c, '#f2eee6', '#c89a50', top, rx=13.5, depth=9)
    lemon_wedge(c, 26, 22, '#6ab02a')
    chopsticks(c)


@item('bisque_de_crabe')
def bisque_de_crabe(c):
    def top(c, m):
        c.fill(arc_band(16, 14, 5, 2, 1, 170, 380) & m, '#fbf0e8')
        c.shape(circle(20, 12, 2.6), R('#e04a2a'), L_sphere(19, 11, 2.6, 2.6), hl=1)
        c.erase(polygon([(20, 12), (23, 9), (21, 8)]))
        herbs(c, 12, 13, seed=3, n=2)
    soup(c, '#f2eee6', '#e88a5a', top)


# ============================================================================ viandes
@item('steak_frites')
def steak_frites(c):
    plate(c)
    fries_pile(c, 22, 19, n=10)
    steak(c, 11, 19, 7, 4.3, '#7a3a1c')
    herbs(c, 11, 18, seed=1, n=2)


@item('poulet_roti')
def poulet_roti(c):
    plate(c)
    for s in (-1, 1):
        drumstick(c, 16 + s * 6, 19, 16 + s * 11, 14, '#c8781e', 3.5)
    body = ellipse(16, 17, 9, 6.5)
    c.shape(body, R('#c8781e'), L_sphere(13, 14, 9, 7, 0.4), hl=3)
    glaze(c, body, '#c8781e', 8)
    c.fill(line_mask(16, 12, 16, 22) & body & ~edge(body), '#9a5010')
    sprig(c, 22, 24, 27, 21, '#4a8a3a', n=2, lw=2, ll=2)


@item('cote_de_porc_grillee')
def cote_de_porc_grillee(c):
    plate(c)
    c.shape(capsule(20, 16, 27, 12, 1.5), R('#f4ecdc'), L_const(0.8))
    m = ellipse(14, 19, 9, 5.5)
    c.shape(m, R('#f0dcc0'), L_const(0.7))
    steak(c, 14, 19, 7.5, 4.3, '#c8884a')
    sprig(c, 5, 23, 10, 25, '#3a6a5a', n=3, lw=1.5, ll=2)


@item('brochettes_de_poulet')
def brochettes_de_poulet(c):
    plate(c)
    skewer(c, 3, 21, 28, 13, ['#e8b060', '#d8281c', '#e8b060', '#f4e8e0', '#e8b060'])
    skewer(c, 4, 26, 29, 18, ['#e8b060', '#3a9a2a', '#e8b060', '#d8281c', '#e8b060'])


@item('hachis_parmentier')
def hachis_parmentier(c):
    inner = gratin_dish(c, '#f0cc70', crust='#c8802a', color='#c8502a')
    for k in range(4):
        c.fill(line_mask(7, 15 + k * 2, 24, 15 + k * 2) & inner & ~edge(inner), '#e0b050')
    c.fill(rect(4, 23, 27, 24) & ~rect(4, 24, 27, 24), '#6a3018')
    herbs(c, 15, 16, seed=3)


@item('pot_au_feu')
def pot_au_feu(c):
    def top(c, m):
        c.shape(ellipse(12, 13, 4, 2.6), R('#8a4a2a'), L_sphere(11, 12, 4, 2.6), hl=1)
        for (x, y) in [(19, 12), (22, 14)]:
            c.shape(capsule(x - 2, y, x + 2, y - 1, 1.2), R('#f07a1a'), L_const(0.7))
        c.shape(ellipse(17, 15, 2, 1.4), R('#f4e4a8'), L_const(0.7))
        c.shape(capsule(7, 14, 9, 12, 1.2), R('#e8e8c8'), L_const(0.8))
        leaf(c, 20, 16, 24, 17, 2.4, '#5a7a3a', vein=False)
    soup(c, '#2a3a52', '#d8a860', top)


def saucy_plate(c, sauce, main):
    plate(c)
    sauce_pool(c, 16, 19.5, 10, 5.5, sauce)
    main(c)


@item('poulet_basquaise')
def poulet_basquaise(c):
    def main(c):
        drumstick(c, 13, 19, 20, 14, '#c8701e', 3.8)
        for (x, y, cc) in [(9, 21, '#d8281c'), (22, 21, '#3a9a2a'), (18, 22, '#f0c020'), (8, 17, '#3a9a2a')]:
            c.shape(capsule(x - 2, y, x + 2, y - 1, 1.1), R(cc), L_const(0.7))
    saucy_plate(c, '#c8401a', main)


@item('poulet_au_miel')
def poulet_au_miel(c):
    plate(c)
    rice_mound(c, 21, 18, 6, 4)
    for (x, y) in [(9, 18), (13, 20), (10, 22), (14, 16.5)]:
        m = ellipse(x, y, 3, 2.3)
        c.shape(m, R('#b8601a'), L_sphere(x - 1, y - 1, 3, 2.3), hl=1)
    c.speckle(ellipse(11, 19, 5, 4), '#f8ecc8', 0.15, seed=4)
    herbs(c, 22, 16, seed=3, n=2)


@item('poulet_frit')
def poulet_frit(c):
    basket = polygon([(4, 16), (28, 16), (25, 29), (7, 29)])
    c.shape(basket, R('#c8302a'), L_horiz(4, 28, 0.9, 0.4))
    c.shape(rect(4, 15, 28, 17), R('#f4f0e8'), L_const(0.8))
    for (x0, y0, x1, y1) in [(9, 13, 6, 5), (16, 12, 17, 3), (22, 13, 26, 5)]:
        m = drumstick(c, x0, y0, x1, y1, '#d8902a', 4.2)
        c.speckle(m, '#a8601a', 0.25, seed=int(x0))
        c.speckle(m, '#f0c060', 0.12, seed=int(y0))


@item('porc_au_caramel')
def porc_au_caramel(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#f2eee6')
    rice = ellipse(16, 14.5, 11, 4)
    c.shape(rice, R('#f6f3ea'), L_const(0.8), edge=False)
    for (x, y) in [(10, 13), (14, 12), (18, 13), (22, 14), (13, 15), (19, 15.5)]:
        m = rrect(x - 2, y - 1, x + 1, y + 1, 1)
        c.shape(m, R('#8a3a10'), L_sphere(x - 1, y - 1, 2, 2), hl=1)
    herbs(c, 16, 12, seed=4, n=3)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')


@item('boulettes_sauce_tomate')
def boulettes_sauce_tomate(c):
    def main(c):
        for (x, y) in [(11, 17), (17, 16), (22, 19), (14, 21), (20, 22)]:
            blob(c, x, y, 3, 2.7, '#7a3a1e', hl=1)
        basil(c, 8, 21, 2.8, -0.4)
        parmesan(c, ellipse(16, 19, 7, 3), 4)
    saucy_plate(c, '#c8301a', main)


@item('chili_con_carne')
def chili_con_carne(c):
    def top(c, m):
        c.speckle(m, '#5a2010', 0.3, seed=2)
        for (x, y) in rng_pts(3, m, 9):
            c.shape(ellipse(x, y, 1.4, 1), R('#8a1a24'), L_const(0.6), edge=True)
        c.shape(ellipse(18, 13, 3.4, 1.8), R('#fbfaf2'), L_const(0.85))
        c.shape(capsule(9, 13, 13, 12, 0.9), R('#d8141a'), L_const(0.7))
    soup(c, '#c85a2a', '#9a3a18', top)


@item('roti_de_boeuf')
def roti_de_boeuf(c):
    plate(c)
    roast = capsule(8, 17, 17, 15, 5)
    c.shape(roast, R('#6a3418'), L_cyl(8, 17, 17, 15, 5), hl=1)
    for k in range(3):
        c.fill(line_mask(9 + k * 3, 12, 10 + k * 3, 21) & roast, '#f4ecdc')
    slices_fan(c, 21, 20, 3, '#6a3418', '#d86a6a', w=3, h=4.5, step=2.8)


@item('boeuf_bourguignon')
def boeuf_bourguignon(c):
    op = cocotte(c, '#8a2a2a', content='#5a2414', top=13)
    chunks(c, op, ['#6a2e18', '#7a3a1e'], n=5, seed=2, r=(1.8, 2.4))
    for (x, y) in [(10, 13), (21, 12)]:
        c.shape(capsule(x - 1, y, x + 1, y, 1), R('#f07a1a'), L_const(0.7))
    for (x, y) in [(15, 12), (18, 14)]:
        c.shape(ellipse(x, y, 1.6, 1.2), R('#c8a070'), L_const(0.7))
    herbs(c, 13, 14, seed=5, n=2)


@item('blanquette_de_veau')
def blanquette_de_veau(c):
    op = cocotte(c, '#f2eee6', content='#f4ecd0', top=13)
    chunks(c, op, ['#f0dcc8', '#e8d0b8'], n=5, seed=4, r=(1.6, 2.2))
    for (x, y) in [(11, 12), (20, 14)]:
        c.shape(ellipse(x, y, 1.6, 1.2), R('#e8e0d0'), L_const(0.7))
    c.shape(capsule(16, 14, 18, 13, 1), R('#f07a1a'), L_const(0.7))
    leaf(c, 21, 12, 25, 10, 2.4, '#5a7a3a', vein=False)


@item('poulet_au_curry')
def poulet_au_curry(c):
    def top(c, m):
        chunks(c, m, ['#f0c890', '#e8b878'], n=5, seed=6, r=(1.5, 2))
        leaf(c, 15, 13, 19, 10, 2.6, '#3aa02a', vein=False)
        c.fill(arc_band(16, 14, 5, 2, 1, 180, 300) & m, '#f8e0a0')
    soup(c, '#2a4a7a', '#e89a20', top)
    rice_mound(c, 25, 25, 5, 3)


@item('cordon_bleu')
def cordon_bleu(c):
    plate(c)
    m = ellipse(14, 19, 10, 5.5)
    c.shape(m, R('#d8902a'), L_sphere(11, 16, 10, 6, 0.4), hl=2)
    c.speckle(m, '#a8601a', 0.25, seed=3)
    cut = polygon([(18, 14), (24, 17), (24, 24), (18, 22)])
    c.erase(cut)
    c.shape(polygon([(18, 15), (22, 17), (22, 23), (18, 22)]), R('#f0e8d8'), L_const(0.8))
    c.shape(rect(18, 18, 22, 19), R('#e8909a'), L_const(0.7), edge=False)
    c.shape(polygon([(19, 19), (23, 19), (25, 24), (21, 23)]), R('#f8d850'), L_const(0.8))
    lemon_wedge(c, 8, 13)


@item('magret_de_canard')
def magret_de_canard(c):
    plate(c)
    for k in range(5):
        x = 8 + k * 3.2
        m = ellipse(x, 19 - k * 0.3, 2.2, 4.5)
        c.shape(m, R('#e0a860'), L_const(0.6))
        c.shape(ellipse(x + 0.4, 19.4 - k * 0.3, 1.4, 3.4), R('#d0505a'), L_const(0.7), edge=False)
    for (x, y) in [(24, 18), (23, 23)]:
        c.shape(ellipse(x, y, 3, 2.5), R('#5a2a5a'), L_const(0.5))
        c.shape(ellipse(x, y - 0.3, 2, 1.7), R('#d84a6a'), L_const(0.7), edge=False)
    drizzle(c, [(6, 24), (12, 24), (18, 23)], '#6a2a1a')


@item('gigot_de_mouton')
def gigot_de_mouton(c):
    plate(c)
    c.shape(capsule(21, 15, 28, 10, 1.6) | circle(28.5, 9.5, 1.8), R('#f4ecdc'), L_const(0.8))
    m = capsule(9, 19, 21, 15, 6.5, 3)
    c.shape(m, R('#9a5028'), L_sphere(8, 15, 10, 8, 0.4), hl=2)
    glaze(c, m, '#9a5028', 6)
    for (x, y) in [(6, 23), (11, 24), (18, 23)]:
        blob(c, x, y, 2.3, 1.8, '#e8c060', hl=1)
    sprig(c, 20, 23, 26, 21, '#3a6a5a', n=2, lw=1.5, ll=2)


@item('navarin_de_mouton')
def navarin_de_mouton(c):
    op = cocotte(c, '#3a5a3a', content='#8a4a24', top=13)
    chunks(c, op, ['#7a3a20'], n=4, seed=3, r=(1.8, 2.3))
    for (x, y) in [(10, 13), (21, 14)]:
        c.shape(ellipse(x, y, 1.6, 1.3), R('#f4f0e0'), L_const(0.7))
    c.shape(capsule(17, 12, 19, 12, 1), R('#f07a1a'), L_const(0.7))
    for (x, y) in [(13, 14), (15, 12), (19, 14)]:
        pea(c, x, y)


@item('lapin_a_la_moutarde')
def lapin_a_la_moutarde(c):
    def main(c):
        m = capsule(10, 20, 19, 15, 4.5, 2.2)
        c.shape(m, R('#d8a060'), L_sphere(9, 16, 7, 6, 0.4), hl=2)
        c.shape(capsule(19, 15, 23, 13, 1), R('#f4ecdc'), L_const(0.8))
        c.speckle(ellipse(16, 20, 9, 4), '#8a6a18', 0.1, seed=3)
        sprig(c, 21, 23, 26, 21, '#6a8a5a', n=2, lw=1.4, ll=2)
    saucy_plate(c, '#e8c040', main)


@item('tajine_de_poulet_au_citron')
def tajine_de_poulet_au_citron(c):
    base = ellipse(16, 24, 14, 5)
    c.shape(base, R('#b8502a'), L_vert(19, 29, 0.9, 0.4))
    c.fill(arc_band(16, 24, 13, 4.5, 1, 20, 160), '#f0c040')
    lid = polygon([(4, 23), (28, 23), (18, 9), (14, 9)]) | ellipse(16, 23, 12, 3)
    c.shape(lid, R('#c85a2a'), L_horiz(4, 28, 0.9, 0.35), hl=2)
    for y, cc in [(19, '#2a6ab0'), (15, '#f0c040')]:
        c.fill(line_mask(8 + (23 - y) * 0.4, y, 24 - (23 - y) * 0.4, y) & lid, cc)
    c.shape(circle(16, 7, 2.6) | rect(15, 7, 17, 10), R('#c85a2a'), L_sphere(15, 6, 2.6, 2.6))
    slice_round(c, 26, 26, 3.2, '#e8c020', '#f8ec80', motif='citrus')


@item('dinde_farcie')
def dinde_farcie(c):
    plate(c, rx=15, ry=9.5)
    for s in (-1, 1):
        drumstick(c, 16 + s * 7, 18, 16 + s * 12, 12, '#b8661e', 4)
    body = ellipse(16, 17, 10.5, 7.5)
    c.shape(body, R('#b8661e'), L_sphere(13, 13, 11, 8, 0.4), hl=3)
    glaze(c, body, '#b8661e', 10)
    c.shape(ellipse(16, 23.5, 4, 1.8), R('#b8864a'), L_const(0.6))
    c.speckle(ellipse(16, 23.5, 4, 1.8), '#4a8a3a', 0.3, seed=2)
    for (x, y) in [(5, 23), (27, 23)]:
        c.shape(circle(x, y, 1.8), R('#c81a2a'), L_const(0.7))


@item('canard_a_l_orange')
def canard_a_l_orange(c):
    plate(c)
    sauce_pool(c, 15, 20, 9, 4.5, '#e8801a')
    for k in range(4):
        x = 9 + k * 3.4
        m = ellipse(x, 19, 2.3, 4.2)
        c.shape(m, R('#c8803a'), L_const(0.6))
        c.shape(ellipse(x + 0.4, 19.4, 1.4, 3.1), R('#c84a50'), L_const(0.7), edge=False)
    slice_round(c, 24, 17, 3.4, '#e8781a', '#f8a830', motif='citrus')
    slice_round(c, 23, 23, 3, '#e8781a', '#f8a830', motif='citrus')


@item('civet_de_sanglier')
def civet_de_sanglier(c):
    op = cocotte(c, '#2a2a2a', content='#4a1a14', top=13)
    chunks(c, op, ['#5a2418', '#6a2e1e'], n=5, seed=8, r=(1.8, 2.4))
    for (x, y) in [(11, 12), (20, 14)]:
        c.shape(ellipse(x, y, 1.6, 1.2), R('#a88058'), L_const(0.7))
    leaf(c, 16, 13, 22, 9, 3, '#5a7a3a')


@item('roti_de_cerf_aux_myrtilles')
def roti_de_cerf_aux_myrtilles(c):
    plate(c)
    slices_fan(c, 13, 19, 4, '#4a2018', '#9a2a3a', w=3, h=5, step=3.2)
    sauce_pool(c, 23, 21, 4.5, 3, '#3a2a6a')
    for (x, y) in [(22, 20), (24, 22), (25, 19), (21, 22)]:
        c.shape(circle(x, y, 1.3), R('#4a4aa0'), L_const(0.7))
    sprig(c, 6, 14, 10, 12, '#6a8a5a', n=2, lw=1.4, ll=2)


@item('couscous_royal')
def couscous_royal(c):
    plate(c, rx=15, ry=9.5, color='#2a6ab0')
    dome = ellipse(16, 18, 10, 6) | ellipse(16, 16, 7, 5)
    c.shape(dome, R('#f0d070'), L_sphere(14, 14, 10, 7, 0.3))
    c.speckle(dome, '#d8b050', 0.25, seed=2)
    sausage(c, 7, 22, 13, 24, 1.8, '#9a3a1e')
    sausage(c, 20, 24, 26, 21, 1.8, '#9a3a1e')
    drumstick(c, 19, 15, 23, 11, '#c8781e', 3)
    for (x, y, cc) in [(10, 15, '#f07a1a'), (13, 19, '#3a8a2a'), (22, 18, '#f07a1a')]:
        c.shape(capsule(x - 1.5, y, x + 1.5, y - 1, 1.1), R(cc), L_const(0.7))
    for (x, y) in [(16, 21), (18, 19), (11, 18)]:
        c.shape(circle(x, y, 1.2), R('#e0c088'), L_const(0.7))


@item('cote_de_boeuf_grillee')
def cote_de_boeuf_grillee(c):
    wood_board(c, 1, 13, 30, 28)
    c.shape(capsule(22, 15, 29, 10, 2), R('#f4ecdc'), L_const(0.8))
    m = ellipse(13, 19, 11, 6)
    c.shape(m, R('#f0dcc0'), L_const(0.6))
    steak(c, 13, 18.5, 9.5, 5, '#6a2e14')
    sprig(c, 4, 26, 12, 26, '#3a6a5a', n=3, lw=1.5, ll=2)
    for (x, y) in [(22, 24), (26, 22)]:
        c.fill(rect(x, y, x + 1, y), '#ffffff')


# ============================================================================ poissons & fruits de mer
@item('poisson_pane')
def poisson_pane(c):
    plate(c)
    for (x, y) in [(11, 17), (18, 21)]:
        m = rrect(x - 6, y - 2, x + 6, y + 2, 2)
        c.shape(m, R('#d8902a'), L_vert(y - 2, y + 2, 0.9, 0.4), hl=1)
        c.speckle(m, '#a8601a', 0.25, seed=x)
    lemon_wedge(c, 24, 15)
    c.shape(ellipse(7, 22, 2.8, 1.8), R('#f4f0dc'), L_const(0.8))
    c.speckle(ellipse(7, 22, 2.6, 1.6), '#4a8a3a', 0.3, seed=3)


@item('saumon_grille')
def saumon_grille(c):
    plate(c)
    m = rrect(6, 15, 22, 23, 3)
    c.shape(m, R('#f07a50'), L_sphere(10, 15, 10, 6, 0.6), hl=2)
    for k in range(3):
        c.fill(line_mask(8 + k * 5, 22, 12 + k * 5, 15) & m & ~edge(m), '#6a2a14')
    c.shape(rect(6, 21, 22, 23) & m, R('#9aa0a8'), L_const(0.5), edge=False)
    slice_round(c, 25, 18, 3.2, '#e8c020', '#f8ec80', motif='citrus')
    leaf(c, 12, 16, 16, 13, 2.6, '#4a9a3a', vein=False)


@item('sardines_grillees')
def sardines_grillees(c):
    plate(c)
    for k, y in enumerate((16, 20, 24)):
        b = fish_body(c, 5, y, 24, y - 2, 4.8, '#4a5a6a', '#c8ccd0', fin='#5a6a7a', seed=k)
        for j in range(3):
            c.fill(line_mask(9 + j * 5, y + 2, 11 + j * 5, y - 3) & b & ~edge(b), '#2a2020')
    lemon_wedge(c, 26, 15)


@item('maquereau_au_four')
def maquereau_au_four(c):
    inner = gratin_dish(c, '#c8482a', color='#e8e0d0')
    b = fish_body(c, 7, 19, 24, 17, 6, '#2a6a6a', '#e8ecec', fin='#3a7a7a', seed=3)
    for k in range(5):
        c.fill(line_mask(10 + k * 2.8, 15, 11 + k * 2.8, 18) & b & ~edge(b), '#102a2a')
    for (x, y) in [(8, 15), (22, 21)]:
        slice_round(c, x, y, 2.2, '#c8281a', '#e8483a', motif='seeds')
    sprig(c, 14, 22, 19, 22, '#6a8a5a', n=2, lw=1.2, ll=1.6)


@item('fish_and_chips')
def fish_and_chips(c):
    paper = polygon([(2, 14), (29, 12), (27, 29), (5, 29)])
    c.shape(paper, R('#e8e0c8'), L_const(0.7))
    for k in range(4):
        c.fill(line_mask(6, 19 + k * 2.5, 25, 18 + k * 2.5) & paper & ~edge(paper), '#b8b0a0')
    fries(c, 17, 20, n=5, length=9)
    m = ellipse(11, 15, 8, 5) | ellipse(6, 16, 4, 4)
    c.shape(m, R('#e0a040'), L_sphere(9, 12, 9, 6, 0.3), hl=2)
    c.speckle(m, '#b8702a', 0.2, seed=2)
    lemon_wedge(c, 23, 25)


@item('saumon_en_papillote')
def saumon_en_papillote(c):
    paper = ellipse(16, 20, 14.5, 9) | polygon([(2, 20), (8, 9), (24, 9), (30, 20)])
    c.shape(paper, R('#f0e8d0'), L_vert(9, 29, 0.95, 0.5))
    inner = ellipse(16, 19, 11, 6)
    c.shape(inner, R('#e0d8c0'), L_const(0.5), edge=False)
    m = rrect(8, 16, 20, 22, 2)
    c.shape(m, R('#f08a60'), L_sphere(11, 16, 8, 5, 0.6), hl=1)
    for k in range(2):
        c.shape(capsule(19, 16 + k * 3, 25, 15 + k * 3, 1), R('#6aa03a'), L_const(0.7))
    slice_round(c, 12, 17, 2.5, '#e8c020', '#f8ec80', motif='citrus')
    leaf(c, 16, 18, 19, 15, 2.2, '#4a9a3a', vein=False)
    c.fill(line_mask(2, 20, 8, 22) | line_mask(30, 20, 24, 22), '#d8ccb0')


@item('truite_aux_amandes')
def truite_aux_amandes(c):
    plate(c)
    fish_body(c, 4, 20, 25, 17, 8, '#9a7a4a', '#d8b888', fin='#8a6a3a', seed=5)
    for (x, y) in rng_pts(4, ellipse(14, 17, 7, 2.5), 7):
        c.fill(rect(x, y, x + 1, y), '#f4e4c0')
        c.fill(rect(x, y + 1, x + 1, y + 1), '#c8a070')
    lemon_wedge(c, 25, 24)


@item('moules_marinieres')
def moules_marinieres(c):
    op = cocotte(c, '#1a1a1a', content='#e8dcb0', top=13)
    for (x, y) in [(9, 12), (13, 11), (17, 12), (21, 11), (11, 14), (16, 14), (20, 14), (24, 13)]:
        mussel(c, x, y)
    herbs(c, 15, 12, seed=4, n=3)


@item('moules_frites')
def moules_frites(c):
    op = cocotte(c, '#1a1a1a', content='#e8dcb0', x0=2, x1=19, top=15, bottom=28)
    for (x, y) in [(6, 14), (10, 13), (14, 14), (8, 16), (13, 16)]:
        mussel(c, x, y)
    cone = paper_cone(c, '#e8e0c8', '#c8c0a8', x0=19, x1=30, top=12, bottom=28)
    fries(c, 20, 13, n=4, length=8)


@item('crevettes_sautees_a_l_ail')
def crevettes_sautees_a_l_ail(c):
    inner = pan(c)
    for (x, y) in [(9, 15), (15, 14), (21, 16), (12, 19), (18, 20)]:
        shrimp_small(c, x, y, 1.1)
    c.speckle(inner, '#f4f0d0', 0.08, seed=3)
    herbs(c, 14, 17, seed=5, n=3)


@item('calamars_frits')
def calamars_frits(c):
    plate(c)
    for (x, y) in [(10, 17), (16, 15.5), (21, 18), (13, 21), (19, 22), (8, 21)]:
        m = arc_band(x, y, 3.3, 2.6, 1.6)
        c.shape(m, R('#e8b050'), L_const(0.7), edge=True)
        c.speckle(m, '#b8782a', 0.25, seed=int(x))
    lemon_wedge(c, 25, 23)


@item('thon_mi_cuit_au_sesame')
def thon_mi_cuit_au_sesame(c):
    plate(c, color='#2a2a2a')
    for k in range(5):
        x = 7.5 + k * 3.6
        m = rect(int(x) - 1, 14, int(x) + 1, 23) | ellipse(x, 14, 1.6, 1)
        c.shape(m, R('#c8323a'), L_const(0.6), edge=True)
        c.fill(rect(int(x) - 1, 14, int(x) + 1, 15), '#f0e0b8')
        c.fill(rect(int(x) - 1, 22, int(x) + 1, 23), '#f0e0b8')
        c.fill(rect(int(x), 17, int(x), 20), '#e8606a')
    drizzle(c, [(6, 25), (14, 24), (22, 25)], '#3a1a0a')


@item('brandade_de_morue')
def brandade_de_morue(c):
    inner = gratin_dish(c, '#f4ecd8', crust='#d8a050', color='#e8e0d0')
    c.speckle(inner, '#e8b870', 0.1, seed=6)
    herbs(c, 12, 16, seed=2)
    herbs(c, 20, 18, seed=3)


@item('sushi_au_saumon')
def sushi_au_saumon(c):
    wood_board(c, 1, 14, 30, 27, '#c8a070')
    for x in (7, 16, 25):
        r = rrect(x - 4, 16, x + 3, 21, 2)
        c.shape(r, R('#f8f6ee'), L_const(0.8))
        c.speckle(r, '#e8e4d8', 0.2, seed=x)
        f = rrect(x - 5, 13, x + 4, 17, 2)
        c.shape(f, R('#f07a50'), L_sphere(x - 2, 13, 5, 3, 0.6), hl=1)
        for k in range(3):
            c.fill(line_mask(x - 4 + k * 3, 17, x - 2 + k * 3, 13) & f & ~edge(f), '#f8c0a0')
    c.shape(ellipse(4, 24, 2, 1.4), R('#7ab83a'), L_const(0.7))
    c.shape(ellipse(27, 24, 2.4, 1.4), R('#f0b0b0'), L_const(0.7))


@item('ceviche')
def ceviche(c):
    body, ly = glass(c, 5, 26, 12, 26, '#e8e8c0', level=0.75, taper=1.5)
    c.shape(rect(13, 26, 18, 29) | ellipse(15.5, 29, 6, 1.5), R('#dcecf2'), L_const(0.6))
    top = rect(6, int(ly) - 3, 25, int(ly))
    cubes(c, top, ['#fbfaf2', '#f4f0e8'], n=10, seed=3, s=2)
    cubes(c, top, ['#a83a7a'], n=4, seed=5, s=1)
    herbs(c, 12, int(ly) - 3, seed=4, n=3)
    lemon_wedge(c, 25, int(ly) - 3, '#6ab02a')


@item('curry_de_crevettes')
def curry_de_crevettes(c):
    def top(c, m):
        for (x, y) in [(10, 13), (16, 12), (21, 14)]:
            shrimp_small(c, x, y, 0.9)
        c.fill(arc_band(16, 14, 5, 2, 1, 200, 320) & m, '#f8d080')
        leaf(c, 14, 14, 17, 16, 2.4, '#3aa02a', vein=False)
    soup(c, '#f2eee6', '#e8781a', top)
    rice_mound(c, 25, 25, 5, 3)


@item('paella')
def paella(c):
    c.shape(capsule(1, 18, 4, 18, 1.5) | capsule(28, 18, 31, 18, 1.5), R('#2a2a2a'), L_const(0.5))
    body = ellipse(16, 19, 13.5, 9)
    c.shape(body, R('#3a3a3e'), L_const(0.4))
    inner = ellipse(16, 18.5, 12, 7.8)
    c.shape(inner, R('#f0b020'), L_sphere(14, 16, 12, 8, 0.4), edge=False)
    c.speckle(inner, '#f8d060', 0.2, seed=2)
    for (x, y) in [(9, 17), (20, 15), (14, 22)]:
        shrimp_small(c, x, y, 1.0)
    for (x, y) in [(15, 15), (22, 20)]:
        mussel(c, x, y)
    for (x, y) in [(11, 20), (18, 19), (24, 17)]:
        pea(c, x, y)
    c.shape(capsule(7, 21, 10, 22, 0.9), R('#d8281c'), L_const(0.7))
    lemon_wedge(c, 19, 23)


@item('bouillabaisse')
def bouillabaisse(c):
    def top(c, m):
        mussel(c, 10, 13)
        shrimp_small(c, 20, 13, 1)
        chunks(c, m, ['#f8f0e4'], n=2, seed=3, r=(1.6, 2))
        c.shape(circle(15, 12, 2), R('#e04a2a'), L_const(0.7))
        herbs(c, 17, 15, seed=4, n=2)
    soup(c, '#2a5a8a', '#d8581a', top)
    bread_slice(c, 22, 20, 30, 26, toast=0.4)


# ============================================================================ pâtes, riz, céréales
def pasta_plate(c, pasta='#f0d070', sauce=None, extra=None):
    plate(c)
    nest = ellipse(16, 18.5, 9.5, 5.5)
    noodles(c, nest, pasta, n=6, dark=R(pasta)[1])
    c.fill(edge(nest) & ~(c.opaque() & False), R(pasta)[1])
    if sauce:
        sauce(c)
    if extra:
        extra(c)
    return nest


@item('pates_au_beurre')
def pates_au_beurre(c):
    def extra(c):
        top = polygon([(13, 15), (17, 13), (20, 15), (16, 17)])
        c.shape(top, R('#f8e070'), L_const(0.85), hl=1)
        c.shape(polygon([(13, 15), (16, 17), (16, 18), (13, 16)]), R('#e8c848'), L_const(0.5))
        c.fill(points([(10, 20), (22, 19), (14, 22)]), '#2a2a2a')
    pasta_plate(c, '#f0d070', extra=extra)


@item('onigiri')
def onigiri(c):
    for (x, y, s) in [(10, 17, 1.0), (21, 19, 1.0)]:
        m = polygon([(x - 8 * s, y + 7), (x, y - 9 * s), (x + 8 * s, y + 7)]) | ellipse(x, y + 5, 7.8 * s, 3)
        c.shape(m, R('#fbfaf4'), L_sphere(x - 3, y - 3, 9, 10, 0.5), hl=2)
        c.speckle(m, '#e8e4d8', 0.15, seed=int(x))
        nori = rect(int(x - 4), int(y + 2), int(x + 3), int(y + 9)) & m
        c.shape(nori, R('#1a2a1a'), L_const(0.4))


@item('porridge')
def porridge(c):
    def top(c, m):
        c.speckle(m, '#c8b088', 0.35, seed=2)
        drizzle(c, [(9, 13), (13, 14), (17, 12), (22, 14)], '#e8a018')
        for (x, y) in [(19, 13), (12, 12)]:
            c.shape(circle(x, y, 1.4), R('#3a3a8a'), L_const(0.7))
    soup(c, '#6a8ab0', '#e8dcc0', top)


@item('spaghetti_bolognaise')
def spaghetti_bolognaise(c):
    def sauce(c):
        m = ellipse(16, 16.5, 5.5, 3.2)
        c.shape(m, R('#a8321a'), L_sphere(14, 15, 6, 3.4), hl=1)
        c.speckle(m, '#6a2010', 0.35, seed=4)
        parmesan(c, m, 3)
        basil(c, 17, 15, 2.6, -0.6)
    pasta_plate(c, '#f0cc60', sauce=sauce)


@item('carbonara')
def carbonara(c):
    def sauce(c):
        cubes(c, ellipse(16, 18, 8, 4), ['#e8a0a0', '#d88080'], n=8, seed=3)
        c.shape(circle(16, 17, 2.4), R('#f4b020'), L_sphere(15, 16, 2.4, 2.4), hl=1)
        c.speckle(ellipse(16, 18, 9, 5), '#2a2a2a', 0.1, seed=6)
    pasta_plate(c, '#f4dc80', sauce=sauce)


@item('pates_au_pesto')
def pates_au_pesto(c):
    def sauce(c):
        c.speckle(ellipse(16, 18.5, 9, 5), '#2a6a1a', 0.2, seed=3)
        basil(c, 15, 16, 3, -0.5)
        parmesan(c, ellipse(16, 18, 8, 4), 4)
    pasta_plate(c, '#9ac050', sauce=sauce)


@item('gratin_de_macaronis')
def gratin_de_macaronis(c):
    inner = gratin_dish(c, '#f0c850', crust='#b86a1a', color='#c8502a')
    for (x, y) in rng_pts(3, inner & ~edge(inner), 10):
        c.shape(arc_band(x, y, 1.8, 1.4, 1, 180, 360) & inner, R('#f4dc80'), L_const(0.7), edge=False)


@item('riz_cantonais')
def riz_cantonais(c):
    dome = heap_in_bowl(c, '#f2eee6', '#f4ecd0')
    grains(c, dome, '#ffffff', 14, seed=2)
    cubes(c, dome, ['#f4c830', '#e8a0a0'], n=8, seed=3)
    for (x, y) in rng_pts(5, dome, 6):
        pea(c, x, y)
    herbs(c, 16, 11, seed=2, n=2)
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')
    chopsticks(c, 22, 1, 30, 12)


@item('riz_au_curry_de_legumes')
def riz_au_curry_de_legumes(c):
    plate(c)
    rice_mound(c, 11, 18, 6, 4)
    sauce_pool(c, 20, 20, 7, 4.2, '#e8901a')
    cubes(c, ellipse(20, 20, 6, 3.5), ['#3a8a2a', '#f4f0e0'], n=5, seed=4)
    for (x, y) in [(18, 19), (22, 21)]:
        c.shape(circle(x, y, 1.2), R('#e0c088'), L_const(0.7))
    leaf(c, 11, 15, 14, 12, 2.4, '#3aa02a', vein=False)


@item('nouilles_sautees_aux_legumes')
def nouilles_sautees_aux_legumes(c):
    box = polygon([(5, 12), (27, 12), (24, 29), (8, 29)])
    c.shape(box, R('#f4f0e8'), L_horiz(5, 27, 0.95, 0.4))
    c.fill(line_mask(8, 22, 24, 22), '#c8302a')
    top = ellipse(16, 12, 11, 4) | ellipse(16, 10, 8, 3.5)
    c.shape(top, R('#d8a040'), L_const(0.6), edge=False)
    noodles(c, top, '#e0b050', n=4, dark='#a86a20')
    cubes(c, top, ['#f07a1a', '#6ab04a', '#c8e090'], n=6, seed=3)
    chopsticks(c, 18, 1, 28, 12)


@item('galette_complete')
def galette_complete(c):
    plate(c)
    sq = polygon([(5, 18), (16, 12), (27, 18), (16, 25)])
    c.shape(sq, R('#9a6030'), L_sphere(13, 15, 12, 8, 0.6), hl=1)
    c.speckle(sq, '#6a3a18', 0.15, seed=3)
    inner = polygon([(9, 18), (16, 14.5), (23, 18), (16, 22)])
    c.shape(inner, R('#e8a0a0'), L_const(0.7), edge=False)
    c.shape(ellipse(16, 18, 4.5, 2.6), R('#fbfaf2'), L_const(0.85), edge=False)
    c.shape(circle(16, 17.5, 1.8), R('#f4a818'), L_sphere(15.5, 17, 1.8, 1.8), hl=1)
    c.speckle(inner, '#f8d860', 0.15, seed=5)


@item('salade_de_quinoa')
def salade_de_quinoa(c):
    dome = heap_in_bowl(c, '#b8844a', '#e8d8a8')
    c.speckle(dome, '#b84a30', 0.1, seed=2)
    cubes(c, dome, ['#a8c848', '#d82a1a', '#6ab04a'], n=10, seed=4)
    bowl_rim(c, 16, 15, 13, 5, '#b8844a')
    lemon_wedge(c, 26, 11)


@item('lasagnes')
def lasagnes(c):
    plate(c)
    front = polygon([(5, 14), (21, 17), (21, 26), (5, 23)])
    side = polygon([(21, 17), (27, 14), (27, 23), (21, 26)])
    top = polygon([(5, 14), (11, 11), (27, 14), (21, 17)])
    for m, L in ((front, 0.6), (side, 0.4)):
        c.shape(m, R('#f0d080'), L_const(L))
    for k, cc in enumerate(['#c83a1a', '#f4ecd0', '#c83a1a', '#f4ecd0']):
        y = 16 + k * 2.2
        c.fill(line_mask(5, y, 21, y + 3) & front, cc)
        c.fill(line_mask(21, y + 3, 27, y) & side, R(cc)[1])
    c.shape(top, R('#e8a840'), L_const(0.8), hl=1)
    c.speckle(top, '#b86a1a', 0.25, seed=3)
    basil(c, 14, 13, 2.4, -0.5)


@item('raviolis_epinards_fromage')
def raviolis_epinards_fromage(c):
    plate(c)
    for (x, y) in [(10, 16), (18, 15), (13, 21), (21, 21)]:
        m = rrect(x - 3, y - 3, x + 3, y + 2, 1)
        c.shape(m, R('#f0d888'), L_sphere(x - 1, y - 1, 3, 3, 0.4), hl=1)
        for k in range(-3, 4, 2):
            c.fill(rect(x + k, y - 3, x + k, y - 3), R('#f0d888')[1])
    drizzle(c, [(7, 19), (12, 18), (17, 19), (24, 18)], '#f8f0d0')
    basil(c, 22, 16, 2.6, -1.2, '#2e7426')


@item('gnocchis_au_beurre')
def gnocchis_au_beurre(c):
    plate(c)
    for (x, y) in [(9, 17), (14, 16), (19, 16), (23, 18), (11, 21), (16, 20.5), (21, 21.5)]:
        m = ellipse(x, y, 2.6, 1.9)
        c.shape(m, R('#f0d8a0'), L_sphere(x - 1, y - 1, 2.6, 2, 0.4), hl=1)
        c.fill(rect(int(x) - 1, int(y), int(x) + 1, int(y)), '#d8b878')
    for (x, y) in [(12, 19), (20, 19)]:
        leaf(c, x, y, x + 2, y - 3, 2.4, '#6a8a5a', vein=False)


@item('risotto_aux_champignons')
def risotto_aux_champignons(c):
    plate(c)
    m = ellipse(16, 18.5, 9.5, 5.5)
    c.shape(m, R('#f0e0b0'), L_sphere(14, 16, 10, 6, 0.4), hl=1)
    grains(c, m, '#fbf4d8', 20, seed=4)
    for (x, y) in [(12, 17), (19, 16), (16, 20)]:
        c.shape(ellipse(x, y, 2, 1.4) & (YY <= y + 0.3), R('#a87a4a'), L_const(0.6))
        c.shape(rect(x, int(y), x, int(y) + 1), R('#e8d8b8'), L_const(0.8), edge=False)
    parmesan(c, m, 3)
    herbs(c, 21, 20, seed=2, n=2)


@item('pad_thai')
def pad_thai(c):
    plate(c)
    nest = ellipse(16, 18.5, 9.5, 5.5)
    noodles(c, nest, '#e8b868', n=6, dark='#b8803a')
    for (x, y) in [(11, 17), (20, 17), (15, 20)]:
        shrimp_small(c, x, y, 0.9)
    c.speckle(nest, '#c8904a', 0.12, seed=5)
    c.speckle(nest, '#f4e070', 0.08, seed=6)
    herbs(c, 17, 16, seed=3, n=2)
    lemon_wedge(c, 25, 22, '#6ab02a')


@item('bibimbap')
def bibimbap(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#3a3030', foot=False)
    inner = ellipse(16, 14.5, 11, 4)
    for (a0, a1, cc) in [(180, 240, '#f07a1a'), (240, 300, '#3a8a2a'), (300, 360, '#8a4a3a'), (0, 90, '#f4ecd0'), (90, 180, '#c83a1a')]:
        c.shape(inner & arc_band(16, 14.5, 11, 4, 11, a0, a1), R(cc), L_const(0.7), edge=False)
    c.shape(circle(16, 14, 2.5), R('#fbfaf2'), L_const(0.85), edge=False)
    c.shape(circle(16, 14, 1.4), R('#f4a818'), L_const(0.8), edge=False)
    c.speckle(inner, '#f8ecc8', 0.05, seed=3)
    bowl_rim(c, 16, 15, 13, 5, '#3a3030')


@item('ramen')
def ramen(c):
    def top(c, m):
        noodles(c, ellipse(13, 14, 6, 2.5), '#f0d070', n=3)
        for (x, y) in [(19, 13), (22, 14.5)]:
            c.shape(ellipse(x, y, 2.6, 1.8), R('#d8a080'), L_const(0.7))
            c.fill(arc_band(x, y, 1.5, 1, 1), '#f4d8c0')
        egg_half(c, 11, 13, 0.7, yolk='#f49818')
        c.shape(rect(20, 10, 23, 13), R('#1a2a1a'), L_const(0.4))
        c.speckle(m, '#4aa03a', 0.1, seed=4)
    soup(c, '#c82a1a', '#d8a860', top)
    chopsticks(c)


# ============================================================================ street food
@item('sandwich_jambon_beurre')
def sandwich_jambon_beurre(c):
    bot = capsule(3, 23, 28, 16, 4.2)
    c.shape(bot, R('#d8903a'), L_cyl(3, 23, 28, 16, 4.2))
    ham = capsule(4, 20, 28, 13, 2.8)
    c.shape(ham, R('#e8909a'), L_const(0.7))
    c.fill(line_mask(4, 21, 28, 14), '#f8f0c8')
    top = capsule(4, 17, 27, 11, 4)
    c.shape(top, R('#d8903a'), L_cyl(4, 17, 27, 11, 4), hl=2)
    for k in range(3):
        x, y = 9 + k * 6, 15 - k * 1.7
        c.fill(line_mask(x - 1, y + 1, x + 2, y - 2) & top & ~edge(top), '#f4dca0')


@item('hot_dog')
def hot_dog(c):
    bun = capsule(4, 22, 27, 16, 5)
    c.shape(bun, R('#d8903a'), L_cyl(4, 22, 27, 16, 5))
    c.shape(capsule(5, 19, 26, 13.5, 3), R('#f0dcb0'), L_const(0.7), edge=False)
    s = sausage(c, 2, 19, 29, 12, 2.8, '#b8482a')
    pts = [(6, 17), (9, 15), (12, 17), (15, 14), (18, 16), (21, 13), (24, 15)]
    drizzle(c, pts, '#e8c020')
    drizzle(c, [(p[0], p[1] + 1.5) for p in pts[1:-1]], '#d01a1a')
    c.shape(capsule(4, 24, 27, 18, 3.4) & ~bun | (capsule(4, 24, 27, 18, 3.4) & (YY > 20 - (XX - 4) * 0.26)), R('#d8903a'), L_cyl(4, 24, 27, 18, 3.4))


@item('pizza_margherita')
def pizza_margherita(c):
    inner = pizza_top(c, cheese='#fbf6e8')
    for (x, y) in [(11, 14), (20, 15), (15, 20), (22, 20)]:
        basil(c, x, y, 2.6, -0.7)


@item('wrap_au_poulet')
def wrap_au_poulet(c):
    for (x0, y0, x1, y1) in [(3, 25, 17, 9), (14, 28, 28, 12)]:
        m = capsule(x0, y0, x1, y1, 4.5)
        c.shape(m, R('#f0d898'), L_cyl(x0, y0, x1, y1, 4.5), hl=1)
        c.speckle(m, '#c89a4a', 0.08, seed=x0)
        end = ellipse(x1, y1, 4.5, 3.2)
        c.shape(end, R('#f0d898'), L_const(0.6))
        inner = ellipse(x1, y1, 3.5, 2.3)
        c.shape(inner, R('#f4ecd8'), L_const(0.8), edge=False)
        cubes(c, inner, ['#e8b060', '#6ac03a', '#e8b060'], n=5, seed=x1, s=1)


@item('frites')
def frites(c):
    fries(c, 8, 18, n=8, length=12)
    box = polygon([(6, 14), (26, 14), (24, 29), (8, 29)])
    c.shape(box, R('#d82020'), L_horiz(6, 26, 0.9, 0.35))
    c.shape(box & (YY < 18) & (XX > 12) & (XX < 20) | rect(10, 20, 22, 20), R('#f4c020'), L_const(0.8), edge=False)
    c.fill(arc_band(16, 14, 10, 3, 1, 0, 180) & box, '#a81010')


@item('croque_monsieur')
def croque_monsieur(c):
    plate(c)
    for k, y in enumerate((20, 17)):
        m = polygon([(5, y + 2), (16, y - 3), (27, y + 1), (16, y + 6)])
        c.shape(m, R('#d8a050' if k == 0 else '#e8b848'), L_const(0.6 + k * 0.2), hl=k)
        if k == 1:
            c.speckle(m, '#b8702a', 0.25, seed=3)
            c.speckle(m, '#f8d880', 0.15, seed=4)
    c.shape(polygon([(5, 22), (16, 26), (16, 25.5), (5, 21.5)]), R('#e8909a'), L_const(0.7), edge=False)
    c.fill(line_mask(16, 25, 25, 22), '#f8d850')


@item('burger_classique')
def burger_classique(c):
    bun_bottom(c, 16, 25, 12)
    patty(c, 16, 21, 12)
    cheese_slice(c, 16, 20, 12)
    tomato_slice_side(c, 16, 19, 11)
    lettuce(c, 16, 17, 12)
    bun_top(c, 16, 16, 12, 10)


@item('burger_de_poulet_croustillant')
def burger_de_poulet_croustillant(c):
    bun_bottom(c, 16, 25, 11)
    m = rrect(3, 19, 29, 24, 2)
    c.shape(m, R('#d8902a'), L_vert(19, 24, 0.9, 0.4), hl=1)
    c.speckle(m, '#a8601a', 0.3, seed=2)
    c.shape(rect(6, 18, 26, 18), R('#fbf4d0'), L_const(0.8))
    lettuce(c, 16, 16, 12)
    bun_top(c, 16, 15, 11, 9.5, color='#e0a048')


@item('pizza_reine')
def pizza_reine(c):
    inner = pizza_top(c)
    for (x, y) in [(10, 15), (19, 13), (14, 20), (22, 19)]:
        c.shape(rrect(x - 2, y - 1, x + 2, y + 1, 1), R('#e8909a'), L_const(0.7))
    for (x, y) in [(15, 14), (21, 16), (10, 19), (18, 21)]:
        c.shape(ellipse(x, y, 1.8, 1.3) & (YY <= y + 0.3), R('#c8a070'), L_const(0.7))
        c.fill(rect(x, int(y), x, int(y) + 1), '#f0e0c0')
    olives(c, inner, 2, seed=5)


@item('pizza_quatre_fromages')
def pizza_quatre_fromages(c):
    inner = pizza_top(c, sauce='#f4ecd0', cheese='#f8d860')
    for (x, y, cc) in [(10, 15, '#fbfaf4'), (20, 14, '#f0c030'), (15, 20, '#e8e4dc'), (22, 20, '#f8e088')]:
        c.shape(ellipse(x, y, 3, 2), R(cc), L_const(0.8), edge=False)
    c.speckle(inner, '#3a5a8a', 0.05, seed=6)
    c.speckle(inner, '#c8802a', 0.08, seed=7)


@item('tacos')
def tacos(c):
    for (cx, cy) in [(10, 17), (22, 21)]:
        back = ellipse(cx, cy, 8, 8) & (YY >= cy)
        c.shape(back, R('#d8a040'), L_const(0.45))
        fill = ellipse(cx, cy + 0.5, 7, 3)
        c.shape(fill, R('#7a3a1e'), L_const(0.5))
        c.speckle(fill, '#5a2a14', 0.3, seed=int(cx))
        lettuce(c, cx, cy - 2, 7, '#7ac84a', seed=int(cx))
        cubes(c, ellipse(cx, cy - 1, 6, 2), ['#d82a1a', '#f4d860'], n=5, seed=int(cy), s=1)
        front = ellipse(cx, cy + 1, 8.5, 7.5) & (YY >= cy + 1.5)
        c.shape(front, R('#f0c050'), L_sphere(cx - 2, cy + 2, 8, 7, 0.3), hl=1)
        c.speckle(front, '#c89030', 0.15, seed=int(cy))


@item('quesadilla')
def quesadilla(c):
    plate(c)
    for (pts, s) in [([(4, 20), (16, 13), (16, 21)], 0), ([(16, 21), (16, 13), (28, 20)], 1), ([(8, 24), (16, 21), (24, 24)], 2)]:
        m = polygon(pts)
        c.shape(m, R('#e8c070'), L_const(0.6 + s * 0.1))
        c.speckle(m, '#b8803a', 0.15, seed=s)
        c.fill(edge(m) & (YY > 16), '#f8d850')
    c.fill(points([(10, 19), (11, 20), (21, 19), (22, 20)]), '#3a9a2a')
    c.fill(points([(12, 20), (20, 20)]), '#d8281c')


@item('kebab')
def kebab(c):
    pita = ellipse(16, 20, 12, 9) & (YY >= 13)
    c.shape(pita, R('#f0d098'), L_sphere(13, 17, 12, 9, 0.4))
    c.speckle(pita, '#c89a4a', 0.1, seed=2)
    fill = ellipse(16, 13, 10, 4.5)
    c.shape(fill, R('#a8602a'), L_const(0.6))
    for (x, y) in rng_pts(3, fill, 7):
        c.shape(rect(x, y, x + 2, y), R('#8a4a20'), L_const(0.5), edge=False)
    lettuce(c, 16, 9, 9, '#7ac84a')
    cubes(c, fill, ['#d82a1a'], n=3, seed=4)
    drizzle(c, [(8, 12), (12, 13), (16, 11), (21, 13), (24, 11)], '#fbfaf2')
    paper = ellipse(16, 24, 12, 6) & (YY >= 21)
    c.shape(paper, R('#f4f0e8'), L_const(0.7))


@item('falafels')
def falafels(c):
    plate(c)
    for (x, y) in [(10, 17), (16, 15.5), (22, 17), (13, 21), (19, 21)]:
        m = circle(x, y, 3.2)
        c.shape(m, R('#9a6a2a'), L_sphere(x - 1, y - 1, 3.2, 3.2), hl=1)
        c.speckle(m, '#5a8a2a', 0.15, seed=int(x))
    c.shape(ellipse(25, 23, 3, 1.8), R('#f4f0e0'), L_const(0.8))
    leaf(c, 7, 22, 4, 19, 2.6, '#3aa02a', vein=False)


@item('croquettes_de_pomme_de_terre')
def croquettes_de_pomme_de_terre(c):
    plate(c)
    for (x, y) in [(9, 17), (15, 15.5), (21, 17), (12, 21), (19, 21)]:
        m = capsule(x - 2.5, y, x + 2.5, y, 2.4)
        c.shape(m, R('#d8982a'), L_cyl(x - 2.5, y, x + 2.5, y, 2.4), hl=1)
        c.speckle(m, '#a8681a', 0.25, seed=int(x + y))
    herbs(c, 24, 22, seed=3, n=2)


@item('cheeseburger_double')
def cheeseburger_double(c):
    bun_bottom(c, 16, 26, 12)
    patty(c, 16, 23, 12)
    cheese_slice(c, 16, 22, 12)
    patty(c, 16, 18, 12)
    cheese_slice(c, 16, 17, 12)
    c.fill(points([(9, 16), (12, 16), (19, 16), (23, 16)]), '#f0e0f0')
    bun_top(c, 16, 15, 12, 9.5)


@item('pizza_calzone')
def pizza_calzone(c):
    plate(c)
    m = ellipse(16, 20, 13, 8) & (YY <= 20 + (XX - 16) * 0.05) | ellipse(16, 20, 13, 3)
    c.shape(m, R('#d8983e'), L_sphere(13, 15, 13, 8, 0.4), hl=3)
    for k in range(10):
        x = 4.5 + k * 2.4
        c.fill(rect(int(x), 21, int(x), 22), '#a8601a')
    c.speckle(m, '#f0c070', 0.08, seed=4)
    for (x, y) in [(12, 15), (19, 14)]:
        c.fill(rect(x, y, x + 1, y + 1), '#8a4a1a')
    basil(c, 16, 14, 2.4, -0.6)


@item('burrito')
def burrito(c):
    for (x0, y0, x1, y1, cut) in [(3, 24, 17, 12, True), (15, 27, 29, 15, False)]:
        m = capsule(x0, y0, x1, y1, 5.5)
        c.shape(m, R('#c8ccd4'), L_cyl(x0, y0, x1, y1, 5.5), hl=2)
        c.fill(line_mask(x0, y0 - 3, x1, y1 - 3) & m & ~edge(m), '#e8ecf0')
        end = ellipse(x1, y1, 5.3, 3.8)
        c.shape(end, R('#f0d898'), L_const(0.6))
        inner = ellipse(x1, y1, 4.3, 2.9)
        c.shape(inner, R('#f4f0e0'), L_const(0.7), edge=False)
        cubes(c, inner, ['#8a1a24', '#7a3a1e', '#a8c848', '#f4d860'], n=8, seed=x0, s=1)


@item('nems')
def nems(c):
    plate(c)
    leaf(c, 5, 22, 3, 15, 6, '#7ac84a')
    for (x0, y0, x1, y1) in [(7, 18, 18, 14), (9, 22, 21, 18), (13, 25, 25, 21)]:
        m = capsule(x0, y0, x1, y1, 2.4)
        c.shape(m, R('#d89a3a'), L_cyl(x0, y0, x1, y1, 2.4), hl=1)
        c.speckle(m, '#f0c070', 0.2, seed=x0)
    c.shape(ellipse(25, 15, 3.4, 2), R('#e8e0d0'), L_const(0.8))
    c.shape(ellipse(25, 14.5, 2.4, 1.2), R('#d8702a'), L_const(0.7), edge=False)
    leaf(c, 22, 23, 26, 20, 2.4, '#4ab04a', vein=False)


@item('gyoza')
def gyoza(c):
    plate(c, color='#2a2a2a')
    for (x, y) in [(9, 18), (16, 16.5), (23, 18), (12.5, 22), (19.5, 22)]:
        m = ellipse(x, y, 4, 2.6) & (YY <= y + 1) | ellipse(x, y + 1, 4, 1.4)
        c.shape(m, R('#f4ecd8'), L_sphere(x - 1, y - 1, 4, 3, 0.5), hl=1)
        for k in range(-2, 3):
            c.fill(rect(int(x + k * 1.5), int(y - 2), int(x + k * 1.5), int(y - 1)) & m, '#d8ccb0')
        c.fill(rect(int(x) - 3, int(y) + 1, int(x) + 3, int(y) + 2) & m, '#c88a3a')
    c.shape(ellipse(26, 25, 3, 1.5), R('#3a1a0a'), L_const(0.5))


@item('bao_au_porc')
def bao_au_porc(c):
    st = ellipse(16, 22, 14, 6) | rect(2, 18, 30, 22)
    c.shape(st, R('#c8a060'), L_vert(16, 28, 0.9, 0.4))
    for x in range(4, 30, 3):
        c.fill(rect(x, 19, x, 25) & st & ~edge(st), '#a8804a')
    for (x, y) in [(10, 14), (22, 15)]:
        top = ellipse(x, y, 6, 4.5) & (YY <= y)
        bot = ellipse(x, y + 1.5, 6, 3) & (YY >= y + 1)
        c.shape(bot, R('#fbfaf2'), L_const(0.7))
        c.shape(rect(x - 5, y, x + 5, y + 1), R('#8a3a1a'), L_const(0.6))
        c.fill(rect(x - 4, y - 1, x + 4, y - 1), '#4ab03a')
        c.shape(top, R('#fbfaf2'), L_sphere(x - 2, y - 3, 6, 4.5, 0.3), hl=1)


# ============================================================================ végétarien & accompagnements
@item('puree_de_pommes_de_terre')
def puree_de_pommes_de_terre(c):
    dome = heap_in_bowl(c, '#f2eee6', '#f4e0a0', h=4)
    for k in range(3):
        c.fill(arc_band(16, 13 - k, 9 - k * 2.5, 3 - k * 0.6, 1, 190, 340) & dome, '#e0c880')
    c.shape(polygon([(14, 9), (18, 8), (19, 10), (15, 11)]), R('#f8e070'), L_const(0.85))
    c.fill(points([(13, 11), (19, 11), (20, 12)]), '#f8e888')
    bowl_rim(c, 16, 15, 13, 5, '#f2eee6')


@item('legumes_grilles')
def legumes_grilles(c):
    plate(c)
    for (x0, y0, x1, y1, cc, w) in [(5, 17, 15, 14, '#5a2a6a', 2.8), (8, 22, 19, 19, '#3a8a2a', 2.5), (16, 17, 26, 15, '#d8281c', 2.3),
                                     (15, 23, 26, 21, '#f0c020', 2.3)]:
        m = capsule(x0, y0, x1, y1, w)
        c.shape(m, R(cc), L_cyl(x0, y0, x1, y1, w), hl=1)
        for k in range(3):
            t = 0.25 + k * 0.25
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            c.fill(line_mask(x - 1, y + 1.5, x + 1, y - 1.5) & m & ~edge(m), '#1a1010')
    sprig(c, 20, 13, 25, 12, '#6a8a5a', n=2, lw=1.2, ll=1.6)


@item('poelee_de_champignons')
def poelee_de_champignons(c):
    inner = pan(c)
    for (x, y) in [(9, 15), (14, 13), (20, 15), (11, 19), (17, 18), (22, 20)]:
        c.shape(ellipse(x, y, 2.6, 1.8) & (YY <= y + 0.5), R('#a8784a'), L_const(0.6))
        c.shape(rect(x - 1, int(y), x, int(y) + 1), R('#e8d8b8'), L_const(0.8), edge=False)
    herbs(c, 13, 17, seed=4, n=4)
    c.speckle(inner, '#f4f0d0', 0.05, seed=5)


@item('brocolis_a_l_ail')
def brocolis_a_l_ail(c):
    plate(c)
    for (x, y) in [(10, 16), (16, 15), (22, 16), (13, 20), (19, 20)]:
        c.shape(rect(x - 1, y, x, y + 3), R('#a8c878'), L_const(0.7))
        for (dx, dy) in [(-1.5, 0), (1.5, 0), (0, -1.5)]:
            m = circle(x + dx - 0.5, y + dy, 2)
            c.shape(m, R('#2e7a2a'), L_sphere(x + dx - 1, y + dy - 1, 2, 2), edge=True)
    for (x, y) in [(8, 21), (24, 20), (16, 23)]:
        c.shape(ellipse(x, y, 1.4, 1), R('#f4ecd0'), L_const(0.8))


@item('epinards_a_la_creme')
def epinards_a_la_creme(c):
    def top(c, m):
        c.speckle(m, '#8ab86a', 0.25, seed=2)
        c.fill(arc_band(16, 14, 5, 2, 1, 180, 330) & m, '#e8f0d8')
        c.speckle(m, '#8a6a3a', 0.04, seed=5)
    soup(c, '#f2eee6', '#3a7a2a', top)


@item('gratin_dauphinois')
def gratin_dauphinois(c):
    inner = gratin_dish(c, '#f0d890', crust='#c8802a', color='#e8e0d0')
    for k in range(4):
        y = 15 + k * 2
        for x in range(7, 25, 4):
            c.fill(arc_band(x + (k % 2) * 2, y, 2.2, 1.4, 1, 180, 360) & inner, '#e0b860')
    c.speckle(inner, '#8a6a3a', 0.04, seed=6)


@item('ratatouille')
def ratatouille(c):
    inner = pan(c, color='#6a3a2a', handle=False)
    c.shape(capsule(1, 17, 4, 17, 1.4) | capsule(26, 17, 30, 17, 1.4), R('#6a3a2a'), L_const(0.5))
    cubes(c, inner, ['#d8281c', '#f0c020', '#3a8a2a', '#5a2a6a', '#e8483a', '#6ab04a'], n=26, seed=4, s=2)
    sprig(c, 14, 15, 20, 13, '#6a8a5a', n=2, lw=1.2, ll=1.8)


@item('quiche_lorraine')
def quiche_lorraine(c):
    def deco(c, inner):
        c.speckle(inner, '#c8802a', 0.15, seed=4)
        cubes(c, inner, ['#e8909a', '#d87a7a'], n=7, seed=5)
    tart(c, '#f0c850', deco=deco)


@item('tarte_aux_legumes')
def tarte_aux_legumes(c):
    def deco(c, inner):
        for (x, y) in [(11, 14), (17, 13), (22, 16), (13, 19), (19, 19)]:
            slice_round(c, x, y, 2.3, '#3a8a2a', '#e8f0b0', motif='ring')
        for (x, y) in [(14, 16), (20, 17), (9, 17)]:
            slice_round(c, x, y, 2.2, '#c8281a', '#e8483a', motif='seeds')
        c.speckle(inner, '#fbfaf4', 0.08, seed=6)
    tart(c, '#f4e4a8', deco=deco)


@item('tian_provencal')
def tian_provencal(c):
    dish = ellipse(16, 19, 14, 9.5)
    c.shape(dish, R('#c8603a'), L_vert(9, 29, 0.9, 0.4))
    inner = ellipse(16, 18.5, 12, 7.8)
    c.shape(inner, R('#c8481a'), L_const(0.4), edge=False)
    cols = ['#3a8a2a', '#d8281c', '#5a2a6a']
    for ring, (rx, ry, n) in enumerate([(9.5, 6, 14), (6, 3.8, 9), (2.6, 1.6, 4)]):
        for k in range(n):
            a = k / n * 2 * math.pi
            x, y = 16 + math.cos(a) * rx, 18.5 + math.sin(a) * ry
            m = circle(x, y, 2.2) & inner
            cc = cols[k % 3]
            c.shape(m, R(cc), L_const(0.6), edge=True)
            c.fill(circle(x - 0.4, y - 0.4, 1) & m, mix(cc, '#ffffff', 0.45))
    sprig(c, 14, 18, 19, 17, '#6a8a5a', n=2, lw=1.2, ll=1.6)


@item('curry_de_legumes')
def curry_de_legumes(c):
    def top(c, m):
        for (x, y) in rng_pts(2, m, 5):
            c.shape(circle(x, y, 1.3), R('#e0c088'), L_const(0.7))
        chunks(c, m, ['#f4f0e0', '#e8a060'], n=4, seed=5, r=(1.4, 1.8))
        c.speckle(m, '#2a6a1a', 0.08, seed=6)
    soup(c, '#1a5a5a', '#e8801a', top)


@item('dahl_de_lentilles')
def dahl_de_lentilles(c):
    def top(c, m):
        c.speckle(m, '#d8901a', 0.3, seed=3)
        c.fill(arc_band(16, 14, 5, 2, 1, 170, 380) & m, '#fbf4e8')
        leaf(c, 16, 12, 20, 10, 2.4, '#3aa02a', vein=False)
        leaf(c, 16, 12, 13, 9, 2.4, '#3aa02a', vein=False)
    soup(c, '#6a3a2a', '#f0b030', top)
    c.shape(ellipse(26, 25, 5, 3), R('#e8c078'), L_sphere(25, 24, 5, 3, 0.5))
    c.speckle(ellipse(26, 25, 4.5, 2.5), '#a8702a', 0.2, seed=4)


@item('poivrons_farcis')
def poivrons_farcis(c):
    plate(c)
    for (cx, cc) in [(10, '#d8281c'), (22, '#f0b020')]:
        m = ellipse(cx, 19, 6, 6) | ellipse(cx - 2.5, 20, 3.5, 5) | ellipse(cx + 2.5, 20, 3.5, 5)
        c.shape(m, R(cc), L_sphere(cx - 2, 16, 6, 7, 0.3), hl=2)
        top = ellipse(cx, 14.5, 5, 2.2)
        c.shape(top, R('#a8602a'), L_const(0.6))
        c.speckle(top, '#f4ecd0', 0.3, seed=int(cx))
        c.speckle(top, '#c8302a', 0.15, seed=int(cx) + 1)
        herbs(c, cx, 13, seed=int(cx), n=2)


@item('aubergines_parmigiana')
def aubergines_parmigiana(c):
    inner = gratin_dish(c, '#c8401a', color='#e8e0d0')
    for (x, y) in rng_pts(2, inner & ~edge(inner), 8):
        c.shape(ellipse(x, y, 2.4, 1.6) & inner, R('#fbf2d0'), L_const(0.8), edge=False)
        c.fill(rect(x - 1, y - 1, x, y - 1) & inner, '#e8b050')
    for (x, y) in [(12, 15), (20, 18)]:
        basil(c, x, y, 2.4, -0.6)


# ============================================================================ petit-déjeuner & snacks
@item('oeuf_au_plat')
def oeuf_au_plat(c):
    inner = pan(c, cx=14)
    fried_egg(c, 14, 17)
    c.fill(points([(9, 16), (19, 19), (12, 20)]), '#2a2a2a')


@item('omelette')
def omelette(c):
    plate(c)
    m = ellipse(16, 18, 10, 5.5) & (YY >= 14 - (XX - 16) * 0.1)
    c.shape(m, R('#f4d050'), L_sphere(13, 15, 10, 6, 0.4), hl=2)
    c.fill(line_mask(7, 18, 25, 16) & m & ~edge(m), '#e0b030')
    c.speckle(m, '#d8a020', 0.08, seed=2)
    herbs(c, 14, 15, seed=3, n=3)
    herbs(c, 19, 17, seed=4, n=2)


@item('omelette_aux_champignons')
def omelette_aux_champignons(c):
    plate(c)
    m = ellipse(16, 18, 10, 5.5)
    c.shape(m, R('#f4d050'), L_sphere(13, 15, 10, 6, 0.4), hl=1)
    for (x, y) in [(11, 17), (16, 16), (21, 18), (14, 20), (19, 20.5)]:
        c.shape(ellipse(x, y, 2, 1.4) & (YY <= y + 0.3), R('#a8784a'), L_const(0.6))
        c.fill(rect(x, int(y), x, int(y) + 1), '#e8d8b8')
    c.speckle(m, '#f8ecb0', 0.1, seed=6)


@item('pancakes')
def pancakes(c):
    plate(c)
    for k in range(4):
        y = 22 - k * 2.6
        m = ellipse(16, y, 10, 3.6)
        c.shape(m, R('#d8903a'), L_vert(y - 3, y + 3, 0.9, 0.4))
        c.fill(arc_band(16, y, 10, 3.6, 1, 20, 160), '#f0d098')
    top = ellipse(16, 13.5, 10, 3.6)
    c.shape(top, R('#c87a2a'), L_const(0.7), edge=False)
    c.shape(polygon([(14, 12), (18, 11), (19, 13), (15, 14)]), R('#f8e070'), L_const(0.85))
    drizzle(c, [(7, 14), (8, 17), (9, 21)], '#b8601a')
    drizzle(c, [(24, 14), (25, 18)], '#b8601a')


@item('crepes_au_sucre')
def crepes_au_sucre(c):
    plate(c)
    for (pts, sh) in [([(5, 21), (16, 13), (27, 21)], 0.6), ([(8, 24), (16, 17), (24, 24)], 0.75)]:
        m = polygon(pts) | ellipse(16, pts[0][1], 11 - sh * 3, 2.5)
        c.shape(m, R('#f0c870'), L_const(sh))
        c.speckle(m, '#d89a40', 0.1, seed=int(sh * 10))
    c.speckle(ellipse(16, 20, 9, 4), '#ffffff', 0.35, seed=4)


@item('pain_perdu')
def pain_perdu(c):
    plate(c)
    for (x0, y0) in [(4, 14), (13, 18)]:
        inner = bread_slice(c, x0, y0, x0 + 14, y0 + 8, crust='#a8601a', crumb='#e8a848', toast=0.2)
        c.speckle(inner, '#ffffff', 0.25, seed=x0)
    c.fill(points([(9, 16), (19, 21), (12, 18)]), '#8a4a1a')


@item('tartine_beurre_miel')
def tartine_beurre_miel(c):
    inner = bread_slice(c, 3, 11, 27, 25, crust='#b86a2a', crumb='#f4dca0')
    c.shape(polygon([(6, 14), (22, 13), (24, 20), (8, 22)]), R('#f8e8a0'), L_const(0.8), edge=False)
    drizzle(c, [(7, 15), (12, 17), (17, 15), (22, 18), (20, 21), (14, 20)], '#e89a18', width=2)
    c.shape(capsule(22, 22, 25, 28, 1.5), R('#e89a18'), L_const(0.7))


@item('pop_corn')
def pop_corn(c):
    for (x, y) in [(9, 9), (13, 6), (17, 7), (21, 9), (11, 12), (16, 10), (20, 12), (24, 11), (7, 12)]:
        m = circle(x, y, 2.8) | circle(x + 1.5, y - 1, 2)
        c.shape(m, R('#fbf4d8'), L_sphere(x - 1, y - 1, 3, 3), edge=True)
        c.fill(rect(x, y + 1, x, y + 1), '#f0c030')
    box = polygon([(5, 13), (27, 13), (24, 29), (8, 29)])
    c.shape(box, R('#f4f0e8'), L_horiz(5, 27, 0.95, 0.4))
    for k in range(6, 27, 5):
        c.fill(polygon([(k, 13), (k + 2.5, 13), (k + 1.5, 29), (k - 0.5, 29)]) & box & ~edge(box), '#d82020')


@item('brochette_de_fruits')
def brochette_de_fruits(c):
    stem(c, [(2, 29), (29, 2)], '#c8a070', width=1)
    specs = [(6, 25, '#f4c020', 'ananas'), (10.5, 20.5, '#e0202a', 'fraise'), (15, 16, '#f4ecb0', 'banane'),
             (19.5, 11.5, '#7ac03a', 'kiwi'), (24, 7, '#f09a3a', 'melon')]
    for (x, y, cc, kind) in specs:
        m = rrect(int(x) - 3, int(y) - 3, int(x) + 2, int(y) + 2, 1) if kind in ('ananas', 'melon') else circle(x, y, 3)
        c.shape(m, R(cc), L_sphere(x - 1, y - 1, 3, 3), hl=1)
        if kind == 'kiwi':
            c.fill(circle(x, y, 1), '#f4f0c8')
        if kind == 'fraise':
            c.speckle(m, '#f8e080', 0.2, seed=3)


@item('oeufs_brouilles_au_bacon')
def oeufs_brouilles_au_bacon(c):
    plate(c)
    m = ellipse(12, 18, 7, 4.5) | ellipse(10, 20, 5, 3.5)
    c.shape(m, R('#f8d860'), L_sphere(10, 16, 7, 5, 0.3), hl=1)
    for (x, y) in rng_pts(3, m, 8):
        c.fill(rect(x, y, x + 1, y), '#fbf0a0')
    for k in range(2):
        y = 15 + k * 4
        pts = [(18, y), (20, y - 1), (22, y + 0.5), (24, y - 0.5), (26, y + 0.5)]
        for i in range(len(pts) - 1):
            c.shape(line_mask(pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], 2), R('#b8403a'), L_const(0.6), edge=False)
        c.fill(line_mask(18, y + 1, 26, y + 1), '#f4d8c8')
    herbs(c, 12, 16, seed=3, n=2)


@item('granola_au_yaourt')
def granola_au_yaourt(c):
    body, ly = glass(c, 7, 24, 7, 28, '#f8f6ee', level=0.85, taper=1.5)
    c.fill(rect(8, 19, 23, 21) & body & ~edge(body), '#c8904a')
    c.speckle(rect(8, 19, 23, 21) & body, '#8a5a2a', 0.3, seed=3)
    top = ellipse(15.5, int(ly), 9, 2.5)
    c.shape(top, R('#d8a050'), L_const(0.7), edge=False)
    c.speckle(top, '#8a5a2a', 0.3, seed=4)
    for (x, y) in [(12, int(ly) - 2), (18, int(ly) - 1)]:
        m = circle(x, y, 2.2)
        c.shape(m, R('#e0202a'), L_sphere(x - 0.5, y - 0.5, 2.2, 2.2), hl=1)
    drizzle(c, [(9, int(ly) + 1), (14, int(ly)), (20, int(ly) + 1)], '#e8a018')


@item('croissant')
def croissant(c):
    segs = [(5, 22, 3), (9, 17, 4.5), (16, 15, 5.5), (23, 17, 4.5), (27, 22, 3)]
    for (x, y, r) in segs:
        m = ellipse(x, y, r, r * 0.95)
        c.shape(m, R('#d8903a'), L_sphere(x - 1, y - 2, r, r, 0.3), hl=1)
        c.fill(arc_band(x, y, r, r * 0.95, 1, 200, 340) & m, '#f4c878')
    c.shape(capsule(3, 24, 1, 26, 1.5) | capsule(29, 24, 31, 26, 1.5), R('#c8802a'), L_const(0.6))


@item('pain_au_chocolat')
def pain_au_chocolat(c):
    top = polygon([(3, 14), (19, 8), (29, 13), (13, 19)])
    front = polygon([(3, 14), (13, 19), (13, 26), (3, 21)])
    side = polygon([(13, 19), (29, 13), (29, 20), (13, 26)])
    c.shape(front, R('#d8903a'), L_const(0.6))
    c.shape(side, R('#c8802a'), L_const(0.45))
    for k in range(1, 3):
        c.fill(line_mask(3, 14 + k * 2.4, 13, 19 + k * 2.4), '#f4c878')
    c.shape(rect(6, 18, 8, 20) | rect(8, 19, 10, 21), R('#3a1a0e'), L_const(0.5), edge=False)
    c.shape(top, R('#e0a040'), L_const(0.85), hl=2)
    c.fill(line_mask(8, 12, 22, 7), '#f0c060')


# ============================================================================ desserts
@item('riz_au_lait')
def riz_au_lait(c):
    op = ramekin(c, '#f2eee6', '#f8f0dc')
    grains(c, op, '#ffffff', 16, seed=3)
    c.speckle(op, '#b8783a', 0.08, seed=4)
    c.shape(capsule(10, 12, 18, 10, 1), R('#3a2418'), L_const(0.4), edge=False)


@item('cookies')
def cookies(c):
    for (x, y, r) in [(20, 11, 8), (11, 20, 8.5), (23, 23, 6.5)]:
        m = circle(x, y, r)
        c.shape(m, R('#d8a050'), L_sphere(x - 2, y - 2, r, r, 0.6), hl=1)
        c.speckle(m, '#b8803a', 0.1, seed=int(x))
        for (px, py) in rng_pts(int(y), m & ~edge(m), int(r)):
            c.fill(rect(px, py, px + 1, py + 1) & m, '#3a1a0e')
            c.fill(rect(px, py, px, py) & m, '#6a3a1e')


@item('salade_de_fruits')
def salade_de_fruits(c):
    op = bowl(c, 16, 15, 13, 5, 9, '#dcecf2')
    dome = ellipse(16, 14, 11, 4) | ellipse(16, 12.5, 8, 3.5)
    c.shape(dome, R('#f8e0a0'), L_const(0.6), edge=False)
    cubes(c, dome, ['#e0202a', '#f4d02a', '#7ac03a', '#f08a14', '#f4ecb0'], n=16, seed=4, s=2)
    leaf(c, 16, 11, 20, 8, 2.6, '#4ab04a', vein=False)
    bowl_rim(c, 16, 15, 13, 5, '#dcecf2')


@item('compote_de_pommes')
def compote_de_pommes(c):
    op = ramekin(c, '#dcecf2', '#e8c880')
    c.speckle(op, '#c8a050', 0.2, seed=3)
    c.shape(capsule(9, 13, 16, 12, 1.2), R('#9a5028'), L_const(0.6))
    leaf(c, 20, 12, 26, 7, 4, '#d8242a', vein=False)
    c.fill(line_mask(21, 12, 25, 8), '#f8f0c8')


@item('tarte_aux_pommes')
def tarte_aux_pommes(c):
    def deco(c, inner):
        for ring, (rx, ry, n) in enumerate([(8.5, 5.5, 12), (4.5, 2.8, 7)]):
            for k in range(n):
                a = k / n * 2 * math.pi
                x, y = 16 + math.cos(a) * rx, 16.7 + math.sin(a) * ry
                m = ellipse(x, y, 2.4, 1.6) & inner
                c.shape(m, R('#f0d080'), L_const(0.75), edge=True)
                c.fill(edge(m) & (YY > y), '#c8603a')
        glaze(c, inner, '#f0d080', 6)
    tart(c, '#f4dc98', deco=deco)


@item('clafoutis_aux_cerises')
def clafoutis_aux_cerises(c):
    op = ramekin(c, '#e8e0d0', '#e8b048', x0=3, x1=28, top=15, bottom=27)
    for (x, y) in [(9, 14), (15, 16), (21, 14), (12, 17), (19, 17), (24, 16)]:
        m = circle(x, y, 1.8)
        c.shape(m, R('#9a1020'), L_sphere(x - 0.5, y - 0.5, 1.8, 1.8), hl=1)
    c.speckle(op, '#ffffff', 0.15, seed=5)


@item('gateau_au_chocolat')
def gateau_au_chocolat(c):
    plate(c)

    def deco(c):
        c.fill(points([(12, 12), (15, 11), (18, 12)]), '#8a5a3a')
    cake_slice(c, [('#4a2414', 3), ('#6a3a24', 2), ('#4a2414', 3)], top='#3a1a0e', deco=deco)


@item('mousse_au_chocolat')
def mousse_au_chocolat(c):
    body, ly = glass(c, 8, 23, 9, 28, '#5a2a14', level=0.8, taper=1.5)
    c.speckle(body & (YY > ly), '#7a4a2a', 0.1, seed=3)
    top = ellipse(15.5, ly, 7, 2.5)
    c.shape(top, R('#fbfaf2'), L_const(0.85))
    c.shape(ellipse(15.5, ly - 2, 4, 2), R('#fbfaf2'), L_sphere(14, ly - 3, 4, 2), edge=True)
    c.speckle(top, '#5a2a14', 0.15, seed=4)
    leaf(c, 16, ly - 3, 20, ly - 6, 2.4, '#4ab04a', vein=False)


@item('crepes_chocolat_banane')
def crepes_chocolat_banane(c):
    plate(c)
    m = polygon([(5, 22), (16, 13), (27, 22)]) | ellipse(16, 22, 11, 3)
    c.shape(m, R('#f0c870'), L_const(0.7))
    drizzle(c, [(7, 20), (11, 17), (15, 20), (19, 16), (24, 20)], '#4a2414', width=2)
    for (x, y) in [(11, 22), (16, 23), (21, 22)]:
        slice_round(c, x, y, 2, '#e8d8a0', '#f8f0c8', motif='seeds', ry=1.5)


@item('gaufres')
def gaufres(c):
    for (x0, y0) in [(3, 12), (13, 16)]:
        top = polygon([(x0, y0 + 4), (x0 + 8, y0), (x0 + 16, y0 + 4), (x0 + 8, y0 + 8)])
        c.shape(shift(top, 0, 2), R('#b8702a'), L_const(0.4))
        c.shape(top, R('#e8a848'), L_const(0.7))
        for k in range(1, 4):
            c.fill(line_mask(x0 + k * 2, y0 + 4 - k, x0 + 8 + k * 2, y0 + 8 - k) & top, '#b8702a')
            c.fill(line_mask(x0 + k * 2, y0 + 4 + k, x0 + 8 + k * 2, y0 + k) & top, '#b8702a')
        c.speckle(top, '#ffffff', 0.25, seed=x0)


@item('churros')
def churros(c):
    cup = polygon([(18, 16), (29, 16), (28, 28), (19, 28)]) | ellipse(23.5, 16, 5.5, 2)
    c.shape(cup, R('#f2eee6'), L_horiz(18, 29, 0.95, 0.4))
    c.shape(ellipse(23.5, 16, 4.5, 1.5), R('#4a2414'), L_const(0.6), edge=False)
    for (x0, y0, x1, y1) in [(3, 27, 12, 4), (7, 28, 17, 6), (11, 28, 22, 10)]:
        m = capsule(x0, y0, x1, y1, 2.3)
        c.shape(m, R('#d8983a'), L_cyl(x0, y0, x1, y1, 2.3), hl=1)
        c.fill(line_mask(x0, y0, x1, y1) & m & ~edge(m), '#a8681a')
        c.speckle(m, '#fbf4e0', 0.2, seed=x0)


@item('brownie_aux_noix')
def brownie_aux_noix(c):
    for (x, y) in [(3, 14), (15, 18), (13, 8)]:
        top = polygon([(x, y + 3), (x + 7, y), (x + 14, y + 3), (x + 7, y + 6)])
        front = polygon([(x, y + 3), (x + 7, y + 6), (x + 7, y + 11), (x, y + 8)])
        side = polygon([(x + 7, y + 6), (x + 14, y + 3), (x + 14, y + 8), (x + 7, y + 11)])
        c.shape(front, R('#4a2414'), L_const(0.6))
        c.shape(side, R('#3a1a0e'), L_const(0.45))
        c.shape(top, R('#5a2e18'), L_const(0.85))
        c.fill(line_mask(x + 1, y + 3, x + 7, y + 0.5), '#7a4a2a')
        for (px, py) in [(x + 5, y + 3), (x + 9, y + 2)]:
            c.fill(rect(px, py, px + 1, py), '#c8a070')


@item('crumble_aux_fruits_rouges')
def crumble_aux_fruits_rouges(c):
    op = ramekin(c, '#c83a3a', '#d8a050', x0=4, x1=27, top=15, bottom=27)
    for (x, y) in rng_pts(3, op, 14):
        c.shape(circle(x, y, 1.2) & op, R('#e8b870'), L_const(0.8), edge=True)
    for (x, y) in [(10, 15), (19, 14), (15, 16)]:
        c.shape(circle(x, y, 1.4), R('#8a1a3a'), L_const(0.7))
    c.fill(points([(22, 16), (8, 14)]), '#3a3a8a')


@item('sorbet_citron')
def sorbet_citron(c):
    cup = polygon([(8, 18), (24, 18), (19, 26), (13, 26)])
    c.shape(cup, R('#dcecf2'), L_horiz(8, 24, 0.9, 0.3))
    c.shape(rect(15, 26, 17, 29) | ellipse(16, 29, 5, 1.5), R('#dcecf2'), L_const(0.6))
    for (x, y, r) in [(12, 16, 4.5), (20, 16, 4.5), (16, 11, 4.8)]:
        m = circle(x, y, r)
        c.shape(m, R('#f8f0a0'), L_sphere(x - 1, y - 1, r, r, 0.3), hl=1)
        c.fill(arc_band(x, y, r, r, 1, 20, 160) & m, '#e8d870')
    leaf(c, 17, 7, 22, 4, 3, '#4ab04a')


@item('tarte_au_citron_meringuee')
def tarte_au_citron_meringuee(c):
    plate(c)

    def deco(c):
        for (x, y) in [(10, 10), (15, 8), (20, 9), (24, 10), (13, 12), (18, 11)]:
            m = circle(x, y, 2.4) | polygon([(x - 1, y - 1), (x, y - 4), (x + 1, y - 1)])
            c.shape(m, R('#fbf4e0'), L_sphere(x - 1, y - 1, 2.4, 2.4), edge=True)
            c.fill(rect(x, y - 3, x, y - 2), '#d8a050')
    cake_slice(c, [('#f4e040', 5), ('#d8a050', 2)], top='#f8f0d0', deco=deco)


@item('tarte_aux_fraises')
def tarte_aux_fraises(c):
    def deco(c, inner):
        for (x, y) in [(10, 15), (15, 13), (21, 14), (12, 19), (18, 18), (23, 18), (16, 16)]:
            m = polygon([(x - 2.2, y - 1), (x + 2.2, y - 1), (x, y + 2.5)]) | ellipse(x, y - 1, 2.2, 1.4)
            c.shape(m, R('#e0202a'), L_sphere(x - 1, y - 1, 2.4, 2.4), edge=True, hl=1)
            c.fill(rect(x, y - 2, x, y - 2), '#3a9a2a')
    tart(c, '#f4d060', deco=deco)


@item('tarte_tatin')
def tarte_tatin(c):
    def deco(c, inner):
        for ring, (rx, ry, n) in enumerate([(8, 5.2, 9), (3.8, 2.4, 5)]):
            for k in range(n):
                a = k / n * 2 * math.pi
                x, y = 16 + math.cos(a) * rx, 16.7 + math.sin(a) * ry
                m = ellipse(x, y, 3, 2.2) & inner
                c.shape(m, R('#c8701a'), L_sphere(x - 1, y - 1, 3, 2.2, 0.4), edge=True, hl=1)
    tart(c, '#a8501a', crust='#d8a050', deco=deco)


@item('fondant_au_chocolat')
def fondant_au_chocolat(c):
    plate(c)
    body = ellipse(15, 13, 7, 2.6) | rect(8, 13, 22, 20) | ellipse(15, 20, 7, 2.6)
    c.shape(body, R('#4a2414'), L_horiz(8, 22, 0.8, 0.3), hl=1)
    c.shape(ellipse(15, 13, 7, 2.6), R('#5a2e18'), L_const(0.7))
    c.speckle(ellipse(15, 13, 6, 2), '#ffffff', 0.3, seed=3)
    flow = polygon([(19, 17), (22, 16), (27, 21), (23, 24), (19, 22)])
    c.shape(flow, R('#3a1a0e'), L_const(0.6), hl=2)
    leaf(c, 12, 11, 15, 8, 2.4, '#4ab04a', vein=False)
    c.shape(circle(18, 11, 1.4), R('#c8102a'), L_const(0.7))


@item('creme_brulee')
def creme_brulee(c):
    op = ramekin(c, '#f2eee6', '#d8903a', x0=3, x1=28, top=15, bottom=27)
    c.speckle(op, '#8a4a14', 0.18, seed=3)
    c.fill(line_mask(10, 14, 20, 16) & op, '#f0c070')
    c.fill(line_mask(14, 13, 18, 16) & op, '#6a3010')


@item('ile_flottante')
def ile_flottante(c):
    def top(c, m):
        isl = ellipse(15, 12, 6, 3.4)
        c.shape(isl, R('#fbfaf4'), L_sphere(13, 10, 6, 3.4, 0.3), hl=2)
        drizzle(c, [(10, 11), (13, 10), (16, 12), (19, 10), (21, 12)], '#c8701a')
        c.fill(points([(9, 15), (22, 15), (17, 16)]), '#3a2418')
    soup(c, '#dcecf2', '#f8e8a8', top)


@item('cheesecake')
def cheesecake(c):
    plate(c)

    def deco(c):
        m = polygon([(4, 16), (12, 11), (27, 12), (26, 14)])
        c.shape(m, R('#b8102a'), L_const(0.7), hl=1)
        for (x, y) in [(10, 12), (18, 11), (23, 12)]:
            c.shape(circle(x, y, 1.4), R('#c81a3a'), L_const(0.7))
    cake_slice(c, [('#f8f0d8', 6), ('#c8904a', 2)], top='#fbf6e4', deco=deco)


@item('glace_a_la_vanille')
def glace_a_la_vanille(c):
    cone = polygon([(9, 17), (23, 17), (16, 31)])
    c.shape(cone, R('#d8a050'), L_horiz(9, 23, 0.9, 0.4))
    for k in range(-2, 4):
        c.fill(line_mask(9 + k * 4, 17, 16 + k * 2, 31) & cone & ~edge(cone), '#a8702a')
        c.fill(line_mask(23 - k * 4, 17, 16 - k * 2, 31) & cone & ~edge(cone), '#a8702a')
    for (x, y, r) in [(12, 15, 5), (20, 14, 5), (16, 9, 5.5)]:
        m = circle(x, y, r)
        c.shape(m, R('#f8f0d0'), L_sphere(x - 1.5, y - 1.5, r, r, 0.3), hl=2)
        c.speckle(m, '#3a2418', 0.05, seed=int(x))
    c.shape(rrect(20, 3, 27, 6, 1), R('#e8c070'), L_const(0.7))


@item('tiramisu')
def tiramisu(c):
    plate(c)
    front = polygon([(5, 13), (21, 16), (21, 25), (5, 22)])
    side = polygon([(21, 16), (27, 13), (27, 22), (21, 25)])
    for m, L in ((front, 0.6), (side, 0.4)):
        c.shape(m, R('#f8f0dc'), L_const(L))
    for k, cc in enumerate(['#8a5a2a', '#8a5a2a']):
        y = 16.5 + k * 4
        c.fill(line_mask(5, y, 21, y + 3, 2) & front, cc)
        c.fill(line_mask(21, y + 3, 27, y, 2) & side, R(cc)[1])
    top = polygon([(5, 13), (11, 10), (27, 13), (21, 16)])
    c.shape(top, R('#6a3a1e'), L_const(0.7))
    c.speckle(top, '#8a5a3a', 0.3, seed=4)


@item('macarons_a_la_framboise')
def macarons_a_la_framboise(c):
    for (x, y) in [(10, 22), (22, 22), (16, 13)]:
        for (dy, cc) in [(2.5, '#e86a8a'), (0, '#f4d8e0'), (-2.5, '#e86a8a')]:
            if cc == '#f4d8e0':
                c.shape(ellipse(x, y + dy, 6.5, 1.6), R(cc), L_const(0.8))
            else:
                m = ellipse(x, y + dy, 7, 3)
                c.shape(m, R(cc), L_sphere(x - 2, y + dy - 1, 7, 3, 0.4), hl=1)
        c.fill(rect(int(x) - 6, int(y) + 1, int(x) + 6, int(y) + 1), '#c84a6a')
    c.shape(circle(26, 10, 2), R('#d02050'), L_const(0.7))


@item('eclair_au_chocolat')
def eclair_au_chocolat(c):
    plate(c)
    m = capsule(6, 20, 26, 15, 4.3)
    c.shape(m, R('#d8983a'), L_cyl(6, 20, 26, 15, 4.3))
    g = capsule(6, 18.5, 26, 13.5, 3)
    c.shape(g, R('#4a2414'), L_cyl(6, 18.5, 26, 13.5, 3), hl=3)
    c.fill(line_mask(7, 21.5, 25, 17), '#f4dca0')


@item('mille_feuille')
def mille_feuille(c):
    plate(c)
    front = polygon([(4, 14), (21, 17), (21, 25), (4, 22)])
    side = polygon([(21, 17), (28, 14), (28, 22), (21, 25)])
    c.shape(front, R('#f4e0a0'), L_const(0.7))
    c.shape(side, R('#e8d090'), L_const(0.5))
    for k, cc in enumerate(['#d89a4a', '#d89a4a', '#d89a4a']):
        y = 15 + k * 3.3
        c.fill(line_mask(4, y, 21, y + 3, 1) & front, cc)
        c.fill(line_mask(21, y + 3, 28, y, 1) & side, R(cc)[1])
    top = polygon([(4, 14), (11, 11), (28, 14), (21, 17)])
    c.shape(top, R('#fbfaf2'), L_const(0.9))
    for k in range(3):
        c.fill(line_mask(8 + k * 5, 12, 12 + k * 5, 15.5), '#6a3a1e')
    c.fill(line_mask(8, 13, 23, 15) & top, '#c8a080')


@item('paris_brest')
def paris_brest(c):
    plate(c)
    ring = arc_band(16, 17, 12, 7.5, 5.5)
    c.shape(ring, R('#d8983a'), L_sphere(13, 14, 12, 8, 0.4), hl=2)
    c.shape(arc_band(16, 17, 10.5, 6, 3, 180, 360) & ring, R('#e0c080'), L_const(0.75), edge=True)
    for k in range(6):
        c.fill(arc_band(16, 17, 10, 5.6, 1, 190 + k * 30, 200 + k * 30), '#c89a5a')
    for (x, y) in rng_pts(4, ring & (YY < 17), 10):
        c.fill(rect(x, y, x + 1, y), '#f8ecc8')
    c.speckle(ring, '#ffffff', 0.12, seed=6)


@item('foret_noire')
def foret_noire(c):
    plate(c)

    def deco(c):
        for (x, y) in [(11, 9), (19, 8), (24, 10)]:
            c.shape(ellipse(x, y + 1, 2.4, 1.8), R('#fbfaf4'), L_const(0.85))
            c.shape(circle(x, y - 1, 1.8), R('#9a1020'), L_sphere(x - 0.5, y - 1.5, 1.8, 1.8), hl=1)
        c.speckle(polygon([(4, 16), (12, 11), (27, 12), (26, 14)]), '#3a1a0e', 0.3, seed=4)
    cake_slice(c, [('#3a1a0e', 2.5), ('#fbfaf4', 1.5), ('#3a1a0e', 2.5), ('#fbfaf4', 1.5), ('#3a1a0e', 2.5)], top='#fbfaf4', deco=deco)


@item('baklava')
def baklava(c):
    plate(c, color='#2a6ab0')
    for (x, y) in [(10, 17), (21, 17), (15.5, 21.5), (15.5, 13)]:
        top = polygon([(x - 5, y), (x, y - 3), (x + 5, y), (x, y + 3)])
        c.shape(shift(top, 0, 1), R('#b8701a'), L_const(0.45))
        c.shape(top, R('#d89a3a'), L_const(0.8), hl=1)
        for k in (-1, 1):
            c.fill(line_mask(x - 3, y + k, x + 3, y + k) & top, '#b8701a')
        c.shape(circle(x, y, 1.2), R('#7ab03a'), L_const(0.7), edge=False)
