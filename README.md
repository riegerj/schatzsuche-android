# Schatzsuche – Android App

Eine Android-App zum Erstellen und Spielen von QR-Code-basierten Schatzsuchen.

## Funktionen

### QR-Karten & PDF
- Vorgefertigte QR-Codes mit lesbaren Nummern (#01, #02, …)
- PDF-Export für A4-Druck (max. 6 Karten pro Seite)
- Anzahl wählbar (Standard: 12, Bereich: 6–36)
- Erstkonfiguration oder später im Admin-Bereich änderbar

### Admin-Modus
- Schatzsuchen mit Themes erstellen (Piraten, Weltraum, Ritter/Verlies, Ägypten, Klassisch, Dschungel)
- Schritte mit Überschrift, Text, Bildern, Audio und Video
- QR-Codes pro Schritt zuweisen
- Aufgaben nach dem Scan: Textfeld, Single/Multiple Choice, Foto/Video/Audio
- QR-Versteck-Hilfe: Übersicht welcher Code zu welchem Schritt gehört
- Verlauf aller durchgeführten Schatzsuchen

### Teilnehmer-Modus
- Schatzsuche auswählen und mit Team-/Spielername starten
- QR-Code scannen zum Fortschritt
- Animierte Schatzkarte mit gestrichelter Route
- Glückwunsch-Bildschirm am Ende mit optionalem Schatz-Hinweis
- Grafische Zusammenfassung mit Zeiten pro Schritt und allen Inhalten

## Technologie

- Kotlin, Jetpack Compose, Material 3
- Room (SQLite) für lokale Datenspeicherung
- CameraX + ML Kit für QR-Scanning
- ZXing für QR-Generierung
- Android PdfDocument für PDF-Export

## Build & Installation

1. Android Studio (Ladybug oder neuer) öffnen
2. Projektordner `Schatzsuche` öffnen
3. Gradle Sync abwarten
4. Auf Gerät oder Emulator installieren (minSdk 26)

```bash
./gradlew assembleDebug
```

## Erste Schritte

1. **App starten** → Erstkonfiguration: QR-Karten generieren und PDF drucken
2. **Admin-Modus** → Neue Schatzsuche anlegen, Schritte bearbeiten, QR-Codes zuweisen
3. **QR-Karten ausdrucken**, ausschneiden und verstecken (Hilfe im Admin-Modus)
4. **Teilnehmer-Modus** → Schatzsuche starten und loslegen!

### Schatzkarten-Hintergründe (optional)

Eigene Hintergrundbilder pro Theme nach `app/src/main/res/drawable-nodpi/` kopieren:

| Theme | Dateiname |
|-------|-----------|
| Piraten | `map_bg_pirates.webp` (oder `.png`) |
| Weltraum | `map_bg_space.webp` |
| Ritter & Verlies | `map_bg_knights.webp` |
| Ägypten | `map_bg_egypt.webp` |
| Klassisch | `map_bg_classic.webp` |
| Dschungel | `map_bg_jungle.webp` |

Fehlt eine Datei, wird der gezeichnete Standard-Hintergrund verwendet. Empfohlen: quadratisches Bild (z. B. 1024×1024 px), dezent und nicht zu kontrastreich.

## Berechtigungen

- Kamera (QR-Scan)
- Mikrofon (Audio-Aufnahmen, optional)
- Medienzugriff (Bilder/Video hochladen)
