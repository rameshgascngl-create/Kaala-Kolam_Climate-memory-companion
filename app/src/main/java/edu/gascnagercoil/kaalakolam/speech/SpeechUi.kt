package edu.gascnagercoil.kaalakolam.speech

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import edu.gascnagercoil.kaalakolam.R

@Composable
fun rememberSpeechController(): SpeechController {
    val context = LocalContext.current
    var missingTamilVoice by remember { mutableStateOf(false) }
    val controller = remember(context) {
        SpeechController(context) { missingTamilVoice = true }
    }
    DisposableEffect(controller) {
        onDispose { controller.shutdown() }
    }
    if (missingTamilVoice) {
        AlertDialog(
            onDismissRequest = { missingTamilVoice = false },
            title = { Text(stringResource(R.string.tts_missing_title)) },
            text = { Text(stringResource(R.string.tts_missing_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        missingTamilVoice = false
                        context.startActivity(SpeechController.installVoiceDataIntent())
                    },
                ) {
                    Text(stringResource(R.string.tts_install))
                }
            },
            dismissButton = {
                TextButton(onClick = { missingTamilVoice = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    return controller
}
