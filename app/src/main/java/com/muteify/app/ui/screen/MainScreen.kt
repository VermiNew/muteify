package com.muteify.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
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
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muteify.app.data.model.AppTheme
import com.muteify.app.data.model.RuleHistoryEntity
import com.muteify.app.data.model.RulePriority
import com.muteify.app.data.model.SchedulePolicy
import com.muteify.app.data.model.SoundAction
import com.muteify.app.data.model.TriggerState
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
    val neverAutoUnmute by viewModel.neverAutoUnmute.collectAsState()
    val automationPauseInput by viewModel.automationPauseInput.collectAsState()
    val automationPauseSummary by viewModel.automationPauseSummary.collectAsState()
    val quietHoursInput by viewModel.quietHoursInput.collectAsState()
    val quietHoursSummary by viewModel.quietHoursSummary.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val rulePriority by viewModel.rulePriority.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val hasNotificationPolicyAccess by viewModel.hasNotificationPolicyAccess.collectAsState()
    val nextScheduleSummary by viewModel.nextScheduleSummary.collectAsState()
    val soundStatusSummary by viewModel.soundStatusSummary.collectAsState()
    val recentHistoryEvents by viewModel.recentHistoryEvents.collectAsState()
    val currentWifiSsid by viewModel.currentWifiSsid.collectAsState()
    val currentWifiState by viewModel.currentWifiState.collectAsState()
    val trustedWifiSsids by viewModel.trustedWifiSsids.collectAsState()
    val effectiveTrustedWifiSsids = (trustedWifiSsids + ssid)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .toSet()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPostNotificationsPermission by remember {
        mutableStateOf(isPostNotificationsPermissionGranted(context))
    }
    var hasWifiLocationPermission by remember {
        mutableStateOf(isWifiLocationPermissionGranted(context))
    }
    var isLocationEnabled by remember {
        mutableStateOf(isLocationEnabled(context))
    }
    var isWifiEnabled by remember {
        mutableStateOf(isWifiEnabled(context))
    }
    var showAdvancedSettings by remember {
        mutableStateOf(false)
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
                viewModel.refreshWifiStatus()
                hasPostNotificationsPermission = isPostNotificationsPermissionGranted(context)
                hasWifiLocationPermission = isWifiLocationPermissionGranted(context)
                isLocationEnabled = isLocationEnabled(context)
                isWifiEnabled = isWifiEnabled(context)
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

            AppStatusCard(
                isRunning = isRunning,
                blockingPermissionCount = blockingPermissionCount(
                    hasNotificationPolicyAccess = hasNotificationPolicyAccess,
                    hasPostNotificationsPermission = hasPostNotificationsPermission,
                    hasWifiLocationPermission = hasWifiLocationPermission
                ),
                nextAction = nextScheduleSummary,
                currentSsid = currentWifiSsid,
                currentWifiState = currentWifiState
            )

            SoundStatusCard(summary = soundStatusSummary)

            NextScheduleCard(summary = nextScheduleSummary)

            LatestDecisionCard(events = recentHistoryEvents.take(3))

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

            SafetyRulesCard(
                neverAutoUnmute = neverAutoUnmute,
                enabled = !isRunning,
                onNeverAutoUnmuteChanged = viewModel::onNeverAutoUnmuteChanged
            )

            QuietHoursCard(
                input = quietHoursInput,
                summary = quietHoursSummary,
                hasNotificationPolicyAccess = hasNotificationPolicyAccess,
                onInputChanged = viewModel::onQuietHoursInputChanged,
                onStart = viewModel::startQuietHours,
                onCancel = viewModel::cancelQuietHours
            )

            AutomationPauseCard(
                input = automationPauseInput,
                summary = automationPauseSummary,
                onInputChanged = viewModel::onAutomationPauseInputChanged,
                onPause = viewModel::pauseAutomation,
                onResume = viewModel::resumeAutomation
            )

            WifiStatusCard(
                currentSsid = currentWifiSsid,
                state = currentWifiState,
                trustedSsids = effectiveTrustedWifiSsids,
                warning = wifiSsidWarning(
                    currentSsid = currentWifiSsid,
                    hasLocationPermission = hasWifiLocationPermission,
                    isLocationEnabled = isLocationEnabled,
                    isWifiEnabled = isWifiEnabled
                ),
                enabled = !isRunning,
                onSetCurrentAsHome = viewModel::setCurrentWifiAsHome,
                onRemoveTrustedSsid = viewModel::removeTrustedWifiSsid
            )

            AdvancedSettingsSection(
                expanded = showAdvancedSettings,
                onExpandedChanged = { showAdvancedSettings = it }
            ) {
                AppThemeDropdown(
                    label = "Motyw aplikacji",
                    selected = appTheme,
                    onSelected = viewModel::onAppThemeChanged
                )

                RulePriorityDropdown(
                    label = "Priorytet reguł",
                    selected = rulePriority,
                    onSelected = viewModel::onRulePriorityChanged
                )

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
            }

            RecentHistorySection(events = recentHistoryEvents)
        }

        Button(
            onClick = viewModel::toggleService,
            modifier = Modifier.fillMaxWidth(),
            enabled = isRunning || effectiveTrustedWifiSsids.isNotEmpty()
        ) {
            Text(if (isRunning) "Zatrzymaj" else "Zapisz i włącz")
        }
    }
}

