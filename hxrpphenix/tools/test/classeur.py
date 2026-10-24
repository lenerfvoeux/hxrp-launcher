"""
Copie un classeur des boss (.xlsx) en changeant quelques cases, pour les tests (appât qui arrive en 3 s au lieu de
30 à 90 s…). Les cases sont désignées par l'onglet, la ligne (valeurs des premières colonnes) et l'en-tête :

    python3 classeur.py source.xlsx sortie.xlsx "Appâts|id boss=phenix|Délai min (s)=3" "Appâts|id boss=phenix|Délai max (s)=5"

Le reste du fichier est recopié octet pour octet ; la case modifiée devient un nombre.
"""
import re
import sys
import unicodedata
import zipfile
import xml.etree.ElementTree as ET

NS = '{http://schemas.openxmlformats.org/spreadsheetml/2006/main}'
RNS = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'


def cle(s):
    s = unicodedata.normalize('NFD', s or '')
    s = ''.join(c for c in s if not unicodedata.combining(c))
    return re.sub(r'\s+', ' ', s.replace('’', "'")).strip().lower()


def lettres(ref):
    return re.match(r'[A-Z]+', ref).group(0)


def chemins(z):
    rels = {r.get('Id'): r.get('Target') for r in ET.fromstring(z.read('xl/_rels/workbook.xml.rels'))}
    out = {}
    for sh in ET.fromstring(z.read('xl/workbook.xml')).iter(NS + 'sheet'):
        cible = rels[sh.get(RNS + 'id')]
        out[cle(sh.get('name'))] = cible.lstrip('/') if cible.startswith('/') else 'xl/' + cible
    return out


def lire_lignes(z, chemin, partages):
    lignes = []
    for row in ET.fromstring(z.read(chemin)).iter(NS + 'row'):
        val = {}
        for c in row.iter(NS + 'c'):
            v = c.find(NS + 'v')
            if c.get('t') == 'inlineStr':
                x = ''.join(t.text or '' for t in c.iter(NS + 't'))
            elif v is None:
                continue
            elif c.get('t') == 's':
                x = partages[int(v.text)]
            else:
                x = v.text
            val[lettres(c.get('r'))] = x
        lignes.append((row.get('r'), val))
    return lignes


def modifier(source, sortie, consignes):
    z = zipfile.ZipFile(source)
    partages = []
    if 'xl/sharedStrings.xml' in z.namelist():
        for si in ET.fromstring(z.read('xl/sharedStrings.xml')).iter(NS + 'si'):
            partages.append(''.join(t.text or '' for t in si.iter(NS + 't')))
    feuilles = chemins(z)
    textes = {}
    for consigne in consignes:
        onglet, filtre, case = consigne.split('|')
        chemin = feuilles[cle(onglet)]
        lignes = lire_lignes(z, chemin, partages)
        entetes = {cle(v): k for k, v in lignes[0][1].items()}
        fcol, fval = filtre.split('=')
        ccol, cval = case.split('=')
        col_f, col_c = entetes[cle(fcol)], entetes[cle(ccol)]
        rang = next(r for r, v in lignes[1:] if v.get(col_f) == fval)
        ref = col_c + rang
        x = textes.get(chemin) or z.read(chemin).decode('utf-8')
        cellule = re.compile(r'<c r="%s"([^>]*?)(/>|>.*?</c>)' % ref, re.S)
        attributs = lambda m: re.sub(r'\s+t="[^"]*"', '', m.group(1))
        if cellule.search(x):
            x = cellule.sub(lambda m: '<c r="%s"%s><v>%s</v></c>' % (ref, attributs(m), cval), x, count=1)
        else:
            raise SystemExit('case %s introuvable dans %s (ligne vide ?)' % (ref, chemin))
        textes[chemin] = x
        print('%s : %s = %s' % (onglet, ref, cval))
    with zipfile.ZipFile(sortie, 'w', zipfile.ZIP_DEFLATED) as out:
        for info in z.infolist():
            data = textes[info.filename].encode('utf-8') if info.filename in textes else z.read(info.filename)
            out.writestr(info, data)


if __name__ == '__main__':
    modifier(sys.argv[1], sys.argv[2], sys.argv[3:])
