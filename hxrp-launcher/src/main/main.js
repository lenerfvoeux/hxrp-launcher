// ============================================
// HxRP Launcher - Processus principal
// ============================================
// Ce fichier est le point d'entrée Electron.
// Il gère la création de la fenêtre principale
// et toute la communication avec le système.
// ============================================

const { app, BrowserWindow, ipcMain, shell } = require('electron');
const path = require('path');

// Mode dev : ouvre la console des devtools auto
const isDev = process.argv.includes('--dev');

let mainWindow = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1100,
    height: 680,
    minWidth: 1000,
    minHeight: 600,
    frame: false, // Pas de barre de titre Windows par défaut (on fera la nôtre)
    resizable: true,
    backgroundColor: '#0a0a0f',
    icon: path.join(__dirname, '../renderer/assets/icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true, // Sécurité : isole le renderer du main
      nodeIntegration: false,
      sandbox: false
    },
    show: false // On affichera quand la page sera prête
  });

  // Charge l'interface HTML
  mainWindow.loadFile(path.join(__dirname, '../renderer/index.html'));

  // Affiche la fenêtre une fois prête (évite le flash blanc)
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
    if (isDev) {
      mainWindow.webContents.openDevTools({ mode: 'detach' });
    }
  });

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// ============================================
// IPC : communication renderer -> main
// ============================================

// Contrôles de la fenêtre custom (minimize, maximize, close)
ipcMain.on('window-minimize', () => {
  if (mainWindow) mainWindow.minimize();
});

ipcMain.on('window-maximize', () => {
  if (!mainWindow) return;
  if (mainWindow.isMaximized()) {
    mainWindow.unmaximize();
  } else {
    mainWindow.maximize();
  }
});

ipcMain.on('window-close', () => {
  if (mainWindow) mainWindow.close();
});

// Ouvrir un lien externe dans le navigateur (pas dans Electron)
ipcMain.on('open-external', (event, url) => {
  shell.openExternal(url);
});

// Placeholder : sera implémenté dans la session 2
ipcMain.handle('launch-game', async (event, options) => {
  console.log('[Launch] Demande de lancement reçue:', options);
  // TODO Session 2 : vérifier Java, télécharger mods, lancer le jeu
  return { success: false, message: 'Le lancement sera implémenté dans la prochaine session !' };
});

// ============================================
// Cycle de vie de l'app
// ============================================

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
