package com.jinatra.wishplay.domain.model

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun catalogTitleKey(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace(Regex("[™®]"), "").replace(Regex("[^\\p{L}\\p{N}]"), "")

fun storeReleaseDate(value: String?): String? {
    validReleaseDate(value)?.let { return it }
    if (value == null) return null
    return listOf("d MMM, uuuu", "MMM d, uuuu", "d MMM uuuu", "MMMM d, uuuu").firstNotNullOfOrNull {
        runCatching { LocalDate.parse(value, DateTimeFormatter.ofPattern(it, Locale.ENGLISH)
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)).toString() }.getOrNull()
    }
}
