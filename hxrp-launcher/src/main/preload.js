// ============================================
// HxRP Launcher - Preload script
// ============================================
// Ce script s'exécute AVANT que la page HTML
// se charge, dans un contexte sécurisé.
// Il expose une API limitée et sûre au renderer.
// ============================================

const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('launcherAPI', {
  // Contrôles de la fenêtre
  minimizeWindow: () => ipcRenderer.send('window-minimize'),
  maximizeWindow: () => ipcRenderer.send('window-maximize'),
  closeWindow: () => ipcRenderer.send('window-close'),

  // Liens externes (Discord, site, etc)
  openExternal: (url) => ipcRenderer.send('open-external', url),

  // Lancer le jeu (sera plus complet en session 2)
  launchGame: (options) => ipcRenderer.invoke('launch-game', options),

  // Écouter les events du main (progression téléchargement, etc)
  onLaunchProgress: (callback) => {
    ipcRenderer.on('launch-progress', (event, data) => callback(data));
  }
});
