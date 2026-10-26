# Material 3 Expressive UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use subagent-driven-dev (recommended) or executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace WishPlay's hand-rolled imitation of Material 3 Expressive with the real
`androidx.compose.material3` 1.5.0 expressive APIs — theme, colour, shape, motion, typography and
components — and restyle the four main tabs with them.

**Architecture:** Every hand-rolled component keeps its current public signature and is
re-implemented as a thin adapter over the official expressive component behind it. That is the
central decision of this plan: `ExpressiveButton`, `ExpressiveChip`, `GroupedItemContainer`,
`WavyProgressIndicator`, `CookieShapeType` and `WishPlayThemeColors` are used by screens that are
explicitly out of scope (Details, Welcome, Custom, Admin, Calendar, seven sheets), so preserving the
seam means those screens inherit the expressive look with zero edits, and the diff stays inside
`ui/theme`, `ui/components` and the four in-scope screens.

**Tech Stack:** Kotlin 2.1.10, AGP 8.8.2, Gradle 8.14, Jetpack Compose (BOM 2026.09.00),
`androidx.compose.material3:material3:1.5.0-beta02` declared outside the BOM,
`com.materialkolor:material-kolor` for seed-to-palette generation, Hilt, JUnit4 + Compose test rule.

## Global Constraints

- Target dependency is exactly `androidx.compose.material3:material3:1.5.0-beta02`. No Compose BOM
  maps to it (BOM 2026.09.00 maps to material3 1.4.0), so it is declared with an explicit version
  and the BOM stays for every other Compose artifact.
- `minSdk` is 26. Dynamic colour APIs require API 31; every dynamic-colour path needs an
  `android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S` guard and a brand fallback.
- Expressive APIs require opt-in. Use `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` at the
  narrowest scope that compiles; do not add a project-wide compiler opt-in flag.
- No public signature of an existing component in `ui/components` or `ui/theme` may change. Behaviour
  and implementation change; call sites do not. The one exception is `AccentColor`, which gains a
  `WALLPAPER` entry (Task 2).
- No change to data, repositories, view-model state shape, navigation structure or feature
  behaviour. This is a presentation-layer change.
- No invented catalog data in any user-facing string, per `.spine/config.md`.
- **Task 1's API inventory is authoritative.** Every later task in this plan was written against the
  1.5.0 release notes, not against the resolved artifact. Where the inventory contradicts a code
  block below, the controller updates this plan before dispatching that task.
- Commit after every task. Conventional-commit prefixes, matching the existing history
  (`feat:`, `fix:`, `refactor:`, `chore:`, `docs:`).

---

## File Structure and Seams

| File | Status | Responsibility | Seam (what callers depend on) |
| --- | --- | --- | --- |
| `gradle/libs.versions.toml` | Modify | Version catalogue | `libs.androidx.material3`, `libs.materialkolor` |
| `Docs/spine/notes/m3-expressive-api-inventory.md` | Create | Verified API surface of material3 1.5.0-beta02 | Read by every later task |
| `ui/theme/WishPlayColorSchemes.kt` | Create | Turns an accent + dark flag + context into a complete `ColorScheme` | `fun wishPlayColorScheme(accent, darkTheme, context): ColorScheme` |
| `ui/theme/Color.kt` | Modify | Accent seeds and the two non-Material roles | `enum AccentColor`, `SuccessGreen*`, `Page*` |
| `ui/theme/WishPlayColors.kt` | Modify | Compatibility shim delegating to `MaterialTheme.colorScheme` | `WishPlayThemeColors.<role>` — unchanged property names |
| `ui/theme/Type.kt` | Modify | Full M3 Expressive type scale on Roboto Flex | `WishPlayTypography`, `DisplayFontFamily`, `BodyFontFamily` |
| `ui/theme/Theme.kt` | Modify | Expressive theme entry point | `WishPlayTheme(darkTheme, accentColor, content)` — unchanged |
| `ui/theme/Motion.kt` | Delete | — | Callers move to `MaterialTheme.motionScheme` |
| `ui/components/CookieShape.kt` | Modify | Maps `CookieShapeType` onto `MaterialShapes` | `enum CookieShapeType`, `CookieShape(type)` — unchanged |
| `ui/components/ExpressiveButton.kt` | Modify | Adapter over Material `Button` family | `ExpressiveButton(...)`, `enum ButtonStyle` — unchanged |
| `ui/components/ExpressiveChip.kt` | Modify | Adapter over `FilterChip` | `ExpressiveChip(...)` — unchanged |
| `ui/components/GroupedCard.kt` | Modify | Adapter over expressive shapes | `GroupedItemContainer(...)`, `enum GroupPosition` — unchanged |
| `ui/components/WavyProgressIndicator.kt` | Modify | Adapter over `LinearWavyProgressIndicator` | `WavyProgressIndicator(progress, modifier, color)` — unchanged |
| `ui/main/MainScreen.kt` | Modify | `ShortNavigationBar` replaces the hand-rolled `Row` | `WishPlayBottomNav(...)` — unchanged |
| `ui/wishlist/WishlistScreen.kt` | Modify | FAB menu, `ButtonGroup` filter row | — |
| `ui/search/SearchScreen.kt` | Modify | `ButtonGroup` platform filters, `LoadingIndicator` | — |
| `ui/alerts/AlertsScreen.kt` | Modify | Expressive containers and motion | — |
| `ui/profile/ProfileScreen.kt` | Modify | Accent picker gains Wallpaper | — |
| `app/src/test/.../ColorSchemeTest.kt` | Create | Unit tests for scheme selection | — |
| `app/src/test/.../TypographyTest.kt` | Create | Unit tests for the type scale | — |
| `app/src/test/.../ShapeMappingTest.kt` | Create | Unit tests for `CookieShapeType` mapping | — |
| `app/src/androidTest/.../ExpressiveComponentTest.kt` | Create | Instrumented tests for the rebuilt components | — |
| `app/src/androidTest/.../AppUiRegressionTest.kt` | Modify | Keep the 9 existing flows green | — |

---

### Task 1: Toolchain, dependency, and API inventory spike

The only task that may change the build configuration. Nothing else starts until it is green.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:106-119` (the Compose dependency block)
- Create: `Docs/spine/notes/m3-expressive-api-inventory.md`

**Interfaces:**
- Consumes: nothing.
- Produces: `libs.androidx.material3` resolving to 1.5.0-beta02; `libs.materialkolor`; the inventory
  document that every later task is checked against.

- [ ] **Step 1: Point the catalogue at the new versions**

In `gradle/libs.versions.toml`, under `[versions]`, change `composeBom` and add two entries:

```toml
composeBom = "2026.09.00"
material3 = "1.5.0-beta02"
materialKolor = "2.0.0"
```

Under `[libraries]`, replace the `androidx-material3` line and add one:

```toml
androidx-material3 = { group = "androidx.compose.material3", name = "material3", version.ref = "material3" }
materialkolor = { group = "com.materialkolor", name = "material-kolor", version.ref = "materialKolor" }
```

`androidx-material3` previously had no `version.ref`, taking its version from the BOM. It now pins
explicitly, which is what overrides the BOM's 1.4.0.

- [ ] **Step 2: Add the materialkolor dependency**

In `app/build.gradle.kts`, in the `dependencies` block immediately after
`implementation(libs.androidx.material3)`:

```kotlin
    implementation(libs.materialkolor)
