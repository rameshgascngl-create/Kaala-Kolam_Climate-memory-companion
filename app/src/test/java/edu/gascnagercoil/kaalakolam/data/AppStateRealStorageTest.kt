package edu.gascnagercoil.kaalakolam.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ElderMode
import edu.gascnagercoil.kaalakolam.domain.ElderSession
import edu.gascnagercoil.kaalakolam.domain.Interview
import edu.gascnagercoil.kaalakolam.domain.InterviewAnswer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStateRealStorageTest {
    private data class StoreHandle(
        val store: DataStore<AppState>,
        val scope: CoroutineScope,
    )

    private fun newStore(file: File): StoreHandle {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val store = DataStoreFactory.create(
            serializer = AppStateSerializer,
            scope = scope,
            produceFile = { file },
        )
        return StoreHandle(store, scope)
    }

    private suspend fun StoreHandle.close() {
        scope.coroutineContext[Job]?.cancelAndJoin()
    }

    private fun freshFile(name: String): File {
        val root = kotlin.io.path.createTempDirectory("kaala-kolam-storage-").toFile()
        return File(root, name)
    }

    @Test
    fun saveThenProcessDeathThenReloadUsesTheSameOnDiskState() = runBlocking {
        val file = freshFile("state.json")
        val expected = AppState(
            language = "ta",
            currentTab = "elders",
            interviews = listOf(
                Interview(
                    id = "iv000001",
                    nickname = "பாட்டி",
                    birthDecade = 2,
                    place = 0,
                    createdAt = 1234L,
                ),
            ),
            elderSession = ElderSession(
                mode = ElderMode.SETUP,
                activeId = "iv000001",
                questionIndex = 0,
            ),
            notes = "persist me",
        )

        val firstProcess = newStore(file)
        firstProcess.store.updateData { expected }
        assertEquals(expected, firstProcess.store.data.first())
        firstProcess.close()

        val secondProcess = newStore(file)
        val reloaded = secondProcess.store.data.first()
        assertEquals(expected, reloaded)
        secondProcess.close()
    }

    @Test
    fun answerWriteCompletesBeforeNextQuestionStateIsEmitted() = runBlocking {
        val file = freshFile("answer-before-next.json")
        val interviewId = "iv000001"
        val initial = AppState(
            currentTab = "elders",
            interviews = listOf(
                Interview(
                    id = interviewId,
                    nickname = "Elder",
                    birthDecade = 1,
                    place = 0,
                    createdAt = 1234L,
                ),
            ),
            elderSession = ElderSession(
                mode = ElderMode.ASK,
                activeId = interviewId,
                questionIndex = 0,
            ),
        )
        val handle = newStore(file)
        handle.store.updateData { initial }

        val nextQuestion = async {
            handle.store.data.first { state ->
                state.elderSession.questionIndex == 1
            }
        }
        val answer = InterviewAnswer(
            answered = true,
            rating = 0,
            confidence = 2,
            story = "Persist before advancing.",
        )
        val nextSession = initial.elderSession.copy(questionIndex = 1)
        handle.store.updateData { current ->
            advanceElderState(
                current = current,
                interviewId = interviewId,
                questionId = "heat",
                answer = answer,
                session = nextSession,
            )
        }

        val emitted = nextQuestion.await()
        assertEquals(1, emitted.elderSession.questionIndex)
        assertEquals(answer, emitted.interviews.single().answers["heat"])
        handle.close()

        val reopened = newStore(file)
        val persisted = reopened.store.data.first()
        assertEquals("elders", persisted.currentTab)
        assertEquals(1, persisted.elderSession.questionIndex)
        assertEquals(answer, persisted.interviews.single().answers["heat"])
        reopened.close()
    }

    @Test
    fun corruptPrimaryFallsBackWithoutTouchingValidBackupCopy() = runBlocking {
        val file = freshFile("state.json")
        val backup = File(file.parentFile, "state.json.bak")
        val validBackup = """{"schemaVersion":2,"language":"ta","notes":"valid backup"}"""
        backup.writeText(validBackup, Charsets.UTF_8)
        file.writeText("{ definitely-not-json", Charsets.UTF_8)

        val handle = newStore(file)
        val state = handle.store.data.first()
        assertEquals(AppState(), state)
        handle.close()

        assertTrue(backup.isFile)
        assertEquals(validBackup, backup.readText(Charsets.UTF_8))
    }

    @Test
    fun sixWebAuditCorruptShapesNeverCrashStorageReload() = runBlocking {
        val cases = linkedMapOf(
            "interviews-null" to """{"schemaVersion":2,"interviews":null}""",
            "interviews-string" to """{"schemaVersion":2,"interviews":"not-a-list"}""",
            "record-missing-fields" to """{"schemaVersion":2,"interviews":[{"id":"iv000001"}]}""",
            "answer-map-null" to """{"schemaVersion":1,"interviews":[{"id":"iv000001","nickname":"Elder","birthDecade":1,"place":0,"a":null}]}""",
            "qi-out-of-range" to """{"schemaVersion":2,"elderSession":{"mode":"LIST","questionIndex":9999}}""",
            "seen-null" to """{"schemaVersion":1,"seen":null}""",
        )

        cases.forEach { (name, raw) ->
            val file = freshFile("$name.json")
            file.writeText(raw, Charsets.UTF_8)
            val handle = newStore(file)
            val state = handle.store.data.first()
            assertEquals(AppState.CURRENT_SCHEMA, state.schemaVersion)
            assertTrue(state.elderSession.questionIndex in 0 until 10)
            handle.close()
        }
    }

    @Test
    fun v1FixtureMigratesToV2WithoutLosingKnownFields() = runBlocking {
        val fixture = requireNotNull(
            javaClass.classLoader?.getResource("fixtures/app-state-v1.json"),
        ).readText()
        val file = freshFile("state.json")
        file.writeText(fixture, Charsets.UTF_8)

        val handle = newStore(file)
        val migrated = handle.store.data.first()
        handle.close()

        assertEquals(2, migrated.schemaVersion)
        assertEquals("ta", migrated.language)
        assertEquals("பாட்டி", migrated.elderAlias)
        assertEquals("v1 note", migrated.notes)
        assertEquals("v1 reflection", migrated.reflection)
        assertEquals(setOf("water"), migrated.learnedTopicIds)
        assertEquals(true, migrated.memoryFlags["intro"])
    }
}
