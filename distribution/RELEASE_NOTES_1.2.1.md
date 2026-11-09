# WishPlay 1.2.1 — Google sign-in connection fix

Fixes the **“An internal error has occurred. [ Pin verification failed ]”** sign-in error in 1.2.0.

The app's network security configuration pinned all Google API connections to a fixed set of certificate keys. Those pins rejected valid certificate chains for Firebase Authentication, token refresh, and Firestore. The same failure was reproduced against all three services from the installed 1.2.0 app.

- Removed the stale third-party certificate pins from Android network configuration and the shared HTTP client.
- HTTPS remains required, with normal system certificate and hostname verification. No permissive trust manager or hostname verifier was added.
- Added live Android regression checks for the three affected endpoints, Firebase Authentication's response to a deliberately invalid test token, and cleartext blocking. These checks do not use or create a real account.
- Verified the original 1.2.0 failure, then passed all three live authentication transport tests with this fix. All 41 unit tests also pass.
- All features remain free; optional bKash donations and the existing wishlist are unchanged.

Install this APK over the existing app to retain your wishlist. It uses the same Jinatra signing certificate and Android package, with version code **5**.

Completing Google account selection and cloud sync with a real account still requires an on-device retry after updating. Catalog-provider outages and device-specific background reminder timing remain separate verification limits.
