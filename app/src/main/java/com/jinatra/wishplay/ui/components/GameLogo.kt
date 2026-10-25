package com.jinatra.wishplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.jinatra.wishplay.ui.theme.DisplayFontFamily

@Composable
fun GameLogo(
    title: String,
    hue: Color,
    shapeType: CookieShapeType = CookieShapeType.C9,
    size: Dp = 52.dp,
    imageUrl: String? = null,
    fallbackImageUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val initials = title
        .replace(Regex("[^A-Za-z0-9 ]"), "")
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.take(1) }
        .uppercase()

    val textSize = (size.value * 0.34f).sp
    var useFallback by remember(imageUrl, fallbackImageUrl) { mutableStateOf(false) }
    val effectiveUrl = (if (useFallback) fallbackImageUrl else imageUrl)
        ?.trim()?.takeUnless { it.isBlank() || it == "null" }
        ?.let { if (it.startsWith("//")) "https:$it" else it }
    var imageState by remember(effectiveUrl) { mutableStateOf("Loading artwork") }

    Box(
        modifier = modifier
            .size(size)
            .clip(if (effectiveUrl != null) RoundedCornerShape(12.dp) else CookieShape(shapeType))
            .background(hue),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifBlank { title.take(1) },
            color = Color.White,
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.ExtraBold,
            fontSize = textSize
        )
        if (effectiveUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(effectiveUrl).crossfade(true).build(),
                contentDescription = "Artwork for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().semantics { stateDescription = imageState },
                onSuccess = { imageState = "Artwork loaded" },
                onError = {
                    imageState = "Artwork unavailable"
                    if (com.jinatra.wishplay.BuildConfig.DEBUG) android.util.Log.w("GameArtwork", "Artwork load failed", it.result.throwable)
                    if (!useFallback && fallbackImageUrl != imageUrl) useFallback = true
                }
            )
        }
    }
}
