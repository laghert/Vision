// Target release date: Sobota, 26 września 2026 00:00:00 Europe/Warsaw
const targetDate = new Date('2026-09-26T00:00:00+02:00').getTime();

const daysEl = document.getElementById('cd-days');
const hoursEl = document.getElementById('cd-hours');
const minutesEl = document.getElementById('cd-minutes');
const secondsEl = document.getElementById('cd-seconds');

const lockedStateEl = document.getElementById('locked-state');
const unlockedStateEl = document.getElementById('unlocked-state');
const simulateUnlockBtn = document.getElementById('simulate-unlock-btn');

let isSimulatedUnlocked = false;

function updateCountdown() {
  if (isSimulatedUnlocked) return;

  const now = new Date().getTime();
  const distance = targetDate - now;

  if (distance <= 0) {
    // Reached countdown date
    daysEl.textContent = '00';
    hoursEl.textContent = '00';
    minutesEl.textContent = '00';
    secondsEl.textContent = '00';

    unlockRelease();
    return;
  }

  const days = Math.floor(distance / (1000 * 60 * 60 * 24));
  const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
  const seconds = Math.floor((distance % (1000 * 60)) / 1000);

  daysEl.textContent = String(days).padStart(2, '0');
  hoursEl.textContent = String(hours).padStart(2, '0');
  minutesEl.textContent = String(minutes).padStart(2, '0');
  secondsEl.textContent = String(seconds).padStart(2, '0');
}

function unlockRelease() {
  lockedStateEl.classList.add('hidden');
  unlockedStateEl.classList.remove('hidden');
}

function lockRelease() {
  unlockedStateEl.classList.add('hidden');
  lockedStateEl.classList.remove('hidden');
}

// Simulating toggle for previewing unlocked state
if (simulateUnlockBtn) {
  simulateUnlockBtn.addEventListener('click', () => {
    isSimulatedUnlocked = !isSimulatedUnlocked;
    if (isSimulatedUnlocked) {
      daysEl.textContent = '00';
      hoursEl.textContent = '00';
      minutesEl.textContent = '00';
      secondsEl.textContent = '00';
      unlockRelease();
      simulateUnlockBtn.textContent = 'Wróć do aktywnego odliczania';
    } else {
      lockRelease();
      simulateUnlockBtn.textContent = 'Podgląd odblokowanego stanu';
      updateCountdown();
    }
  });
}

// Start countdown
updateCountdown();
setInterval(updateCountdown, 1000);

// Tabbed Gallery Navigation
const tabs = document.querySelectorAll('.gallery-tab');
const views = document.querySelectorAll('.screen-view');

tabs.forEach((tab) => {
  tab.addEventListener('click', () => {
    const targetId = tab.getAttribute('data-target');

    tabs.forEach((t) => t.classList.remove('active'));
    views.forEach((v) => v.classList.remove('active'));

    tab.classList.add('active');
    const targetView = document.getElementById(targetId);
    if (targetView) {
      targetView.classList.add('active');
    }
  });
});
