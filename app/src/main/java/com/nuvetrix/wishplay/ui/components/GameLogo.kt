package com.nuvetrix.wishplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nuvetrix.wishplay.ui.theme.DisplayFontFamily

@Composable
fun GameLogo(
    title: String,
    hue: Color,
    shapeType: CookieShapeType = CookieShapeType.C9,
    size: Dp = 52.dp,
    imageUrl: String? = null,
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
    var isImageError by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CookieShape(shapeType))
            .background(hue),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank() && !isImageError) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Logo for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { isImageError = true }
            )
        } else {
            Text(
                text = initials,
                color = Color.White,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = textSize
            )
        }
    }
}
