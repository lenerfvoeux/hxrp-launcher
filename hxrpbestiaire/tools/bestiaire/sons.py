"""
Sons du bestiaire, synthétisés puis encodés en Ogg Vorbis (ffmpeg) : bourdon de la Reine-guêpe et de ses soldats,
stridulation, râle et gargouillis de l'Abomination, murmure et plainte du Nécromancien, incantation. Écrit aussi
sounds.json. Les empreintes (SHA-1 des échantillons) évitent de réencoder un son inchangé.
"""
import hashlib
import json
import os
import subprocess
import tempfile
import wave

import numpy as np

ICI = os.path.dirname(os.path.abspath(__file__))
SR = 44100


def t(d):
    return np.arange(int(SR * d)) / SR


def bruit(d, graine):
    return np.random.RandomState(graine).uniform(-1, 1, int(SR * d))


def enveloppe(n, attaque, declin, maintien=0.0):
    x = np.arange(n) / SR
    e = np.minimum(1, x / max(attaque, 1e-4))
    return e * np.exp(-np.maximum(0, x - attaque - maintien) / max(declin, 1e-4))


def passe_bas(x, f):
    """Filtre passe-bas d'ordre 1 (rapide)."""
    a = np.exp(-2 * np.pi * f / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def norm(x, pic=0.9):
    m = np.max(np.abs(x)) or 1
    return x / m * pic


def fondu(x, d=0.03):
    n = min(len(x) // 2, int(SR * d))
    x = x.copy()
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def scie(f, tt):
    ph = np.cumsum(f) / SR if np.ndim(f) else f * tt
    return 2 * (ph % 1.0) - 1


# ============================================================================ sons
def bourdon(f0=118, d=1.6, graine=1):
    """Battement d'ailes d'insecte : dent de scie grave modulée, harmoniques rêches."""
    tt = t(d)
    f = f0 * (1 + 0.03 * np.sin(2 * np.pi * 3.1 * tt) + 0.01 * np.sin(2 * np.pi * 7.3 * tt))
    x = scie(f, tt) * 0.7 + scie(f * 2.01, tt) * 0.25
    x *= 0.7 + 0.3 * np.sin(2 * np.pi * f0 / 2 * tt)
    x = passe_bas(x, 2600) + bruit(d, graine) * 0.05
    x *= enveloppe(len(x), 0.15, 0.5, d - 0.7)
    return fondu(norm(x, 0.8), 0.1)


def bourdon_aigu():
    return bourdon(236, 0.9, 2)


def stridulation():
    """Cri d'insecte blessé : grésillement rapide et aigu."""
    d = 0.6
    tt = t(d)
    pulses = (np.sin(2 * np.pi * 38 * tt) > 0.3).astype(float)
    x = scie(1400 + 600 * np.sin(2 * np.pi * 2 * tt), tt) * pulses * 0.6 + bruit(d, 3) * pulses * 0.4
    x = passe_bas(x, 5000) * enveloppe(len(x), 0.01, 0.25, 0.15)
    return fondu(norm(x, 0.85), 0.01)


def reine_mort():
    """La reine s'effondre : bourdon qui ralentit et s'éteint, craquement de chitine."""
    d = 2.4
    tt = t(d)
    f = 150 * np.exp(-tt / 1.4) + 30
    x = scie(f, tt) * 0.7 * np.exp(-tt / 1.2)
    x += bruit(d, 4) * np.exp(-tt / 0.08) * 0.8
    return fondu(norm(passe_bas(x, 2200), 0.85), 0.05)


def rale():
    """Râle humide de l'Abomination : grondement grave, souffle, glouglous."""
    d = 1.8
    tt = t(d)
    f = 62 + 10 * np.sin(2 * np.pi * 1.3 * tt)
    x = scie(f, tt) * 0.6 * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * tt))
    x += passe_bas(bruit(d, 5), 700) * 1.5
    x *= enveloppe(len(x), 0.2, 0.6, 0.8)
    return fondu(norm(passe_bas(x, 1200), 0.85), 0.1)


