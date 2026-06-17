# 🤝 Session Handoff — Storage Cleanup Android App

**Date:** 2026-06-17  
**Project:** Storage Cleanup — Android app for finding duplicate files and managing trash  
**Status:** Build passes. Debug APK successfully built via GitHub Actions.  
**Repository:** https://github.com/unhuman-web/AngleV1

---

## 1. What This Project Is

An Android app called **Storage Cleanup** that helps users free up device storage by:
- Scanning for **duplicate files** (exact match via SHA-256 hashing)
- Managing a **trash/recently deleted** system
- Providing a clean **Material 3 UI** with real-time scan progress

The user cannot run Android Studio locally, so the entire build pipeline runs via **GitHub Actions CI/CD**.

---

## 2. Decisions Locked In

| Topic | Decision |
|-------|----------|
| App name / package | `com.example.storagecleanup` |
| Language / UI | Kotlin + Jetpack Compose (Material 3) |
| Min SDK | API 26 (Android 8.0) |
| Target / Compile SDK | 35 |
| Architecture | MVVM — ViewModel + StateFlow |
| Async | Kotlin Coroutines for all file scanning/hashing |
| Build system | Gradle (Kotlin DSL, `build.gradle.kts`) |
| Deletion strategy | `ContentResolver.delete()` for direct deletion; quarantine via app-managed folder on API 26-29 |
| Scan scope | User-selectable folders (MediaStore + SAF) |
| CI/CD | GitHub Actions — builds debug APK on push to `main`, release APK via manual trigger with keystore secrets |
| Branch | `main` (renamed from `master`) |

---

## 3. Tech Stack

- `vite` → N/A (Android project)
- Kotlin 2.1.0
- Jetpack Compose (BOM 2024.12.01)
- Material 3 (`material3`, `material-icons-extended`)
- Navigation Compose 2.8.5
- Lifecycle ViewModel Compose 2.8.7
- Room 2.6.1 (with KSP)
- Kotlinx Serialization JSON 1.7.3
- Kotlinx Coroutines Android 1.9.0

---

## 4. Architecture / File Structure

```
storage-cleanup/
├── .github/workflows/
│   └── build-apk.yml              # CI/CD: debug on push, release on manual trigger
├── app/
│   ├── build.gradle.kts            # App dependencies, signing config, Compose setup
│   ├── proguard-rules.pro          # ProGuard rules for Room + Serialization
│   └── src/main/
│       ├── AndroidManifest.xml     # Permissions, Activity, FileProvider
│       ├── java/com/example/storagecleanup/
│       │   ├── MainActivity.kt     # Entry point + NavHost with bottom navigation
│       │   ├── StorageCleanupApp.kt # Application class
│       │   ├── model/
│       │   │   ├── FileItem.kt     # Data class: file metadata + formatting
│       │   │   ├── FolderSelection.kt # URI + name for selected folders
│       │   │   └── ScanState.kt    # Sealed class: Idle/Scanning/Progress/Complete/Error
│       │   ├── scanner/
│       │   │   └── DuplicateScanner.kt  # MediaStore queries + SHA-256 hashing
│       │   ├── trash/
│       │   │   └── TrashManager.kt     # Trash/delete via ContentResolver + quarantine
│       │   ├── viewmodel/
│       │   │   └── CleanupViewModel.kt # MVVM state management
│       │   ├── ui/
│       │   │   ├── theme/
│       │   │   │   ├── Color.kt    # Material 3 color palette
│       │   │   │   ├── Theme.kt    # Dynamic color + light/dark themes
│       │   │   │   └── Type.kt     # Typography
│       │   │   ├── screens/
│       │   │   │   ├── DashboardScreen.kt  # Home: storage stats + scan button
│       │   │   │   ├── ScanScreen.kt       # Permission flow + progress UI
│       │   │   │   ├── DuplicatesScreen.kt # Grouped duplicates + selection
│       │   │   │   └── TrashScreen.kt      # Trash list + restore/delete
│       │   │   └── components/
│       │   │       ├── FileItemCard.kt  # Reusable file card + dialogs
│       │   │       └── StatCard.kt      # Storage stat cards
│       │   └── util/
│       │       └── FileUtils.kt    # SHA-256 hashing, MediaStore helpers, permissions
│       └── res/
│           ├── drawable/            # Launcher foreground icon
│           ├── mipmap-anydpi-v26/   # Adaptive icon
│           ├── values/
│           │   ├── colors.xml       # Icon background color
│           │   ├── strings.xml      # All string resources
│           │   └── themes.xml       # Base theme
│           └── xml/
│               └── file_paths.xml   # FileProvider paths for quarantine
├── build.gradle.kts                # Root: plugin versions (AGP 8.7.3, Kotlin 2.1.0, KSP)
├── settings.gradle.kts             # Project name + repo config
├── gradle.properties               # JVM args, AndroidX config
├── gradle/wrapper/
│   └── gradle-wrapper.properties   # Gradle 8.11.1
├── .gitignore
└── README.md                       # Build instructions + documentation
```

---

## 5. Core Technical Design

### Duplicate Detection
- **Two-pass approach**: Group by file size first (cheap comparison), then SHA-256 hash only files that share a size
- **Hashing**: 8KB buffered reads via `MessageDigest`, runs on `Dispatchers.IO`
- **Grouping**: Files with identical hashes are grouped as duplicates
- **UI**: Grouped cards showing name, count, total size, recoverable space, with "Select Duplicates" convenience button

