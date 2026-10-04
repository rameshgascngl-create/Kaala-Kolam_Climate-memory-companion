package edu.gascnagercoil.kaalakolam.content

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class GapManifestContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun gapsManifestHasExpectedEnglishOnlyContract() {
        val file = File("src/main/assets/content/gaps.json")
        val manifest = json.decodeFromString<GapManifest>(file.readText(Charsets.UTF_8))

        assertEquals(77, manifest.count)
        assertEquals("English only", manifest.uiLabel)
        assertEquals(
            mapOf(
                "deepDive" to 24,
                "councilDescription" to 7,
                "eldersCrossCheck" to 30,
                "predictExplanation" to 16,
            ),
            manifest.countsByKind,
        )
        assertEquals(77, manifest.gaps.size)
        manifest.gaps.forEach { gap ->
            require(gap.en.isNotBlank())
        }
    }
}
