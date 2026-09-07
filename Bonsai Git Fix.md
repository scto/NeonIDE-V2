# Bonsai Submodule Git Fix (Commit & Push)

Wenn du im Unterordner `bonsai` Korrekturen vorgenommen hast (wie z. B. das Anpassen der `settings.gradle.kts`), musst du diese Änderung zuerst im Submodule selbst einchecken, bevor das Haupt-Repository den neuen Commit-Verweis übernehmen kann.

Führe diese Befehle nacheinander in deinem Terminal aus:

## Schritt 1: In den Submodule-Ordner wechseln
```bash
cd bonsai
```

## Schritt 2: Git-Status prüfen & Änderungen holen
```bash
git fetch origin
git checkout 10cc75f18732be09537801adae75c8840832c6fa
git switch -c fix/bonsai-settings     # Branch anlegen, damit du nicht im Detached-HEAD bist
```

## Schritt 3: Geänderte `settings.gradle.kts` committen und pushen
```bash
git add settings.gradle.kts
git commit -m "Fix: Bonsai settings.gradle.kts"
git push -u origin fix/bonsai-settings
```

## Schritt 4: Ins Hauptverzeichnis zurückkehren und Submodule-Verweis aktualisieren
```bash
cd ..

git add bonsai
git commit -m "Fix: Update bonsai submodule reference"
git push
```