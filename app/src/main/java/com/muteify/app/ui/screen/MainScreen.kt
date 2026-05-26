package com.muteify.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muteify.app.data.model.RuleHistoryEntity
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val ssid by viewModel.ssid.collectAsState()
    val actionEnter by viewModel.actionEnter.collectAsState()
    val actionLeave by viewModel.actionLeave.collectAsState()
    val morningTime by viewModel.morningTime.collectAsState()
    val nightTime by viewModel.nightTime.collectAsState()
    val morningScheduleEnabled by viewModel.morningScheduleEnabled.collectAsState()
    val morningScheduleAction by viewModel.morningScheduleAction.collectAsState()
    val morningSchedulePolicy by viewModel.morningSchedulePolicy.collectAsState()
    val morningCountdownSeconds by viewModel.morningCountdownSeconds.collectAsState()
    val eveningScheduleEnabled by viewModel.eveningScheduleEnabled.collectAsState()
    val eveningScheduleAction by viewModel.eveningScheduleAction.collectAsState()
    val eveningSchedulePolicy by viewModel.eveningSchedulePolicy.collectAsState()
    val eveningCountdownSeconds by viewModel.eveningCountdownSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val hasNotificationPolicyAccess by viewModel.hasNotificationPolicyAccess.collectAsState()
    val recentHistoryEvents by viewModel.recentHistoryEvents.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPostNotificationsPermission by remember {
        mutableStateOf(isPostNotificationsPermissionGranted(context))
    }
    var hasWifiLocationPermission by remember {
        mutableStateOf(isWifiLocationPermissionGranted(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPostNotificationsPermission = isGranted
    }
    val wifiLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasWifiLocationPermission = isGranted
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
                hasPostNotificationsPermission = isPostNotificationsPermissionGranted(context)
                hasWifiLocationPermission = isWifiLocationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
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

            PermissionStatusCard(
                hasNotificationPolicyAccess = hasNotificationPolicyAccess,
                hasPostNotificationsPermission = hasPostNotificationsPermission,
                hasWifiLocationPermission = hasWifiLocationPermission,
                onOpenSettings = viewModel::openNotificationPolicySettings,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestWifiLocationPermission = {
                    wifiLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
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

            Text(
                text = "Harmonogram",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = morningTime,
                    onValueChange = viewModel::onMorningTimeChanged,
                    label = { Text("Rano") },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    singleLine = true
                )
                OutlinedTextField(
                    value = nightTime,
                    onValueChange = viewModel::onNightTimeChanged,
                    label = { Text("Wieczorem") },
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning,
                    singleLine = true
                )
            }

            ScheduleSlotBehaviorControls(
                title = "Rano",
                enabled = morningScheduleEnabled,
                action = morningScheduleAction,
                policy = morningSchedulePolicy,
                countdownSeconds = morningCountdownSeconds,
                controlsEnabled = !isRunning,
                onEnabledChanged = viewModel::onMorningScheduleEnabledChanged,
                onActionChanged = viewModel::onMorningScheduleActionChanged,
                onPolicyChanged = viewModel::onMorningSchedulePolicyChanged,
                onCountdownSecondsChanged = viewModel::onMorningCountdownSecondsChanged
            )

            ScheduleSlotBehaviorControls(
                title = "Wieczorem",
                enabled = eveningScheduleEnabled,
                action = eveningScheduleAction,
                policy = eveningSchedulePolicy,
                countdownSeconds = eveningCountdownSeconds,
                controlsEnabled = !isRunning,
                onEnabledChanged = viewModel::onEveningScheduleEnabledChanged,
                onActionChanged = viewModel::onEveningScheduleActionChanged,
                onPolicyChanged = viewModel::onEveningSchedulePolicyChanged,
                onCountdownSecondsChanged = viewModel::onEveningCountdownSecondsChanged
            )

            RecentHistorySection(events = recentHistoryEvents)
        }

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
fun RecentHistorySection(events: List<RuleHistoryEntity>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Ostatnie zdarzenia",
                style = MaterialTheme.typography.titleMedium
            )
            if (events.isEmpty()) {
                Text(
                    text = "Brak zdarzeń",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                events.forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    HistoryEventRow(event = event)
                }
            }
        }
    }
}

@Composable
fun HistoryEventRow(event: RuleHistoryEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${formatHistoryTime(event.occurredAtMillis)} · ${sourceLabel(event.source)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "${outcomeLabel(event.outcome)} · ${actionLabel(event.action)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Stan: ${triggerStateLabel(event.triggerState)} · Zasada: ${policyLabel(event.policy)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ScheduleSlotBehaviorControls(
    title: String,
    enabled: Boolean,
    action: SoundAction,
    policy: SchedulePolicy,
    countdownSeconds: Int,
    controlsEnabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onActionChanged: (SoundAction) -> Unit,
    onPolicyChanged: (SchedulePolicy) -> Unit,
    onCountdownSecondsChanged: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall
            )
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChanged,
                enabled = controlsEnabled
            )
        }
        SoundActionDropdown(
            label = "$title: akcja",
            selected = action,
            onSelected = onActionChanged,
            enabled = controlsEnabled && enabled
        )
        SchedulePolicyDropdown(
            label = "$title: zachowanie",
            selected = policy,
            onSelected = onPolicyChanged,
            enabled = controlsEnabled && enabled
        )
        OutlinedTextField(
            value = countdownSeconds.toString(),
            onValueChange = onCountdownSecondsChanged,
            label = { Text("$title: odliczanie (s)") },
            modifier = Modifier.fillMaxWidth(),
            enabled = controlsEnabled && enabled && policy == SchedulePolicy.AUTO_AFTER_COUNTDOWN,
            singleLine = true
        )
    }
}

