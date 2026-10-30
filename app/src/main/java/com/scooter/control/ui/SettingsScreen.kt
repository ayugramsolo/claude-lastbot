package com.scooter.control.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scooter.control.data.SettingsStore
import com.scooter.control.ui.theme.AllPalettes
import com.scooter.control.ui.theme.LocalPalette
import com.scooter.control.ui.theme.Palette
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(settings: SettingsStore, onBack: () -> Unit) {
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    val themeId by settings.themeFlow.collectAsState(initial = "orange")
    val animations by settings.animationsFlow.collectAsState(initial = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader("Настройки", onBack)
        Spacer(Modifier.height(16.dp))

        Reveal(0) {
            Text("Тема", style = MaterialTheme.typography.titleMedium, color = p.textSecondary)
        }
        Spacer(Modifier.height(10.dp))

        AllPalettes.forEachIndexed { i, palette ->
            Reveal(i + 1) {
                Column {
                    ThemeRow(palette, selected = palette.id == themeId) {
                        scope.launch { settings.setTheme(palette.id) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Reveal(AllPalettes.size + 1) {
            Surface(color = p.surface, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Анимации", style = MaterialTheme.typography.titleMedium, color = p.textPrimary, fontWeight = FontWeight.Bold)
                        Text(
                            "Появление блоков, пульс BLE, плавная смена темы",
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.textSecondary,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = animations,
                        onCheckedChange = { scope.launch { settings.setAnimations(it) } },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = p.onAccent,
                            checkedTrackColor = p.accent,
                            uncheckedThumbColor = p.textSecondary,
                            uncheckedTrackColor = p.surfaceVariant,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Reveal(AllPalettes.size + 2) {
            Column {
                Text("О приложении", style = MaterialTheme.typography.titleMedium, color = p.textSecondary)
                Spacer(Modifier.height(8.dp))
                Text("LastBot by excellent", style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
                Text("Версия 1.0 · демо-симулятор", style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ThemeRow(palette: Palette, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val borderColor = animColor(if (selected) p.accent else Color.Transparent)
    val shape = RoundedCornerShape(18.dp)
    Surface(
        color = p.surface,
        shape = shape,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(palette.background, palette.surface, palette.accent).forEach { c ->
                    Box(
                        Modifier
                            .size(26.dp)
                            .background(c, CircleShape)
                            .border(1.dp, p.surfaceVariant, CircleShape)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Text(
                palette.title,
                style = MaterialTheme.typography.titleMedium,
                color = p.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = "selected", tint = p.accent)
            }
        }
    }
}