### Trash System
- **API 30+**: Uses `ContentResolver.delete()` directly (simplified from `MediaStore.createTrashRequest` which had compilation issues)
- **API 26-29**: App-managed quarantine folder in `context.filesDir/quarantine/`, with JSON metadata file for tracking original paths
- **Restore**: Copies file back from quarantine to MediaStore

### Permission Flow
- **API 26-32**: `READ_EXTERNAL_STORAGE`
- **API 33+**: `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` (granular)
- **API 30+**: Optional `MANAGE_EXTERNAL_STORAGE` for full access (user-triggered via settings)
- All permissions requested via `ActivityResultContracts.RequestMultiplePermissions`

---

## 6. CI/CD Pipeline

**File:** `.github/workflows/build-apk.yml`

- **Trigger**: Push to `main` → builds debug APK; manual dispatch → choose debug or release
- **Runner**: `ubuntu-latest`
- **Steps**: Checkout → JDK 17 (Temurin) → Android SDK → Generate Gradle wrapper → Build APK → Upload artifact
- **Release builds**: Require 4 GitHub Secrets for keystore signing:
  - `KEYSTORE_BASE64` — base64-encoded `.jks` file
  - `KEYSTORE_PASSWORD`
  - `KEY_ALIAS`
  - `KEY_PASSWORD`
- **Artifact**: Downloadable from Actions tab, retained for 30 days

---

## 7. Build History (This Session)

| Build | Commit | Status | Issue |
|-------|--------|--------|-------|
| #1 | Initial | ✅ Success | First build on master branch |
| #2-#4 | Various fixes | ❌ Failed | Release build without keystore secrets |
| #5 | Progress lambda fix | ❌ Failed | 8 Kotlin compilation errors |
| #6 | @OptIn fix | ❌ Failed | Same 8 errors (pendingIntent, icons, naming) |
| #7 | All 8 errors fixed | ❌ Failed | `PendingIntent.send(context)` wrong signature |
| #8 | PendingIntent fix | ❌ Failed | `MediaStore.createTrashRequest` compilation error |
| #9 | Simplified TrashManager | ✅ **Success** | Debug APK built successfully |

### Errors Fixed (Cumulative)
1. `StorageCleanupApp` naming conflict → Renamed composable to `StorageCleanupContent()`
2. `PendingIntent.send(context)` → `pendingIntent.send()` (no-arg)
3. `context.registerReceiver(null, IntentFilter)` → Removed incorrect pattern
4. `Icons.Default.Trash` → Changed to `Icons.Default.DeleteForever`
5. `Icons.Default.FolderDelete` → Changed to `Icons.Default.Folder`
6. `Icons.Default.AudioFile` → Changed to `Icons.Default.InsertDriveFile`
7. `Icons.Default.BrokenImage` → Changed to `Icons.Default.Image`
8. `CircularProgressIndicator(progress = animatedProgress)` → `progress = { animatedProgress }` (lambda)
9. `uris.append()` → `uris.add()` (MutableList method)
10. Missing `@OptIn(ExperimentalMaterial3Api::class)` on `TrashItemCard`
11. Missing `kotlin.plugin.serialization` in app module
12. `MediaStore.createTrashRequest`/`createDeleteRequest` → Replaced with `ContentResolver.delete()`
13. Duplicate import `FontWeight` in TrashScreen.kt
14. Unused imports (`FileItemCard`, `DeleteSweep`, `Folder`)

---

## 8. How to Build & Deploy

### Local (if Android Studio available)
```bash
cd storage-cleanup
gradle wrapper --gradle-version 8.11.1
./gradlew assembleDebug
```

### Via GitHub Actions (current setup)
1. Push to `main` branch
2. Go to https://github.com/unhuman-web/AngleV1/actions
3. Wait for "Build APK" workflow to complete (green checkmark)
4. Click the workflow run → download "storage-cleanup-debug" artifact
5. Transfer APK to Android device and install

### For Release APK
1. Generate keystore: `keytool -genkey -v -keystore release-key.jks -alias storage-cleanup -keyalg RSA -keysize 2048 -validity 10000`
2. Encode: `base64 -i release-key.jks | tr -d '\n'`
3. Add 4 GitHub Secrets (see Section 6)
4. Go to Actions → "Build APK" → Run workflow → Select "release"

---

## 9. Known Limitations & Future Work

- **Trash implementation**: Currently uses direct `ContentResolver.delete()` instead of `MediaStore.createTrashRequest()` (API 30+ system trash). The system trash API had compilation issues with the current SDK setup.
- **Quarantine metadata**: Uses simple JSON file (not Room DB). Could be upgraded for better querying.
- **No tests**: Unit tests and instrumentation tests not yet written.
- **ProGuard**: Rules are minimal. May need updating for production.
- **UI polish**: Functional but could benefit from animations, loading skeletons, empty state illustrations.

---

## 10. Key Commands

```bash
# Git
git status
git log --oneline
git push

# Build (local)
./gradlew assembleDebug
./gradlew assembleRelease  # requires keystore.properties

# Check GitHub Actions status
curl -s https://api.github.com/repos/unhuman-web/AngleV1/actions/runs?per_page=1 | grep -E '"conclusion"|"status"'
```

---

## 11. Working Directory & Key Paths

- **Primary working dir**: `C:\Users\Administrator\storage-cleanup`
- **Repository**: https://github.com/unhuman-web/AngleV1
- **Branch**: `main`
- **Latest passing commit**: `8b5a292` (Build #9)
- **Debug APK**: Available as GitHub Actions artifact from Build #9
