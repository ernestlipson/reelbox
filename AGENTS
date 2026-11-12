# AGENTS.md — Reelbox Engineering & Release Guide

Guidelines, standards, and operational workflows for AI agents working in the Reelbox repository.

---

## 1. Project Overview

Reelbox is a native Android movie & TV browser and streaming application built with **Kotlin** and **Jetpack Compose** (`compileSdk = 36`, `minSdk = 26`, Material 3).

### Key Modules & Architecture
- **`app/src/main/java/dev/reelbox/ui/`**: Compose UI screens, navigation, and theme.
  - `PlayerScreen.kt`: Hardened in-app media player with real-time ad/popup blocking, automatic server failover cascade, quick seek (-10s / +10s), and fullscreen landscape controls.
  - `DetailScreen.kt`, `HomeScreen.kt`, `SearchScreen.kt`, `WatchlistScreen.kt`.
- **`app/src/main/java/dev/reelbox/data/`**: Data models, APIs, and utilities.
  - `AdBlocker.kt`: Intercepts network requests to block known pop-under/ad networks and injects scripts to disable popup windows and auto-play video.
  - `StreamingServers.kt`: Streaming server configurations and URL builders for embed sources.
  - `UpdateChecker.kt`: Queries the live site's `version.json` at app launch to display update prompts to users.
- **`website/`**: Single-file static marketing and download site hosted on Vercel.
  - `index.html`: Site markup, styling, and download links.
  - `version.json`: Release manifest fetched by the Android app for in-app updates.
  - `reelbox-<version>.apk`: The distributed signed release APK.
- **`scripts/`**: Automation scripts.
  - `scripts/release.py`: The single-command release tool.

---

## 2. The One-Command Release Flow

All app releases are automated through `scripts/release.py`.

### Release Command

Run from the repository root:

```bash
python3 scripts/release.py <version> "<release notes>" [--push] [--dry-run]
```

#### Example (Shipping version 1.2):
```bash
python3 scripts/release.py 1.2 "Hardened player with ad-blocking and automatic server failover." --push
```

### Options & Flags
- `--dry-run`: Prints the planned version bump and file modifications without touching anything.
- `--push`: Pushes the release commit immediately to `origin/main`. This triggers the Vercel deployment, updates the download link on the site, and prompts installed apps to update on their next launch.
- *(Leaving off `--push`)*: Cuts the release and commits locally only. Allows manual inspection (`git log -1`, `git diff HEAD~1`) before you run `git push`.

---

## 3. What `scripts/release.py` Executes Automatically

1. **Precondition Checks:**
   - Verifies the git working tree is clean (aborts if uncommitted tracked changes exist).
   - Verifies `keystore.properties` and the `.jks` release key exist in the repository root.
   - Confirms the requested version is valid and not already the active `versionName`.
2. **Version Bumping:**
   - Increments `versionCode` by 1 and sets `versionName = "<version>"` in `app/build.gradle.kts`.
3. **Signed Release Build:**
   - Runs `./gradlew :app:assembleRelease -q`.
   - If the build fails, it safely restores `app/build.gradle.kts` and aborts without publishing.
4. **Binary & Asset Swap:**
   - Copies the built APK into `website/reelbox-<version>.apk`.
   - Removes the previous version's APK using `git rm`.
5. **Website & Manifest Updates:**
   - Updates `website/version.json` (`versionCode`, `versionName`, `apkUrl`, `notes`).
   - Updates the APK download link, version badge, and file size in `website/index.html` and `website/README.md`.
6. **Git Commit & Optional Push:**
   - Stages all modified release files and commits: `Release <version>\n\n<notes>`.
   - If `--push` is present, pushes to `main`.

---

## 4. Mandatory Rules for Agents

1. **Commit Feature Changes Before Releasing:**
   `scripts/release.py` will refuse to run if there are uncommitted changes to tracked files. Always stage and commit your functional changes before invoking the release script.
2. **Short, User-Facing Notes:**
   The release notes text is what users read inside the in-app update dialog. Keep it concise, descriptive, and free of internal jargon (e.g., *"Added hardened player with ad-blocking and automatic server failover."*).
3. **Rollback Procedure (If inspecting locally):**
   If an unpushed release commit needs to be reverted or re-run:
   ```bash
   git reset --hard HEAD~1
   ```
