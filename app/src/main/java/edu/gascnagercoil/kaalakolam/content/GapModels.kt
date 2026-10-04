package edu.gascnagercoil.kaalakolam.content

import kotlinx.serialization.Serializable

@Serializable
data class GapManifest(
    val schemaVersion: Int,
    val status: String,
    val uiLabel: String,
    val count: Int,
    val countsByKind: Map<String, Int>,
    val gaps: List<GapEntry>,
)

@Serializable
data class GapEntry(
    val kind: String,
    val logicalPath: String,
    val asset: String,
    val pointer: String,
    val en: String,
)
