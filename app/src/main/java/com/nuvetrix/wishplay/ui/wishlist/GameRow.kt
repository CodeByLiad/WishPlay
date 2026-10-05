package com.nuvetrix.wishplay.ui.wishlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.domain.model.ReleaseStatus
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.GameLogo
import com.nuvetrix.wishplay.ui.components.GroupPosition
import com.nuvetrix.wishplay.ui.components.GroupedItemContainer
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GameRow(
    game: Game,
    position: GroupPosition,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColors = WishPlayThemeColors
    val daysUntil = game.daysUntilRelease()

    val (whenText, badgeBg, badgeTextColor, isDashed) = when {
        game.earliestDate == null -> {
            val label = if (game.expectedYear != null) "TBA ${game.expectedYear}" else "Date TBA"
            Tuple4(label, Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant, true)
        }
        daysUntil != null && daysUntil < 0 -> {
            Tuple4("Out now", customColors.successContainer, customColors.success, false)
        }
        daysUntil == 0 -> {
            Tuple4("Today", customColors.accent, customColors.onAccent, false)
        }
        daysUntil == 1 -> {
            Tuple4("Tomorrow", customColors.accent, customColors.onAccent, false)
        }
        daysUntil != null && daysUntil <= 14 -> {
            Tuple4("In $daysUntil days", customColors.accent, customColors.onAccent, false)
        }
        else -> {
            val formatted = try {
                LocalDate.parse(game.earliestDate).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            } catch (e: Exception) {
                game.earliestDate ?: ""
            }
            Tuple4(formatted, customColors.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface, false)
        }
    }

    val platformsText = game.platforms.keys.joinToString(", ")

    GroupedItemContainer(
        position = position,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameLogo(
                title = game.title,
                hue = Color(android.graphics.Color.parseColor(game.hueHex)),
                shapeType = CookieShapeType.fromKey(game.shapeKey),
                size = 52.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (game.isCustom) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    if (game.movedFromDate != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.tertiaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Moved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Text(
                        text = platformsText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // When badge
            val badgeShape = RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .clip(badgeShape)
                    .background(badgeBg)
                    .then(
                        if (isDashed) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), badgeShape)
                        else Modifier
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = whenText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor
                )
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
