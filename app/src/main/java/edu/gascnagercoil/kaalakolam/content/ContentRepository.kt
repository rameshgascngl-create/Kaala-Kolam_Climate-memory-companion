package edu.gascnagercoil.kaalakolam.content

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ContentRepository(context: Context) {
    private val assets = context.applicationContext.assets
    private val json = Json { ignoreUnknownKeys = true }

    fun loadGaps(): GapManifest {
        val raw = assets.open("content/gaps.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(raw)
    }

    fun loadElders(): EldersAsset {
        val raw = assets.open("content/elders.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return json.decodeFromString(raw)
    }

    fun loadElderSliceB(): ElderSliceBContent = loadElders().sliceB
}
