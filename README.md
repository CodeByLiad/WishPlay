# WishPlay 🎮

[![Android](https://img.shields.io/badge/Platform-Android_8.0+-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3_Expressive-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Database](https://img.shields.io/badge/Room-SQLCipher_Encrypted-003B57?logo=sqlite&logoColor=white)](https://www.zetetic.net/sqlcipher/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

> **Never miss a game drop.** One list for every video game you are waiting for across PC, PlayStation, Xbox, Nintendo Switch, Android, and iOS.

WishPlay is an offline-first native Android game wishlist and release tracking app designed with Google's Material 3 Expressive design tokens. It provides countdown reminders, date-change notifications, PC system requirements, trailer playback, cloud sync, and a one-time lifetime Pro upgrade.

---

## ✨ Features

- **Multi-Platform Tracking:** Follow games across PC (Steam), PS5, Xbox Series X/S, Nintendo Switch / Switch 2, Android, and iOS with independent release dates per platform.
- **Offline-First & Encrypted:** Complete local functionality using Room encrypted with **SQLCipher** (256-bit AES-GCM key backed by Android Keystore).
- **Custom Games:** Add any unannounced or indie game that isn't in public databases yet with custom dates and notes.
- **Smart Alerts Engine:**
  - Automated daily checks via WorkManager.
  - Release-day reminders and configurable countdown leads (1 day, 3 days, 1 week before release).
  - Date-change detection (delays, moved dates, or returns to TBA).
  - 4 distinct Android notification channels with reboot persistence (`BootReceiver`).
- **Cloud Sync & Accounts:** Seamless Google Sign-In through Android Credential Manager. Local guest wishlists automatically merge on first sign-in.
- **Home-Screen Widget:** Interactive Jetpack Glance widget displaying the next upcoming release and a live countdown badge.
- **Material 3 Expressive UI:** Curated color schemes, fluid springs, custom cookie-shaped artwork containers (`C4`, `C6`, `C9`, `C12`, `SQ`), and light/dark/system theme support.
- **Pro Entitlement & Payments:**
  - Unlimited wishlist (bypasses 25-game free cap).
  - PC Price-drop alerts (Steam / IsThereAnyDeal).
  - Custom accent themes (violet, teal, coral swatches).
  - Multi-channel payments: Paddle Billing (cards) and bKash Send Money.
  - Promo code engine (`WP-XXXX-XXXX`) with single-transaction atomic redemption.
- **In-App Admin Panel:** Secured with `FLAG_SECURE` (screenshot & screen recording blocked), live bKash approval queue with audit logging, and code management.
- **Hardened & Optimized:** Certificate pinning with backup pins, cleartext HTTP strictly disabled, R8 full-mode minification, and in-app updater with SHA-256 + certificate matching.

---

## 🛠 Tech Stack & Architecture

- **Language:** Kotlin 2.1.10
- **UI Framework:** Jetpack Compose with Material 3 Expressive
- **Architecture:** MVVM + Clean Architecture + Repository Pattern
- **Dependency Injection:** Hilt / Dagger
- **Local Storage:** Room Database + SQLCipher + DataStore Preferences
- **Asynchronous Flow:** Kotlin Coroutines & Flow
- **Background Scheduling:** Android Jetpack WorkManager
- **Networking:** OkHttp (Certificate Pinning) + Kotlinx Serialization
- **Image Loading:** Coil (Disk Cache)
- **Home Widget:** Jetpack Glance 1.1.1
- **Auth:** Google Credential Manager + Firebase Auth
- **Backend:** Supabase / Firebase Cloud Functions (TypeScript)

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 21
- Android SDK 35 (Minimum SDK: 26 / Android 8.0 Oreo)

### Build Commands
```bash
# Clone the repository
git clone https://github.com/mdliad-se/WishPlay.git
cd WishPlay

# Run all unit test suites
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug

# Build optimized release APK (R8 full mode + resource shrinking)
./gradlew assembleRelease
```

The compiled release APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

---

## 🔒 Security & Privacy

- **No Secrets in APK:** All third-party game proxy keys live in server environment variables.
- **Encrypted Storage:** Room SQLite database encrypted via SQLCipher with keys in the Android Keystore.
- **No Third-Party Trackers:** Zero ad networks, zero privacy-invasive tracking SDKs.
- **Backup Exclusions:** Database and token preferences explicitly excluded from cloud backups.

---

## 👤 Author

Developed by **Md Liad** ([@mdliad-se](https://github.com/mdliad-se))  
Contact: `mdliad.se@gmail.com`
