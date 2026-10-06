package edu.gascnagercoil.kaalakolam.content

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedContent(
    val en: String,
    val ta: String,
) {
    fun text(language: String): String = if (language == "ta") ta else en
}

@Serializable
data class ElderSliceBContent(
    val back: LocalizedContent,
    val consent: LocalizedContent,
    val decadeLabel: LocalizedContent,
    val decades: List<String>,
    val defaultNickname: LocalizedContent,
    val delete: LocalizedContent,
    val deleteConfirm: LocalizedContent,
    val empty: LocalizedContent,
    val intro: LocalizedContent,
    val limitMessage: LocalizedContent,
    val listTitle: LocalizedContent,
    val newTitle: LocalizedContent,
    val nicknameLabel: LocalizedContent,
    val nicknamePlaceholder: LocalizedContent,
    val placeLabel: LocalizedContent,
    val places: List<LocalizedContent>,
    val bornPrefix: LocalizedContent,
    val start: LocalizedContent,
)

@Serializable
data class EldersAsset(
    val schemaVersion: Int,
    val sliceB: ElderSliceBContent,
)