@Composable
fun AdvancedSettingsSection(
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ustawienia zaawansowane",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { onExpandedChanged(!expanded) }) {
                    Text(if (expanded) "Ukryj" else "Pokaż")
                }
            }
            if (expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun AppStatusCard(
    isRunning: Boolean,
    blockingPermissionCount: Int,
    nextAction: String,
    currentSsid: String?,
    currentWifiState: TriggerState
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Stan aplikacji",
                style = MaterialTheme.typography.titleMedium
            )
            StatusLine(
                label = "Monitoring",
                value = if (isRunning) "aktywny" else "zatrzymany",
                isPositive = isRunning
            )
            StatusLine(
                label = "Uprawnienia",
                value = if (blockingPermissionCount == 0) {
                    "działają"
                } else {
                    "braki: $blockingPermissionCount"
                },
                isPositive = blockingPermissionCount == 0
            )
            StatusLine(
                label = "Następna akcja",
                value = nextAction
            )
            StatusLine(
                label = "Kontekst",
                value = "${currentSsid ?: "Sieć nieznana"} · ${triggerStateLabel(currentWifiState.name)}"
            )
        }
    }
}

@Composable
private fun StatusLine(
    label: String,
    value: String,
    isPositive: Boolean? = null
) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = when (isPositive) {
            true -> MaterialTheme.colorScheme.primary
            false -> MaterialTheme.colorScheme.error
            null -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
fun WifiStatusCard(
    currentSsid: String?,
    state: TriggerState,
    trustedSsids: Set<String>,
    warning: String?,
    enabled: Boolean,
    onSetCurrentAsHome: () -> Unit,
    onRemoveTrustedSsid: (String) -> Unit
) {
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
                text = "Wi-Fi",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Aktualna sieć: ${currentSsid ?: "Nieznana"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Stan: ${triggerStateLabel(state.name)}",
                style = MaterialTheme.typography.bodyMedium,
                color = when (state) {
                    TriggerState.HOME -> MaterialTheme.colorScheme.primary
                    TriggerState.AWAY -> MaterialTheme.colorScheme.onSurface
                    TriggerState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = "Zaufane sieci",
                style = MaterialTheme.typography.titleSmall
            )
            if (trustedSsids.isEmpty()) {
                Text(
                    text = "Brak. Ustaw obecną sieć jako Dom.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                trustedSsids.sorted().forEach { ssid ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ssid,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { onRemoveTrustedSsid(ssid) },
                            enabled = enabled
                        ) {
                            Text("Usuń")
                        }
                    }
                }
            }
            if (warning != null) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            OutlinedButton(
                onClick = onSetCurrentAsHome,
                enabled = enabled && currentSsid != null
            ) {
                Text("Ustaw obecną jako Dom")
            }
        }
    }
}

@Composable
fun AutomationPauseCard(
    input: String,
    summary: String,
    onInputChanged: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Pauza automatyzacji",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                label = { Text("Do godziny lub dnia") },
                placeholder = { Text("08:00 lub 2026-05-27 08:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPause,
                    modifier = Modifier.weight(1f),
                    enabled = input.isNotBlank()
                ) {
                    Text("Pauzuj")
                }
                OutlinedButton(
                    onClick = onResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Wznów")
                }
            }
        }
    }
}

