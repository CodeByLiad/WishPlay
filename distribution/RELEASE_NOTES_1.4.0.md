# WishPlay 1.4.0 — broader game discovery

- Added independent Nintendo, PlayStation, Microsoft, and Epic catalogs. Console search now has alternatives when RAWG is unavailable; PC search includes games outside Steam.
- Added a separate Switch 2 filter and verified platform membership from source metadata. PC-only Microsoft records are not labelled Xbox; older Nintendo systems are not labelled Switch.
- Added full Steam and Microsoft search alongside their quick results, with real pagination for Steam, Microsoft, Nintendo, PlayStation, and Epic catalogs.
- Added real product detail lookups and source links for the new catalogs. Saved Epic entries can refresh after reopening using their saved title while validating the stable offer identity.
- Excluded PlayStation costumes, demos, upgrades and unrelated products. Epic requires game categories and explicit platform tags.
- Fixed Retry: it refreshes the failed page, bypasses the search snapshot, and retains visible results.
- Store names remain visible under platform filters. Missing dates say “Date not listed”; confirmed duplicate listings merge metadata/evidence, while different stores and same-name games remain identifiable.
- Reduced repeated title-normalization work during result merging and sorting.
- Kept incremental results, cancellation and recent-search caching. Production code contains no title-specific discovery patch or fictional fallback games.

Thirty unrelated live title checks cover Android, PC, PS4, PS5, Xbox Series, Switch, and Switch 2. Additional live checks verify new records on later Steam, Nintendo, and PlayStation pages. See [the discovery verification matrix](https://github.com/CodeByLiad/WishPlay/blob/main/Docs/Catalog_Discovery.md).

Verification includes 46 unit tests, successful runs for 23 focused Android checks, and a separate smoke check of the exact original-key signed APK. Expected provider/title matches appeared in 44–2,089 ms in the final timed emulator run; network and device speeds vary. See [the exact verification scope](https://github.com/CodeByLiad/WishPlay/blob/main/Docs/Release_Verification.md).

Every feature remains free. Donations are optional, via bKash number **01978900129** only. No admin or paid upgrade screens.

Install over the existing Jinatra app to retain your wishlist. Package `com.jinatra.wishplay`, version code **8**, original release signing certificate.

Public catalogs can omit regional, removed, unindexed, or unannounced games. Dates and requirements are shown only when supplied by their sources. This release expands general discovery; it does not claim every game ever made is indexed. Real-account sync and physical-phone background notification delivery remain separate verification scopes.
