package com.scooter.control.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scooter.control.data.DeepLink
import com.scooter.control.data.Profile
import com.scooter.control.data.SessionStore
import com.scooter.control.ui.theme.LocalAnimations
import com.scooter.control.ui.theme.LocalPalette

/** Ссылка на Telegram-бота, который выдаёт deep-link для входа. */
private const val TELEGRAM_BOT_URL = "https://t.me/jhabablastbot"

@Composable
fun LoginScreen(
    deepLink: DeepLink?,
    sessionStore: SessionStore,
    onLoggedIn: () -> Unit,
) {
    val p = LocalPalette.current
    val anim = LocalAnimations.current
    val context = LocalContext.current

    // Пришли по deep-link — сохраняем токен и профиль из Telegram и входим.
    LaunchedEffect(deepLink) {
        if (deepLink != null && deepLink.token.isNotBlank()) {
            sessionStore.saveSession(
                deepLink.token,
                Profile(
                    name = deepLink.name,
                    username = deepLink.username,
                    uid = deepLink.uid,
                    server = deepLink.server,
                )
            )
            onLoggedIn()
        }
    }

    // Токен уже сохранён с прошлого раза — не показываем логин заново.
    val savedToken by sessionStore.tokenFlow.collectAsState(initial = null)
    LaunchedEffect(savedToken) {
        if (!savedToken.isNullOrBlank() && deepLink == null) onLoggedIn()
    }

    val transition = rememberInfiniteTransition(label = "logo")
    val logoScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    val s = if (anim) logoScale else 1f
                    scaleX = s
                    scaleY = s
                }
                .background(p.accent, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = p.onAccent,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "LastBot by excellent",
            style = MaterialTheme.typography.headlineMedium,
            color = p.textPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Это демо-симулятор. Откройте Telegram-бота и нажмите «Войти» — бот сразу выдаст ключ доступа и перебросит вас обратно сюда, профиль подтянется из Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            color = p.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TELEGRAM_BOT_URL)))
            },
            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Открыть Telegram-бота", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))

        Surface(
            color = p.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "После входа бот отправит ссылку вида scooterapp://auth?token=..., " +
                    "которая откроет это приложение и залогинит вас автоматически.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.textSecondary,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
