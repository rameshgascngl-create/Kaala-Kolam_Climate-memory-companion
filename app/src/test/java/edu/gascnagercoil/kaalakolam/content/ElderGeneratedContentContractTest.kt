package edu.gascnagercoil.kaalakolam.content

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElderGeneratedContentContractTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun asset(): EldersAsset {
        val candidates = listOf(
            File("src/main/assets/content/elders.json"),
            File("app/src/main/assets/content/elders.json"),
        )
        val file = requireNotNull(candidates.firstOrNull(File::isFile)) {
            "elders.json not found; user.dir=" + System.getProperty("user.dir")
        }
        return json.decodeFromString(file.readText(Charsets.UTF_8))
    }

    @Test
    fun setupNicknamePrivacyNoteExistsInBothLanguages() {
        val copy = asset().sliceB.nicknameLabel
        assertEquals("Nickname (not a real name)", copy.en)
        assertEquals("செல்லப்பெயர் (உண்மைப் பெயர் வேண்டாம்)", copy.ta)
        assertTrue(copy.en.isNotBlank())
        assertTrue(copy.ta.any { it.code in 0x0B80..0x0BFF })
    }

    @Test
    fun setupListsMatchAuditedPrototype() {
        val slice = asset().sliceB
        assertEquals(
            listOf("1940s", "1950s", "1960s", "1970s", "1980s", "1990s"),
            slice.decades,
        )
        assertEquals(
            listOf("Coast", "Delta or farmland", "Hills", "Dry plains", "Town or city"),
            slice.places.map { it.en },
        )
        assertEquals(
            listOf("கடலோரம்", "டெல்டா / விளைநிலம்", "மலைப்பகுதி", "வறண்ட சமவெளி", "நகரம் / பேரூர்"),
            slice.places.map { it.ta },
        )
    }

    @Test
    fun questionFlowCopyAndScaleComeFromGeneratedPrototypeContent() {
        val asset = asset()
        val slice = asset.sliceC
        assertEquals(10, asset.elderQuestions.size)
        assertEquals(listOf("−−", "−", "=", "+", "++"), slice.scaleGlyphs)
        assertEquals(listOf("#1f6f8b", "#6bb3c4", "#d8d2c4", "#eba46d", "#c8452c"), slice.scaleColours)
        assertEquals("Cannot say", slice.cannotSay.en)
        assertEquals("சொல்ல இயலாது", slice.cannotSay.ta)
        assertEquals(400, slice.webStoryMaxUtf16)
    }
}
