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

- Android Studio Ladybug or later
- JDK 17+
- Android SDK 35

### Local Build

```bash
# Clone the repository
git clone <repository-url>
cd storage-cleanup

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

### CI/CD (GitHub Actions)

The project includes a GitHub Actions workflow that builds a signed release APK.

#### Required GitHub Secrets

To build signed APKs, add these secrets to your repository:

1. **`KEYSTORE_BASE64`**: Base64-encoded keystore file
   ```bash
   base64 -i release-key.jks | tr -d '\n'
   ```

2. **`KEYSTORE_PASSWORD`**: Keystore password

3. **`KEY_ALIAS`**: Key alias in the keystore

4. **`KEY_PASSWORD`**: Key password

#### Generating a Keystore

```bash
keytool -genkey -v \
  -keystore release-key.jks \
  -alias storage-cleanup \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
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
