package com.jinatra.wishplay.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jinatra.wishplay.ui.components.ExpressiveButton
import com.jinatra.wishplay.ui.components.ButtonStyle

@Composable
fun WishlistBackupSection(viewModel: ProfileViewModel) {
    val state by viewModel.backupState.collectAsState()
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.previewBackup(uri)
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Wishlist backups", style = MaterialTheme.typography.headlineMedium)
        Text("Save games, notes and reminder choices. Imports keep your existing entries.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpressiveButton(text = "Export", enabled = !state.busy, modifier = Modifier.weight(1f),
                onClick = { export.launch("WishPlay-wishlist-${java.time.LocalDate.now()}.json") })
            ExpressiveButton(text = "Import", style = ButtonStyle.TONAL, enabled = !state.busy, modifier = Modifier.weight(1f),
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
        }
        if (state.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    state.preview?.let { preview ->
        AlertDialog(onDismissRequest = viewModel::dismissBackupPreview,
            title = { Text("Import wishlist?") },
            text = {
                Column(modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${preview.newCount} new games will be added. ${preview.duplicateCount} existing entries will stay unchanged.")
                    if (preview.backup.games.isNotEmpty()) Text(preview.backup.games.take(5).joinToString("\n") { it.title },
                        style = MaterialTheme.typography.bodySmall)
                    Text("No games will be removed.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(enabled = !state.busy && preview.newCount > 0, onClick = viewModel::importBackup) { Text("Import ${preview.newCount} games") } },
            dismissButton = { TextButton(enabled = !state.busy, onClick = viewModel::dismissBackupPreview) { Text("Cancel") } })
    }
}