```

- [ ] **Step 3: Resolve the dependency and confirm the version that wins**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep material3`

Expected: a line showing `androidx.compose.material3:material3:1.5.0-beta02` with no `->` arrow
downgrading it. If the BOM wins instead, the explicit `version.ref` is missing from the catalogue
entry — fix Step 1 before continuing.

- [ ] **Step 4: Compile the project unchanged against the new dependency**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL. If it fails on the Compose compiler or Kotlin version, record the exact
error, raise the `kotlin` / `agp` / `composeBom` entries in `gradle/libs.versions.toml` to the
lowest versions that compile, and re-run. Record whatever you changed in the inventory document in
Step 6 — a toolchain bump is in scope for this task and only this task.

- [ ] **Step 5: Verify the existing tests still pass**

Run: `./gradlew testReleaseUnitTest`

Expected: 36 tests, all passing.

- [ ] **Step 6: Extract the real API surface**

Locate the resolved artifact and dump the public signatures of the classes this plan relies on:

```bash
AAR=$(find ~/.gradle/caches/modules-2/files-2.1/androidx.compose.material3 -name "material3-android-1.5.0-beta02.aar" | head -1)
mkdir -p /tmp/m3 && cd /tmp/m3 && unzip -o "$AAR" classes.jar >/dev/null && unzip -o classes.jar >/dev/null
for c in MaterialExpressiveTheme MaterialShapes MotionScheme ButtonGroupKt FloatingActionButtonMenuKt ShortNavigationBarKt WavyProgressIndicatorKt LoadingIndicatorKt ButtonDefaults ShapeDefaults; do
  echo "=== $c ==="; javap -public "androidx/compose/material3/$c.class" 2>/dev/null
done
```

Write the output, trimmed to the members this plan uses, into
`Docs/spine/notes/m3-expressive-api-inventory.md`. For each of the following, record the exact name,
parameter list and required opt-in annotation, or record `ABSENT` if it does not exist:
`MaterialExpressiveTheme`, `MotionScheme.expressive()`, `MaterialTheme.motionScheme` and its spec
accessors, `MaterialShapes` members, the `RoundedPolygon` to `Shape` conversion, `ButtonGroup` and
its scope functions, `FloatingActionButtonMenu` / `FloatingActionButtonMenuItem` /
`ToggleFloatingActionButton`, `ShortNavigationBar` / `ShortNavigationBarItem`,
`LinearWavyProgressIndicator`, `LoadingIndicator`, `ButtonDefaults.shapes()` and the expressive
container-height constants, and the Emphasized typography role names.

- [ ] **Step 7: Reconcile this plan with reality**

Read Tasks 2 through 15 against the inventory. For every API this plan names that the inventory
records as `ABSENT` or differently shaped, correct the code block in this plan file. Do not start
Task 2 with a known contradiction outstanding.

- [ ] **Step 8: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts Docs/spine/notes/m3-expressive-api-inventory.md Docs/spine/plans/2026-10-26-material3-expressive-ui.md
git commit -m "chore: move to material3 1.5.0-beta02 and record its API surface"
```

---

### Task 2: Generated brand colour schemes and Material You

**Files:**
- Create: `app/src/main/java/com/jinatra/wishplay/ui/theme/WishPlayColorSchemes.kt`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/theme/WishPlayColors.kt`
- Test: `app/src/test/java/com/jinatra/wishplay/ui/theme/ColorSchemeTest.kt`