def gargouillis():
    d = 0.7
    tt = t(d)
    bulles = np.zeros_like(tt)
    rs = np.random.RandomState(6)
    for _ in range(9):
        a = rs.uniform(0, d - 0.1)
        f = rs.uniform(180, 420)
        k = tt - a
        m = k >= 0
        bulles[m] += np.sin(2 * np.pi * f * k[m] * (1 + k[m] * 3)) * np.exp(-k[m] / 0.04)
    x = bulles + scie(80, tt) * 0.4 * enveloppe(len(tt), 0.02, 0.2)
    return fondu(norm(x, 0.85), 0.01)


def chair_mort():
    d = 2.0
    tt = t(d)
    x = scie(70 * np.exp(-tt / 1.2) + 25, tt) * np.exp(-tt / 0.9) * 0.7
    x += passe_bas(bruit(d, 7), 500) * np.exp(-tt / 0.6) * 1.5
    return fondu(norm(passe_bas(x, 900), 0.85), 0.05)


def murmure():
    """Murmure du Nécromancien : souffle chuchoté filtré, voix grave qui ondule."""
    d = 2.2
    tt = t(d)
    souffle = passe_bas(bruit(d, 8), 3000) - passe_bas(bruit(d, 8), 600)
    syll = 0.5 + 0.5 * np.sin(2 * np.pi * 4.2 * tt) ** 2
    voix = np.sin(2 * np.pi * (96 + 6 * np.sin(2 * np.pi * 0.7 * tt)) * tt) * 0.3
    x = (souffle * 1.4 + voix) * syll * enveloppe(len(tt), 0.3, 0.6, 1.0)
    return fondu(norm(x, 0.8), 0.15)


def plainte():
    d = 0.8
    tt = t(d)
    f = 220 * np.exp(-tt / 0.6) + 90
    x = (np.sin(2 * np.pi * np.cumsum(f) / SR) + 0.4 * np.sin(4 * np.pi * np.cumsum(f) / SR)) * enveloppe(len(tt), 0.02, 0.3, 0.2)
    x += passe_bas(bruit(d, 9), 2000) * 0.3 * enveloppe(len(tt), 0.01, 0.1)
    return fondu(norm(x, 0.85), 0.02)


def ame_mort():
    d = 3.0
    tt = t(d)
    f = 300 * np.exp(-tt / 1.5) + 60
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-tt / 1.4) * 0.5
    x += (passe_bas(bruit(d, 10), 2500) - passe_bas(bruit(d, 10), 400)) * np.exp(-tt / 1.2)
    return fondu(norm(x, 0.85), 0.1)


def incantation():
    """Incantation : accord grave qui monte, chœur d'harmoniques, souffle."""
    d = 2.0
    tt = t(d)
    x = np.zeros_like(tt)
    for r in (1, 1.5, 2, 3):
        x += np.sin(2 * np.pi * (70 * r) * (1 + 0.15 * tt / d) * tt) / r
    x *= np.minimum(1, tt / 0.6) * np.exp(-np.maximum(0, tt - 1.4) / 0.3)
    x += (passe_bas(bruit(d, 11), 1500) - passe_bas(bruit(d, 11), 300)) * 0.4
    return fondu(norm(x, 0.85), 0.05)


# ============================================================================ lot 2
def sifflement():
    """Xerra : sifflement d'araignée géante, souffle aigu et cliquetis de chélicères."""
    d = 1.6
    tt = t(d)
    souffle = passe_bas(bruit(d, 12), 7000) - passe_bas(bruit(d, 12), 2500)
    clics = np.zeros_like(tt)
    rs = np.random.RandomState(13)
    for _ in range(14):
        a = rs.uniform(0, d - 0.05)
        k = tt - a
        m = k >= 0
        clics[m] += np.sin(2 * np.pi * 2200 * k[m]) * np.exp(-k[m] / 0.006)
    x = souffle * 1.3 * enveloppe(len(tt), 0.2, 0.5, 0.8) + clics * 0.6
    return fondu(norm(x, 0.8), 0.08)


