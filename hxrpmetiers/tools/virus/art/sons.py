"""
Sons du Hunter Virus, synthétisés (bruit filtré, enveloppes, résonances) puis encodés en Ogg Vorbis par ffmpeg :
toux, éternuement, sifflement d'acouphènes, murmures d'hallucination, battements, et les gestes de l'officine
(pilon, bouillon, verre, seringue, lame, page, meule). Écrit aussi sounds.json.
"""
import hashlib
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))

SR = 44100
RNG = np.random.RandomState(1234)


def t(d):
    return np.arange(int(SR * d)) / SR


def bruit(d, seed=None):
    rs = np.random.RandomState(seed) if seed is not None else RNG
    return rs.uniform(-1, 1, int(SR * d))


def env(n, a, d, sustain=1.0, r=None):
    """Enveloppe attaque / déclin exponentiel (en secondes)."""
    x = np.arange(n) / SR
    e = np.minimum(1, x / max(a, 1e-4))
    e = e * np.exp(-np.maximum(0, x - a) / max(d, 1e-4))
    return e * sustain


def biquad(x, f, q, type='bp'):
    w = 2 * np.pi * f / SR
    al = np.sin(w) / (2 * q)
    cw = np.cos(w)
    if type == 'bp':
        b = [al, 0, -al]
        a = [1 + al, -2 * cw, 1 - al]
    elif type == 'lp':
        b = [(1 - cw) / 2, 1 - cw, (1 - cw) / 2]
        a = [1 + al, -2 * cw, 1 - al]
    else:
        b = [(1 + cw) / 2, -(1 + cw), (1 + cw) / 2]
        a = [1 + al, -2 * cw, 1 - al]
    b = np.array(b) / a[0]
    a = np.array(a) / a[0]
    y = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i in range(len(x)):
        v = b[0] * x[i] + b[1] * x1 + b[2] * x2 - a[1] * y1 - a[2] * y2
        x2, x1 = x1, x[i]
        y2, y1 = y1, v
        y[i] = v
    return y


def norm(x, pic=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * pic


def coller(*morceaux, gap=0.0):
    out = []
    for m in morceaux:
        out.append(m)
        if gap:
            out.append(np.zeros(int(SR * gap)))
    return np.concatenate(out)


# ============================================================================ corps
def toux(seed, n=2):
    parts = []
    for k in range(n):
        d = 0.22 + RNG.uniform(-0.03, 0.05)
        x = bruit(d, seed + k)
        x = biquad(x, 700 + 150 * k, 1.6) * 0.8 + biquad(x, 1800, 2.5) * 0.4 + biquad(x, 300, 1.2) * 0.5
        x *= env(len(x), 0.008, 0.07)
        parts.append(x)
    return norm(coller(*parts, gap=0.12))


def eternuement():
    a = bruit(0.45, 7)
    a = biquad(a, 900, 3) * env(len(a), 0.35, 1.0) * np.linspace(0.2, 1, len(a))
    tt = t(0.45)
    a += 0.15 * np.sin(2 * np.pi * (300 + 220 * tt) * tt) * np.linspace(0, 1, len(tt))
    b = bruit(0.32, 8)
    b = (biquad(b, 2800, 1.4) + biquad(b, 5200, 2) * 0.6 + biquad(b, 400, 1) * 0.5) * env(len(b), 0.005, 0.09)
    return norm(coller(a * 0.5, np.zeros(int(SR * 0.05)), b))


def sifflement():
    tt = t(3.2)
    f = 7200 + 60 * np.sin(2 * np.pi * 5 * tt)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.25 * np.sin(2 * np.pi * np.cumsum(f * 1.5) / SR)
    e = np.minimum(1, tt / 0.6) * np.minimum(1, (3.2 - tt) / 0.6)
    return norm(x * e, 0.35)


def murmure():
    d = 2.2
    x = bruit(d, 11)
    tt = t(d)
    syll = 0.5 + 0.5 * np.sin(2 * np.pi * 3.2 * tt + 1.5 * np.sin(2 * np.pi * 0.7 * tt))
    y = biquad(x, 1100, 4) * 0.6 + biquad(x, 2400, 5) * 0.4 + biquad(x, 600, 3) * 0.4
    y *= syll ** 2 * np.minimum(1, tt / 0.3) * np.minimum(1, (d - tt) / 0.4)
    return norm(y, 0.6)


def battement():
    def coup(f, d):
        tt = t(d)
        return np.sin(2 * np.pi * f * tt * (1 - 0.3 * tt / d)) * env(len(tt), 0.004, 0.05)
    return norm(coller(coup(55, 0.18), np.zeros(int(SR * 0.08)), coup(48, 0.2) * 0.8, np.zeros(int(SR * 0.4))))


# ============================================================================ officine
def pilon(seed):
    x = bruit(0.18, seed)
    y = biquad(x, 180, 1.5) * 1.2 + biquad(x, 900, 3) * 0.4 + biquad(x, 2600, 4) * 0.2
    return norm(y * env(len(y), 0.002, 0.04))


def bouillon(seed):
    out = np.zeros(int(SR * 1.2))
    rs = np.random.RandomState(seed)
    for _ in range(9):
        p = rs.randint(0, len(out) - 4000)
        f = rs.uniform(250, 600)
        tt = t(0.06)
        b = np.sin(2 * np.pi * (f + 900 * tt) * tt) * env(len(tt), 0.002, 0.018)
        out[p:p + len(b)] += b * rs.uniform(0.4, 1)
    return norm(out, 0.6)


def verre(seed):
    tt = t(0.6)
    rs = np.random.RandomState(seed)
    f = rs.uniform(2600, 3400)
    x = sum(np.sin(2 * np.pi * f * k * tt) / k for k in (1, 2.7, 4.1)) * env(len(tt), 0.001, 0.12)
    return norm(x, 0.5)


def seringue_son():
    tt = t(0.35)
    f = 1500 + 900 * tt / 0.35
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * 0.3 + biquad(bruit(0.35, 21), 3000, 3) * 0.4
    return norm(x * np.minimum(1, tt / 0.05) * np.minimum(1, (0.35 - tt) / 0.08), 0.45)


def lame(seed):
    x = bruit(0.25, seed)
    choc = (biquad(x, 220, 1.2) * 1.2 + biquad(x, 1400, 2.5) * 0.5) * env(len(x), 0.001, 0.035)
    tt = t(0.25)
    metal = np.sin(2 * np.pi * 3900 * tt) * env(len(tt), 0.001, 0.06) * 0.15
    return norm(choc + metal)


def page(seed):
    x = bruit(0.4, seed)
    tt = t(0.4)
    y = biquad(x, 3500, 0.8, 'hp') * (0.3 + 0.7 * np.abs(np.sin(2 * np.pi * 6 * tt))) * env(len(x), 0.05, 0.15)
    return norm(y, 0.45)


def meule():
    x = bruit(0.9, 31)
    tt = t(0.9)
    y = biquad(x, 140, 2) * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * tt)) + biquad(x, 600, 3) * 0.3
    return norm(y * np.minimum(1, tt / 0.1) * np.minimum(1, (0.9 - tt) / 0.2), 0.6)


