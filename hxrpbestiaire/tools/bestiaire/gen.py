"""
Génère les ressources du bestiaire (assets/hxrpbestiaire) :
- modèles GeckoLib depuis les .bbmodel de tools/bestiaire/blockbench (bbconv.py), plus les animations « à créer »
  (anims.py), et durees.properties (durée de chaque animation, lue par le serveur) ;
- icônes des butins, œufs et projectiles, modèles 3D et textures des appâts (art.py) ;
- sons (sons.py), traductions, et la copie du classeur des boss.

    python3 tools/bestiaire/gen.py            # tout
    python3 tools/bestiaire/gen.py modeles    # seulement les modèles et animations
"""
import json
import os
import shutil
import sys

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
import anims as creations  # noqa: E402
import bbconv  # noqa: E402

RACINE = os.path.normpath(os.path.join(ICI, '..', '..'))
ASSETS = os.path.join(RACINE, 'src', 'main', 'resources', 'assets', 'hxrpbestiaire')
SOURCES = os.path.join(ICI, 'blockbench')

# identifiant GeckoLib → modèle Blockbench (lot 1 ; les autres boss arriveront par lots)
MODELES = {
    'reine_guepe': 'wasp_boss',
    'abomination': 'abomination',
    'necromancien': 'necro',
    'archer_squelette': 'skeletonarcher',
}


def ecrire_json(chemin, d, compact=False):
    os.makedirs(os.path.dirname(chemin), exist_ok=True)
    with open(chemin, 'w', encoding='utf-8') as f:
        if compact:
            json.dump(d, f, separators=(',', ':'), ensure_ascii=False)
        else:
            json.dump(d, f, indent=1, ensure_ascii=False)
            f.write('\n')


def modeles():
    durees = {}
    for ident, source in MODELES.items():
        geo, an, im = bbconv.convertir(os.path.join(SOURCES, source + '.bbmodel'), ident, ASSETS)
        courtes = {k.split('.', 2)[2]: v for k, v in an['animations'].items()}
        if ident in creations.CREATIONS:
            for nom, a in creations.CREATIONS[ident](courtes).items():
                an['animations']['animation.%s.%s' % (ident, nom)] = a
                courtes[nom] = a
        ecrire_json(os.path.join(ASSETS, 'animations', ident + '.animation.json'), an, compact=True)
        for nom, a in sorted(courtes.items()):
            durees['%s.%s' % (ident, nom)] = max(1, int(round(float(a['animation_length']) * 20)))
    with open(os.path.join(ASSETS, 'durees.properties'), 'w', encoding='utf-8') as f:
        f.write('# Durée (ticks) de chaque animation, écrite par tools/bestiaire/gen.py : le serveur cale les attaques dessus.\n')
        for k in sorted(durees):
            f.write('%s=%d\n' % (k, durees[k]))
    print('%d modèles, %d animations' % (len(MODELES), len(durees)))


def classeur():
    src = os.path.join(RACINE, '..', 'hxrpphenix', 'src', 'main', 'resources', 'assets', 'hxrpphenix', 'bestiaire_stats.xlsx')
    shutil.copyfile(src, os.path.join(ASSETS, 'bestiaire_stats.xlsx'))


if __name__ == '__main__':
    quoi = sys.argv[1:] or ['tout']
    if 'tout' in quoi or 'modeles' in quoi:
        modeles()
    if 'tout' in quoi:
        classeur()
        import art
        art.tout(ASSETS)
        import sons
        sons.ecrire(ASSETS)