def cri_xerra():
    """Xerra blessée : cri strident, mi-femme mi-insecte."""
    d = 0.9
    tt = t(d)
    f = 900 * np.exp(-tt / 0.7) + 380
    x = scie(f, tt) * 0.5 + np.sin(2 * np.pi * np.cumsum(f * 1.5) / SR) * 0.3
    x = x * (0.7 + 0.3 * np.sin(2 * np.pi * 32 * tt)) + bruit(d, 14) * 0.15
    return fondu(norm(passe_bas(x, 6000) * enveloppe(len(tt), 0.01, 0.3, 0.3), 0.85), 0.02)


def xerra_mort():
    d = 2.6
    tt = t(d)
    f = 700 * np.exp(-tt / 0.9) + 90
    x = scie(f, tt) * 0.5 * np.exp(-tt / 1.3)
    x += (passe_bas(bruit(d, 15), 3000) - passe_bas(bruit(d, 15), 500)) * np.exp(-tt / 0.8) * 0.8
    x += bruit(d, 16) * np.exp(-np.maximum(0, tt - 1.6) / 0.05) * (tt > 1.6) * 0.6
    return fondu(norm(x, 0.85), 0.05)


def craquement_glace():
    """Roi des glaces : grondement sourd, glace qui craque et tinte."""
    d = 2.0
    tt = t(d)
    x = passe_bas(bruit(d, 17), 200) * 3 * enveloppe(len(tt), 0.3, 0.6, 0.8)
    rs = np.random.RandomState(18)
    for _ in range(10):
        a = rs.uniform(0, d - 0.2)
        k = tt - a
        m = k >= 0
        f = rs.uniform(1800, 4200)
        x[m] += (np.sin(2 * np.pi * f * k[m]) * 0.4 + bruit(d, 19)[m] * 0.5) * np.exp(-k[m] / 0.03)
    return fondu(norm(x, 0.8), 0.1)


def grognement_glacial():
    d = 1.4
    tt = t(d)
    f = 70 + 12 * np.sin(2 * np.pi * 2.5 * tt)
    x = scie(f, tt) * (0.6 + 0.4 * np.sin(2 * np.pi * 11 * tt)) + passe_bas(bruit(d, 20), 900) * 1.2
    x += (passe_bas(bruit(d, 21), 6000) - passe_bas(bruit(d, 21), 3000)) * 0.4
    return fondu(norm(passe_bas(x, 2500) * enveloppe(len(tt), 0.08, 0.4, 0.6), 0.85), 0.05)


def roi_mort():
    """La glace du Roi vole en éclats : fracas, puis pluie d'éclats qui tintent."""
    d = 2.8
    tt = t(d)
    x = bruit(d, 22) * np.exp(-tt / 0.15) * 0.9 + passe_bas(bruit(d, 23), 150) * np.exp(-tt / 0.6) * 3
    rs = np.random.RandomState(24)
    for _ in range(30):
        a = rs.uniform(0.1, d - 0.2)
        k = tt - a
        m = k >= 0
        x[m] += np.sin(2 * np.pi * rs.uniform(2500, 6000) * k[m]) * np.exp(-k[m] / 0.02) * 0.3 * np.exp(-a / 1.2)
    return fondu(norm(x, 0.85), 0.05)


def couinement():
    """Rat des glaces : couinement aigu, deux notes."""
    d = 0.35
    tt = t(d)
    f = np.where(tt < 0.16, 2600 + 900 * tt / 0.16, 3100 - 800 * (tt - 0.16) / 0.19)
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * enveloppe(len(tt), 0.01, 0.08, 0.15)
    x *= (np.abs(tt - 0.16) > 0.015)
    return fondu(norm(x + bruit(d, 25) * 0.05, 0.75), 0.01)


def grognement_gobelin():
    """Gobelin : grognement guttural, court."""
    d = 0.6
    tt = t(d)
    f = 140 + 30 * np.sin(2 * np.pi * 6 * tt)
    x = scie(f, tt) * (0.5 + 0.5 * np.sin(2 * np.pi * 23 * tt)) + passe_bas(bruit(d, 26), 1200) * 0.8
    return fondu(norm(passe_bas(x, 2000) * enveloppe(len(tt), 0.03, 0.2, 0.25), 0.8), 0.02)


