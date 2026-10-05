# WishPlay v1.0.0 🎮

Never miss a game drop. One unified list for every video game you are waiting for across PC, PlayStation, Xbox, Nintendo Switch, Android, and iOS.

---

### ✨ Features & Highlights

- **Multi-Platform Game Discovery:** Search over 500,000+ titles powered by live game database integration with platform-specific release dates, cover art, and PC system specs.
- **Offline-First & SQLCipher Encrypted:** Fully functional offline using a local Room SQLite database encrypted with hardware-backed 256-bit AES-GCM keys.
- **Smart Background Alerts:**
  - Automated daily checks via WorkManager.
  - Release-day reminders & configurable countdown leads (1 day, 3 days, 1 week).
  - Instant date-change alerts for delays or moved launches.
- **Google Sign-In & Multi-Device Sync:** Back up your wishlist and seamlessly sync across devices using Android Credential Manager.
- **Home-Screen Widget:** Interactive Jetpack Glance widget with next-drop countdown and live progress badge.
- **Pro Gating & Upgrades:** Lifetime Pro upgrade unlocking unlimited wishlist capacity, PC price-drop alerts, custom accent color themes, and widget customization.
- **In-App Admin Panel:** Secured with `FLAG_SECURE` screenshot blocking, live promo code engine (`WP-XXXX-XXXX`), and bKash approval queue.
- **Hardened Security:** Built in R8 full mode, certificate pinning with backup pins, HTTP cleartext disabled, and in-app updater with SHA-256 and signing certificate matching.

---

### 📦 Release Assets & Verification

| Asset | Size | SHA-256 Checksum |
| :--- | :--- | :--- |
| **`wishplay-v1.0.0.apk`** | ~17.5 MB | `da9233c6c3f95e4befea8cb9c353d3c9f24457b9035a5a7502a6d5faf250745e` |
| **`update_manifest.json`** | ~400 B | In-app update descriptor |

#### Verify APK Checksum
```bash
# Windows PowerShell
Get-FileHash wishplay-v1.0.0.apk -Algorithm SHA256

# Linux / macOS
sha256sum wishplay-v1.0.0.apk
```

**Requirements:** Android 8.0 (Oreo / API 26) or higher.
