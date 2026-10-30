package com.scooter.control.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scooter.control.data.BleStatus
import com.scooter.control.data.FakeDevice
import com.scooter.control.data.Flashlight
import com.scooter.control.data.LockState
import com.scooter.control.data.Profile
import com.scooter.control.data.ScooterRepository
import com.scooter.control.data.ScooterState
import com.scooter.control.data.SessionStore
import com.scooter.control.data.SettingsStore
import com.scooter.control.ui.theme.LocalAnimations
import com.scooter.control.ui.theme.LocalPalette
import com.scooter.control.ui.theme.motion
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(
    repository: ScooterRepository,
    session: SessionStore,
    settings: SettingsStore,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val state by repository.state.collectAsState()
    val devices by repository.devices.collectAsState()
    val profile by session.profileFlow.collectAsState(initial = Profile())
    val disclaimerSeen by settings.disclaimerSeenFlow.collectAsState(initial = true)
    val scope = rememberCoroutineScope()
    val canAct = !state.isBusy &&
        state.ble != BleStatus.SCANNING && state.ble != BleStatus.CONNECTING
    var showDevicePicker by remember { mutableStateOf(false) }

    // Реальный фонарик телефона следует состоянию "headlight" из демо-данных.
    LaunchedEffect(state.headlightOn) { Flashlight.set(context, state.headlightOn) }
    // Гасим фонарик, если экран покидают с включённым светом.
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { Flashlight.set(context, false) }
    }

    if (!disclaimerSeen) {
        SimulatorDisclaimer { scope.launch { settings.setDisclaimerSeen() } }
    }

    if (showDevicePicker) {
        DevicePickerDialog(
            devices = devices,
            scanning = state.ble == BleStatus.SCANNING,
            onRescan = { scope.launch { repository.startScan() } },
            onPick = { device ->
                showDevicePicker = false
                scope.launch { repository.connectTo(device) }
            },
            onDismiss = { showDevicePicker = false },
        )
        LaunchedEffect(Unit) { if (devices.isEmpty()) repository.startScan() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(20.dp))

        Reveal(0) { TopBar(profile, state, session.avatarFile(), onProfile, onSettings, settings) }

        Spacer(Modifier.height(20.dp))

        Reveal(1) {
            Column {
                Text(
                    if (state.ble == BleStatus.CONNECTED) state.model else "LastBot",
                    style = MaterialTheme.typography.headlineLarge, color = p.textPrimary
                )
                Text(
                    if (state.ble == BleStatus.CONNECTED) "подключено" else "by excellent",
                    style = MaterialTheme.typography.titleMedium, color = p.accent
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Reveal(2) { StatusCard(state) }

        Spacer(Modifier.height(14.dp))

        Reveal(3) {
            BleCard(state, enabled = canAct) { showDevicePicker = true }
        }

        Spacer(Modifier.height(26.dp))

        Reveal(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("quick controls", style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
                Text(
                    if (canAct) "ready" else "busy",
                    color = if (canAct) p.accent else p.textSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Reveal(5) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ControlCard(
                    title = "unlock", subtitle = "open lock",
                    icon = Icons.Filled.LockOpen,
                    highlighted = state.lock == LockState.OPEN,
                    enabled = canAct, modifier = Modifier.weight(1f)
                ) { scope.launch { repository.unlock() } }

                ControlCard(
                    title = "lock", subtitle = "close lock",
                    icon = Icons.Filled.Lock,
                    highlighted = state.lock == LockState.LOCKED,
                    enabled = canAct, modifier = Modifier.weight(1f)
                ) { scope.launch { repository.lock() } }
            }
        }

        Spacer(Modifier.height(14.dp))

        Reveal(6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ControlCard(
                    title = "headlight",
                    subtitle = if (state.headlightOn) "on" else "off",
                    icon = Icons.Filled.WbSunny,
                    highlighted = state.headlightOn,
                    enabled = canAct, modifier = Modifier.weight(1f)
                ) { scope.launch { repository.toggleHeadlight() } }

                ControlCard(
                    title = "find", subtitle = "locate scooter",
                    icon = Icons.Filled.NotificationsActive,
                    highlighted = false, accent = true,
                    enabled = canAct, modifier = Modifier.weight(1f)
                ) { scope.launch { repository.find() } }
            }
        }

        Spacer(Modifier.height(14.dp))

        Reveal(7) {
            ControlCard(
                title = "horn", subtitle = "sound signal",
                icon = Icons.Filled.Campaign,
                highlighted = false, accent = true,
                enabled = canAct, modifier = Modifier.fillMaxWidth().height(90.dp)
            ) { scope.launch { repository.horn() } }
        }

        MessageCard(state)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SimulatorDisclaimer(onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Это симулятор") },
        text = {
            Text(
                "LastBot — демо-приложение. Все устройства, UUID, заряд батареи и скорость — " +
                    "случайные числа, которые генерирует телефон. Никакого реального BLE-подключения " +
                    "к настоящим самокатам нет, приложение ничем не управляет."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Понятно") }
        },
    )
}

@Composable
private fun DevicePickerDialog(
    devices: List<FakeDevice>,
    scanning: Boolean,
    onRescan: () -> Unit,
    onPick: (FakeDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val p = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (scanning) "Поиск устройств…" else "Найденные устройства") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                items(devices) { device ->
                    Surface(
                        color = p.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPick(device) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(device.name, color = p.textPrimary, fontWeight = FontWeight.Bold)
                            Text(
                                "UUID: ${device.uuid}  ·  ${device.rssi} dBm",
                                color = p.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                if (devices.isEmpty() && !scanning) {
                    item { Text("Ничего не найдено", color = p.textSecondary) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRescan) { Text("Искать снова") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        },
    )
}

@Composable
private fun TopBar(
    profile: Profile,
    state: ScooterState,
    avatarFile: File,
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    settings: SettingsStore,
) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    var tapCount by remember { mutableIntStateOf(0) }
    val title = if (state.ble == BleStatus.CONNECTED) state.model else "LastBot by excellent"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = p.textPrimary,
            modifier = Modifier.clickable {
                // Секретный kawaii-режим: 5 тапов по заголовку.
                tapCount++
                if (tapCount >= 5) {
                    tapCount = 0
                    scope.launch {
                        settings.setKawaii(true)
                        settings.setTheme("kawaii")
                    }
                }
            }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "settings", tint = p.textSecondary)
            }
            Avatar(
                profile = profile,
                avatarFile = avatarFile,
                size = 36.dp,
                modifier = Modifier.clip(CircleShape).clickable { onProfile() },
            )
        }
    }
}

@Composable
private fun StatusCard(state: ScooterState) {
    val p = LocalPalette.current
    var target by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(state.battery) { target = state.battery / 100f }
    val fraction by animateFloatAsState(target, motion(900), label = "battery")

    Surface(color = p.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(p.accent))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.ble == BleStatus.CONNECTED) "live" else "offline",
                            color = p.accent, style = MaterialTheme.typography.labelLarge
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(state.model, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${state.battery}%", style = MaterialTheme.typography.headlineLarge, color = p.textPrimary)
                    Text("battery", color = p.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(p.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(50))
                        .background(p.accent)
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = p.surfaceVariant)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoColumn("speed", "${state.speedKmh} km/h")
                InfoColumn("range", "${state.rangeKm} km")
                InfoColumn("lock", if (state.lock == LockState.OPEN) "open" else "locked")
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    val p = LocalPalette.current
    Column {
        Text(label, color = p.textSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        Text(value, color = p.textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BleCard(state: ScooterState, enabled: Boolean, onConnect: () -> Unit) {
    val p = LocalPalette.current
    val active = state.ble == BleStatus.SCANNING || state.ble == BleStatus.CONNECTING
    val statusText = when (state.ble) {
        BleStatus.IDLE -> "не подключено"
        BleStatus.SCANNING -> "поиск устройств…"
        BleStatus.CONNECTING -> "подключение к ${state.bleDevice ?: "устройству"}…"
        BleStatus.CONNECTED -> "подключено: ${state.bleDevice}"
        BleStatus.FAILED -> "не удалось подключиться"
    }
    Surface(color = p.surface, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PulseIcon(active)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("BLE", style = MaterialTheme.typography.titleMedium, color = p.textPrimary, fontWeight = FontWeight.Bold)
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.ble == BleStatus.FAILED) p.danger else p.textSecondary
                )
            }
            Button(
                onClick = onConnect,
                enabled = enabled,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.accent,
                    contentColor = p.onAccent,
                    disabledContainerColor = p.surfaceVariant,
                    disabledContentColor = p.textSecondary,
                ),
            ) {
                Text(
                    when (state.ble) {
                        BleStatus.FAILED -> "Повторить"
                        BleStatus.CONNECTED -> "Сменить"
                        else -> "Подключить"
                    }
                )
            }
        }
    }
}

@Composable
private fun PulseIcon(active: Boolean) {
    val p = LocalPalette.current
    val anim = LocalAnimations.current
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.7f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "pulseScale",
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.45f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "pulseAlpha",
    )
    Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
        if (active && anim) {
            Box(
                Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .background(p.accent, CircleShape)
            )
        }
        Box(
            Modifier.size(44.dp).background(p.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (active) Icons.Filled.BluetoothSearching else Icons.Filled.Bluetooth,
                contentDescription = "ble",
                tint = p.accent,
            )
        }
    }
}

