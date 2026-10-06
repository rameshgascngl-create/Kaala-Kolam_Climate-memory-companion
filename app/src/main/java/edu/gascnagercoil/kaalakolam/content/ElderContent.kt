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
data class ElderQuestionContent(
    val id: String,
    val title: LocalizedContent,
    val ask: LocalizedContent,
)

@Serializable
data class ElderSliceCContent(
    val askAloud: LocalizedContent,
    val answerGroupLabel: LocalizedContent,
    val ratingLabels: List<LocalizedContent>,
    val scaleColours: List<String>,
    val scaleTextColours: List<String>,
    val scaleGlyphs: List<String>,
    val cannotSay: LocalizedContent,
    val memoryClarity: LocalizedContent,
    val confidenceLabels: List<LocalizedContent>,
    val storyLabel: LocalizedContent,
    val back: LocalizedContent,
    val next: LocalizedContent,
    val skip: LocalizedContent,
    val finish: LocalizedContent,
    val webStoryMaxUtf16: Int,
)

@Serializable
data class EldersAsset(
    val schemaVersion: Int,
    val elderQuestions: List<ElderQuestionContent>,
    val sliceB: ElderSliceBContent,
    val sliceC: ElderSliceCContent,
)
