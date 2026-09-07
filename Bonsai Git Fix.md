# Bonsai Submodule Git Troubleshooting & Guide

## Woran es liegt

Deine Git-Status-Ausgabe zeigt:

```
modified:   bonsai (modified content)
```

Das heißt: **Innerhalb des bonsai-Ordners** (des Submoduls) sind Dateien verändert, aber dort noch **nicht committet**. Ein `git add .` im Hauptprojekt erfasst nur den Submodul-**Zeiger** (also den Commit-Hash). Die Änderungen *in* bonsai kannst du nur committen, wenn du dich *in* bonsai befindest – deshalb bleibt bei dir alles „Changes not staged".

Schau zuerst, was genau im Submodul los ist:

```bash
cd bonsai
git status
git remote -v
```

Danach gibt es zwei typische Szenarien:

---

## Fall A: Du willst bonsai nur auf den gewünschten Commit bringen

Du willst den Stand von `scto/bonsai` auf den Commit `10cc75f…` setzen und die lokale Änderung in bonsai **verwerfen**:

```bash
cd bonsai
git fetch origin
git checkout 10cc75f18732be09537801adae75c8840832c6fa
cd ..

# Jetzt im Hauptprojekt den neuen Submodul-Stand committen:
git add bonsai
git commit -m "Update bonsai auf 10cc75f"
git push
```

Falls Git sich wegen der lokalen Änderung weigert (`checkout` schlägt fehl), vorher verwerfen:

```bash
cd bonsai
git restore .          # verwirft Änderungen
# oder, wenn auch neue ungetrackte Dateien stören:
git clean -fd
git checkout 10cc75f18732be09537801adae75c8840832c6fa
cd ..
```

---

## Fall B: Du willst deine `settings.gradle.kts`-Änderung behalten und als Fix pushen

Dann muss der Commit **im Submodul selbst** passieren – das Submodul ist ein eigenes Repository mit eigenem Remote:

```bash
cd bonsai
git add settings.gradle.kts     # oder die tatsächlich geänderte Datei
git commit -m "Fix: Bonsai settings.gradle.kts"
git push                         # pusht ins Remote des Submoduls!
cd ..

# Jetzt den neuen Submodul-Zeiger im Hauptprojekt committen:
git add bonsai
git commit -m "Fix: Bonsai settings.gradle.kts"
git push
```

⚠️ **Wichtiger Punkt:** `git push` im Hauptprojekt pusht **niemals** Inhalte des Submoduls. Du brauchst also Schreibrechte auf das bonsai-Remote (siehe `git remote -v` im Submodul) – oder du musst bonsai vorher zu einem eigenen Fork umziehen und die Submodule-URL ändern.

---

## Kombination: Lokalen Fix + gewünschten Commit `10cc75f`

Falls du beides willst – erst auf den Stand `10cc75f` aktualisieren *und dann* deine settings-Fix darauf anwenden:

```bash
cd bonsai
git fetch origin
git checkout 10cc75f18732be09537801adae75c8840832c6fa
git switch -c fix/bonsai-settings     # Branch anlegen, damit du nicht im Detached-HEAD bist

# settings.gradle.kts anpassen/übernehmen, dann:
git add settings.gradle.kts
git commit -m "Fix: Bonsai settings.gradle.kts"
git push -u origin fix/bonsai-settings
cd ..

git add bonsai
git commit -m "Fix: Bonsai settings.gradle.kts"
git push
```

---

## Kurzfassung

| Dein Ziel | Befehl im Submodul `bonsai` | Danach im Hauptprojekt |
|---|---|---|
| Nur Commit wechseln | `git checkout <sha>` | `git add bonsai && git commit && git push` |
| Lokalen Fix pushen | `git commit` + `git push` | `git add bonsai && git commit && git push` |

Dein bisheriges Problem war genau der Zwischenschritt: **erst im Submodul committen und pushen**, dann den Pointer im Hauptprojekt aktualisieren. Ohne einen neuen Commit in bonsai hat `git add .` im Hauptprojekt nämlich nichts Sinnvolles zum Stagen.