@Composable
private fun MessageCard(state: ScooterState) {
    val p = LocalPalette.current
    val anim = LocalAnimations.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state.eventId) {
        if (anim && state.eventId > 0) {
            repeat(3) {
                shake.animateTo(10f, tween(45))
                shake.animateTo(-10f, tween(45))
            }
            shake.animateTo(0f, tween(45))
        }
    }
    AnimatedVisibility(
        visible = state.lastAction != null,
        enter = if (anim) fadeIn() + expandVertically() else EnterTransition.None,
        exit = if (anim) fadeOut() + shrinkVertically() else ExitTransition.None,
    ) {
        Column {
            Spacer(Modifier.height(18.dp))
            Surface(
                color = if (state.lastIsError) p.danger.copy(alpha = 0.16f) else p.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value }
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (state.lastIsError) {
                        Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = p.danger)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        state.lastAction.orEmpty(),
                        color = if (state.lastIsError) p.danger else p.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    highlighted: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.95f else 1f, motion(120), label = "press")
    val on = highlighted || accent
    val bg = animColor(if (on) p.accent else p.surface)
    val fg = animColor(if (on) p.onAccent else p.textPrimary)
    val sub = animColor(if (on) p.onAccent.copy(alpha = 0.65f) else p.textSecondary)

    Surface(
        color = bg,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .height(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.6f
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = title, tint = fg, modifier = Modifier.size(26.dp))
            Column {
                Text(title, color = fg, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = sub, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
