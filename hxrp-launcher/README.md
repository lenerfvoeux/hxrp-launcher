# HxRP Launcher

Launcher officiel du serveur **Hunter x RP** (Minecraft 1.12.2 + Forge + modpack custom).

## État du projet

🚧 **En développement** — Session 1 sur 5 (UI + scaffold)

### Roadmap

- [x] **Session 1** : Scaffold Electron + UI complète
- [ ] Session 2 : Téléchargement Java/Zulu + mods + Forge
- [ ] Session 3 : Lancement du jeu + auto-connexion serveur
- [ ] Session 4 : Authentification Microsoft + offline mode
- [ ] Session 5 : Auto-update + packaging .exe distribuable

## Installation pour développement

### Prérequis

- **Node.js 18+** (télécharge depuis https://nodejs.org)
- **Windows** (le launcher cible Windows en priorité)

### Lancer le launcher en mode dev

```bash
# Dans le dossier hxrp-launcher
npm install
npm run dev
```

La fenêtre du launcher s'ouvre. Les DevTools (F12) s'ouvrent aussi automatiquement en mode dev.

### Lancement normal (sans DevTools)

```bash
npm start
```

## Structure du projet

```
hxrp-launcher/
├── package.json              # Configuration npm + Electron
├── src/
│   ├── main/
│   │   ├── main.js           # Processus principal (gère la fenêtre)
│   │   └── preload.js        # Bridge sécurisé main ↔ renderer
│   └── renderer/
│       ├── index.html        # Structure HTML
│       ├── renderer.js       # Logique de l'interface
│       ├── styles/
│       │   └── main.css      # Design system HxRP
│       └── assets/           # Images, icônes (à venir)
└── README.md
```

## Architecture technique

Le launcher utilise **Electron** : c'est essentiellement Chrome embarqué + Node.js. Tu écris du HTML/CSS/JS comme pour une app web, mais ça tourne comme une vraie app desktop avec accès au système de fichiers.

Deux processus communiquent via IPC (Inter-Process Communication) :

- **Main process** (`src/main/`) : a accès au système (fichiers, lancement de processus, etc).
- **Renderer process** (`src/renderer/`) : c'est le HTML/JS de l'interface, isolé pour la sécurité.

Le **preload script** est le pont sécurisé entre les deux : il expose une API limitée (`window.launcherAPI`) que le HTML peut appeler.

## Build pour distribution

```bash
# Génère un installeur Windows (.exe NSIS)
npm run build

# Génère une version portable (pas d'installation)
npm run build:portable
```

Le résultat est dans le dossier `dist/`.

## Pour Ilan (notes de dev)

- Le design utilise **vanilla CSS** avec variables (`:root`) pour pouvoir tout retoucher facilement
- Le jaune signature HxH est défini sur `--accent: #f7c331`
- La typo est en system fonts pour la perf, peut être remplacée par une custom plus tard
- Toutes les actions placeholder (Microsoft login, lancement) sont commentées avec `TODO Session X`