SONS = {
    'virus.toux': ('player', [('toux1', lambda: toux(1, 2)), ('toux2', lambda: toux(5, 3))]),
    'virus.eternuement': ('player', [('eternuement', eternuement)]),
    'virus.sifflement': ('master', [('sifflement', sifflement)]),
    'virus.murmure': ('ambient', [('murmure', murmure)]),
    'virus.battement': ('player', [('battement', battement)]),
    'virus.pilon': ('block', [('pilon1', lambda: pilon(41)), ('pilon2', lambda: pilon(42))]),
    'virus.bouillon': ('block', [('bouillon1', lambda: bouillon(51)), ('bouillon2', lambda: bouillon(52))]),
    'virus.verre': ('block', [('verre1', lambda: verre(61)), ('verre2', lambda: verre(62))]),
    'virus.seringue': ('player', [('seringue', seringue_son)]),
    'virus.lame': ('block', [('lame1', lambda: lame(71)), ('lame2', lambda: lame(72))]),
    'virus.page': ('player', [('page1', lambda: page(81)), ('page2', lambda: page(82))]),
    'virus.meule': ('block', [('meule', meule)]),
}


def ecrire_ogg(x, chemin, empreintes=None):
    """Encode en Ogg Vorbis ; si le son n'a pas changé depuis le dernier encodage, garde le fichier existant
    (l'encodeur n'est pas déterministe : on évite de réécrire des fichiers identiques à l'oreille)."""
    brut = (np.clip(x, -1, 1) * 32000).astype('<i2').tobytes()
    h = hashlib.sha1(brut).hexdigest()
    nom = os.path.basename(chemin)
    if empreintes is not None and empreintes.get(nom) == h and os.path.exists(chemin):
        return
    if empreintes is not None:
        empreintes[nom] = h
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        tmp = f.name
    try:
        with wave.open(tmp, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((np.clip(x, -1, 1) * 32000).astype('<i2').tobytes())
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', chemin], check=True)
    finally:
        os.remove(tmp)


def generer(dossier, sounds_json):
    os.makedirs(dossier, exist_ok=True)
    fichier_empreintes = os.path.join(HERE, 'sons_empreintes.json')
    empreintes = json.load(open(fichier_empreintes)) if os.path.exists(fichier_empreintes) else {}
    table = {}
    gardes = set()
    for evt, (cat, fichiers) in SONS.items():
        lst = []
        for nom, fn in fichiers:
            ecrire_ogg(fn(), os.path.join(dossier, nom + '.ogg'), empreintes)
            gardes.add(nom + '.ogg')
            lst.append('hxrpmetiers:virus/' + nom)
        table[evt] = {'category': cat, 'subtitle': 'subtitles.hxrpmetiers.' + evt, 'sounds': lst}
    for f in os.listdir(dossier):
        if f.endswith('.ogg') and f not in gardes:
            os.remove(os.path.join(dossier, f))
    with open(fichier_empreintes, 'w') as f:
        json.dump({k: v for k, v in sorted(empreintes.items()) if k in gardes}, f, indent=1)
        f.write('\n')
    existant = {}
    if os.path.exists(sounds_json):
        existant = {k: v for k, v in json.load(open(sounds_json, encoding='utf-8')).items() if not k.startswith('virus.')}
    existant.update(table)
    with open(sounds_json, 'w', encoding='utf-8') as f:
        json.dump(existant, f, ensure_ascii=False, indent=1)
        f.write('\n')