@Composable
fun PermissionStatusCard(
    hasNotificationPolicyAccess: Boolean,
    hasPostNotificationsPermission: Boolean,
    hasWifiLocationPermission: Boolean,
    onOpenSettings: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestWifiLocationPermission: () -> Unit
) {
    val policyStatusText = if (hasNotificationPolicyAccess) "Przyznany" else "Brak"
    val notificationStatusText = if (hasPostNotificationsPermission) "Przyznane" else "Brak"
    val wifiLocationStatusText = if (hasWifiLocationPermission) "Przyznana" else "Brak"
    val policyStatusColor = if (hasNotificationPolicyAccess) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val notificationStatusColor = if (hasPostNotificationsPermission) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }
    val wifiLocationStatusColor = if (hasWifiLocationPermission) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Uprawnienia",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Dostęp do trybu Nie przeszkadzać: $policyStatusText",
                style = MaterialTheme.typography.bodyMedium,
                color = policyStatusColor
            )
            Text(
                text = "Powiadomienia: $notificationStatusText",
                style = MaterialTheme.typography.bodyMedium,
                color = notificationStatusColor
            )
            Text(
                text = "Lokalizacja dla Wi-Fi: $wifiLocationStatusText",
                style = MaterialTheme.typography.bodyMedium,
                color = wifiLocationStatusColor
            )
            if (!hasNotificationPolicyAccess) {
                TextButton(onClick = onOpenSettings) {
                    Text("Otwórz ustawienia")
                }
            }
            if (!hasPostNotificationsPermission) {
                TextButton(onClick = onRequestNotificationPermission) {
                    Text("Zezwól na powiadomienia")
                }
            }
            if (!hasWifiLocationPermission) {
                TextButton(onClick = onRequestWifiLocationPermission) {
                    Text("Zezwól na lokalizację")
                }
            }
        }
    }
}

private fun isPostNotificationsPermissionGranted(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun isWifiLocationPermissionGranted(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun formatHistoryTime(occurredAtMillis: Long): String {
    return Instant.ofEpochMilli(occurredAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun sourceLabel(source: String): String {
    return when (source) {
        "schedule:morning" -> "Harmonogram rano"
        "schedule:evening" -> "Harmonogram wieczorem"
        else -> source
    }
}

private fun actionLabel(action: String): String {
    return when (action) {
        SoundAction.SILENCE.name -> "Wycisz"
        SoundAction.UNSILENCE.name -> "Odcisz"
        SoundAction.VIBRATE.name -> "Wibracje"
        SoundAction.DO_NOTHING.name -> "Bez zmian"
        else -> action
    }
}

private fun policyLabel(policy: String): String {
    return when (policy) {
        SchedulePolicy.AUTO_AFTER_COUNTDOWN.name -> "Po odliczaniu"
        SchedulePolicy.REQUIRE_CONFIRMATION.name -> "Potwierdzenie"
        SchedulePolicy.NOTIFY_ONLY.name -> "Tylko powiadom"
        else -> policy
    }
}

private fun outcomeLabel(outcome: String): String {
    return when (outcome) {
        "prompted" -> "Pokazano monit"
        "confirmed" -> "Potwierdzono"
        "auto_executed" -> "Wykonano automatycznie"
        "dismissed" -> "Anulowano"
        "skipped_disabled" -> "Pominięto: wyłączone"
        "skipped_missing_notification_policy_access" -> "Pominięto: brak dostępu"
        "skipped_policy_changed" -> "Pominięto: zmiana zasad"
        else -> outcome
    }
}

private fun triggerStateLabel(triggerState: String): String {
    return when (triggerState) {
        "HOME" -> "Dom"
        "AWAY" -> "Poza domem"
        "UNKNOWN" -> "Nieznane"
        "NOT_APPLICABLE" -> "Nie dotyczy"
        else -> triggerState
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SchedulePolicyDropdown(
    label: String,
    selected: SchedulePolicy,
    onSelected: (SchedulePolicy) -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        SchedulePolicy.AUTO_AFTER_COUNTDOWN to "Po odliczaniu",
        SchedulePolicy.REQUIRE_CONFIRMATION to "Wymagaj potwierdzenia",
        SchedulePolicy.NOTIFY_ONLY to "Tylko powiadom"
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
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                ),
            enabled = enabled
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            SchedulePolicy.entries.forEach { policy ->
                DropdownMenuItem(
                    text = { Text(labels[policy] ?: "") },
                    onClick = {
                        onSelected(policy)
                        expanded = false
                    }
                )
            }
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
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = enabled
                ),
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