**Interfaces:**
- Consumes: `libs.materialkolor` from Task 1.
- Produces:
  - `enum class AccentColor(val seed: Color, val label: String)` with entries
    `GOLD`, `VIOLET`, `TEAL`, `CORAL`, `WALLPAPER`.
  - `fun AccentColor.isAvailable(): Boolean`
  - `@Composable fun wishPlayColorScheme(accent: AccentColor, darkTheme: Boolean): ColorScheme`
  - `WishPlayThemeColors` keeps every property name it has today.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/jinatra/wishplay/ui/theme/ColorSchemeTest.kt`:

```kotlin
package com.jinatra.wishplay.ui.theme

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorSchemeTest {

    @Test
    fun `every accent except wallpaper carries a seed`() {
        val seeded = AccentColor.entries.filter { it != AccentColor.WALLPAPER }
        assertEquals(4, seeded.size)
        seeded.forEach { accent ->
            assertTrue("${accent.name} must not be transparent", accent.seed.alpha == 1f)
        }
    }

    @Test
    fun `fromKey falls back to gold for unknown keys`() {
        assertEquals(AccentColor.GOLD, AccentColor.fromKey("not-an-accent"))
        assertEquals(AccentColor.TEAL, AccentColor.fromKey("teal"))
        assertEquals(AccentColor.WALLPAPER, AccentColor.fromKey("WALLPAPER"))
    }

    @Test
    fun `wallpaper is unavailable below api 31`() {
        assertFalse(AccentColor.WALLPAPER.isAvailable(apiLevel = Build.VERSION_CODES.R))
        assertTrue(AccentColor.WALLPAPER.isAvailable(apiLevel = Build.VERSION_CODES.S))
        assertTrue(AccentColor.GOLD.isAvailable(apiLevel = Build.VERSION_CODES.O))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.theme.ColorSchemeTest"`

Expected: compilation failure — `AccentColor` has no `seed`, no `WALLPAPER`, no `isAvailable`.

- [ ] **Step 3: Rewrite the accent enum**

In `ui/theme/Color.kt`, delete every `*Light` and `*Dark` role constant except the four below, and
replace the `AccentColor` enum. The roles Material does not define are the only hexes that survive:

```kotlin
// The two roles Material 3 does not define. Everything else comes from the generated scheme.
val SuccessGreenLight = Color(0xFF1E6E47)
val SuccessGreenContainerLight = Color(0xFFCDEFD9)
val SuccessGreenDark = Color(0xFF7ED3A4)
val SuccessGreenContainerDark = Color(0xFF0F3D27)

enum class AccentColor(val seed: Color, val label: String) {
    GOLD(Color(0xFFFFB938), "Gold"),
    VIOLET(Color(0xFFB9A8FF), "Violet"),
    TEAL(Color(0xFF6ED6C4), "Teal"),
    CORAL(Color(0xFFFF9E8A), "Coral"),
    WALLPAPER(Color(0xFFFFB938), "Wallpaper");

    fun isAvailable(apiLevel: Int = android.os.Build.VERSION.SDK_INT): Boolean =
        this != WALLPAPER || apiLevel >= android.os.Build.VERSION_CODES.S

    companion object {
        fun fromKey(key: String): AccentColor =
            entries.find { it.name.equals(key, ignoreCase = true) }?.takeIf { it.isAvailable() }
                ?: GOLD
    }
}
```

`WALLPAPER` carries the Gold seed so that a stored "WALLPAPER" preference on a device that later
drops below the dynamic-colour path still produces a valid scheme rather than a crash.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.theme.ColorSchemeTest"`

Expected: 3 tests pass.

- [ ] **Step 5: Add the scheme builder**

Create `ui/theme/WishPlayColorSchemes.kt`:

```kotlin
package com.jinatra.wishplay.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * The single source of colour for the app. Brand accents expand their seed into a complete
 * expressive tonal palette; WALLPAPER defers to the platform's dynamic colour on API 31+.
 */
@Composable
fun wishPlayColorScheme(accent: AccentColor, darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    val useWallpaper = accent == AccentColor.WALLPAPER &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return when {
        useWallpaper && darkTheme -> dynamicDarkColorScheme(context)
        useWallpaper -> dynamicLightColorScheme(context)
        else -> rememberDynamicColorScheme(
            seedColor = accent.seed,
            isDark = darkTheme,
            isAmoled = false,
            style = PaletteStyle.Expressive
        )
    }
}
```

- [ ] **Step 6: Turn the custom colour holder into a shim**

Replace the body of `ui/theme/WishPlayColors.kt` so that every role Material defines is read from
the scheme and only the two extras are stored. The property names callers already use are kept, so
no screen outside this task changes:

```kotlin
package com.jinatra.wishplay.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * Compatibility shim. Roles that Material 3 defines are forwarded to the active ColorScheme;
 * only `success`, `successContainer` and `pageBackground` are genuinely ours.
 */
object WishPlayThemeColors {
    val surfaceContainer: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer
    val surfaceContainerHigh: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainerHighest: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val outlineVariant: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
    val scrim: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.scrim
    val error: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.error
    val errorContainer: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.errorContainer
    val track: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    val onAccent: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary
    val pageBackground: Color
        @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceDim
    val success: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) SuccessGreenDark else SuccessGreenLight
    val successContainer: Color
        @Composable @ReadOnlyComposable get() =
            if (isAppInDarkTheme()) SuccessGreenContainerDark else SuccessGreenContainerLight
}
```

Add the small helper the two success roles need, in the same file:

```kotlin
@Composable
@ReadOnlyComposable
private fun isAppInDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f
```

with `import androidx.compose.ui.graphics.luminance`. Deriving darkness from the scheme rather than
from a `CompositionLocal` keeps the shim free of the theme's own plumbing, which matters because
`WishPlayTheme` is rewritten in Task 4.

Delete `WishPlayCustomColors` and `LocalWishPlayColors` from this file; Task 4 removes the
`CompositionLocalProvider` that supplied them.

- [ ] **Step 7: Verify nothing else referenced the deleted symbols**

Run: `grep -rn "WishPlayCustomColors\|LocalWishPlayColors" app/src/main/java`

Expected: one remaining hit, in `ui/theme/Theme.kt`, which Task 4 rewrites. Any other hit must be
migrated to `WishPlayThemeColors` now.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/theme/ app/src/test/java/com/jinatra/wishplay/ui/theme/ColorSchemeTest.kt
git commit -m "feat: generate brand tonal palettes and add wallpaper accent"
```

---

### Task 3: Roboto Flex and the full expressive type scale

**Files:**
- Create: `app/src/main/res/font/roboto_flex.ttf`
- Create: `app/src/main/res/font/roboto_flex_family.xml`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/theme/Type.kt`
- Test: `app/src/test/java/com/jinatra/wishplay/ui/theme/TypographyTest.kt`

**Interfaces:**
- Consumes: nothing from earlier tasks.
- Produces: `WishPlayTypography: Typography` with all 15 roles plus the Emphasized set;
  `DisplayFontFamily` and `BodyFontFamily` keep their names (`GameLogo.kt` imports
  `DisplayFontFamily` directly).

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/jinatra/wishplay/ui/theme/TypographyTest.kt`:

```kotlin
package com.jinatra.wishplay.ui.theme

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyTest {

    @Test
    fun `all fifteen base roles are defined with a real font`() {
        val roles = with(WishPlayTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall,
                headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall,
                bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall
            )
        }
        assertTrue(roles.all { it.fontSize.value > 0f })
        assertTrue(roles.all { it.lineHeight.value >= it.fontSize.value })
        assertTrue(roles.none { it.fontFamily == FontFamily.SansSerif })
    }

    @Test
    fun `display sizes descend`() {
        assertTrue(WishPlayTypography.displayLarge.fontSize > WishPlayTypography.displayMedium.fontSize)
        assertTrue(WishPlayTypography.displayMedium.fontSize > WishPlayTypography.displaySmall.fontSize)
    }

    @Test
    fun `body and display use different families`() {
        assertNotEquals(DisplayFontFamily, BodyFontFamily)
    }
}
```

The third test documents the intent that display and body are distinguishable. Roboto Flex supplies
both through different weight and width axes, so the two families are built from different
`Font` entries of the same file, not from the same `FontFamily` instance.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.theme.TypographyTest"`

Expected: FAIL — `fontFamily == FontFamily.SansSerif` for every role, and
`DisplayFontFamily == BodyFontFamily`.

- [ ] **Step 3: Add the font**

Download Roboto Flex (variable, SIL Open Font License 1.1) into `app/src/main/res/font/roboto_flex.ttf`:

```bash
mkdir -p app/src/main/res/font
curl -L -o app/src/main/res/font/roboto_flex.ttf \
  "https://github.com/googlefonts/roboto-flex/raw/main/fonts/variable/RobotoFlex%5BGRAD,XOPQ,XTRA,YOPQ,YTAS,YTDE,YTFI,YTLC,YTUC,opsz,slnt,wdth,wght%5D.ttf"
```

Verify it arrived and is a real font, not an HTML error page:

```bash
file app/src/main/res/font/roboto_flex.ttf
```

Expected: `TrueType Font data`. If the URL 404s, fetch the same file from
https://fonts.google.com/specimen/Roboto+Flex and place it at the same path. Record the licence in
`Docs/` alongside the existing asset notes.

- [ ] **Step 4: Rewrite the type scale**

Replace `ui/theme/Type.kt` with the Material 3 Expressive scale. Weight and width axes carry
emphasis, which is why the variable font is required:

```kotlin
package com.jinatra.wishplay.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jinatra.wishplay.R

private fun robotoFlex(weight: FontWeight, width: Float) = FontFamily(
    Font(
        resId = R.font.roboto_flex,
        weight = weight,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(weight.weight),
            FontVariation.width(width)
        )
    )
)

/** Display and headline roles: slightly wider, heavier. */
val DisplayFontFamily = robotoFlex(FontWeight.Bold, width = 110f)

/** Body, title and label roles: normal width. */
val BodyFontFamily = robotoFlex(FontWeight.Normal, width = 100f)

private val Emphasis = robotoFlex(FontWeight.ExtraBold, width = 125f)

private fun display(size: Int, line: Int, tracking: Float) = TextStyle(
    fontFamily = DisplayFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

private fun body(size: Int, line: Int, weight: FontWeight, tracking: Float) = TextStyle(
    fontFamily = BodyFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

val WishPlayTypography = Typography(
    displayLarge = display(57, 64, -0.25f),
    displayMedium = display(45, 52, 0f),
    displaySmall = display(36, 44, 0f),
    headlineLarge = display(32, 40, 0f),
    headlineMedium = display(28, 36, 0f),
    headlineSmall = display(24, 32, 0f),
    titleLarge = body(22, 28, FontWeight.Medium, 0f),
    titleMedium = body(16, 24, FontWeight.Medium, 0.15f),
    titleSmall = body(14, 20, FontWeight.Medium, 0.1f),
    bodyLarge = body(16, 24, FontWeight.Normal, 0.5f),
    bodyMedium = body(14, 20, FontWeight.Normal, 0.25f),
    bodySmall = body(12, 16, FontWeight.Normal, 0.4f),
    labelLarge = body(14, 20, FontWeight.Medium, 0.1f),
    labelMedium = body(12, 16, FontWeight.Medium, 0.5f),
    labelSmall = body(11, 16, FontWeight.Medium, 0.5f)
)
```

- [ ] **Step 5: Add the Emphasized roles**

Extend the `Typography(...)` call above with the Emphasized parameters, using the exact parameter
names recorded in the Task 1 inventory. Each mirrors its base role but takes the `Emphasis` family:

```kotlin
    displayLargeEmphasized = display(57, 64, -0.25f).copy(fontFamily = Emphasis),
    headlineLargeEmphasized = display(32, 40, 0f).copy(fontFamily = Emphasis),
    headlineMediumEmphasized = display(28, 36, 0f).copy(fontFamily = Emphasis),
    titleLargeEmphasized = body(22, 28, FontWeight.Bold, 0f).copy(fontFamily = Emphasis),
    labelLargeEmphasized = body(14, 20, FontWeight.Bold, 0.1f).copy(fontFamily = Emphasis),
```

If the inventory records these parameters as `ABSENT` on the `Typography` constructor, drop this
step and note it in the inventory — the base scale in Step 4 still stands on its own.

- [ ] **Step 6: Run the test to verify it passes**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.theme.TypographyTest"`

Expected: 3 tests pass.

- [ ] **Step 7: Check the size cost**

Run: `ls -la app/src/main/res/font/roboto_flex.ttf`

Record the byte size in the commit body. Anything above 2 MB should be subset before merging.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/res/font app/src/main/java/com/jinatra/wishplay/ui/theme/Type.kt app/src/test/java/com/jinatra/wishplay/ui/theme/TypographyTest.kt
git commit -m "feat: ship Roboto Flex and the full expressive type scale"
```

---

### Task 4: MaterialExpressiveTheme and MotionScheme

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/theme/Theme.kt`
- Delete: `app/src/main/java/com/jinatra/wishplay/ui/theme/Motion.kt`
- Modify: every file importing `WishPlayMotion` (found in Step 3)

**Interfaces:**
- Consumes: `wishPlayColorScheme(accent, darkTheme)` (Task 2), `WishPlayTypography` (Task 3).
- Produces: `WishPlayTheme(darkTheme, accentColor, content)` — signature unchanged, now expressive.

- [ ] **Step 1: Rewrite the theme**

Replace the body of `ui/theme/Theme.kt`:

```kotlin
package com.jinatra.wishplay.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WishPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: AccentColor = AccentColor.GOLD,
    content: @Composable () -> Unit
) {
    val colorScheme = wishPlayColorScheme(accentColor, darkTheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = WishPlayTypography,
        motionScheme = MotionScheme.expressive(),
        content = content
    )
}
```

The `CompositionLocalProvider(LocalWishPlayColors ...)` wrapper is gone; Task 2 made
`WishPlayThemeColors` read from the scheme instead.

- [ ] **Step 2: Delete the hand-rolled motion**

```bash
git rm app/src/main/java/com/jinatra/wishplay/ui/theme/Motion.kt
```

- [ ] **Step 3: Find every caller**

Run: `grep -rln "WishPlayMotion" app/src/main/java`

Expected: `ExpressiveButton.kt`, `ExpressiveChip.kt`, `GroupedCard.kt`, `MainScreen.kt`,
`WishlistScreen.kt`, `NextDropHeroCard.kt`, `SearchScreen.kt`.

- [ ] **Step 4: Migrate each caller**

In every file from Step 3, replace the import and the call. Bounding-box animations (size, offset,
corner radius, scale) take the spatial spec; colour and alpha take the effects spec:

```kotlin
// was: animationSpec = WishPlayMotion.expressiveSpring()
animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
```

```kotlin
// for colour or alpha animations
animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
```

Remove the now-unused `import com.jinatra.wishplay.ui.theme.WishPlayMotion` from each file. Use the
exact accessor names from the Task 1 inventory.

- [ ] **Step 5: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL, no unresolved reference to `WishPlayMotion`, `WishPlayCustomColors` or
`LocalWishPlayColors`.

- [ ] **Step 6: Run the unit tests**

Run: `./gradlew testReleaseUnitTest`

Expected: 36 existing tests plus the 6 added in Tasks 2 and 3, all passing.

- [ ] **Step 7: Commit**

```bash
git add -A app/src/main/java/com/jinatra/wishplay/ui
git commit -m "feat: adopt MaterialExpressiveTheme and the expressive motion scheme"
```

---

### Task 5: MaterialShapes behind CookieShapeType

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/components/CookieShape.kt`
- Test: `app/src/test/java/com/jinatra/wishplay/ui/components/ShapeMappingTest.kt`

**Interfaces:**
- Consumes: material3 1.5.0 `MaterialShapes` (Task 1 inventory).
- Produces: `enum class CookieShapeType` with the same entries and the same `fromKey`;
  `fun CookieShape(type: CookieShapeType): Shape` — note this becomes a function, not a class, and
  every existing call site already writes `CookieShape(shapeType)`, so no caller changes.

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/jinatra/wishplay/ui/components/ShapeMappingTest.kt`:

```kotlin
package com.jinatra.wishplay.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeMappingTest {

    @Test
    fun `fromKey maps every documented key`() {
        assertEquals(CookieShapeType.C4, CookieShapeType.fromKey("c4"))
        assertEquals(CookieShapeType.C6, CookieShapeType.fromKey("c6"))
        assertEquals(CookieShapeType.C9, CookieShapeType.fromKey("c9"))
        assertEquals(CookieShapeType.C12, CookieShapeType.fromKey("c12"))
        assertEquals(CookieShapeType.CIRCLE, CookieShapeType.fromKey("circle"))
        assertEquals(CookieShapeType.SQUIRCLE, CookieShapeType.fromKey("sq"))
        assertEquals(CookieShapeType.SQUIRCLE, CookieShapeType.fromKey("squircle"))
    }

    @Test
    fun `unknown keys fall back to the nine-sided cookie`() {
        assertEquals(CookieShapeType.C9, CookieShapeType.fromKey(""))
        assertEquals(CookieShapeType.C9, CookieShapeType.fromKey("hexagon"))
    }

    @Test
    fun `every type resolves to a material shape`() {
        CookieShapeType.entries.forEach { type ->
            assertEquals(
                "${type.name} must map to a polygon",
                true,
                type.polygon().vertices.isNotEmpty()
            )
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.components.ShapeMappingTest"`

Expected: compilation failure — `polygon()` does not exist, and `CookieShapeType` still carries
`lobes` and `amplitude` constructor parameters.

- [ ] **Step 3: Replace the polar-equation implementation**

Replace `ui/components/CookieShape.kt` entirely:

```kotlin
package com.jinatra.wishplay.ui.components

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.graphics.Shape
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

enum class CookieShapeType {
    C4, C6, C9, C12, CIRCLE, SQUIRCLE;

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    fun polygon(): RoundedPolygon = when (this) {
        C4 -> MaterialShapes.Cookie4Sided
        C6 -> MaterialShapes.Cookie6Sided
        C9 -> MaterialShapes.Cookie9Sided
        C12 -> MaterialShapes.Cookie12Sided
        CIRCLE -> MaterialShapes.Circle
        SQUIRCLE -> MaterialShapes.Square
    }

    companion object {
        fun fromKey(key: String): CookieShapeType = when (key.lowercase()) {
            "c4" -> C4
            "c6" -> C6
            "c9" -> C9
            "c12" -> C12
            "circle" -> CIRCLE
            "sq", "squircle" -> SQUIRCLE
            else -> C9
        }
    }
}

/**
 * Kept as a function with the old constructor's name and arity so that existing call sites
 * — `Modifier.clip(CookieShape(shapeType))` — compile unchanged.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun CookieShape(type: CookieShapeType): Shape = type.polygon().toShape()
```

Use whichever conversion the Task 1 inventory records for turning a `RoundedPolygon` into a `Shape`.
If no extension exists, implement it in this file as a `Shape` whose `createOutline` returns
`Outline.Generic(polygon.toPath().asComposePath())` scaled to the given `Size`, and say so in the
inventory.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew testReleaseUnitTest --tests "com.jinatra.wishplay.ui.components.ShapeMappingTest"`

Expected: 3 tests pass.

- [ ] **Step 5: Confirm no caller broke**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL. `GameLogo.kt`, `WishlistScreen.kt`, `NextDropHeroCard.kt`,
`GameRow.kt`, `SearchScreen.kt`, `ProfileScreen.kt`, `ProPaymentSheet.kt` and `WelcomeScreen.kt` all
reference `CookieShapeType` and must still compile untouched.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/components/CookieShape.kt app/src/test/java/com/jinatra/wishplay/ui/components/ShapeMappingTest.kt
git commit -m "refactor: back the cookie shapes with MaterialShapes"
```

---

### Task 6: ExpressiveButton as an adapter over the Material button family

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/components/ExpressiveButton.kt`
- Test: `app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt` (create)

**Interfaces:**
- Consumes: the theme from Task 4.
- Produces: `ExpressiveButton(onClick, modifier, style, isLarge, enabled, leadingIcon, text)` and
  `enum class ButtonStyle { FILLED, GOLD, TONAL, OUTLINED, TEXT, DANGER }` — both unchanged, so the
  22 call sites across eight screens stay as they are.

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt`:

```kotlin
package com.jinatra.wishplay

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.jinatra.wishplay.ui.components.ButtonStyle
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.theme.WishPlayTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExpressiveComponentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyButtonStyleRendersAndClicks() {
        ButtonStyle.entries.forEach { style ->
            var clicks = 0
            compose.setContent {
                WishPlayTheme {
                    ExpressiveButton(
                        onClick = { clicks++ },
                        style = style,
                        text = "Tap ${style.name}"
                    )
                }
            }
            compose.onNodeWithText("Tap ${style.name}").assertIsDisplayed().assertHasClickAction()
            compose.onNodeWithText("Tap ${style.name}").performClick()
            assertEquals("$style did not fire onClick", 1, clicks)
        }
    }

    @Test
    fun disabledButtonDoesNotFire() {
        var clicks = 0
        compose.setContent {
            WishPlayTheme {
                ExpressiveButton(onClick = { clicks++ }, enabled = false, text = "Disabled")
            }
        }
        compose.onNodeWithText("Disabled").performClick()
        assertEquals(0, clicks)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.ExpressiveComponentTest"`

Expected: FAIL — the file is new, and `ButtonStyle.entries` iteration over the current
implementation will not yet exercise the expressive shapes. Record the failure text.

- [ ] **Step 3: Rewrite the button**

Replace the `ExpressiveButton` body in `ui/components/ExpressiveButton.kt`, dropping the manual
`animateDpAsState` corner interpolation and `graphicsLayer` scale in favour of the built-in shape
morph:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.FILLED,
    isLarge: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    text: String
) {
    val scheme = MaterialTheme.colorScheme
    val height = if (isLarge) ButtonDefaults.LargeContainerHeight else ButtonDefaults.MediumContainerHeight
    val contentPadding =
        if (isLarge) ButtonDefaults.LargeContentPadding else ButtonDefaults.MediumContentPadding
    val textStyle =
        if (isLarge) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge

    val content: @Composable RowScope.() -> Unit = {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        }
        Text(text = text, style = textStyle)
    }

    val sizedModifier = modifier.heightIn(min = height)

    when (style) {
        ButtonStyle.FILLED -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = contentPadding,
            content = content
        )
        ButtonStyle.GOLD -> Button(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            ),
            contentPadding = contentPadding,
            content = content
        )
        ButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = contentPadding,
            content = content
        )
        ButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = contentPadding,
            content = content
        )
        ButtonStyle.TEXT -> TextButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            contentPadding = contentPadding,
            content = content
        )
        ButtonStyle.DANGER -> OutlinedButton(
            onClick = onClick,
            modifier = sizedModifier,
            enabled = enabled,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.error),
            border = BorderStroke(1.dp, scheme.error),
            contentPadding = contentPadding,
            content = content
        )
    }
}
```

`GOLD` now resolves to the scheme's `primary`, which after Task 2 *is* the chosen accent — the
separate accent plumbing it used before is gone.

If the inventory records `ButtonDefaults.shapes()` or the container-height constants as `ABSENT`,
pass `shape = ButtonDefaults.shape` instead and note the gap; everything else in this step stands.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.ExpressiveComponentTest"`

