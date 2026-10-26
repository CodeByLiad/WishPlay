# Material 3 Expressive UI — design

Date: 2026-10-26
Branch: main (work will be isolated in a worktree)
Status: awaiting approval

## 1. Problem

`.spine/config.md` describes the stack as "Jetpack Compose Material 3 Expressive". The build does not
match that claim. `gradle/libs.versions.toml` pins `composeBom = "2025.02.00"`, which resolves
`androidx.compose.material3` to 1.3.1 — a release that contains none of the Material 3 Expressive
APIs. The expressive look currently in the app is hand-rolled on top of Material 3 1.3.1:

| File | What it hand-rolls |
| --- | --- |
| `ui/components/CookieShape.kt` | Polar-equation cookie/squircle outlines, sampled every 3° into straight line segments |
| `ui/components/ExpressiveButton.kt` | Corner-radius + scale press animation across six ad-hoc styles |
| `ui/components/WavyProgressIndicator.kt` | Sine-wave progress track |
| `ui/components/GroupedCard.kt` | Grouped list container |
| `ui/main/MainScreen.kt` | Bottom navigation built from a `Row` of `clickable` boxes |
| `ui/theme/Motion.kt` | A single spring helper plus one cubic-bezier easing |
| `ui/theme/WishPlayColors.kt` | A parallel colour container holding roles Material 3 already defines |

The result reads as an app that imitates Material 3 Expressive rather than one that uses it. The
shapes are approximations, the motion is one spring applied uniformly, the colour scheme is twelve
hand-picked hexes per mode, and the type scale defines 10 of the 15 Material roles with no font file
shipped at all.

## 2. Decisions taken during framing

All five were decided by the user; each is recorded with the reasoning that was presented.

1. **Dependency target: `androidx.compose.material3:material3:1.5.0-beta02`.**
   Material 3 1.4.0 is the current stable release and carries `MotionScheme` and `LoadingIndicator`,
   but the shape system (`MaterialShapes`, `Morph`), `MaterialExpressiveTheme`, `ButtonGroup`,
   `FloatingToolbar`, `ShortNavigationBar` and the emphasized type styles land in the 1.5.0 line.
   The visual complaint is mostly shape- and component-driven, so stable alone would not resolve it.
2. **Colour: brand tonal palettes plus a Material You toggle.**
   The gold and lilac identity is kept and becomes the default, but it is generated into complete
   tonal palettes rather than hand-picked. On Android 12+ the user can switch to wallpaper-derived
   dynamic colour as an additional accent choice.
3. **Typography: ship Roboto Flex.**
   All 15 Material roles plus the Emphasized set, driven by the variable font's weight and width
   axes. The Bricolage Grotesque / Figtree names currently in `Type.kt` are fiction — no font file
   exists in the project — so nothing is lost by replacing them.
4. **Scope: foundations plus the four main tabs.**
   Theme, colour, shape, motion, typography and the shared component library are rebuilt; Wishlist,
   Search, Alerts and Profile get a hand pass with the new components. Details, Welcome, Custom,
   Admin, Calendar and the seven sheets inherit the new foundations but get no bespoke work.
5. **Wishlist add action: FAB Menu.**
   The current stacked FAB overlaps a game row. It is replaced by an expressive FAB menu offering
   the two add paths that already exist (search the catalog, add a custom game).

## 3. Foundations

### 3.1 Colour

`ui/theme/Color.kt` and `ui/theme/WishPlayColors.kt` are replaced by generated schemes.

- Four brand schemes, one per existing `AccentColor` (Gold, Violet, Teal, Coral), each generated as
  a full light and dark `ColorScheme` from the accent seed. Compose material3 exposes no public
  seed-to-palette API, so generation uses `com.materialkolor:material-kolor`
  (`rememberDynamicColorScheme(seed, isDark, style = PaletteStyle.Expressive)`). Every
  Material role is populated: `surfaceContainerLowest` through `surfaceContainerHighest`,
  `surfaceBright`, `surfaceDim`, the `*Fixed` roles and the `inverse*` roles.
- A fifth option, "Wallpaper", calls `dynamicLightColorScheme` / `dynamicDarkColorScheme` on API 31
  and above. Below API 31 the option is hidden and Gold is used (`minSdk` is 26).
- `primaryContainer` stops carrying the accent. The accent is the scheme's `primary`, which is what
  it semantically is, and `primaryContainer` goes back to being a tonal container.
- `WishPlayCustomColors` is deleted. The two roles it holds that Material does not define — the
  success pair and the page background — move to a much smaller extension holder; everything else
  (`surfaceContainer*`, `outlineVariant`, `scrim`, `error*`, `track`) comes from the scheme.

Every screen must be checked against all five accent options in both modes, because dynamic colour
means arbitrary hues can now reach surfaces that previously only ever saw lilac.

### 3.2 Shape

`CookieShape.kt` is deleted in favour of `MaterialShapes`. The polar-equation cookie becomes the
real `MaterialShapes.Cookie9Sided` / `Cookie12Sided` family, and the squircle becomes the
corresponding rounded-polygon shape, so the outlines are curve-accurate instead of 120 straight
segments. The game-logo badges in Wishlist rows and the Next Drop hero keep their per-game shape
assignment; only the shape source changes. Shape morph on press (`Morph` between a rest shape and a
pressed shape) replaces the manual `animateDpAsState` corner interpolation in `ExpressiveButton`.

### 3.3 Typography