@Composable
fun QuietHoursCard(
    input: String,
    summary: String,
    hasNotificationPolicyAccess: Boolean,
    onInputChanged: (String) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ciche godziny",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChanged,
                label = { Text("Wycisz do") },
                placeholder = { Text("08:00 lub 2026-05-27 08:00") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (!hasNotificationPolicyAccess) {
                Text(
                    text = "Brakuje dostępu do zmiany trybu dzwonka.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                    enabled = input.isNotBlank() && hasNotificationPolicyAccess
                ) {
                    Text("Wycisz")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Anuluj")
                }
            }
        }
    }
}

@Composable
fun SafetyRulesCard(
    neverAutoUnmute: Boolean,
    enabled: Boolean,
    onNeverAutoUnmuteChanged: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Zasady bezpieczeństwa",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Nigdy nie odciszaj automatycznie",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (neverAutoUnmute) {
                            "Odciszenie wymaga potwierdzenia."
                        } else {
                            "Odciszenie może wykonać się po odliczaniu."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = neverAutoUnmute,
                    onCheckedChange = onNeverAutoUnmuteChanged,
                    enabled = enabled
                )
            }
        }
    }
}

@Composable
fun LatestDecisionCard(events: List<RuleHistoryEntity>) {
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
                text = "Ostatnie decyzje",
                style = MaterialTheme.typography.titleMedium
            )
            if (events.isEmpty()) {
                Text(
                    text = "Brak zapisanych decyzji",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                events.forEachIndexed { index, event ->
                    if (index > 0) {
                        HorizontalDivider()
                    }
                    Text(
                        text = "${formatHistoryTime(event.occurredAtMillis)} · ${outcomeLabel(event.outcome)} · ${actionLabel(event.action)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = sourceLabel(event.source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SoundStatusCard(summary: String) {
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
                text = "Stan dźwięku",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun NextScheduleCard(summary: String) {
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
                text = "Następna akcja",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentHistorySection(events: List<RuleHistoryEntity>) {
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    val filteredEvents = events.filter { event -> selectedFilter.matches(event) }
    val context = LocalContext.current

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
            HistoryFilterRow(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it }
            )
            OutlinedButton(
                onClick = { shareHistoryCsv(context, events) },
                enabled = events.isNotEmpty()
            ) {
                Text("Eksportuj CSV")
            }
            if (filteredEvents.isEmpty()) {
                Text(
                    text = if (events.isEmpty()) "Brak zdarzeń" else "Brak zdarzeń dla filtra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                filteredEvents.forEachIndexed { index, event ->
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
@OptIn(ExperimentalLayoutApi::class)
private fun HistoryFilterRow(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) }
            )
        }
    }
}

@Composable
fun HistoryEventRow(event: RuleHistoryEntity) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${formatHistoryTime(event.occurredAtMillis)} · ${sourceLabel(event.source)}",
            style = MaterialTheme.typography.bodyMedium
        )
        HistoryDetailLine(
            label = "Trigger",
            value = sourceLabel(event.source)
        )
        HistoryDetailLine(
            label = "Kontekst",
            value = "Stan: ${triggerStateLabel(event.triggerState)} · Zasada: ${policyLabel(event.policy)}"
        )
        HistoryDetailLine(
            label = "Decyzja",
            value = "${outcomeLabel(event.outcome)} · ${actionLabel(event.action)}"
        )
        if (event.details.isNotBlank()) {
            HistoryDetailLine(
                label = if (event.outcome.startsWith("skipped")) "Powód" else "Szczegóły",
                value = historyDetailsLabel(event.details)
            )
        }
    }
}

