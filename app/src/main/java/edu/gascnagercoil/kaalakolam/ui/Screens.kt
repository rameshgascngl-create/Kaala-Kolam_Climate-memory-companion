package edu.gascnagercoil.kaalakolam.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.gascnagercoil.kaalakolam.R
import edu.gascnagercoil.kaalakolam.content.GapManifest
import edu.gascnagercoil.kaalakolam.domain.AppState

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.home_body),
            style = MaterialTheme.typography.bodyLarge,
        )
        Surface(
            tonalElevation = 3.dp,
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = "காலக்கோலம் · Kaala Kolam",
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text("This area is scaffolded in M1 and receives its full parity workflow in its scheduled milestone.")
    }
}

@Composable
fun GapListScreen(
    title: String,
    manifest: GapManifest?,
    kind: String,
) {
    if (manifest == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val gaps = manifest.gaps.filter { it.kind == kind }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = stringResource(R.string.content_review_note),
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }
        items(gaps, key = { it.logicalPath }) { gap ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = manifest.uiLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(gap.en, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun AboutScreen(
    appState: AppState,
    onBackup: (AppState) -> String,
    onValidateBackup: (String) -> AppState?,
    onRestore: (AppState) -> Unit,
    onReset: () -> Unit,
) {
    val context = LocalContext.current
    var backupCode by remember { mutableStateOf<String?>(null) }
    var restoreEntryOpen by remember { mutableStateOf(false) }
    var restoreText by remember { mutableStateOf("") }
    var invalidRestore by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<AppState?>(null) }
    var resetOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Text(stringResource(R.string.about), style = MaterialTheme.typography.headlineMedium) }
        item { AboutSection(R.string.about_purpose_title, R.string.about_purpose) }
        item { AboutSection(R.string.about_storage_title, R.string.about_storage) }
        item {
            Text(stringResource(R.string.about_version), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.about_credit))
            Text(
                stringResource(R.string.contact_placeholder),
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        item { AboutSection(R.string.about_sources_title, R.string.about_sources) }
        item { AboutSection(R.string.about_limits_title, R.string.about_limits) }
        item {
            Button(
                onClick = { backupCode = onBackup(appState) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_data))
            }
        }
        backupCode?.let { code ->
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.backup_explanation))
                        SelectionContainer {
                            Text(code, style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(
                                    ClipData.newPlainText("Kaala Kolam backup", code),
                                )
                            },
                        ) {
                            Text(stringResource(R.string.copy))
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = {
                    restoreText = ""
                    invalidRestore = false
                    restoreEntryOpen = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.restore_data))
            }
        }
        item {
            OutlinedButton(
                onClick = { resetOpen = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.reset_data))
            }
        }
    }

    if (restoreEntryOpen) {
        AlertDialog(
            onDismissRequest = { restoreEntryOpen = false },
            title = { Text(stringResource(R.string.restore_data)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = {
                            restoreText = it.take(65_536)
                            invalidRestore = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (invalidRestore) {
                        Text(
                            text = stringResource(R.string.restore_invalid),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val decoded = onValidateBackup(restoreText)
                        if (decoded == null) {
                            invalidRestore = true
                        } else {
                            restoreEntryOpen = false
                            pendingRestore = decoded
                        }
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { restoreEntryOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    pendingRestore?.let { validated ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRestore = null
                        onRestore(validated)
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestore = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (resetOpen) {
        AlertDialog(
            onDismissRequest = { resetOpen = false },
            title = { Text(stringResource(R.string.reset_confirm_title)) },
            text = { Text(stringResource(R.string.reset_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetOpen = false
                        onReset()
                    },
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { resetOpen = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AboutSection(
    title: Int,
    body: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body))
    }
}