Expected: 2 tests pass on emulator `lockout_qa`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/components/ExpressiveButton.kt app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt
git commit -m "refactor: build ExpressiveButton on the Material expressive button family"
```

---

### Task 7: ExpressiveChip as an adapter over FilterChip

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/components/ExpressiveChip.kt`
- Modify: `app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt`

**Interfaces:**
- Consumes: the theme from Task 4.
- Produces: `ExpressiveChip(selected, onClick, modifier, leadingIcon, label, count)` — unchanged.
  Call sites in `WishlistScreen` (7), `CustomGameScreen` (5), `GameDetailsScreen` (4) and
  `SearchScreen` (2) stay as they are.

- [ ] **Step 1: Write the failing test**

Append to `ExpressiveComponentTest.kt`:

```kotlin
    @Test
    fun chipReportsSelectionAndShowsCount() {
        var selected = false
        compose.setContent {
            WishPlayTheme {
                ExpressiveChip(
                    selected = selected,
                    onClick = { selected = !selected },
                    label = "Upcoming",
                    count = 4
                )
            }
        }
        compose.onNodeWithText("Upcoming").assertIsDisplayed()
        compose.onNodeWithText("4").assertIsDisplayed()
        compose.onNodeWithText("Upcoming").performClick()
        compose.waitForIdle()
        assertEquals(true, selected)
    }
```

