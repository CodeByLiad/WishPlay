package com.nuvetrix.wishplay.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    @SerialName("results") val results: List<GameDto> = emptyList()
)

@Serializable
data class GameDto(
    @SerialName("id") val id: String,
    @SerialName("igdbId") val igdbId: Long? = null,
    @SerialName("title") val title: String,
    @SerialName("developer") val developer: String? = null,
    @SerialName("hueHex") val hueHex: String = "#1F7A6E",
    @SerialName("shapeKey") val shapeKey: String = "c9",
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("logoUrl") val logoUrl: String? = null,
    @SerialName("platforms") val platforms: Map<String, String?> = emptyMap(),
    @SerialName("storageSizes") val storageSizes: Map<String, String?> = emptyMap(),
    @SerialName("about") val about: String? = null,
    @SerialName("hasTrailer") val hasTrailer: Boolean = false,
    @SerialName("trailerYoutubeId") val trailerYoutubeId: String? = null,
    @SerialName("requirements") val requirements: SystemRequirementsDto? = null,
    @SerialName("price") val price: GamePriceDto? = null,
    @SerialName("movedFromDate") val movedFromDate: String? = null,
    @SerialName("expectedYear") val expectedYear: String? = null,
    @SerialName("progress") val progress: Float = 0.5f
)

@Serializable
data class SystemRequirementsDto(
    @SerialName("min") val min: RequirementsLevelDto? = null,
    @SerialName("rec") val rec: RequirementsLevelDto? = null
)

@Serializable
data class RequirementsLevelDto(
    @SerialName("OS") val os: String? = null,
    @SerialName("CPU") val cpu: String? = null,
    @SerialName("GPU") val gpu: String? = null,
    @SerialName("RAM") val ram: String? = null
)

@Serializable
data class GamePriceDto(
    @SerialName("store") val store: String,
    @SerialName("now") val now: String,
    @SerialName("was") val was: String? = null
)
