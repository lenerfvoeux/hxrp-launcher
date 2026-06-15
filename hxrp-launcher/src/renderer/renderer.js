// ============================================
// HxRP Launcher - Interface logic (renderer)
// ============================================
// Ce script gère toute l'interaction utilisateur :
// - Contrôles de la fenêtre
// - Navigation entre tabs
// - Bouton Jouer
// - Validation du pseudo
// ============================================

// --------- Contrôles de la fenêtre custom ---------
document.getElementById('btn-minimize').addEventListener('click', () => {
  window.launcherAPI.minimizeWindow();
});

document.getElementById('btn-maximize').addEventListener('click', () => {
  window.launcherAPI.maximizeWindow();
});

document.getElementById('btn-close').addEventListener('click', () => {
  window.launcherAPI.closeWindow();
});

// --------- Navigation par tabs ---------
const navItems = document.querySelectorAll('.nav-item');
const tabs = document.querySelectorAll('.tab');

navItems.forEach(item => {
  item.addEventListener('click', () => {
    const targetTab = item.dataset.tab;

    // Update nav active state
    navItems.forEach(n => n.classList.remove('active'));
    item.classList.add('active');

    // Switch tab
    tabs.forEach(tab => {
      if (tab.dataset.tabContent === targetTab) {
        tab.classList.add('tab-active');
      } else {
        tab.classList.remove('tab-active');
      }
    });
  });
});

// --------- Liens sociaux (Discord etc) ---------
document.querySelectorAll('.social-link').forEach(link => {
  link.addEventListener('click', () => {
    const url = link.dataset.url;
    if (url) window.launcherAPI.openExternal(url);
  });
});

// --------- Pseudo : restaurer le dernier utilisé ---------
const usernameInput = document.getElementById('username-input');
const savedUsername = localStorage.getItem('hxrp.username');
if (savedUsername) {
  usernameInput.value = savedUsername;
}

usernameInput.addEventListener('input', () => {
  // Sauvegarde au fur et à mesure
  localStorage.setItem('hxrp.username', usernameInput.value);
});

// Validation pseudo Minecraft : 3-16 caractères alphanumériques + underscore
function isValidUsername(name) {
  return /^[a-zA-Z0-9_]{3,16}$/.test(name);
}

// --------- Microsoft login (placeholder, sera implémenté session 4) ---------
document.getElementById('btn-microsoft').addEventListener('click', () => {
  alert('Microsoft login sera disponible dans la prochaine version du launcher.\n\nPour l\'instant, utilise un pseudo libre.');
});

// --------- Bouton JOUER ---------
const btnPlay = document.getElementById('btn-play');
const progressWrap = document.getElementById('progress-wrap');
const progressFill = document.getElementById('progress-fill');
const progressText = document.getElementById('progress-text');

btnPlay.addEventListener('click', async () => {
  const username = usernameInput.value.trim();

  if (!isValidUsername(username)) {
    alert('Pseudo invalide.\n\nEntre un pseudo de 3 à 16 caractères (lettres, chiffres, underscore).');
    usernameInput.focus();
    return;
  }

  // Désactive le bouton et affiche la barre de progression
  btnPlay.disabled = true;
  progressWrap.hidden = false;
  progressText.textContent = 'Préparation du lancement...';

  try {
    const result = await window.launcherAPI.launchGame({
      username: username,
      authType: 'offline' // 'microsoft' en session 4
    });

    if (result.success) {
      progressText.textContent = 'Jeu lancé ! Bon RP 🎮';
    } else {
      progressText.textContent = result.message || 'Une erreur est survenue.';
    }
  } catch (err) {
    console.error(err);
    progressText.textContent = 'Erreur : ' + (err.message || err);
  } finally {
    // Réactive le bouton après 3 secondes
    setTimeout(() => {
      btnPlay.disabled = false;
      progressWrap.hidden = true;
      progressFill.style.width = '0%';
    }, 3000);
  }
});

// --------- Écoute la progression du lancement (préparé pour session 2) ---------
window.launcherAPI.onLaunchProgress((data) => {
  if (data.percent !== undefined) {
    progressFill.style.width = data.percent + '%';
  }
  if (data.text) {
    progressText.textContent = data.text;
  }
});

// --------- Statut serveur (placeholder, sera implémenté session 3) ---------
// Pour l'instant on simule des chiffres pour que ça ait l'air vivant
setTimeout(() => {
  document.getElementById('stat-online').textContent = '—';
  document.getElementById('stat-status').textContent = '●';
  document.getElementById('stat-status').style.color = 'var(--text-tertiary)';
}, 500);

console.log('[HxRP Launcher] Interface chargée');