with `import com.jinatra.wishplay.ui.components.ExpressiveChip`.

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.ExpressiveComponentTest"`

Expected: the count node is not found as separate text — the current implementation renders it
inside the same `Row` but the assertion on "4" passes only once the label structure is right. Record
the exact failure.

- [ ] **Step 3: Rewrite the chip**

Replace the `ExpressiveChip` body:

```kotlin
@Composable
fun ExpressiveChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    label: String,
    count: Int? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelLarge)
                if (count != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalContentColor.current.copy(alpha = 0.7f)
                    )
                }
            }
        },
        leadingIcon = leadingIcon,
        shape = MaterialTheme.shapes.large
    )
}
```

`FilterChip` supplies the selected-state container, the border, the ripple and the check affordance,
so the hand-rolled `selectable` + `border` + three `animateColorAsState` calls all go away.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.ExpressiveComponentTest"`

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/components/ExpressiveChip.kt app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt
git commit -m "refactor: build ExpressiveChip on FilterChip"
```

---

### Task 8: Grouped containers on the expressive shape scale

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/components/GroupedCard.kt`

**Interfaces:**
- Consumes: the theme from Task 4.
- Produces: `GroupedItemContainer(position, modifier, onClick, backgroundColor, content)` and
  `enum class GroupPosition { ONLY, FIRST, MIDDLE, LAST }` — unchanged. `AlertsScreen` (9 uses),
  `GameRow` (2) and `WelcomeScreen` (2) stay as they are.