@Composable
private fun HistoryDetailLine(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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
            enabled = controlsEnabled && enabled && policy.runsAfterCountdown(),
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
    val diagnostics = listOf(
        PermissionDiagnostic(
            title = "Zmiana trybu dzwonka",
            status = if (hasNotificationPolicyAccess) "Działa" else "Blokuje automatyzację",
            details = if (hasNotificationPolicyAccess) {
                "Aplikacja może wykonać wyciszenie, wibracje i odciszenie po potwierdzeniu."
            } else {
                "Bez dostępu do trybu Nie przeszkadzać aplikacja nie zmieni trybu dzwonka."
            },
            isBlocking = !hasNotificationPolicyAccess
        ),
        PermissionDiagnostic(
            title = "Monity i odliczanie",
            status = if (hasPostNotificationsPermission) "Działa" else "Blokuje widoczne monity",
            details = if (hasPostNotificationsPermission) {
                "Aplikacja może pokazywać monity z akcją wykonania lub anulowania."
            } else {
                "Bez powiadomień użytkownik może nie zobaczyć prośby o decyzję."
            },
            isBlocking = !hasPostNotificationsPermission
        ),
        PermissionDiagnostic(
            title = "Rozpoznawanie sieci domowej",
            status = if (hasWifiLocationPermission) "Działa" else "Blokuje wykrywanie Wi-Fi",
            details = if (hasWifiLocationPermission) {
                "Aplikacja może odczytać SSID i rozpoznać sieć Dom."
            } else {
                "Android wymaga lokalizacji, żeby aplikacja mogła odczytać nazwę Wi-Fi."
            },
            isBlocking = !hasWifiLocationPermission
        ),
        PermissionDiagnostic(
            title = "Dokładne alarmy",
            status = "Opcjonalne teraz",
            details = "Bez nich harmonogram może być mniej punktualny, ale aplikacja nadal działa z alarmami systemowymi.",
            isBlocking = false,
            isOptional = true
        )
    )
    val blockingCount = diagnostics.count { it.isBlocking }
    val summaryText = if (blockingCount == 0) {
        "Wszystkie wymagane elementy działają."
    } else {
        "Do działania brakuje: $blockingCount"
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
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (blockingCount == 0) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            diagnostics.forEach { diagnostic ->
                PermissionDiagnosticRow(diagnostic = diagnostic)
            }
            if (!hasNotificationPolicyAccess) {
                TextButton(onClick = onOpenSettings) {
                    Text("Otwórz dostęp do trybu")
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

@Composable
private fun PermissionDiagnosticRow(diagnostic: PermissionDiagnostic) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "${diagnostic.title}: ${diagnostic.status}",
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                diagnostic.isBlocking -> MaterialTheme.colorScheme.error
                diagnostic.isOptional -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.primary
            }
        )
        Text(
            text = diagnostic.details,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class PermissionDiagnostic(
    val title: String,
    val status: String,
    val details: String,
    val isBlocking: Boolean,
    val isOptional: Boolean = false
)

private enum class HistoryFilter(val label: String) {
    ALL("Wszystkie"),
    SCHEDULE("Harmonogram"),
    WIFI("Wi-Fi"),
    CANCELLED("Anulowane"),
    EXECUTED("Wykonane");

    fun matches(event: RuleHistoryEntity): Boolean {
        return when (this) {
            ALL -> true
            SCHEDULE -> event.source.startsWith("schedule:")
            WIFI -> event.source.startsWith("wifi:")
            CANCELLED -> event.outcome == "dismissed"
            EXECUTED -> event.outcome in setOf("confirmed", "auto_executed")
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

private fun blockingPermissionCount(
    hasNotificationPolicyAccess: Boolean,
    hasPostNotificationsPermission: Boolean,
    hasWifiLocationPermission: Boolean
): Int {
    return listOf(
        hasNotificationPolicyAccess,
        hasPostNotificationsPermission,
        hasWifiLocationPermission
    ).count { !it }
}

private fun isLocationEnabled(context: Context): Boolean {
    val locationManager = context.getSystemService(LocationManager::class.java)
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

private fun isWifiEnabled(context: Context): Boolean {
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return wifiManager.isWifiEnabled
}

private fun wifiSsidWarning(
    currentSsid: String?,
    hasLocationPermission: Boolean,
    isLocationEnabled: Boolean,
    isWifiEnabled: Boolean
): String? {
    if (currentSsid != null) return null
    return when {
        !hasLocationPermission ->
            "SSID niedostępny: brakuje uprawnienia lokalizacji."
        !isLocationEnabled ->
            "SSID niedostępny: lokalizacja systemowa jest wyłączona."
        !isWifiEnabled ->
            "SSID niedostępny: Wi-Fi jest wyłączone."
        else ->
            "SSID niedostępny: Android nie zwrócił nazwy bieżącej sieci."
    }
}

private fun shareHistoryCsv(context: Context, events: List<RuleHistoryEntity>) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, "Mute-ify historia.csv")
        putExtra(Intent.EXTRA_TEXT, events.toHistoryCsv())
    }
    context.startActivity(Intent.createChooser(shareIntent, "Eksportuj historię"))
}

private fun List<RuleHistoryEntity>.toHistoryCsv(): String {
    val header = listOf(
        "time",
        "source",
        "trigger_state",
        "action",
        "policy",
        "outcome",
        "details"
    ).joinToString(",")
    val rows = map { event ->
        listOf(
            formatHistoryTime(event.occurredAtMillis),
            sourceLabel(event.source),
            triggerStateLabel(event.triggerState),
            actionLabel(event.action),
            policyLabel(event.policy),
            outcomeLabel(event.outcome),
            historyDetailsLabel(event.details)
        ).joinToString(",") { it.csvEscape() }
    }
    return (listOf(header) + rows).joinToString("\n")
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return "\"$escaped\""
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
        "quiet:one_off" -> "Ciche godziny"
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
        SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN.name -> "Bez monitu po odliczaniu"
        SchedulePolicy.REQUIRE_CONFIRMATION.name -> "Potwierdzenie"
        SchedulePolicy.NOTIFY_ONLY.name -> "Tylko powiadom"
        "ONE_OFF" -> "Jednorazowo"
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
        "skipped_rule_priority" -> "Pominięto: priorytet reguł"
        else -> outcome
    }
}

private fun historyDetailsLabel(details: String): String {
    return when (details) {
        "Schedule slot is disabled" -> "Slot harmonogramu jest wyłączony."
        "Schedule trigger handled" -> "Obsłużono zdarzenie harmonogramu."
        "Pending schedule action was skipped because the slot is disabled" ->
            "Pominięto, bo slot harmonogramu jest wyłączony."
        "Schedule action was skipped because notification policy access is missing" ->
            "Pominięto, bo brakuje dostępu do trybu Nie przeszkadzać."
        "Schedule action applied" -> "Akcja harmonogramu została wykonana."
        "Pending schedule action was skipped because policy changed" ->
            "Pominięto, bo zasady zmieniły się po utworzeniu monitu."
        "Pending schedule action was cancelled" -> "Użytkownik anulował oczekującą akcję."
        "Schedule unmute was skipped because Wi-Fi has priority" ->
            "Pominięto odciszenie, bo Wi-Fi ma pierwszeństwo przed harmonogramem."
        "Pending schedule unmute was skipped because Wi-Fi has priority" ->
            "Pominięto oczekujące odciszenie, bo Wi-Fi ma pierwszeństwo."
        "One-off quiet hours started" -> "Rozpoczęto jednorazowe ciche godziny."
        "One-off quiet hours cancelled" -> "Anulowano jednorazowe ciche godziny."
        "One-off quiet hours ended" -> "Zakończono jednorazowe ciche godziny."
        "One-off quiet hours could not end because notification policy access is missing" ->
            "Nie udało się zakończyć, bo brakuje dostępu do trybu Nie przeszkadzać."
        else -> details
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
        SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN to "Bez monitu po odliczaniu",
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

private fun SchedulePolicy.runsAfterCountdown(): Boolean {
    return this == SchedulePolicy.AUTO_AFTER_COUNTDOWN ||
        this == SchedulePolicy.AUTO_SILENT_AFTER_COUNTDOWN
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppThemeDropdown(
    label: String,
    selected: AppTheme,
    onSelected: (AppTheme) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        AppTheme.OLED to "OLED",
        AppTheme.DAY to "Dzień",
        AppTheme.NIGHT to "Noc",
        AppTheme.READING to "Czytanie"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            AppTheme.entries.forEach { theme ->
                DropdownMenuItem(
                    text = { Text(labels[theme] ?: "") },
                    onClick = {
                        onSelected(theme)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RulePriorityDropdown(
    label: String,
    selected: RulePriority,
    onSelected: (RulePriority) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val labels = mapOf(
        RulePriority.SCHEDULE_FIRST to "Harmonogram przed Wi-Fi",
        RulePriority.WIFI_FIRST to "Wi-Fi przed harmonogramem"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = labels[selected] ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(
                    type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                    enabled = true
                )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            RulePriority.entries.forEach { priority ->
                DropdownMenuItem(
                    text = { Text(labels[priority] ?: "") },
                    onClick = {
                        onSelected(priority)
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
