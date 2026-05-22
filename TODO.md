# Muteify TODO

## Product Vision

Muteify manages the phone's silent state with a safety-first approach.

The app should proactively prevent unwanted ringing, especially when the user leaves a trusted zone or reaches a scheduled quiet time. It should be conservative when restoring sound, because an accidental unmute can be more harmful than an accidental mute.

Core product rule:

```text
Muting can be proactive.
Unmuting must be deliberate.
```

## MVP Scope

- Support one trusted zone first, named Home.
- Support multiple trusted zones later.
- Detect Home primarily by Wi-Fi.
- Add Bluetooth detection later as a second presence signal.
- Keep GPS/geofencing as the final fallback and disabled by default.
- Provide editable time rules.
- Show notification prompts before automatic muting.
- Avoid persistent always-on notifications.
- Require explicit user confirmation before unmuting.
- Keep the app battery-efficient and event-driven.

## Safety Rules

### Muting

- Automatic muting is allowed after a visible prompt and countdown.
- Default countdown: 30 seconds.
- The user can cancel the pending mute.
- If the user does not react, the app may mute the phone.

### Unmuting

- Automatic unmuting is disabled by default.
- Unmuting requires explicit user confirmation.
- If the phone is outside Home or the location state is unknown, the app must not unmute automatically.
- The app may show a prompt asking whether the user wants to unmute.
- No response means the phone remains muted.

### Manual User Decisions

- Manual user decisions should be respected.
- Canceling a pending mute suppresses only the current trigger event.
- For zone-based triggers, suppression lasts until the user re-enters the zone and leaves again.
- For time-based triggers, suppression lasts until the next scheduled occurrence.

## Zone Detection

### Level 1: Wi-Fi

- Wi-Fi is the primary Home detection signal.
- Detection should rely on the currently connected SSID.
- Avoid active Wi-Fi scanning.
- Handle missing permissions and unavailable SSID values explicitly.

### Level 2: Bluetooth

- Bluetooth should initially rely on connection state to trusted paired devices.
- Avoid continuous Bluetooth scanning in MVP.
- Nearby-device scanning may be considered later only if there is a strong product reason.
- Bluetooth should support Home confidence when Wi-Fi is unavailable or unstable.

### Level 3: GPS / Geofencing

- GPS is a fallback only.
- GPS/geofencing should be added last.
- It must be opt-in and clearly explained to the user.
- Prefer geofencing over continuous location polling.

## Time Rules

Time rules are editable by the user.

Example defaults:

### 06:00 - Unmute Check

- At 06:00, check the current zone state.
- If the phone is at Home, prompt the user:

```text
You are at home. Unmute the phone?
```

- If the phone is outside Home, do not unmute automatically.
- If the phone is outside Home, show a cautious prompt:

```text
You are outside your trusted zone. The phone will remain muted. Unmute anyway?
```

- If the state is unknown, keep the phone muted and ask for confirmation.
- No response means no unmute.

### 22:00 - Mute Countdown

- At 22:00, show a prompt that the phone will be muted.
- If the user does not cancel within 30 seconds, mute the phone.
- If the user cancels, skip this scheduled mute until the next occurrence.

## Zone Exit Rule

When the user leaves Home:

- Show a prompt that the phone will be muted in 30 seconds.
- If the user cancels, do not mute for this exit event.
- If the user does not react, mute the phone.
- If the user re-enters Home during the countdown, cancel the pending mute.

## Decision Model

The app should use one shared decision engine for all triggers.

```text
Trigger: time / Wi-Fi / Bluetooth / GPS
Context: at home / outside home / unknown
Action: mute / unmute / vibrate / do nothing
Policy: require confirmation / auto after countdown / notify only
```

Suggested policies:

- `AUTO_AFTER_COUNTDOWN`: allowed for muting.
- `REQUIRE_CONFIRMATION`: required for unmuting.
- `NOTIFY_ONLY`: useful for warnings and blocked actions.

## Battery Strategy

