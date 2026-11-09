package com.jinatra.wishplay.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.ui.components.CookieShapeType
import com.jinatra.wishplay.ui.components.GameLogo
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

@Composable
fun NextDropHeroCard(game: Game, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = WishPlayThemeColors
    val days = game.daysUntilRelease()
    val countdown = when (days) {
        null -> "Date TBA"
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "In $days days"
    }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClickLabel = "View ${game.title}", onClick = onClick).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GameLogo(title = game.title,
            hue = runCatching { Color(android.graphics.Color.parseColor(game.hueHex)) }.getOrDefault(colors.accent),
            shapeType = CookieShapeType.SQUIRCLE, size = 64.dp,
            imageUrl = game.coverUrl ?: game.logoUrl, fallbackImageUrl = game.logoUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Next release · $countdown", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(game.title, style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
            Text(game.reminderPlatforms.filterValues { it == game.releaseDate }.keys.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