def grondement_demon():
    """Démon noir : grondement très grave, souffle qui vibre."""
    d = 2.4
    tt = t(d)
    f = 42 + 5 * np.sin(2 * np.pi * 0.8 * tt)
    x = scie(f, tt) * 0.8 + scie(f * 1.5, tt) * 0.3 + passe_bas(bruit(d, 27), 400) * 2
    x *= 0.7 + 0.3 * np.sin(2 * np.pi * 7 * tt)
    return fondu(norm(passe_bas(x, 900) * enveloppe(len(tt), 0.4, 0.6, 1.2), 0.85), 0.15)


def rugissement_demon():
    d = 1.6
    tt = t(d)
    f = 95 + 60 * np.exp(-((tt - 0.3) / 0.25) ** 2)
    x = scie(f, tt) * 0.7 + scie(f * 2.02, tt) * 0.25 + passe_bas(bruit(d, 28), 2500) * 0.9
    return fondu(norm(passe_bas(x, 3000) * enveloppe(len(tt), 0.05, 0.5, 0.6), 0.9), 0.05)


def demon_mort():
    d = 3.0
    tt = t(d)
    f = 110 * np.exp(-tt / 1.5) + 30
    x = scie(f, tt) * np.exp(-tt / 1.4) * 0.8 + passe_bas(bruit(d, 29), 300) * np.exp(-tt / 1.0) * 2.5
    return fondu(norm(passe_bas(x, 1200), 0.85), 0.1)


SONS = {
    'bourdon': ('Bourdonnement de la Reine-guêpe', bourdon),
    'bourdon_aigu': ('Bourdonnement de guêpe soldat', bourdon_aigu),
    'stridulation': ('Stridulation de guêpe', stridulation),
    'reine_mort': ("La Reine-guêpe s'effondre", reine_mort),
    'rale': ("Râle de l'Abomination", rale),
    'gargouillis': ("Gargouillis de l'Abomination", gargouillis),
    'chair_mort': ("L'Abomination s'affaisse", chair_mort),
    'murmure': ('Murmure du Nécromancien', murmure),
    'plainte': ('Plainte du Nécromancien', plainte),
    'ame_mort': ('Le Nécromancien se dissipe', ame_mort),
    'incantation': ('Incantation', incantation),
    'sifflement': ('Sifflement de Xerra', sifflement),
    'cri_xerra': ('Cri de Xerra', cri_xerra),
    'xerra_mort': ("Xerra s'effondre", xerra_mort),
    'craquement_glace': ('La glace craque', craquement_glace),
    'grognement_glacial': ('Grognement du Roi des glaces', grognement_glacial),
    'roi_mort': ('Le Roi des glaces vole en éclats', roi_mort),
    'couinement': ('Couinement de rat des glaces', couinement),
    'grognement_gobelin': ('Grognement de gobelin', grognement_gobelin),
    'grondement_demon': ('Grondement du Démon noir', grondement_demon),
    'rugissement_demon': ('Rugissement du Démon noir', rugissement_demon),
    'demon_mort': ('Le Démon noir tombe', demon_mort),
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


def ecrire(assets):
    dossier = os.path.join(assets, 'sounds')
    os.makedirs(dossier, exist_ok=True)
    f_emp = os.path.join(ICI, 'sons_empreintes.json')
    empreintes = json.load(open(f_emp)) if os.path.exists(f_emp) else {}
    table = {}
    for nom, (sous_titre, fn) in SONS.items():
        ecrire_ogg(fn(), os.path.join(dossier, nom + '.ogg'), empreintes)
        table[nom] = {'category': 'hostile', 'subtitle': 'subtitles.hxrpbestiaire.' + nom, 'sounds': ['hxrpbestiaire:' + nom]}
    with open(f_emp, 'w') as f:
        json.dump({k: v for k, v in sorted(empreintes.items()) if k[:-4] in SONS}, f, indent=1)
        f.write('\n')
    with open(os.path.join(assets, 'sounds.json'), 'w', encoding='utf-8') as f:
        json.dump(table, f, indent=1, ensure_ascii=False)
        f.write('\n')
    print('%d sons écrits' % len(SONS))