- [ ] **Step 1: Replace the four `animateDpAsState` corner animations**

In `GroupedItemContainer`, delete the four corner-radius animations and the manual
`RoundedCornerShape` assembly, and build the shape from the theme's shape scale instead:

```kotlin
    val outer = MaterialTheme.shapes.extraLarge
    val inner = MaterialTheme.shapes.extraSmall

    val shape = RoundedCornerShape(
        topStart = if (position == GroupPosition.FIRST || position == GroupPosition.ONLY) outer.topStart else inner.topStart,
        topEnd = if (position == GroupPosition.FIRST || position == GroupPosition.ONLY) outer.topEnd else inner.topEnd,
        bottomStart = if (position == GroupPosition.LAST || position == GroupPosition.ONLY) outer.bottomStart else inner.bottomStart,
        bottomEnd = if (position == GroupPosition.LAST || position == GroupPosition.ONLY) outer.bottomEnd else inner.bottomEnd
    )
```

- [ ] **Step 2: Animate the press through the motion scheme**

Keep the pressed-state background change, but drive it from the scheme rather than a bespoke spring:

```kotlin
    val background by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.surfaceContainerHigh else backgroundColor,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "grouped_bg"
    )
```

and use `background` in the `Modifier.background(...)` call.

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Verify the grouped list still renders correctly**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: the existing 9 flows pass — `AlertsScreen` is the heaviest user of this container and the
`"Nothing scheduled"` assertion covers it.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/components/GroupedCard.kt
git commit -m "refactor: drive grouped containers from the expressive shape scale"
```

---

### Task 9: LinearWavyProgressIndicator in the Next Drop hero

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/components/WavyProgressIndicator.kt`

**Interfaces:**
- Consumes: the theme from Task 4.
- Produces: `WavyProgressIndicator(progress: Float, modifier: Modifier, color: Color)` — unchanged,
  so `NextDropHeroCard.kt` stays as it is.

- [ ] **Step 1: Replace the hand-drawn canvas**

Replace the whole file body below the package declaration:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WavyProgressIndicator(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    LinearWavyProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth(),
        color = color,
        trackColor = color.copy(alpha = 0.28f)
    )
}
```

The 60 lines of sine sampling, the infinite transition, the manual clip and the terminal dot are all
provided by the component.

- [ ] **Step 2: Compile**

Run: `./gradlew :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Add a rendering test**

Append to `ExpressiveComponentTest.kt`:

```kotlin
    @Test
    fun wavyProgressClampsOutOfRangeValues() {
        compose.setContent {
            WishPlayTheme {
                Column {
                    WavyProgressIndicator(progress = -0.5f, modifier = Modifier.testTag("under"))
                    WavyProgressIndicator(progress = 1.8f, modifier = Modifier.testTag("over"))
                }
            }
        }
        compose.onNodeWithTag("under").assertIsDisplayed()
        compose.onNodeWithTag("over").assertIsDisplayed()
    }
```

with imports for `Column`, `Modifier`, `testTag`, `onNodeWithTag` and
`com.jinatra.wishplay.ui.components.WavyProgressIndicator`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.ExpressiveComponentTest"`

Expected: 4 tests pass. A crash here means the component rejects out-of-range progress and the
`coerceIn` must stay — which it does in the code above.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/components/WavyProgressIndicator.kt app/src/androidTest/java/com/jinatra/wishplay/ExpressiveComponentTest.kt
git commit -m "refactor: use LinearWavyProgressIndicator for the next-drop countdown"
```

---

### Task 10: ShortNavigationBar

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/main/MainScreen.kt:110-176`
- Modify: `app/src/androidTest/java/com/jinatra/wishplay/AppUiRegressionTest.kt`

**Interfaces:**
- Consumes: the theme from Task 4.
- Produces: `WishPlayBottomNav(selectedTab, onSelectTab, modifier)` — unchanged signature.

- [ ] **Step 1: Confirm what the existing tests depend on**

Run: `grep -n "Wishlist\|Search\|Alerts\|Profile" app/src/androidTest/java/com/jinatra/wishplay/AppUiRegressionTest.kt`

Expected: five `onNodeWithText` matchers on the four tab labels. The replacement must keep rendering
those exact labels as text, or these break.

- [ ] **Step 2: Replace the hand-rolled row**

Replace the whole `WishPlayBottomNav` body:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WishPlayBottomNav(
    selectedTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    ShortNavigationBar(modifier = modifier) {
        NavigationTab.entries.forEach { tab ->
            val icon = when (tab) {
                NavigationTab.WISHLIST -> Icons.Default.List
                NavigationTab.SEARCH -> Icons.Default.Search
                NavigationTab.ALERTS -> Icons.Default.Notifications
                NavigationTab.PROFILE -> Icons.Default.Person
            }
            ShortNavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(imageVector = icon, contentDescription = tab.title) },
                label = { Text(tab.title) }
            )
        }
    }
}
```

The animated indicator width, the manual `clickable` with `indication = null`, the hand-set
`surfaceContainer` background and the hard-coded 84.dp height are all handled by the component.

- [ ] **Step 3: Delete the three dead placeholder composables**

`SearchPlaceholderTab`, `AlertsPlaceholderTab` and `ProfilePlaceholderTab` at the end of
`MainScreen.kt` describe phases that already shipped and reference `ExpressiveButton`. Confirm they
are unused, then remove them:

```bash
grep -rn "SearchPlaceholderTab\|AlertsPlaceholderTab\|ProfilePlaceholderTab" app/src --include=*.kt
```

Expected: hits only inside `MainScreen.kt` itself. If anything else references them, leave them in
place and note it in the commit body.

- [ ] **Step 4: Run the full regression suite**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: all 9 flows pass. The inset assertion at line 50 —
`onNodeWithText("Profile").boundsInWindow.bottom <= windowHeight - navigationBottom` — is the one
most likely to fail, because the component's own height and insets differ from the removed 84.dp
row. If it fails, fix it by letting the component apply its own window insets rather than by
loosening the assertion.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/main/MainScreen.kt app/src/androidTest/java/com/jinatra/wishplay/AppUiRegressionTest.kt
git commit -m "feat: replace the hand-rolled bottom bar with ShortNavigationBar"
```

