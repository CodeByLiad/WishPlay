package com.nuvetrix.wishplay.ui.wishlist

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvetrix.wishplay.R
import com.nuvetrix.wishplay.domain.model.Game
import com.nuvetrix.wishplay.ui.components.CookieShapeType
import com.nuvetrix.wishplay.ui.components.GameLogo
import com.nuvetrix.wishplay.ui.components.WavyProgressIndicator
import com.nuvetrix.wishplay.ui.theme.DisplayFontFamily
import com.nuvetrix.wishplay.ui.theme.WishPlayMotion
import com.nuvetrix.wishplay.ui.theme.WishPlayThemeColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun NextDropHeroCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val customColors = WishPlayThemeColors

    val normalShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp, bottomEnd = 36.dp, bottomStart = 12.dp)
    val pressedShape = RoundedCornerShape(22.dp)

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 22.dp else 36.dp,
        animationSpec = WishPlayMotion.expressiveSpring(),
        label = "hero_radius"
    )

    val shape = if (isPressed) pressedShape else normalShape

    val daysLeft = game.daysUntilRelease() ?: 0
    val unitText = if (daysLeft == 1) "day" else "days"

    val earliestDateFormatted = try {
        game.earliestDate?.let {
            LocalDate.parse(it).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        } ?: ""
    } catch (e: Exception) {
        game.earliestDate ?: ""
    }

    val platformsText = game.platforms.keys.joinToString(", ")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(customColors.accent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(22.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Row: Number + Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.next_drop),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = customColors.onAccent
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$daysLeft",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 80.sp,
                            lineHeight = 72.sp,
                            letterSpacing = (-0.05).sp,
                            color = customColors.onAccent
                        )
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text(
                            text = unitText,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = customColors.onAccent,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                GameLogo(
                    title = game.title,
                    hue = Color(android.graphics.Color.parseColor(game.hueHex)),
                    shapeType = CookieShapeType.fromKey(game.shapeKey),
                    size = 64.dp,
                    imageUrl = game.logoUrl ?: game.coverUrl
                )
            }

            // Game title & platforms
            Column {
                Text(
                    text = game.title,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                    color = customColors.onAccent
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$earliestDateFormatted on $platformsText",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = customColors.onAccent
                )
            }

            // Wavy Progress Indicator
            WavyProgressIndicator(
                progress = game.progress,
                color = customColors.onAccent
            )
        }
    }
}
