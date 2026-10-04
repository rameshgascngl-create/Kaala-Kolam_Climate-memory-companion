package edu.gascnagercoil.kaalakolam.content

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GapManifestContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun gapsManifestHasNoCurrentEnglishOnlyGaps() {
        val file = File("src/main/assets/content/gaps.json")
        val manifest = json.decodeFromString<GapManifest>(file.readText(Charsets.UTF_8))

        assertEquals(0, manifest.count)
        assertEquals("English only", manifest.uiLabel)
        assertEquals(
            mapOf(
                "deepDive" to 0,
                "councilDescription" to 0,
                "eldersCrossCheck" to 0,
                "predictExplanation" to 0,
            ),
            manifest.countsByKind,
        )
        assertEquals(0, manifest.gaps.size)
        manifest.gaps.forEach { gap ->
            require(gap.en.isNotBlank())
        }
    }
}
