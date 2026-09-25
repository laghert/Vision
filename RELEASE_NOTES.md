# Vision 1.0.0-beta.1 — Calm Focus & Fluid Interface Edition

Wydanie **Vision 1.0.0-beta.1** wprowadza gruntowną przebudowę interfejsu w duchu **Calm Focus M3** oraz zasad projektowania płynnych interfejsów (**Apple Fluid Design / WWDC**).

---

### ✨ Główne nowości i usprawnienia

#### 📱 Aplikacja Android (Vision)
- **Fizyka i animacje (Critically Damped Springs):**
  - Zastosowano tłumienie krytyczne (`DampingRatioNoBouncy = 1.0f`) w domyślnych przejściach i nawigacji, eliminując nienaturalne odbicia.
  - Wprowadzono dynamiczne przejścia oparte na pędzie (`momentumSpringSpec`, `dampingRatio = 0.8f`) dla gestów przeciągania i kart.
- **Hierarchia powierzchni i głębi (OLED Tactile Hierarchy):**
  - Przywrócono warstwowość materiałów w trybie ciemnym OLED: tło `#000000` z subtelnymi poziomami kontenerów (`#0D0F12`, `#14171C`, `#1C1F26`, `#262A33`), co zapewnia natychmiastowe rozróżnienie elementów bez męczenia wzroku.
- **Inteligentny pasek bieżącej lekcji (NowPlayingLessonBar):**
  - Pasek jest teraz kontekstowy – chowa się automatycznie z płynną animacją sprężynową na ekranach Głównym oraz Planu Lekcji, eliminując duplikację informacji.
- **Haptyka i mikrointerakcje:**
  - Zmiękczono skalowanie przycisków doku (`1.02f` / `1.08f`), połączone ze sprężystą animacją.
  - Zaimplementowano bezpośrednią haptykę systemową `KEYBOARD_TAP`.
- **Typografia i poprawność językowa:**
  - Optyczny kerning/tracking: nagłówki o subtelnym ujemnym trackingu (`-0.015em` / `-0.01em`), etykiety z dodatnim rozstrzeleniem (`+0.05em`) oraz liczby o stałej szerokości (tabular numerals).
  - Poprawiona odmiana w nagłówkach planu lekcji (dopełniacz z półpauzą, np. *14–18 września* zamiast *14-18 wrzesień*).

#### 🌐 Web Showcase (Prezentacja WWW)
- **Płynne gesty 1:1:**
  - Bezpośrednie śledzenie gestów przeciągania na makiecie ekranu telefonu.
  - Projekcja pędu (`project(velocity)`) z fizyką sprężynową Eulera i efektem gumowej taśmy na krawędziach (boundary rubber-banding).
  - Zerowe opóźnienie reakcji (`pointerdown`).
- **Mikrodźwięki fizyczne:**
  - Nowy syntezator Web Audio z krótkimi impulsami kliknięć i zapadek (detent).
- **Dostępność i adaptacyjność:**
  - Obsługa `prefers-reduced-motion`, `prefers-reduced-transparency` oraz `prefers-contrast`.

---

### 📦 Szczegóły pakietu instalacyjnego (APK)
- **Plik:** `Vision-v1.0.0-beta.1.apk`
- **Wersja:** `BETA 1.0` (Unofficial Release)
- **Rozmiar:** ~13.7 MB (zoptymalizowany i odchudzony przez R8/ProGuard)
- **Podpis:** Certyfikat APK Signature Scheme v2 (gotowy do bezpośredniej instalacji na urządzeniach z systemem Android 8.0+)
- **SHA256:** `fb4f68cca147fe00481938f6226612c92f99098f1b5dc4bcc093895eb837412c`
