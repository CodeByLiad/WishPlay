# Broad catalog discovery — 1.4.0

WishPlay queries independent catalogs in parallel. Each working source appears as soon as it answers; a slow or unavailable source cannot hold back the others. This is a general discovery change: production code contains no special case or bundled record for Offroad League Online or any other tested title.

Search rows identify their store even when a platform filter is active. A missing date says **Date not listed**. Duplicate records are combined when their provider IDs match, or when their title, platform set, and verified listing URL match. Other store offers remain separately identified: a shared title alone cannot prove two records represent the same game. This preserves homonyms and different editions while combining quick/full responses for the same listing.

## Sources

| Source | Verified membership | More results | Details |
| --- | --- | --- | --- |
| RAWG | Provider platform records | Provider pages | Provider details and linked stores |
| Google Play | Featured/compact records classified as games | Public search response | Public game listing |
| Steam | Game-filtered full search plus quick suggestions | 50 search rows per page | Native game details, requirements and storage |
| App Store | Software records with the Games genre | Up to 100 search records | Native lookup |
| Nintendo Europe | Explicit Switch / Switch 2 system names | 40 records per page | Catalog record by Nintendo ID |
| PlayStation Store US | Explicit PS4 / PS5 product platforms and full-game classification | Store cursor and 24 search records per page | Server-rendered public product metadata |
| Microsoft Store US | Product type, SKU allowed platforms, and Xbox generation compatibility | Fast suggestions plus full search with store continuation cursors; details hydrated in bounded batches | Product lookup by stable Microsoft ID |
| Epic Games Store US | Game categories and explicit Windows / Android / iOS tags | 40 offers per page | Public search refreshed using the saved title; stable namespace/offer ID must match |

Switch 2 has its own search filter. WishPlay does not substitute an unfiltered RAWG query for an unknown platform identifier. Nintendo dates are labelled Europe. Other store evidence is labelled US. Epic PC listing dates are not reused as native Android or iOS release dates.

PlayStation costumes, demos, upgrade packs, and unrelated products are excluded. Microsoft PC-only products are not labelled Xbox. Old Nintendo systems are not relabelled Switch. Unknown dates and requirements remain unknown.

## Live checks on the dedicated Android emulator

Thirty unrelated query/title checks passed using actual production adapters and real network responses:

| Platform / source | Tested titles or queries |
| --- | --- |
| Android / Google Play | Offroad League Online, PUBG MOBILE, Free Fire, Mobile Legends, Call of Duty, Roblox, Clash of Clans, Candy Crush, Minecraft, Balatro, Vampire Survivors, Stardew Valley, Monument Valley, Genshin Impact |
| Switch / Nintendo | Zelda: Breath of the Wild, Mario Kart 8, Animal Crossing: New Horizons |
| Switch 2 / Nintendo | Mario Kart World, Donkey Kong Bananza |
| PS4 / PlayStation | Bloodborne |
| PS5 / PlayStation | Astro Bot, Stellar Blade |
| Xbox / Microsoft | Halo Infinite, Forza Horizon 5 |
| PC / Microsoft | Minecraft: Java & Bedrock Edition for PC, Sea of Thieves |
| PC / Epic | Fortnite, Alan Wake 2, VALORANT, Genshin Impact |

The 16 non-Android-matrix checks also open details with a new API instance and the saved title, simulating a saved entry reopened without the previous search cache. The checks assert actual IDs, expected titles, platform membership, and source links, rather than merely a nonempty result list. The Minecraft PC check was tightened to require the original Java & Bedrock edition: the earlier broad substring accepted Minecraft Legends and was not sufficient proof for the original game.

The latest live pagination run returned 50 additional Steam games, 40 Nintendo games, 11 PlayStation games, and 13 Microsoft games on their second pages. Source-level page checks do not depend on RAWG; counts can vary as store ranking and listings change.

In the final November 12 broad live run, the expected title from the required provider appeared in **44–2,089 ms** across the 16 non-Android-matrix queries. A previous run exposed a nearly 14-second outlier; title matching now reuses compiled patterns and cached normalized names to reduce repeated merge work. The final Alan Wake 2 match appeared in 451 ms. The check separately records the first result from any provider, so an unrelated early result cannot improve this expected-title metric. Network and artwork timing vary; this is measured emulator evidence, not an instant-network guarantee. Complete source aggregation can take longer while another provider reaches its deadline. Existing cache/cancellation checks cover immediate repeated searches and cancellation of superseded HTTP calls.

## Limits and maintenance

No public catalog guarantees every game ever made. Listings may be regional, removed, not yet indexed, or missing platform information. WishPlay preserves provider evidence rather than fabricating matches. Google Play and the App Store still have search-response limits; those sources do not gain artificial page numbers that repeat the same records. Microsoft's short suggestions are supplemented by its full paginated search.

Public store response formats and PlayStation's published persisted-query hash can change. A provider failure is isolated and reported while other results remain available. The PlayStation operation/hash was verified from its own storefront bundle on November 11, 2026. Epic direct-offer requests were unavailable during development; saved-title lookup uses its working public search operation and validates the stable offer identity.

The older Supabase demo search is not used: it contains fictional fallback games and cannot provide real coverage. Universal coverage is not claimed by this release or by the 30-title matrix.
