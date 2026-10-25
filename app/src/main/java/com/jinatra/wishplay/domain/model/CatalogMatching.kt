package com.jinatra.wishplay.domain.model

import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Trademark marks are stripped before NFKC, not after: NFKC expands "™" to the letters "TM" and
// "℠" to "SM", which then survive the punctuation filter and make "Wisps™" key differently from
// "Wisps". "®" has no compatibility decomposition but is stripped here for consistency.
fun catalogTitleKey(title: String): String = Normalizer.normalize(title.replace(Regex("[™®℠]"), ""), Normalizer.Form.NFKC)
    .lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]"), "")

fun storeReleaseDate(value: String?): String? {
    validReleaseDate(value)?.let { return it }
    if (value == null) return null
    return listOf("d MMM, uuuu", "MMM d, uuuu", "d MMM uuuu", "MMMM d, uuuu").firstNotNullOfOrNull {
        runCatching { LocalDate.parse(value, DateTimeFormatter.ofPattern(it, Locale.ENGLISH)
            .withResolverStyle(java.time.format.ResolverStyle.STRICT)).toString() }.getOrNull()
    }
}
