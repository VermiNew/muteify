package com.muteify.app.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muteify.app.data.model.SoundAction

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val ssid by viewModel.ssid.collectAsState()
    val actionEnter by viewModel.actionEnter.collectAsState()
    val actionLeave by viewModel.actionLeave.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mute-ify",
            style = MaterialTheme.typography.headlineLarge
        )

        Text(
            text = if (isRunning) "● Monitorowanie aktywne" else "○ Zatrzymane",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = ssid,
            onValueChange = viewModel::onSsidChanged,
            label = { Text("Nazwa sieci Wi-Fi (SSID)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            singleLine = true
        )

        SoundActionDropdown(
            label = "Wchodząc do domu",
            selected = actionEnter,
            onSelected = viewModel::onActionEnterChanged,
            enabled = !isRunning
        )

        SoundActionDropdown(
            label = "Wychodząc z domu",
            selected = actionLeave,
            onSelected = viewModel::onActionLeaveChanged,
            enabled = !isRunning
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = viewModel::toggleService,
            modifier = Modifier.fillMaxWidth(),
            enabled = isRunning || ssid.isNotBlank()
        ) {
            Text(if (isRunning) "Zatrzymaj" else "Zapisz i włącz")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SoundActionDropdown(
    label: String,
    selected: SoundAction,
    onSelected: (SoundAction) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        SoundAction.SILENCE to "Wycisz",
        SoundAction.UNSILENCE to "Odcisz",
        SoundAction.VIBRATE to "Wibracje",
        SoundAction.DO_NOTHING to "Bez zmian"
    )

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded && enabled) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            SoundAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(labels[action] ?: "") },
                    onClick = {
                        onSelected(action)
                        expanded = false
                    }
                )
            }
        }
    }
}
