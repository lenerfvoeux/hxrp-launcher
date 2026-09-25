"""Détecte le « z-fighting » : deux faces coplanaires, orientées pareil, qui se chevauchent (elles scintillent)."""
AXES = {'north': (2, -1), 'south': (2, 1), 'west': (0, -1), 'east': (0, 1), 'down': (1, -1), 'up': (1, 1)}


def faces(el):
    f, t = el['from'], el['to']
    for name, fd in el['faces'].items():
        ax, sgn = AXES[name]
        plane = t[ax] if sgn > 0 else f[ax]
        o = [i for i in range(3) if i != ax]
        yield name, round(plane, 4), (f[o[0]], f[o[1]], t[o[0]], t[o[1]]), fd


def conflits(model):
    lst = []
    for i, el in enumerate(model['elements']):
        if 'rotation' in el and el['rotation'].get('angle', 0) != 0:
            continue
        for name, plane, r, fd in faces(el):
            lst.append((i, name, plane, r, fd))
    out = []
    for a in range(len(lst)):
        for b in range(a + 1, len(lst)):
            i, n1, p1, r1, f1 = lst[a]
            j, n2, p2, r2, f2 = lst[b]
            if i == j or n1 != n2 or p1 != p2:
                continue
            w = min(r1[2], r2[2]) - max(r1[0], r2[0])
            h = min(r1[3], r2[3]) - max(r1[1], r2[1])
            if w > 1e-3 and h > 1e-3:
                out.append((i, j, n1, p1, round(w * h, 3), f1['texture'], f2['texture']))
    return out
