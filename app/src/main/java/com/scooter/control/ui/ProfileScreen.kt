package com.scooter.control.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scooter.control.data.Profile
import com.scooter.control.data.ProfileApi
import com.scooter.control.data.SessionStore
import com.scooter.control.ui.theme.LocalPalette
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    session: SessionStore,
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by session.profileFlow.collectAsState(initial = Profile())
    var refreshing by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun doRefresh() {
        if (profile.server.isBlank()) {
            message = "Сервер бота не указан в ссылке входа"
            return
        }
        refreshing = true
        val ok = ProfileApi.refresh(context.applicationContext, session)
        refreshing = false
        message = if (ok) "Профиль обновлён из Telegram" else "Не удалось обновить профиль"
    }

    LaunchedEffect(Unit) {
        if (profile.server.isNotBlank()) doRefresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        ScreenHeader("Профиль", onBack)
        Spacer(Modifier.height(20.dp))

        Reveal(0) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(profile, session.avatarFile(), 104.dp)
                Spacer(Modifier.height(14.dp))
                Text(
                    profile.name.ifBlank { "Гость" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = p.textPrimary,
                )
                if (profile.username.isNotBlank()) {
                    Text(
                        "@${profile.username}",
                        style = MaterialTheme.typography.titleMedium,
                        color = p.accent,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Reveal(1) {
            Surface(color = p.surface, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    InfoRow("Telegram ID", profile.uid.ifBlank { "—" })
                    HorizontalDivider(color = p.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow("Источник", "Telegram-бот")
                    HorizontalDivider(color = p.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    InfoRow(
                        "Сервер",
                        profile.server.removePrefix("https://").removePrefix("http://").ifBlank { "—" }
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Reveal(2) {
            Column {
                Button(
                    onClick = { scope.launch { doRefresh() } },
                    enabled = !refreshing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = p.accent,
                        contentColor = p.onAccent,
                        disabledContainerColor = p.surfaceVariant,
                        disabledContentColor = p.textSecondary,
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = p.textSecondary,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text("Обновить из Telegram", fontWeight = FontWeight.SemiBold)
                }

                message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
                }

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, p.danger),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = p.danger),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Выйти", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = p.textSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = p.textPrimary)
    }
}
