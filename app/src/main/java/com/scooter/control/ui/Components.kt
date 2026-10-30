package com.scooter.control.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.scooter.control.data.Profile
import com.scooter.control.ui.theme.LocalAnimations
import com.scooter.control.ui.theme.LocalPalette
import com.scooter.control.ui.theme.motion
import java.io.File

/** Плавная смена цвета (мгновенная, если анимации выключены). */
@Composable
fun animColor(target: Color): Color =
    animateColorAsState(target, motion(), label = "color").value

/** Появление блока снизу со ступенчатой задержкой. */
@Composable
fun Reveal(index: Int, content: @Composable () -> Unit) {
    val anim = LocalAnimations.current
    val state = remember { MutableTransitionState(!anim).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(420, delayMillis = index * 70)) +
            slideInVertically(tween(420, delayMillis = index * 70)) { it / 5 },
    ) { content() }
}

@Composable
fun Avatar(profile: Profile, avatarFile: File, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val bitmap = remember(profile.avatarStamp) {
        avatarFile.takeIf { it.exists() }
            ?.let { BitmapFactory.decodeFile(it.path)?.asImageBitmap() }
    }
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(p.accent),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            val initial = profile.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(
                initial,
                color = p.onAccent,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value / 2.4f).sp,
            )
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = p.textPrimary)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = p.textPrimary)
    }
}