`Type.kt` is rewritten against the Material 3 Expressive type scale: display, headline, title, body
and label at large/medium/small, plus the Emphasized variants for the roles that have them. Roboto
Flex ships as a variable font in `app/src/main/res/font`, and emphasis is expressed through the
weight and width axes rather than by swapping a bolder static face.

### 3.4 Motion

`ui/theme/Motion.kt` is deleted. `MotionScheme.expressive()` is passed to the theme, and components
read their specs from it — `MaterialTheme.motionScheme.defaultSpatialSpec()`,
`fastEffectsSpec()` and so on — instead of every animation sharing one `dampingRatio = 0.55f`
spring regardless of whether it moves a bounding box or fades a colour.

### 3.5 Theme entry point

`WishPlayTheme` switches from `MaterialTheme` to `MaterialExpressiveTheme`, supplying the chosen
colour scheme, the new typography, the expressive shape scheme and `MotionScheme.expressive()`. The
existing `darkTheme` and `accentColor` parameters are kept so no call site changes signature; the
accent parameter gains a "Wallpaper" case.

## 4. Component library

| Current | Replacement |
| --- | --- |
| `ExpressiveButton` (6 ad-hoc styles) | Material `Button` / `FilledTonalButton` / `OutlinedButton` / `TextButton` with the expressive size variants and shape morph. The wrapper is kept only as a thin alias so call sites migrate incrementally. |
| `ExpressiveChip` | `FilterChip` / `AssistChip` from the expressive set |
| `WavyProgressIndicator` | `LinearWavyProgressIndicator` |
| `CookieShape` | `MaterialShapes` |
| `GroupedCard` | `Card` with the expressive shape scheme |
| `WishPlayBottomNav` (`Row` of clickables) | `ShortNavigationBar` |
| Stacked FAB in Wishlist | `FloatingActionButtonMenu` |
| — (new) | `ButtonGroup` for the Wishlist filter row and the Search platform filters |
| — (new) | `LoadingIndicator` for in-flight search and catalog fetches |

**API-surface spike (first task of the build).** The exact names, parameters and opt-in annotations
above are taken from the 1.5.0 release notes and reference pages, not from the artifact. Because
1.5.0-beta02 is not mapped by any Compose BOM (BOM 2026.09.00 still maps to material3 1.4.0), the
dependency must be declared explicitly outside the BOM, and the first build task is to add the
dependency, compile a scratch file exercising each API in the table, and correct this document
where the real surface differs. No component work starts before that spike passes.

## 5. Screen-level work

**Wishlist.** FAB menu replaces the stacked FAB and its row overlap. The filter row
(All / Upcoming / Out now / Date TBA) becomes a `ButtonGroup`. The Next Drop hero keeps its gold
container but takes the expressive shape and the real wavy progress indicator. Month group headers
and `GameRow` take the expressive shapes and spatial motion.

**Search.** `LoadingIndicator` during queries; platform filters become a `ButtonGroup`; result rows
match the new `GameRow`. The cross-platform exclusions section added in `1945035` keeps its
behaviour and gets the new container styling.

**Alerts.** List items and the alert sheets take the new containers, shapes and motion.

**Profile.** The accent picker gains the Wallpaper option and previews each scheme using real
generated tones rather than a single swatch.

## 6. Out of scope

- Game Details, Welcome, Custom game, Admin, Calendar and the seven sheets — these inherit the new
  colour, type and motion through the theme but receive no bespoke restyle.
- The Glance widget. It uses `glance-material3` and a separate colour path; it is not touched.
- Any change to data, repositories, view-model state shape, navigation structure or feature
  behaviour. This is a presentation-layer change.
- Shipping a release build. The branch targets a beta dependency; whether 1.1.1 ships on it is a
  separate decision at finish time.

## 7. Risks

1. **Beta dependency in a shipped app.** material3 1.5.0-beta02 is outside the BOM and its
   experimental APIs can change before stable. Mitigated by pinning the exact version, keeping the
   expressive opt-ins confined to `ui/theme` and `ui/components`, and treating the spike as a gate.
2. **Toolchain drift.** The project is on AGP 8.8.2, Kotlin 2.1.10 and Compose BOM 2025.02.00.
   Moving material3 forward by two minor versions will pull a newer Compose runtime, which may
   require a newer BOM and may in turn require a newer Kotlin or AGP. The spike surfaces this
   before any UI work is committed.
3. **Instrumented tests.** Nine `connectedDebugAndroidTest` tests drive the UI by node matching.
   Replacing the bottom navigation and the FAB will break matchers that key on structure. These are
   updated as part of the task that changes the component, not afterwards.
4. **Dynamic colour contrast.** Wallpaper-derived schemes can produce combinations the hand-picked
   palette never did. Every screen is checked in both modes against the four brand accents and at
   least two contrasting wallpaper-derived schemes.
5. **APK size.** Roboto Flex adds roughly 1.5 MB. Acceptable, but it is a real regression to note in
   the release notes.

## 8. Verification

- `./gradlew testReleaseUnitTest` — 36 unit tests stay green.
- `./gradlew connectedDebugAndroidTest` — 9 instrumented tests, updated alongside the components
  they exercise.
- Live pass on emulator `lockout_qa` over the four tabs, in light and dark, across all five accent
  options, per the `browser-qa` stage.
- The boot assertion in `.spine/config.md` is unaffected: this change touches neither the signing
  configuration nor the package name.

## 9. Follow-on

`.spine/config.md` currently claims a stack the build does not provide. When this branch lands, the
Stack line is corrected to name the actual material3 coordinate and version, and the contract is
re-asserted.
