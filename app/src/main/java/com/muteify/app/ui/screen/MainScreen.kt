package com.muteify.app.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
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
    val isRunning by viewModel.isRunning.collectAsState()
    val hasNotificationPolicyAccess by viewModel.hasNotificationPolicyAccess.collectAsState()
    val nextScheduleSummary by viewModel.nextScheduleSummary.collectAsState()
    val soundStatusSummary by viewModel.soundStatusSummary.collectAsState()
    val recentHistoryEvents by viewModel.recentHistoryEvents.collectAsState()
    val currentWifiSsid by viewModel.currentWifiSsid.collectAsState()
    val currentWifiState by viewModel.currentWifiState.collectAsState()
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
                viewModel.refreshWifiStatus()
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

            WifiStatusCard(
                currentSsid = currentWifiSsid,
                state = currentWifiState
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
fun WifiStatusCard(
    currentSsid: String?,
    state: TriggerState
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