- Prefer event-driven system callbacks over polling.
- Avoid continuous GPS usage.
- Avoid continuous Bluetooth scanning.
- Avoid active Wi-Fi scanning.
- Use scheduled alarms for time rules instead of keeping a service alive only to wait for a time.
- Use a foreground service only when required by Android behavior or during active monitoring/prompt flows.
- Show notifications only when user action or awareness is required.
- Keep exactly one active pending countdown for the current event.

## Android Permission Areas

The app must guide the user through required permissions clearly.

- Notification permission for prompts.
- Notification Policy Access for changing silent / Do Not Disturb related state.
- Wi-Fi / location permissions required to read SSID.
- Bluetooth permissions for trusted device detection.
- Exact alarm permission or fallback behavior for precise time rules.
- Background location only if GPS/geofencing fallback is added later.

## Visual Direction

- Modern Android UI using Jetpack Compose and Material 3.
- Calm, focused, premium utility style.
- UI language: Polish.
- Code, identifiers, and comments: English.
- The design should communicate safety and control.
- Avoid making automation feel reckless or hidden.

## App Themes

The app should support four themes:

- Day: neutral light theme.
- Night: dark theme with comfortable contrast.
- Reading: warm light theme with yellow / cream tones.
- OLED: true black theme for OLED screens.

Theme selection should be stored persistently.

## Task List

### Now

- [x] Fix the manifest location permission lint error.
- [x] Add a permission/status section in the main UI.
- [x] Add Notification Policy Access detection before changing audio state.
- [x] Replace automatic service-side action execution with a real notification prompt.
- [ ] Remove the persistent always-on foreground notification.
- [x] Change pending mute countdown from 60 seconds to 30 seconds.
- [x] Make pending countdown jobs cancellable.
- [x] Cancel pending countdowns when monitoring stops.

### MVP

- [x] Persist one Home zone.
- [ ] Persist editable 06:00 and 22:00 time rules.
- [x] Implement safe unmute confirmation flow.
- [x] Implement automatic mute after countdown.
- [ ] Store settings with DataStore.
- [ ] Store rule history with Room.
- [x] Restore saved Home rule after app/process recreation.
- [ ] Add recent event history.

### Later

- [ ] Add trusted Bluetooth device detection.
- [ ] Add multiple trusted zones.
- [ ] Add opt-in GPS/geofencing fallback.
- [ ] Add advanced rule priority settings.
- [ ] Add four polished app themes: Day, Night, Reading, OLED.

## Suggested Implementation Phases

### Phase 1: Stabilize Current Prototype

- Fix the current lint error around location permissions.
- Add a real permission/status screen or section.
- Make notification prompts real.
- Change the current delay from 60 seconds to the product default of 30 seconds.
- Ensure pending countdowns are cancellable.
- Ensure stopping monitoring cancels pending work.

### Phase 2: MVP Rules

- Implement one Home zone based on Wi-Fi.
- Implement editable 06:00 and 22:00 time rules.
- Implement safe unmute confirmation.
- Implement automatic mute after countdown.
- Persist settings and rules.

### Phase 3: Reliability and Battery

- Replace any unnecessary always-on behavior with event-driven flows.
- Add robust service/state recovery after process recreation.
- Add history of recent decisions and actions.
- Add handling for unknown zone state.

### Phase 4: Bluetooth Presence

- Add trusted paired Bluetooth device selection.
- Use connection state as a presence signal.
- Avoid continuous scanning.

### Phase 5: Advanced Features

- Add multiple trusted zones.
- Add GPS/geofencing fallback as opt-in.
- Add more advanced rule priorities and exceptions.
- Add theme customization polish.

## Current Known Project Issues

- The current foreground service type should be reviewed against the actual app behavior.
- The current service still uses a persistent always-on foreground notification.
- The current app does not yet request or explain runtime Wi-Fi / location permissions.
- Room is connected for one Home rule, but history is not implemented yet.
- DataStore is present but not yet connected to the product flow.
- Existing tests are placeholder tests only.
