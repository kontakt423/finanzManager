# FinanzManager – Installationsanleitung
## Xiaomi 13T Pro mit HyperOS 3.0 via Android Studio

---

## 📋 Voraussetzungen

### Entwicklungsrechner
| Software | Version | Download |
|---|---|---|
| Android Studio | Ladybug (2024.2+) | https://developer.android.com/studio |
| JDK | 17 (enthalten in Android Studio) | – |
| Android SDK | API 35 (Android 15) | Im SDK Manager |
| Git | Optional | https://git-scm.com |

### Xiaomi 13T Pro
- HyperOS 3.0 (Android 15 Basis)
- USB-Kabel (USB-C)
- Entwickleroptionen müssen aktiviert sein

---

## 🔧 Schritt 1 – Android Studio einrichten

### 1.1 Android Studio installieren
1. Android Studio von https://developer.android.com/studio herunterladen
2. Installer ausführen, Standard-Einstellungen übernehmen
3. Beim ersten Start: **Standard Setup** wählen → fertigstellen

### 1.2 Android SDK konfigurieren
Im Android Studio:
```
File → Settings → Languages & Frameworks → Android SDK
```
Folgende SDK-Versionen installieren:
- ✅ Android 15 (API 35) – *compileSdk*
- ✅ Android 8.0 (API 26) – *minSdk*

Unter **SDK Tools** sicherstellen:
- ✅ Android SDK Build-Tools 35.x
- ✅ Android Emulator
- ✅ Android SDK Platform-Tools

---

## 📱 Schritt 2 – Xiaomi 13T Pro vorbereiten

### 2.1 Entwickleroptionen aktivieren
```
Einstellungen → Über das Telefon → Alle Spezifikationen
→ MIUI-Version (oder HyperOS-Version) 7× tippen
```
Meldung erscheint: **"Sie sind jetzt Entwickler"**

### 2.2 USB-Debugging aktivieren
```
Einstellungen → Zusätzliche Einstellungen → Entwickleroptionen
```
Folgende Optionen aktivieren:
- ✅ **USB-Debugging** → EIN
- ✅ **Wireless Debugging** (optional, für WLAN-Debugging)
- ✅ **USB-Debugging (Sicherheitseinstellungen)** → EIN  
  *(Falls sichtbar – erlaubt ADB-Befehle über USB)*

### 2.3 HyperOS 3 spezifische Einstellung
HyperOS 3.0 hat eine zusätzliche Sicherheitsschicht:

```
Einstellungen → Datenschutz → Besondere App-Zugriffe
→ Installieren unbekannter Apps → Android Studio / ADB → Erlauben
```

Außerdem:
```
Entwickleroptionen → Disallow ADB debugging in charge-only mode → AUS
```

---

## 📂 Schritt 3 – Projekt in Android Studio öffnen

### 3.1 ZIP entpacken
1. `FinanzManager.zip` entpacken  
2. Ordner `FinanzManager/` merken (z. B. `C:\Projekte\FinanzManager`)

### 3.2 Projekt öffnen
```
Android Studio → File → Open
→ Ordner "FinanzManager" auswählen → OK
```

### 3.3 Gradle Sync abwarten
- Android Studio lädt automatisch alle Abhängigkeiten (Internet erforderlich)
- Unten rechts erscheint: **"Gradle sync finished"**
- Dauer: ca. 2–5 Minuten beim ersten Mal

> ⚠️ **Falls Gradle-Fehler auftreten:**
> ```
> File → Invalidate Caches → Invalidate and Restart
> ```

---

## 🔌 Schritt 4 – Gerät verbinden

### 4.1 USB verbinden
1. Xiaomi 13T Pro per USB-C anschließen
2. Auf dem Handy erscheint: **"USB-Debugging erlauben?"**
3. **"Immer von diesem Computer erlauben"** → ✅ **OK**

### 4.2 Gerät in Android Studio prüfen
In der Toolbar oben:
```
[▶] Run Button → Dropdown daneben
```
Das Gerät sollte erscheinen als:
```
Xiaomi 13T Pro (API 35)
```

