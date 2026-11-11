package com.jinatra.wishplay.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.jinatra.wishplay.domain.model.Game
import com.jinatra.wishplay.ui.components.GroupPosition
import com.jinatra.wishplay.ui.theme.WishPlayThemeColors

/** Artwork leads the library; release information stays readable beneath it. */
@Composable
fun GameRow(game: Game, position: GroupPosition, onClick: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val days = game.daysUntilRelease()
    val status = when {
        game.releaseDate == null -> "Date unavailable"
        days == 0 -> "Out today"
        days != null && days < 0 -> "Out now"
        days == 1 -> "Tomorrow"
        days != null && days <= 14 -> "In $days days"
        else -> game.releaseDate.orEmpty()
    }
    var artwork by remember(game.coverUrl) { mutableStateOf("Loading artwork") }
    Card(onClick = onClick, modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Box(Modifier.fillMaxWidth().aspectRatio(if (wide) 2.1f else 1.25f).background(colors.secondaryContainer)) {
            if (game.coverUrl != null || game.logoUrl != null) {
                AsyncImage(model = game.coverUrl ?: game.logoUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize().semantics { stateDescription = artwork },
                    contentScale = ContentScale.Crop, onSuccess = { artwork = "Artwork loaded" },
                    onError = { artwork = "Artwork unavailable" })
            } else Text(game.title.take(2), Modifier.padding(24.dp), style = MaterialTheme.typography.displayMedium)
            Surface(Modifier.padding(12.dp), shape = RoundedCornerShape(12.dp), color = colors.surface.copy(alpha = .96f)) {
                Text(status, Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (days != null && days < 0) WishPlayThemeColors.success else colors.primary)
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(game.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                maxLines = 2, minLines = if (wide) 1 else 2, overflow = TextOverflow.Ellipsis)
            Text(game.platforms.keys.joinToString(" · "), style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
