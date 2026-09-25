/**
 * Vision Ultra JavaScript — Laghert Labs Ecosystem
 * High performance, zero external libraries, Web Audio procedural synthesis.
 */

(function () {
  'use strict';

  // --- 1. Procedural Web Audio Sound Engine (Tactile Haptics) ---
  class SoundEngine {
    constructor() {
      this.ctx = null;
      this.enabled = localStorage.getItem('vision_sound_enabled') !== 'false';
    }

    init() {
      if (!this.ctx && typeof AudioContext !== 'undefined') {
        this.ctx = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (this.ctx && this.ctx.state === 'suspended') {
        this.ctx.resume();
      }
    }

    toggle() {
      this.enabled = !this.enabled;
      localStorage.setItem('vision_sound_enabled', this.enabled);
      if (this.enabled) this.playClick();
      return this.enabled;
    }

    playClick() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'sine';
      osc.frequency.setValueAtTime(340, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(120, this.ctx.currentTime + 0.016);

      gain.gain.setValueAtTime(0.18, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.016);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.016);
    }

    playDetent() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(420, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(180, this.ctx.currentTime + 0.012);

      gain.gain.setValueAtTime(0.12, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.012);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.012);
    }

    playPop() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;

      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();

      osc.type = 'triangle';
      osc.frequency.setValueAtTime(320, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(580, this.ctx.currentTime + 0.06);

      gain.gain.setValueAtTime(0.15, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.06);

      osc.connect(gain);
      gain.connect(this.ctx.destination);

      osc.start();
      osc.stop(this.ctx.currentTime + 0.06);
    }

    playSuccess() {
      if (!this.enabled) return;
      this.init();
      if (!this.ctx) return;

      const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
      notes.forEach((freq, idx) => {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, this.ctx.currentTime + idx * 0.06);

        gain.gain.setValueAtTime(0.1, this.ctx.currentTime + idx * 0.06);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + idx * 0.06 + 0.18);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start(this.ctx.currentTime + idx * 0.06);
        osc.stop(this.ctx.currentTime + idx * 0.06 + 0.18);
      });
    }
  }

  const sound = new SoundEngine();

  // --- 2. Sound Toggle Button ---
  const soundToggleBtn = document.getElementById('sound-toggle');
  const soundLabel = document.getElementById('sound-label');

  function updateSoundUI() {
    if (soundToggleBtn && soundLabel) {
      if (sound.enabled) {
        soundToggleBtn.classList.add('active');
        soundLabel.textContent = 'Dźwięk: Wł.';
      } else {
        soundToggleBtn.classList.remove('active');
        soundLabel.textContent = 'Wyciszony';
      }
    }
  }
  updateSoundUI();

  if (soundToggleBtn) {
    soundToggleBtn.addEventListener('pointerdown', () => {
      sound.toggle();
      updateSoundUI();
    });
  }

  // --- 3. Material You Dynamic Accent Picker ---
  const swatches = document.querySelectorAll('.my-swatch');
  const savedColor = localStorage.getItem('vision_accent_color') || 'purple';

  function applyAccentColor(colorName) {
    document.documentElement.setAttribute('data-accent', colorName);
    localStorage.setItem('vision_accent_color', colorName);
    swatches.forEach(s => {
      if (s.getAttribute('data-color') === colorName) s.classList.add('active');
      else s.classList.remove('active');
    });
  }
  applyAccentColor(savedColor);

  swatches.forEach(swatch => {
    swatch.addEventListener('pointerdown', () => {
      const color = swatch.getAttribute('data-color');
      sound.playClick();
      applyAccentColor(color);
    });
  });

  // --- 4. Live School Widget (Ticking Lesson & Bell) ---
  let lessonRemainingSeconds = 11 * 60 + 24;
  const totalLessonSeconds = 45 * 60;
  const lswCountdownEl = document.getElementById('lsw-countdown');
  const lswProgressEl = document.getElementById('lsw-progress');

  function tickSchoolWidget() {
    if (lessonRemainingSeconds > 0) {
      lessonRemainingSeconds--;
    } else {
      lessonRemainingSeconds = 45 * 60;
    }

    const mins = Math.floor(lessonRemainingSeconds / 60);
    const secs = lessonRemainingSeconds % 60;
    if (lswCountdownEl) {
      lswCountdownEl.textContent = `Dzwonek za ${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
    }
    if (lswProgressEl) {
      const elapsed = totalLessonSeconds - lessonRemainingSeconds;
      const pct = Math.min(100, Math.max(5, (elapsed / totalLessonSeconds) * 100));
      lswProgressEl.style.width = `${pct.toFixed(1)}%`;
    }
  }
  setInterval(tickSchoolWidget, 1000);

  // --- 5. Countdown to Sobota, 26 września 2026 ---
  const targetDate = new Date('2026-09-26T00:00:00+02:00').getTime();
  const daysEl = document.getElementById('cd-days');
  const hoursEl = document.getElementById('cd-hours');
  const minutesEl = document.getElementById('cd-minutes');
  const secondsEl = document.getElementById('cd-seconds');

  function updateCountdown() {
    const now = new Date().getTime();
    const distance = targetDate - now;

    if (distance <= 0) {
      if (daysEl) daysEl.textContent = '00';
      if (hoursEl) hoursEl.textContent = '00';
      if (minutesEl) minutesEl.textContent = '00';
      if (secondsEl) secondsEl.textContent = '00';
      return;
    }

    const days = Math.floor(distance / (1000 * 60 * 60 * 24));
    const hours = Math.floor((distance % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
    const minutes = Math.floor((distance % (1000 * 60 * 60)) / (1000 * 60));
    const seconds = Math.floor((distance % (1000 * 60)) / 1000);

    if (daysEl) daysEl.textContent = String(days).padStart(2, '0');
    if (hoursEl) hoursEl.textContent = String(hours).padStart(2, '0');
    if (minutesEl) minutesEl.textContent = String(minutes).padStart(2, '0');
    if (secondsEl) secondsEl.textContent = String(seconds).padStart(2, '0');
  }
  updateCountdown();
  setInterval(updateCountdown, 1000);

  // --- 6. 1-Click Reminder Dropdown & .ics Exporter ---
  const calendarBtn = document.getElementById('calendar-btn');
  const reminderDropdown = document.getElementById('reminder-dropdown');
  const downloadIcsBtn = document.getElementById('download-ics-btn');

  if (calendarBtn && reminderDropdown) {
    calendarBtn.addEventListener('pointerdown', (e) => {
      e.stopPropagation();
      sound.playClick();
      reminderDropdown.classList.toggle('open');
    });

    document.addEventListener('click', (e) => {
      if (!calendarBtn.contains(e.target) && !reminderDropdown.contains(e.target)) {
        reminderDropdown.classList.remove('open');
      }
    });
  }

  if (downloadIcsBtn) {
    downloadIcsBtn.addEventListener('click', () => {
      sound.playSuccess();
      const icsContent = [
        'BEGIN:VCALENDAR',
        'VERSION:2.0',
        'PRODID:-//Laghert Labs//Vision Release Calendar//PL',
        'CALSCALE:GREGORIAN',
        'METHOD:PUBLISH',
        'BEGIN:VEVENT',
        'SUMMARY:Premiera Vision APK - Laghert Labs',
        'DESCRIPTION:Oficjalne wydanie i udostępnienie pliku APK aplikacji Vision na https://laghert.pl/vision/',
        'LOCATION:https://laghert.pl/vision/',
        'STATUS:CONFIRMED',
        'DTSTART:20260926T100000Z',
        'DTEND:20260926T120000Z',
        'BEGIN:VALARM',
        'TRIGGER:-PT15M',
        'ACTION:DISPLAY',
        'DESCRIPTION:Premiera Vision APK za 15 minut!',
        'END:VALARM',
        'END:VEVENT',
        'END:VCALENDAR'
      ].join('\r\n');

      const blob = new Blob([icsContent], { type: 'text/calendar;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'premiera-vision.ics');
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
      if (reminderDropdown) reminderDropdown.classList.remove('open');
    });
  }

  // --- 7. Interactive Smartphone Device & MD3 Bottom Dock ---
  const dockButtons = document.querySelectorAll('.dock-btn');
  const screenImages = document.querySelectorAll('.phone-screen-img');
  const phoneScreenFrame = document.querySelector('.phone-screen-frame');
  const DOCK_TABS = ['home', 'plan', 'oceny', 'msg'];
  let currentTabIndex = 0;

  function switchPhoneTab(targetDock) {
    const targetIdx = DOCK_TABS.indexOf(targetDock);
    if (targetIdx !== -1) currentTabIndex = targetIdx;
    const targetBtn = document.querySelector(`.dock-btn[data-dock="${targetDock}"]`);
    if (!targetBtn) return;
    sound.playClick();

    dockButtons.forEach(b => b.classList.remove('active'));
    targetBtn.classList.add('active');

    screenImages.forEach(img => {
      if (img.id === `screen-${targetDock}`) img.classList.add('active');
      else img.classList.remove('active');
    });
  }

  dockButtons.forEach(btn => {
    btn.addEventListener('pointerdown', (e) => {
      e.preventDefault();
      switchPhoneTab(btn.getAttribute('data-dock'));
    });
  });

  // Direct manipulation: Horizontal swipe on phone screen (WWDC 2018 Fluid Interfaces)
  if (phoneScreenFrame) {
    let swipeStartX = 0;
    let swipeStartY = 0;
    let isPhoneSwiping = false;

    phoneScreenFrame.addEventListener('pointerdown', (e) => {
      swipeStartX = e.clientX;
      swipeStartY = e.clientY;
      isPhoneSwiping = true;
      phoneScreenFrame.setPointerCapture(e.pointerId);
    });

    phoneScreenFrame.addEventListener('pointerup', (e) => {
      if (!isPhoneSwiping) return;
      isPhoneSwiping = false;
      try { phoneScreenFrame.releasePointerCapture(e.pointerId); } catch (_) {}

      const dx = e.clientX - swipeStartX;
      const dy = e.clientY - swipeStartY;

      // Only trigger if horizontal intent is clear (more horizontal than vertical)
      if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 35) {
        if (dx < 0 && currentTabIndex < DOCK_TABS.length - 1) {
          switchPhoneTab(DOCK_TABS[currentTabIndex + 1]);
        } else if (dx > 0 && currentTabIndex > 0) {
          switchPhoneTab(DOCK_TABS[currentTabIndex - 1]);
        }
      }
    });

    phoneScreenFrame.addEventListener('pointercancel', (e) => {
      isPhoneSwiping = false;
      try { phoneScreenFrame.releasePointerCapture(e.pointerId); } catch (_) {}
    });
  }

  // --- 8. Bento Tile 1: Comparison Slider with Apple Fluid Physics ---
  const sliderContainer = document.getElementById('compare-slider');
  const sliderOverlay = document.getElementById('compare-overlay');
  const sliderHandle = document.getElementById('compare-handle');

  if (sliderContainer && sliderOverlay && sliderHandle) {
    let isDragging = false;
    let currentPercent = 50;
    let pointerHistory = [];
    let animFrame = null;
    let lastCrossedMilestone = 50;

    // Apple rubberband resistance at boundaries (Designing Fluid Interfaces)
    function rubberband(overshoot, dimension, constant = 0.45) {
      return (overshoot * dimension * constant) / (dimension + constant * Math.abs(overshoot));
    }

    // Apple momentum projection (exponential decay form)
    function project(v, decelerationRate = 0.992) {
      return (v / 1000) * decelerationRate / (1 - decelerationRate);
    }

    function applyVisual(p) {
      const clampedP = Math.max(0, Math.min(100, p));
      sliderOverlay.style.clipPath = `polygon(0 0, ${clampedP}% 0, ${clampedP}% 100%, 0 100%)`;
      sliderOverlay.style.webkitClipPath = `polygon(0 0, ${clampedP}% 0, ${clampedP}% 100%, 0 100%)`;
      sliderHandle.style.left = `${clampedP}%`;

      // Haptic transient on crossing 50% center split or edges
      const snapTarget = Math.round(p / 25) * 25;
      if (Math.abs(p - snapTarget) < 2 && lastCrossedMilestone !== snapTarget) {
        sound.playDetent();
        lastCrossedMilestone = snapTarget;
      } else if (Math.abs(p - snapTarget) > 4) {
        lastCrossedMilestone = -1;
      }
    }

    // Interruptible physical spring animation with velocity handoff
    function springTo(target, initialVelocity = 0) {
      if (animFrame) cancelAnimationFrame(animFrame);
      let pos = currentPercent;
      let vel = initialVelocity;
      let lastTime = performance.now();

      const hasMomentum = Math.abs(initialVelocity) > 60;
      const damping = hasMomentum ? 0.82 : 1.0; // Under-damped on flick, critically damped otherwise
      const omega = 20; // Natural frequency (snappy ~0.35s settle)

      function step(now) {
        const dt = Math.min((now - lastTime) / 1000, 0.032);
        lastTime = now;

        const displacement = pos - target;
        const springForce = -omega * omega * displacement;
        const dampingForce = -2 * damping * omega * vel;
        const accel = springForce + dampingForce;

        vel += accel * dt;
        pos += vel * dt;
        currentPercent = pos;

        applyVisual(currentPercent);

        if (Math.abs(displacement) > 0.05 || Math.abs(vel) > 0.2) {
          animFrame = requestAnimationFrame(step);
        } else {
          currentPercent = target;
          applyVisual(target);
          animFrame = null;
        }
      }

      animFrame = requestAnimationFrame(step);
    }

    function calcPercentFromClientX(clientX) {
      const rect = sliderContainer.getBoundingClientRect();
      const rawX = clientX - rect.left;
      let p = (rawX / rect.width) * 100;

      if (p < 0) {
        p = rubberband(p, 100);
      } else if (p > 100) {
        p = 100 + rubberband(p - 100, 100);
      }
      return p;
    }

    sliderContainer.addEventListener('pointerdown', (e) => {
      if (animFrame) cancelAnimationFrame(animFrame);
      isDragging = true;
      sliderContainer.setPointerCapture(e.pointerId);
      pointerHistory = [{ x: e.clientX, time: performance.now() }];
      currentPercent = calcPercentFromClientX(e.clientX);
      applyVisual(currentPercent);
      sound.playClick();
    });

    sliderContainer.addEventListener('pointermove', (e) => {
      if (!isDragging) return;
      const now = performance.now();
      pointerHistory.push({ x: e.clientX, time: now });
      if (pointerHistory.length > 5) pointerHistory.shift();

      currentPercent = calcPercentFromClientX(e.clientX);
      applyVisual(currentPercent);
    });

    function stopDrag(e) {
      if (!isDragging) return;
      isDragging = false;
      try { sliderContainer.releasePointerCapture(e.pointerId); } catch (_) {}

      let releaseVelocity = 0;
      if (pointerHistory.length >= 2) {
        const rect = sliderContainer.getBoundingClientRect();
        const first = pointerHistory[0];
        const last = pointerHistory[pointerHistory.length - 1];
        const dt = (last.time - first.time) / 1000;
        if (dt > 0.005) {
          const dxPercent = ((last.x - first.x) / rect.width) * 100;
          releaseVelocity = dxPercent / dt;
        }
      }

      // Momentum projection: project where user's flick is landing
      const projectedEndpoint = currentPercent + project(releaseVelocity);

      let target = 50;
      if (projectedEndpoint < 25) target = 0;
      else if (projectedEndpoint > 75) target = 100;
      else target = 50;

      springTo(target, releaseVelocity);
    }

    sliderContainer.addEventListener('pointerup', stopDrag);
    sliderContainer.addEventListener('pointercancel', stopDrag);
  }

  // --- 9. Bento Tile 3: Target Grade Calculator ---
  (function () {
    const slider = document.getElementById('target-curr-slider');
    const currValEl = document.getElementById('target-curr-val');
    const goalBtns = document.querySelectorAll('#target-goals-group .target-pill-btn');
    const weightBtns = document.querySelectorAll('#target-weights-group .target-pill-btn');
    const neededGradeEl = document.getElementById('target-needed-grade');
    const resTextEl = document.getElementById('target-res-text');

    let currentAvg = 3.85;
    let targetGoal = 4.75;
    let targetWeight = 3;

    function recalculateTarget() {
      if (!neededGradeEl || !resTextEl) return;
      const baseW = 12;
      const rawNeeded = targetGoal + (baseW / targetWeight) * (targetGoal - currentAvg);

      let badgeVal = '';
      let descText = '';

      if (rawNeeded <= 2.0) {
        badgeVal = '2';
        descText = `Masz bezpieczną średnią (${currentAvg.toFixed(2)}). Nawet ocena <strong>2</strong> wagi ${targetWeight} wystarczy do celu ${targetGoal.toFixed(2)}.`;
      } else if (rawNeeded <= 3.0) {
        badgeVal = '3';
        descText = `Wystarczy ocena <strong>3 (dostateczny)</strong> ze sprawdzianu wagi ${targetWeight}, aby osiągnąć cel ${targetGoal.toFixed(2)}.`;
      } else if (rawNeeded <= 4.0) {
        badgeVal = '4';
        descText = `Musisz otrzymać ocenę <strong>4 (dobry)</strong> ze sprawdzianu wagi ${targetWeight}, by podnieść średnią do ${targetGoal.toFixed(2)}.`;
      } else if (rawNeeded <= 4.8) {
        badgeVal = '5';
        descText = `Wymagana ocena to <strong>5 (bardzo dobry)</strong> ze sprawdzianu wagi ${targetWeight}. Dzięki temu Twoja średnia wzrośnie do ${targetGoal.toFixed(2)}.`;
      } else if (rawNeeded <= 6.0) {
        badgeVal = '6';
        descText = `Wymagany jest <strong>celujący (6)</strong> ze sprawdzianu wagi ${targetWeight}, aby osiągnąć pułap ${targetGoal.toFixed(2)}!`;
      } else {
        badgeVal = '6+';
        descText = `Jeden sprawdzian o wadze ${targetWeight} nie wystarczy, by przeskoczyć z ${currentAvg.toFixed(2)} do ${targetGoal.toFixed(2)}. Potrzebujesz co najmniej dwóch wysokich ocen (5 lub 6).`;
      }

      neededGradeEl.textContent = badgeVal;
      resTextEl.innerHTML = descText;
    }

    if (slider) {
      slider.addEventListener('input', (e) => {
        currentAvg = parseFloat(e.target.value);
        if (currValEl) currValEl.textContent = currentAvg.toFixed(2);
        recalculateTarget();
      });
    }

    goalBtns.forEach(btn => {
      btn.addEventListener('pointerdown', () => {
        sound.playClick();
        goalBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        targetGoal = parseFloat(btn.getAttribute('data-goal'));
        recalculateTarget();
      });
    });

    weightBtns.forEach(btn => {
      btn.addEventListener('pointerdown', () => {
        sound.playClick();
        weightBtns.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        targetWeight = parseInt(btn.getAttribute('data-w'), 10);
        recalculateTarget();
      });
    });

    recalculateTarget();
  })();

  // --- 10. Bento Tile 4: Direct API & Offline Mode ---
  const modeOnlineBtn = document.getElementById('mode-online-btn');
  const modeOfflineBtn = document.getElementById('mode-offline-btn');
  const netDot = document.getElementById('network-status-dot');
  const netText = document.getElementById('network-status-text');
  const phoneNetBadge = document.getElementById('phone-net-badge');
  let isOfflineMode = false;

  function setNetworkMode(offline) {
    isOfflineMode = offline;
    sound.playClick();
    if (isOfflineMode) {
      if (modeOnlineBtn) modeOnlineBtn.classList.remove('active');
      if (modeOfflineBtn) modeOfflineBtn.classList.add('active');
      if (netDot) netDot.className = 'dot-status yellow';
      if (netText) {
        netText.innerHTML = `Librus API: <strong>Brak zasięgu (Offline)</strong> · <code id="network-ping-val">0.1 ms SQLite</code>`;
      }
      if (phoneNetBadge) phoneNetBadge.textContent = 'Offline';
    } else {
      if (modeOfflineBtn) modeOfflineBtn.classList.remove('active');
      if (modeOnlineBtn) modeOnlineBtn.classList.add('active');
      if (netDot) netDot.className = 'dot-status green pulse';
      if (netText) {
        netText.innerHTML = `Librus Synergia API: <strong>Aktywne</strong> · <code id="network-ping-val">38 ms</code>`;
      }
      if (phoneNetBadge) phoneNetBadge.textContent = '5G';
    }
  }

  if (modeOnlineBtn) modeOnlineBtn.addEventListener('pointerdown', () => setNetworkMode(false));
  if (modeOfflineBtn) modeOfflineBtn.addEventListener('pointerdown', () => setNetworkMode(true));

  // Ping fluctuation
  setInterval(() => {
    if (isOfflineMode) return;
    const pingEl = document.getElementById('network-ping-val');
    if (pingEl) {
      const ms = Math.floor(Math.random() * 11) + 34;
      pingEl.textContent = `${ms} ms`;
    }
  }, 4000);

  // --- 11. Security Modal & Shortcuts Modal ---
  const secModal = document.getElementById('security-modal');
  const openSecBtn = document.getElementById('open-security-modal');
  const closeSecBtn = document.getElementById('close-security-modal');

  const shortcutsModal = document.getElementById('shortcuts-modal');
  const closeShortcutsBtn = document.getElementById('close-shortcuts-modal');

  if (openSecBtn && secModal) {
    openSecBtn.addEventListener('pointerdown', () => {
      sound.playClick();
      secModal.showModal();
    });
  }

  if (closeSecBtn && secModal) {
    closeSecBtn.addEventListener('pointerdown', () => {
      sound.playClick();
      secModal.close();
    });
  }

  if (closeShortcutsBtn && shortcutsModal) {
    closeShortcutsBtn.addEventListener('pointerdown', () => {
      sound.playClick();
      shortcutsModal.close();
    });
  }

  [secModal, shortcutsModal].forEach(modal => {
    if (modal) {
      modal.addEventListener('click', (e) => {
        if (e.target === modal) {
          sound.playClick();
          modal.close();
        }
      });
    }
  });

  // --- 12. FAQ Accordion ---
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const btn = item.querySelector('.faq-button');
    const content = item.querySelector('.faq-content');

    if (btn && content) {
      btn.addEventListener('click', () => {
        sound.playClick();
        const isOpen = item.classList.contains('active');

        faqItems.forEach(otherItem => {
          if (otherItem !== item) {
            otherItem.classList.remove('active');
            const otherContent = otherItem.querySelector('.faq-content');
            if (otherContent) otherContent.style.maxHeight = null;
          }
        });

        if (isOpen) {
          item.classList.remove('active');
          content.style.maxHeight = null;
        } else {
          item.classList.add('active');
          content.style.maxHeight = `${content.scrollHeight + 20}px`;
        }
      });
    }
  });

  // --- 13. Global Keyboard Shortcuts ---
  const colors = ['purple', 'sky', 'emerald', 'amber', 'rose'];
  let currentColorIdx = 0;

  document.addEventListener('keydown', (e) => {
    const tag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
    if (tag === 'input' || tag === 'textarea' || tag === 'select') return;

    if (e.key === 'm' || e.key === 'M') {
      currentColorIdx = (currentColorIdx + 1) % colors.length;
      applyAccentColor(colors[currentColorIdx]);
      sound.playClick();
    } else if (e.key === '1') {
      switchPhoneTab('home');
    } else if (e.key === '2') {
      switchPhoneTab('plan');
    } else if (e.key === '3') {
      switchPhoneTab('oceny');
    } else if (e.key === '4') {
      switchPhoneTab('msg');
    } else if (e.key === 's' || e.key === 'S') {
      if (soundToggleBtn) soundToggleBtn.click();
    } else if (e.key === 'o' || e.key === 'O') {
      setNetworkMode(!isOfflineMode);
    } else if (e.key === '?' || (e.shiftKey && e.key === '/')) {
      if (shortcutsModal) {
        if (shortcutsModal.open) shortcutsModal.close();
        else {
          sound.playClick();
          shortcutsModal.showModal();
        }
      }
    } else if (e.key === 'Escape') {
      if (shortcutsModal && shortcutsModal.open) shortcutsModal.close();
      if (secModal && secModal.open) secModal.close();
    }
  });

})();
