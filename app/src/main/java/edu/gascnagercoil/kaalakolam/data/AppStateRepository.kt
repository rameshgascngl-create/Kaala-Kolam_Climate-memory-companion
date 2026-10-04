package edu.gascnagercoil.kaalakolam.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import edu.gascnagercoil.kaalakolam.domain.AppState
import edu.gascnagercoil.kaalakolam.domain.ThemeMode
import edu.gascnagercoil.kaalakolam.domain.repair
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val stateJson = Json {
    encodeDefaults = true
    explicitNulls = false
    ignoreUnknownKeys = true
}

internal object AppStateSerializer : Serializer<AppState> {
    override val defaultValue: AppState = AppState()

    override suspend fun readFrom(input: InputStream): AppState =
        runCatching {
            stateJson.decodeFromString<AppState>(input.readBytes().decodeToString()).repair()
        }.getOrElse { defaultValue }

    override suspend fun writeTo(t: AppState, output: OutputStream) {
        output.write(stateJson.encodeToString(t.repair()).encodeToByteArray())
    }
}

private val Context.appStateDataStore: DataStore<AppState> by dataStore(
    fileName = "kaala_kolam_state.json",
    serializer = AppStateSerializer,
)

class AppStateRepository(context: Context) {
    private val store = context.applicationContext.appStateDataStore

    val state: Flow<AppState> = store.data
        .catch { emit(AppState()) }
        .map { it.repair() }

    suspend fun setLanguage(language: String) {
        store.updateData { current -> current.copy(language = language).repair() }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        store.updateData { current -> current.copy(themeMode = themeMode).repair() }
    }

    suspend fun replace(validated: AppState) {
        store.updateData { validated.repair() }
    }

    suspend fun reset() {
        store.updateData { AppState() }
    }
}