> ⚠️ **Falls Gerät nicht erkannt wird:**
> ```bash
> # In der Android Studio Terminal (unten):
> adb devices
> # Ausgabe sollte zeigen:
> # List of devices attached
> # XXXXXXXX    device
> ```
> Falls `unauthorized`: Auf dem Handy nochmal den Dialog bestätigen.

---

## ▶️ Schritt 5 – App installieren und starten

### 5.1 Run-Konfiguration prüfen
Oben in Android Studio:
```
app ▼  →  Xiaomi 13T Pro  →  ▶ (Play Button)
```

### 5.2 Build & Deploy
1. Den grünen **▶ Play-Button** klicken
2. Android Studio kompiliert die App (~30–60 Sekunden)
3. APK wird automatisch auf das Gerät übertragen
4. App startet automatisch auf dem Xiaomi 13T Pro

### 5.3 HyperOS 3.0 Sicherheitsdialog
Beim ersten Start kann HyperOS fragen:
```
"Diese App wurde von einer unbekannten Quelle installiert.
Möchten Sie fortfahren?"
```
→ **"Installieren"** tippen

---

## 🛠️ Fehlerbehebung

### Problem: "INSTALL_FAILED_USER_RESTRICTED"
HyperOS blockiert USB-Sideloading. Lösung:
```
Einstellungen → Entwickleroptionen
→ Installieren über USB → EIN
```

### Problem: "Manifest merger failed"
```
Build → Clean Project
Build → Rebuild Project
```

### Problem: App stürzt beim Start ab
```
Android Studio → Logcat (unten) → Filter: "finanzmanager"
```
Den Fehler dort ablesen und melden.

### Problem: Gradle kann Bibliotheken nicht laden
Sicherstellen, dass Internet-Verbindung besteht, dann:
```
File → Settings → Build Tools → Gradle
→ Gradle JDK: "Embedded JDK" auswählen
```

### Problem: ADB-Gerät wird nicht erkannt (Windows)
Xiaomi-USB-Treiber installieren:
- https://xiaomifirmwareupdater.com/drivers/

---

## 📦 APK manuell erstellen (für spätere Verteilung)

Falls du die App ohne Android Studio installieren willst:

### Debug APK erstellen
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```
APK liegt in:
```
app/build/outputs/apk/debug/app-debug.apk
```

### APK auf Gerät übertragen
1. `app-debug.apk` per USB oder Google Drive aufs Handy kopieren
2. Datei-Manager öffnen → APK antippen
3. HyperOS fragt: **"Unbekannte Quelle erlauben?"** → Ja

---

## 🏗️ Projektstruktur (Kurzübersicht)

```
FinanzManager/
├── app/build.gradle.kts        ← Dependencies & SDK-Versionen
├── gradle/libs.versions.toml   ← Version Catalog
└── app/src/main/
    ├── AndroidManifest.xml
    └── java/com/example/finanzmanager/
        ├── MainActivity.kt          ← Einstiegspunkt
        ├── FinanzManagerApp.kt      ← WorkManager
        ├── data/
        │   ├── database/            ← Room (SQLite)
        │   └── repository/          ← Business Logic
        ├── domain/Models.kt         ← Datenmodelle
        ├── workers/                 ← Background Tasks
        └── ui/
            ├── FinanzViewModel.kt   ← State Management
            ├── theme/Theme.kt       ← Material Design 3
            ├── components/          ← Wiederverw. UI
            └── screens/             ← Alle Screens
```

---

## ✅ Funktionen der App

| Funktion | Status |
|---|---|
| Dashboard mit Konto-Karten | ✅ |
| Transaktionen anlegen/bearbeiten | ✅ |
| Daueraufträge (Background Worker) | ✅ |
| MwSt.-Berechnung (19%, "Rgnr"-Prefix) | ✅ |
| Split-Topf (50% / 100% Partner) | ✅ |
| Zeitvergleich (Datum A ↔ Datum B) | ✅ |
| Investment Chart (Canvas) | ✅ |
| Dark Mode (Material Design 3) | ✅ |
| Export/Import JSON-Backup | ✅ |
| Room SQLite Datenbank | ✅ |

---

*Erstellt für Xiaomi 13T Pro · HyperOS 3.0 · Android 15 · Kotlin + Jetpack Compose*
