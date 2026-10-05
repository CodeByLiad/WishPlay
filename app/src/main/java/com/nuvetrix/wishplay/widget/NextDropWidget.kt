package com.nuvetrix.wishplay.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nuvetrix.wishplay.data.local.dao.WishlistDao
import com.nuvetrix.wishplay.data.local.prefs.UserPreferences
import com.nuvetrix.wishplay.ui.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NextDropWidgetEntryPoint {
    fun wishlistDao(): WishlistDao
    fun userPreferences(): UserPreferences
}

class NextDropWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                NextDropWidgetEntryPoint::class.java
            )
        } catch (_: Exception) {
            null
        }

        val userPrefs = entryPoint?.userPreferences()
        val wishlistDao = entryPoint?.wishlistDao()

        val isPro = userPrefs?.isPro?.first() ?: false
        val wishlist = wishlistDao?.getWishlistSync() ?: emptyList()

        val today = LocalDate.now().toString()

        // Find nearest upcoming game
        val upcomingGame = wishlist
            .map { it.game }
            .filter { gameEntity ->
                val platformsMap = try {
                    Json.decodeFromString<Map<String, String?>>(gameEntity.platformsJson)
                } catch (_: Exception) {
                    emptyMap()
                }
                val earliest = platformsMap.values.filterNotNull().sorted().firstOrNull()
                earliest != null && earliest >= today
            }
            .minByOrNull { gameEntity ->
                val platformsMap = try {
                    Json.decodeFromString<Map<String, String?>>(gameEntity.platformsJson)
                } catch (_: Exception) {
                    emptyMap()
                }
                platformsMap.values.filterNotNull().sorted().firstOrNull() ?: "9999"
            }

        val daysLeft: Int? = upcomingGame?.let { gameEntity ->
            val platformsMap = try {
                Json.decodeFromString<Map<String, String?>>(gameEntity.platformsJson)
            } catch (_: Exception) {
                emptyMap()
            }
            val earliest = platformsMap.values.filterNotNull().sorted().firstOrNull()
            earliest?.let { d ->
                try {
                    val target = LocalDate.parse(d)
                    ChronoUnit.DAYS.between(LocalDate.now(), target).toInt()
                } catch (_: Exception) {
                    null
                }
            }
        }

        provideContent {
            GlanceTheme {
                WidgetContent(
                    context = context,
                    isPro = isPro,
                    gameId = upcomingGame?.id,
                    gameTitle = upcomingGame?.title,
                    daysLeft = daysLeft
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun WidgetContent(
        context: Context,
        isPro: Boolean,
        gameId: String?,
        gameTitle: String?,
        daysLeft: Int?
    ) {
        val goldColor = Color(0xFFFFB938)
        val onGoldColor = Color(0xFF16132E)
        val mutedText = Color(0xFF4A4560)
        val cardBg = Color(0xFFFFB938) // Matches prototype var(--pc)

        if (!isPro) {
            // Pro Locked State matching prototype
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(Color(0xFF201C3E))
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(38.dp)
                            .cornerRadius(19.dp)
                            .background(goldColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PRO",
                            style = TextStyle(
                                color = ColorProvider(onGoldColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = "Next drop countdown",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        )
                        Text(
                            text = "WishPlay Pro feature · Tap to unlock",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFB0ACC0)),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }
        } else if (gameTitle == null || daysLeft == null) {
            // Empty State
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(ColorProvider(cardBg))
                    .padding(14.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Next drop: All caught up",
                        style = TextStyle(
                            color = ColorProvider(onGoldColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Tap to add games to your wishlist",
                        style = TextStyle(
                            color = ColorProvider(mutedText),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        } else {
            // Active Countdown State matching prototype
            val extraKey = ActionParameters.Key<String>("extra_game_id")
            val clickAction = if (gameId != null) {
                actionStartActivity<MainActivity>(actionParametersOf(extraKey to gameId))
            } else {
                actionStartActivity<MainActivity>()
            }

            val subtitleText = when (daysLeft) {
                0 -> "Drops today"
                1 -> "Out tomorrow"
                else -> "Out in $daysLeft days"
            }

            val initials = gameTitle.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()

            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(24.dp)
                    .background(ColorProvider(cardBg))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(clickAction),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Initials Badge
                    Box(
                        modifier = GlanceModifier
                            .size(42.dp)
                            .cornerRadius(14.dp)
                            .background(Color(0xFF16132E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials.ifBlank { "WP" },
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(12.dp))

                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = gameTitle,
                            style = TextStyle(
                                color = ColorProvider(onGoldColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = subtitleText,
                            style = TextStyle(
                                color = ColorProvider(mutedText),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                    }

                    Text(
                        text = "$daysLeft",
                        style = TextStyle(
                            color = ColorProvider(onGoldColor),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        )
                    )
                }
            }
        }
    }
}

class NextDropWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextDropWidget()
}

object NextDropWidgetUpdater {
    suspend fun updateWidget(context: Context) {
        try {
            NextDropWidget().updateAll(context)
        } catch (_: Exception) {
            // Non-fatal
        }
    }
}
