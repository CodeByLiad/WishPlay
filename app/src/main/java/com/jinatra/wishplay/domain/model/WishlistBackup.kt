package com.jinatra.wishplay.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class WishlistBackup(
    val format: String = "wishplay-wishlist",
    val version: Int = 1,
    val exportedAt: String,
    val games: List<Game>
)

object WishlistBackupCodec {
    const val MAX_BYTES = 5 * 1024 * 1024
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    fun encode(games: List<Game>, exportedAt: String = java.time.Instant.now().toString()): String =
        json.encodeToString(WishlistBackup(exportedAt = exportedAt, games = games))

    fun decode(contents: String): WishlistBackup {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup files must be smaller than 5 MB." }
        val backup = try { json.decodeFromString<WishlistBackup>(contents) }
            catch (_: Exception) { throw IllegalArgumentException("This isn't a valid WishPlay backup file.") }
        require(backup.format == "wishplay-wishlist" && backup.version == 1) { "This backup format isn't supported. Update WishPlay and try again." }
        require(backup.games.size <= 2000) { "A backup can contain up to 2,000 games." }
        require(backup.games.map { it.id }.distinct().size == backup.games.size) { "The backup contains duplicate game IDs." }
        backup.games.forEach { game ->
            require(game.id.matches(Regex("[A-Za-z0-9_-]{1,128}")) && game.title.isNotBlank() && game.title.length <= 500) { "A game in this backup has an invalid name or ID." }
            require(game.platforms.size in 1..100 && game.platforms.keys.all { it.isNotBlank() && it.length <= 100 }) { "A game has invalid platforms." }
            require(game.platforms.values.all { it == null || validReleaseDate(it) != null }) { "A game has an invalid release date." }
            require(game.trackedPlatforms == null || (game.trackedPlatforms.isNotEmpty() && game.trackedPlatforms.size <= 100 && game.trackedPlatforms.all { it.isNotBlank() && it.length <= 100 })) { "A game has invalid reminder platforms." }
            require(game.coverUrl == null || game.coverUrl.startsWith("https://")) { "Backup artwork must use HTTPS." }
            require(game.logoUrl == null || game.logoUrl.startsWith("https://")) { "Backup artwork must use HTTPS." }
            require(game.releaseEvidence.size <= 100 && game.releaseEvidence.values.all { it.size <= 10 } &&
                game.releaseEvidence.values.flatten().all { it.source.length in 1..100 && it.url.length <= 2000 && it.url.startsWith("https://") && (it.date == null || validReleaseDate(it.date) != null) }) { "A game has invalid release sources." }
            require(game.customNotes.orEmpty().length <= 20_000 && game.about.orEmpty().length <= 100_000) { "A game description is too long." }
        }
        return backup
    }
}
