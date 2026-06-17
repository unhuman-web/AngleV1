# Storage Cleanup

An Android app that scans for duplicate files and manages trash/recently deleted items.

## Features

- **Duplicate File Scanner**: Detects duplicate files using SHA-256 hashing with a two-pass approach (size grouping first, then hashing)
- **Trash Management**: Native trash on API 30+ via `MediaStore.createTrashRequest()`, app-managed quarantine for API 26-29
- **User-Selectable Folders**: Choose which directories to scan using the Storage Access Framework
- **Material 3 UI**: Clean, modern interface with dynamic color support

## Tech Stack

- Kotlin + Jetpack Compose
- Material 3 with Dynamic Color
- MVVM Architecture (ViewModel + StateFlow)
- Kotlin Coroutines for background work
- Room Database for quarantine metadata

## Permissions

The app requires the following permissions:

| API Level | Permission | Purpose |
|-----------|------------|---------|
| 26-32 | `READ_EXTERNAL_STORAGE` | Access all media files |
| 33+ | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` | Granular media access |
| 30+ | `MANAGE_EXTERNAL_STORAGE` | Full file system access (user-selectable) |

## Build

### Prerequisites

- Android Studio Ladybug or later (optional — can build entirely via GitHub Actions)
- JDK 17+
- Android SDK 35

### Option 1: Build via GitHub Actions (Recommended)

This project is designed to build entirely via GitHub Actions. No local Android Studio needed.

**Step 1: Create a GitHub repository**
```bash
git remote add origin <your-repo-url>
git push -u origin master
```

**Step 2: Build Debug APK**
1. Go to your repository on GitHub
2. Click **Actions** → **Build APK**
3. Click **Run workflow**
4. Select `debug` as the build type
5. Click **Run workflow**
6. When complete, download the APK from the workflow artifacts

**Step 3: Build Release APK (Optional)**
1. Generate a keystore (see below)
2. Add GitHub Secrets (see below)
3. Run the workflow with `release` build type, or push to `main` branch

#### Required GitHub Secrets for Release Builds

Go to your repository → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | Base64-encoded keystore (see below) |
| `KEYSTORE_PASSWORD` | Your keystore password |
| `KEY_ALIAS` | Key alias (e.g., `storage-cleanup`) |
| `KEY_PASSWORD` | Your key password |

**Generating a keystore:**
```bash
keytool -genkey -v \
  -keystore release-key.jks \
  -alias storage-cleanup \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000

# Convert to base64 for GitHub Secret
base64 -i release-key.jks | tr -d '\n'
```

### Option 2: Local Build (Requires Android Studio)

```bash
# Clone the repository
git clone <repository-url>
cd storage-cleanup

# Generate Gradle wrapper (if not present)
gradle wrapper

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires keystore.properties)
./gradlew assembleRelease
```

## How It Works

### Duplicate Detection

1. **Size Grouping**: Files are first grouped by size (cheap comparison)
2. **SHA-256 Hashing**: Only files with matching sizes are hashed
3. **Grouping**: Files with identical hashes are grouped as duplicates
4. **Selection**: User chooses which copies to keep vs. delete

### Trash Behavior

#### API 30+ (Android 11+)
- Uses `MediaStore.createTrashRequest()` for system-level trash
- Items auto-delete after 30 days
- Can restore items by setting `IS_TRASHED = 0`

#### API 26-29 (Android 8-10)
- App-managed quarantine folder in app-private storage
- Metadata stored in JSON file
- Items moved to quarantine directory before original is deleted
- Can restore by copying back to original location

## Project Structure

```
storage-cleanup/
├── app/
│   └── src/main/
│       ├── java/com/example/storagecleanup/
│       │   ├── MainActivity.kt          # Main entry point
│       │   ├── StorageCleanupApp.kt     # Application class
│       │   ├── model/                   # Data classes
│       │   ├── scanner/                 # Duplicate scanning logic
│       │   ├── trash/                   # Trash management
│       │   ├── viewmodel/               # MVVM ViewModels
│       │   ├── ui/                      # Compose UI
│       │   │   ├── theme/               # Material 3 theme
│       │   │   ├── screens/             # Screen composables
│       │   │   └── components/          # Reusable UI components
│       │   └── util/                    # Utility functions
│       └── res/                         # Android resources
├── .github/workflows/                   # CI/CD configuration
└── build.gradle.kts                     # Build configuration
```

## License

MIT License