---

### Task 11: Wishlist — FAB menu and ButtonGroup filters

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt`
- Modify: `app/src/androidTest/java/com/jinatra/wishplay/AppUiRegressionTest.kt:71-73`

**Interfaces:**
- Consumes: `ExpressiveChip` (Task 7), the theme (Task 4).
- Produces: no new public API; the screen keeps its existing parameters.

- [ ] **Step 1: Read the current add-path and filter row**

Run: `grep -n "FloatingActionButton\|ExpressiveChip\|onNavigateToCustom\|onNavigateToSearch" app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt`

Record the exact line numbers of the FAB and of the filter `Row`. Both are replaced below.

- [ ] **Step 2: Replace the FAB with a FAB menu**

Replace the `FloatingActionButton` block with:

```kotlin
    var menuExpanded by rememberSaveable { mutableStateOf(false) }

    FloatingActionButtonMenu(
        expanded = menuExpanded,
        button = {
            ToggleFloatingActionButton(
                checked = menuExpanded,
                onCheckedChange = { menuExpanded = it }
            ) {
                Icon(
                    imageVector = if (menuExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = if (menuExpanded) "Close add menu" else "Add a game"
                )
            }
        }
    ) {
        FloatingActionButtonMenuItem(
            onClick = {
                menuExpanded = false
                onNavigateToSearch()
            },
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            text = { Text("Search for a game") }
        )
        FloatingActionButtonMenuItem(
            onClick = {
                menuExpanded = false
                onNavigateToCustom()
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add a custom game") }
        )
    }
```

The menu goes in the `Scaffold`'s `floatingActionButton` slot. Both labels name paths that already
exist in the screen's parameters — no new navigation.

- [ ] **Step 3: Replace the filter chips with a ButtonGroup**

The four filters (All / Upcoming / Out now / Date TBA) are mutually exclusive, which is what
`ButtonGroup` is for. Replace the `Row` of `ExpressiveChip`s:

```kotlin
    ButtonGroup(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        WishlistFilter.entries.forEach { filter ->
            toggleableItem(
                checked = state.activeFilter == filter,
                onCheckedChange = { viewModel.onFilterSelected(filter) },
                label = "${filter.label} ${state.countFor(filter)}"
            )
        }
    }
```

Use the real filter enum and the real count accessor that Step 1 found — do not invent names. If the
screen holds the filters as a `List<FilterOption>` rather than an enum, iterate that instead, keeping
the same label text so the regression assertions still match.

- [ ] **Step 4: Fix the regression test's add path**

`AppUiRegressionTest.kt:72` taps `"Add a custom game"` directly from the Wishlist. That label now
lives behind the FAB, so the test must open the menu first:

```kotlin
            compose.onNodeWithText("Wishlist", useUnmergedTree = false).performClick()
            compose.onNodeWithContentDescription("Add a game").performClick()
            compose.onNodeWithText("Add a custom game").performClick()
            compose.onNodeWithText("Different release dates per platform").assertExists()
```

- [ ] **Step 5: Run the regression suite**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: all 9 flows pass.

- [ ] **Step 6: Confirm the overlap is gone**

Launch the app on `lockout_qa`, open the Wishlist with at least two games, and screenshot:

```bash
adb -s emulator-5554 exec-out screencap -p > /tmp/wishlist-fab.png
```

Expected: the collapsed FAB no longer sits on top of a game row. Attach the observation to the
commit body.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/wishlist/WishlistScreen.kt app/src/androidTest/java/com/jinatra/wishplay/AppUiRegressionTest.kt
git commit -m "feat: give the wishlist a FAB menu and a button-group filter row"
```

---

### Task 12: Search — ButtonGroup platform filters and LoadingIndicator

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/search/SearchScreen.kt`

**Interfaces:**
- Consumes: `ExpressiveChip` (Task 7), the theme (Task 4).
- Produces: no new public API.

- [ ] **Step 1: Find the loading state and the platform filter**

Run: `grep -n "isLoading\|CircularProgress\|ExpressiveChip\|platform" app/src/main/java/com/jinatra/wishplay/ui/search/SearchScreen.kt`

Record the exact state property the screen reads while a query is in flight, and how the platform
filter is currently rendered.

- [ ] **Step 2: Replace the in-flight spinner**

Wherever the screen currently shows a progress spinner for an in-flight query:

```kotlin
    if (state.isLoading) {
        LoadingIndicator(modifier = Modifier.align(Alignment.Center))
    }
```

Use the state property Step 1 found. Keep any existing `contentDescription` or semantics the tests
rely on.

- [ ] **Step 3: Make the platform filters a ButtonGroup**

The platform filter is a single-choice row, same shape as Task 11's filters:

```kotlin
    ButtonGroup(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        platformOptions.forEach { option ->
            toggleableItem(
                checked = state.selectedPlatform == option,
                onCheckedChange = { viewModel.onPlatformSelected(option) },
                label = option.label
            )
        }
    }
```

Use the real option list and callback names from Step 1. `AppUiRegressionTest.kt:52` taps the text
`"Android"` in this row, so the option labels must keep rendering as plain text.

- [ ] **Step 4: Leave the cross-platform exclusions section intact**

The section added in commit `1945035` ("games the platform filter excluded") keeps its behaviour and
its copy. It picks up the new container styling from the theme — do not restructure it.

- [ ] **Step 5: Run the regression suite**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: all 9 flows pass, including the `"Android"` tap at line 52 and the `"Genshin"` assertion
at line 54.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/search/SearchScreen.kt
git commit -m "feat: give search a button-group platform filter and an expressive loading indicator"
```

---

### Task 13: Alerts

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/alerts/AlertsScreen.kt`

**Interfaces:**
- Consumes: `GroupedItemContainer` (Task 8), the theme (Task 4).
- Produces: no new public API.

- [ ] **Step 1: Remove the leftover direct colour reads**

Run: `grep -n "WishPlayThemeColors" app/src/main/java/com/jinatra/wishplay/ui/alerts/AlertsScreen.kt`

Expected: 10 hits. Each one that names a role Material defines
(`surfaceContainer`, `surfaceContainerHigh`, `outlineVariant`, `error`, `errorContainer`, `scrim`,
`track`, `accent`, `onAccent`) becomes the direct `MaterialTheme.colorScheme.<role>` read. Leave
`success`, `successContainer` and `pageBackground` on the shim — those are genuinely ours.

- [ ] **Step 2: Give the list its expressive grouping**

The screen already uses `GroupedItemContainer` nine times, so it inherits Task 8's shape scale. Check
each call passes the right `GroupPosition` for its index in the group:

```kotlin
    position = when {
        items.size == 1 -> GroupPosition.ONLY
        index == 0 -> GroupPosition.FIRST
        index == items.lastIndex -> GroupPosition.LAST
        else -> GroupPosition.MIDDLE
    }
```

- [ ] **Step 3: Compile and run the regression suite**

Run: `./gradlew :app:compileDebugKotlin && ./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: BUILD SUCCESSFUL, 9 flows pass, including the `"Nothing scheduled"` assertion.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/alerts/AlertsScreen.kt
git commit -m "refactor: move the alerts list onto expressive containers"
```

---

### Task 14: Profile — the accent picker gains Wallpaper

**Files:**
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/profile/ProfileScreen.kt`
- Modify: `app/src/main/java/com/jinatra/wishplay/ui/profile/ProfileViewModel.kt` (only if it
  enumerates accents)

**Interfaces:**
- Consumes: `AccentColor.isAvailable()` and `wishPlayColorScheme` (Task 2).
- Produces: no new public API.

- [ ] **Step 1: Find the picker**

Run: `grep -n "AccentColor" app/src/main/java/com/jinatra/wishplay/ui/profile/ProfileScreen.kt app/src/main/java/com/jinatra/wishplay/ui/profile/ProfileViewModel.kt`

Record where the list of accents is built and how one is persisted.

- [ ] **Step 2: Filter the list by availability**

Wherever the picker iterates accents, gate on the API level so Wallpaper is hidden below API 31:

```kotlin
    AccentColor.entries.filter { it.isAvailable() }.forEach { accent ->
        // existing swatch composable, unchanged
    }
```

- [ ] **Step 3: Preview each accent with real generated tones**

A single flat swatch no longer represents a scheme that has a full tonal palette behind it. Render
each option as three stacked tones taken from that accent's own generated scheme:

```kotlin
    val preview = wishPlayColorScheme(accent, darkTheme = isSystemInDarkTheme())
    Row(Modifier.clip(CookieShape(CookieShapeType.C9))) {
        Box(Modifier.size(16.dp).background(preview.primary))
        Box(Modifier.size(16.dp).background(preview.secondaryContainer))
        Box(Modifier.size(16.dp).background(preview.tertiary))
    }
```

For `WALLPAPER`, this resolves to the device's own dynamic scheme, so the swatch previews the real
wallpaper colours rather than the Gold fallback.

- [ ] **Step 4: Verify persistence round-trips the new value**

Run: `grep -rn "fromKey" app/src/main/java/com/jinatra/wishplay/data`

Expected: the stored accent is read back through `AccentColor.fromKey`, which Task 2 already made
fall back to `GOLD` for an unavailable value. Confirm no second parsing path exists.

- [ ] **Step 5: Test it on device**

Run: `./gradlew connectedDebugAndroidTest --tests "com.jinatra.wishplay.AppUiRegressionTest"`

Expected: 9 flows pass, including the Profile assertions at lines 66-69.

Then, manually on `lockout_qa`: select Wallpaper, confirm the whole app recolours, background the
app, change the device wallpaper, reopen, and confirm the scheme follows.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/jinatra/wishplay/ui/profile/
git commit -m "feat: offer wallpaper colours in the accent picker"
```

---

### Task 15: Full-surface QA and contract correction

**Files:**
- Modify: `.spine/config.md`
- Modify: `Docs/spine/specs/2026-10-26-material3-expressive-ui-design.md` (only if the build
  diverged from it)

**Interfaces:**
- Consumes: everything.
- Produces: a corrected config contract and a recorded QA pass.

- [ ] **Step 1: Run the whole test suite**

Run: `./gradlew testReleaseUnitTest && ./gradlew connectedDebugAndroidTest`

Expected: 36+ unit tests and 9+ instrumented tests, all passing.

- [ ] **Step 2: Walk every surface**

On `lockout_qa`, in both light and dark mode, for each of the five accents (Gold, Violet, Teal,
Coral, Wallpaper), open: Wishlist, Search, Alerts, Profile, Game details, Welcome, Custom game,
Calendar, and each of the seven sheets. Look for unreadable text, invisible borders, and containers
that vanish into the background. Record anything found; fix contrast by choosing a different scheme
role, never by hard-coding a hex.

- [ ] **Step 3: Check the out-of-scope screens inherited correctly**

Details, Welcome, Custom, Admin, Calendar and the sheets were never edited. Confirm each one now
renders with Roboto Flex, the generated palette and the expressive component shapes. Any screen
still showing the old look means a component adapter lost its seam — fix the adapter, not the screen.

- [ ] **Step 4: Correct the config contract**

In `.spine/config.md`, change the Stack line so it names what the build actually resolves:

```
- Stack:            Kotlin / Android (minSdk 26, targetSdk 35, compileSdk 35) / Jetpack Compose with androidx.compose.material3:material3:1.5.0-beta02 (Material 3 Expressive) + com.materialkolor:material-kolor / Hilt DI / ...
```

Keep every other field as it is.

- [ ] **Step 5: Re-assert the contract**

The contract changed, so its recorded hash is stale and Gate C will reject it. Re-run the assertion
and re-record:

```bash
sh "$CLAUDE_PLUGIN_ROOT/hooks/config-hash.sh" .spine/config.md > .spine/config-asserted
```

If `$CLAUDE_PLUGIN_ROOT` is empty, resolve it with the bounded fallback in the `using-spine` skill.

- [ ] **Step 6: Commit**

```bash
git add .spine/config.md .spine/config-asserted Docs/spine/
git commit -m "chore: record the expressive QA pass and correct the stack contract"
```

---

## Self-Review

**Spec coverage.** Spec §3.1 colour → Task 2. §3.2 shape → Task 5. §3.3 typography → Task 3. §3.4
motion → Task 4. §3.5 theme entry → Task 4. §4 component table → Tasks 5-12 (every row has a task;
`LoadingIndicator` is Task 12, `ButtonGroup` is Tasks 11 and 12, `ShortNavigationBar` is Task 10,
`FloatingActionButtonMenu` is Task 11). §4 API spike → Task 1. §5 screens → Tasks 11-14. §7 risk 3
(instrumented tests) → Tasks 10, 11, 12. §7 risk 4 (dynamic contrast) → Task 15 Step 2. §7 risk 5
(APK size) → Task 3 Step 7. §8 verification → Task 15. §9 follow-on → Task 15 Steps 4-5.

**One deliberate divergence from the spec.** The spec says `WishPlayCustomColors` is deleted. The
plan keeps the *name* `WishPlayThemeColors` as a shim that forwards to `MaterialTheme.colorScheme`,
because 60+ references to it live in files the spec puts out of scope. The holder class and its
`CompositionLocal` are deleted as specified; only the accessor object survives, and it stores
nothing. This is recorded here rather than silently done.

**Placeholder scan.** Tasks 11, 12 and 14 instruct the implementer to read real symbol names out of
the screen before editing (Step 1 of each) rather than hard-coding names this plan cannot verify —
`WishlistFilter`, `platformOptions`, `state.isLoading` and the accent-picker composable are named as
*examples to replace with what Step 1 found*, and each step says so explicitly. Every other code
block is complete and literal.

**Type consistency.** `AccentColor.seed` (Task 2) is read by `wishPlayColorScheme` (Task 2) and the
Profile preview (Task 14). `AccentColor.isAvailable()` is used in Tasks 2 and 14. `CookieShapeType`
keeps its six entries across Tasks 5 and 14. `WishPlayThemeColors` property names are fixed in
Task 2 and read in Task 13. `ExpressiveButton`'s six `ButtonStyle` entries are unchanged between the
current code, Task 6 and its test.

---

## Execution Handoff

Plan complete and saved to `Docs/spine/plans/2026-10-26-material3-expressive-ui.md`.
