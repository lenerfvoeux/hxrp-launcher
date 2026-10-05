"""
Sons du Phénix, synthétisés puis encodés en Ogg Vorbis (ffmpeg) : cri de rapace incandescent, battements
d'ailes, souffle de flammes, boule de feu, impact, plongée sifflante, onde de choc, tempête, œuf de cendres
(cœur qui bat, crépitements), craquement, renaissance, mort. Écrit aussi sounds.json.

Les empreintes (SHA-1 des échantillons) évitent de réencoder un son inchangé : l'encodeur n'est pas
déterministe et on ne veut pas de fichiers modifiés à chaque génération.
"""
import hashlib
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
ASSETS = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpphenix')
SR = 44100


def t(d):
    return np.arange(int(SR * d)) / SR


def bruit(d, graine):
    return np.random.RandomState(graine).uniform(-1, 1, int(SR * d))


def enveloppe(n, attaque, declin, maintien=0.0):
    x = np.arange(n) / SR
    e = np.minimum(1, x / max(attaque, 1e-4))
    return e * np.exp(-np.maximum(0, x - attaque - maintien) / max(declin, 1e-4))


def biquad(x, f, q, type_='bp'):
    w = 2 * np.pi * np.clip(f, 20, SR * 0.45) / SR
    f_var = np.ndim(w) > 0
    out = np.zeros_like(x)
    x1 = x2 = y1 = y2 = 0.0
    for i in range(len(x)):
        wi = w[i] if f_var else w
        al = np.sin(wi) / (2 * q)
        cw = np.cos(wi)
        if type_ == 'bp':
            b0, b1, b2 = al, 0.0, -al
        elif type_ == 'lp':
            b0, b1, b2 = (1 - cw) / 2, 1 - cw, (1 - cw) / 2
        else:
            b0, b1, b2 = (1 + cw) / 2, -(1 + cw), (1 + cw) / 2
        a0, a1, a2 = 1 + al, -2 * cw, 1 - al
        v = (b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0
        x2, x1 = x1, x[i]
        y2, y1 = y1, v
        out[i] = v
    return out


def norm(x, pic=0.9):
    m = np.max(np.abs(x)) or 1
    return x / m * pic


def fondu(x, d=0.02):
    n = min(len(x) // 2, int(SR * d))
    x = x.copy()
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def crepitement(d, graine, densite=60, clair=1.0):
    """Crépitements de feu : petites impulsions filtrées, au hasard."""
    rs = np.random.RandomState(graine)
    n = int(SR * d)
    x = np.zeros(n)
    for _ in range(int(densite * d)):
        i = rs.randint(0, n - 400)
        k = rs.randint(40, 300)
        x[i:i + k] += rs.uniform(-1, 1, k) * np.exp(-np.arange(k) / (k / 4)) * rs.uniform(0.3, 1.0)
    return biquad(x, 2500 * clair, 0.8, 'hp' if clair > 1.2 else 'bp')


def voix(f0, d, harmoniques=7, vibrato=(28, 0.012), rugosite=0.0, graine=1):
    """Voix d'oiseau : fréquence fondamentale variable f0(t), harmoniques décroissantes, vibrato."""
    tt = t(d)
    f = f0(tt) * (1 + vibrato[1] * np.sin(2 * np.pi * vibrato[0] * tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = np.zeros_like(tt)
    for h in range(1, harmoniques + 1):
        x += np.sin(ph * h + h * 0.7) / h ** 1.1
    if rugosite:
        x += bruit(d, graine) * rugosite
        x = biquad(x, 3000, 0.7, 'bp') * 0.5 + x * 0.5
    return x


# ============================================================================ sons
def cri(d=1.6, haut=1.0, graine=11):
    def f0(tt):
        k = tt / d
        return haut * (2300 + 700 * np.sin(np.pi * np.minimum(1, k * 1.6)) - 900 * np.maximum(0, k - 0.55) ** 1.3)
    a = voix(f0, d, 8, (31, 0.018), 0.25, graine)
    b = voix(lambda tt: f0(tt) * 1.012, d, 5, (27, 0.02), 0.0, graine + 1) * 0.5
    souffle = biquad(bruit(d, graine + 2), 3200, 1.2) * 0.35
    x = (a + b + souffle) * enveloppe(len(a), 0.05, d * 0.35, d * 0.3)
    x += crepitement(d, graine + 3, 40) * 0.25
    return fondu(norm(x, 0.85))


def cri_court():
    return cri(0.55, 1.1, 21)


def ailes():
    d = 0.5
    n = bruit(d, 31)
    lourd = biquad(n, 260, 0.9, 'lp') * enveloppe(len(n), 0.035, 0.12)
    air = biquad(n, 900, 1.5) * enveloppe(len(n), 0.06, 0.1) * 0.5
    x = lourd * 2.2 + air + crepitement(d, 32, 30) * 0.2
    return fondu(norm(x, 0.8))


def souffle_feu(d=1.25, graine=41):
    n = bruit(d, graine)
    tt = t(d)
    flottement = 0.65 + 0.35 * np.sin(2 * np.pi * 7 * tt + np.sin(2 * np.pi * 2.3 * tt) * 2)
    grondement = biquad(n, 320, 0.8) * 1.6 + biquad(n, 1100, 0.6, 'lp') * 0.9
    x = grondement * flottement * enveloppe(len(n), 0.08, d * 0.5, d * 0.4)
    x += crepitement(d, graine + 1, 90) * 0.45
    return fondu(norm(x, 0.85))


def boule():
    d = 0.85
    n = bruit(d, 51)
    tt = t(d)
    f = 280 + 1600 * (tt / d) ** 0.7
    x = biquad(n, f, 2.2) * enveloppe(len(n), 0.12, 0.3, 0.15) * 1.5
    x += biquad(n, 180, 0.7, 'lp') * enveloppe(len(n), 0.02, 0.15) * 1.2
    x += crepitement(d, 52, 60) * 0.3
    return fondu(norm(x, 0.85))


def boum(d, f, graine, crep=60):
    tt = t(d)
    n = bruit(d, graine)
    basse = np.sin(2 * np.pi * (f * tt - f * 0.4 * tt ** 2)) * np.exp(-tt / (d * 0.22))
    corps = biquad(n, 520, 0.7, 'lp') * enveloppe(len(n), 0.005, d * 0.18)
    x = basse * 1.4 + corps * 1.6 + crepitement(d, graine + 1, crep) * 0.4 * np.exp(-tt / (d * 0.5))
    return fondu(norm(x, 0.92), 0.01)


def impact():
    return boum(1.2, 70, 61)


def onde():
    x = boum(1.7, 48, 71, 90)
    # écho de l'onde
    e = np.zeros_like(x)
    k = int(SR * 0.18)
    e[k:] = x[:-k] * 0.35
    return norm(x + e, 0.92)


def plongee():
    d = 1.5
    n = bruit(d, 81)
    tt = t(d)
    f = 500 + 2200 * (tt / d) ** 1.6
    x = biquad(n, f, 6) * (0.2 + 0.8 * (tt / d) ** 1.5)
    x += biquad(n, 400, 0.7, 'lp') * (tt / d) ** 2 * 0.8
    x *= np.minimum(1, (d - tt) / 0.08)
    return fondu(norm(x, 0.85))


def tempete():
    d = 2.2
    n = bruit(d, 91)
    tt = t(d)
    tourne = 0.6 + 0.4 * np.sin(2 * np.pi * 1.1 * tt) * np.sin(2 * np.pi * 0.37 * tt + 1)
    f = 420 + 260 * np.sin(2 * np.pi * 0.9 * tt)
    x = biquad(n, f, 1.4) * 1.5 * tourne + biquad(n, 160, 0.7, 'lp') * 1.2
    x += crepitement(d, 92, 120) * 0.4
    x *= enveloppe(len(n), 0.25, 9, 0)
    return fondu(norm(x, 0.85), 0.15)


def oeuf():
    d = 1.6
    tt = t(d)
    x = np.zeros_like(tt)
    for debut, f in ((0.05, 52), (0.32, 46), (0.95, 52), (1.22, 46)):
        k = tt - debut
        m = k >= 0
        x[m] += np.sin(2 * np.pi * f * k[m]) * np.exp(-k[m] / 0.09)
    x += crepitement(d, 101, 70) * 0.35
    x += biquad(bruit(d, 102), 200, 0.7, 'lp') * 0.3
    return fondu(norm(x, 0.88))


def craquement():
    d = 0.35
    n = bruit(d, 111)
    x = biquad(n, 2800, 0.6, 'hp') * enveloppe(len(n), 0.002, 0.03)
    x += biquad(n, 600, 1.0) * enveloppe(len(n), 0.002, 0.07) * 0.8
    x += crepitement(d, 112, 80, 1.5) * 0.4
    return fondu(norm(x, 0.88), 0.005)


def renaissance():
    d = 2.6
    tt = t(d)
    n = bruit(d, 121)
    gonfle = (tt / d) ** 1.4
    x = biquad(n, 300 + 1500 * gonfle, 1.0) * gonfle * 1.8
    acc = np.zeros_like(tt)
    for r in (1.0, 1.25, 1.5):
        acc += voix(lambda u, r=r: (180 + 460 * (u / d) ** 1.2) * r, d, 5, (6, 0.01), 0, 122) * 0.35
    x += acc * gonfle
    fin = cri(1.1, 1.05, 123)
    k = int(SR * 1.45)
    x[k:k + len(fin)] += fin[:len(x) - k] * 1.4
    x += crepitement(d, 124, 110) * 0.35
    return fondu(norm(x, 0.9), 0.05)


def mort():
    d = 2.6
    x = voix(lambda tt: 2100 * np.exp(-tt / 1.1) + 380, d, 7, (24, 0.03), 0.3, 131) * enveloppe(int(SR * d), 0.04, 0.8, 0.5)
    tt = t(d)
    x += biquad(bruit(d, 132), 1800, 0.6, 'lp') * np.exp(-tt / 1.2) * 0.4      # souffle qui s'éteint
    x += crepitement(d, 133, 50) * np.exp(-tt / 0.9) * 0.4
    return fondu(norm(x, 0.85), 0.08)


SONS = {
    'cri': cri, 'cri_court': cri_court, 'ailes': ailes, 'souffle': souffle_feu, 'boule': boule, 'impact': impact,
    'plongee': plongee, 'onde': onde, 'tempete': tempete, 'oeuf': oeuf, 'craquement': craquement,
    'renaissance': renaissance, 'mort': mort,
}


def ecrire_ogg(x, chemin, empreintes):
    brut = (np.clip(x, -1, 1) * 32000).astype('<i2').tobytes()
    h = hashlib.sha1(brut).hexdigest()
    nom = os.path.basename(chemin)
    if empreintes.get(nom) == h and os.path.exists(chemin):
        return
    empreintes[nom] = h
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as f:
        tmp = f.name
    try:
        with wave.open(tmp, 'wb') as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(brut)
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', tmp, '-c:a', 'libvorbis', '-q:a', '4', chemin], check=True)
    finally:
        os.remove(tmp)


def ecrire():
    dossier = os.path.join(ASSETS, 'sounds')
    os.makedirs(dossier, exist_ok=True)
    f_emp = os.path.join(ICI, 'sons_empreintes.json')
    empreintes = json.load(open(f_emp)) if os.path.exists(f_emp) else {}
    table = {}
    for nom, fn in SONS.items():
        ecrire_ogg(fn(), os.path.join(dossier, nom + '.ogg'), empreintes)
        table[nom] = {'category': 'hostile', 'subtitle': 'subtitles.hxrpphenix.' + nom, 'sounds': ['hxrpphenix:' + nom]}
    with open(f_emp, 'w') as f:
        json.dump({k: v for k, v in sorted(empreintes.items()) if k[:-4] in SONS}, f, indent=1)
        f.write('\n')
    with open(os.path.join(ASSETS, 'sounds.json'), 'w', encoding='utf-8') as f:
        json.dump(table, f, indent=1, ensure_ascii=False)
        f.write('\n')
    print('%d sons écrits' % len(SONS))


if __name__ == '__main__':
    ecrire()
