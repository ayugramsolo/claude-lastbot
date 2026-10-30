package com.scooter.control.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp
import com.scooter.control.ui.theme.LocalPalette
import kotlin.random.Random

private const val LANES = 3

private data class Obstacle(var lane: Int, var y: Float)

/**
 * Мини-игра "LastBot Dash": уворачивайся от препятствий, падающих по трём полосам.
 * Чисто развлекательная вставка, с демо-данными приложения не связана.
 */
@Composable
fun GameScreen(onBack: () -> Unit) {
    val p = LocalPalette.current

    var lane by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var speed by remember { mutableFloatStateOf(0.012f) }
    val obstacles = remember { mutableStateListOf<Obstacle>() }
    var spawnTimer by remember { mutableFloatStateOf(0f) }

    fun restart() {
        obstacles.clear()
        lane = 1
        score = 0
        speed = 0.012f
        spawnTimer = 0f
        running = true
    }

    LaunchedEffect(running) {
        var last = withFrameNanos { it }
        while (running) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now

            spawnTimer += dt
            val spawnEvery = (0.9f - speed * 8f).coerceAtLeast(0.35f)
            if (spawnTimer >= spawnEvery) {
                spawnTimer = 0f
                obstacles.add(Obstacle(Random.nextInt(LANES), 0f))
            }

            for (o in obstacles) o.y += speed
            val passed = obstacles.filter { it.y > 1.05f }
            if (passed.isNotEmpty()) {
                score += passed.size
                obstacles.removeAll(passed)
            }

            if (obstacles.any { it.lane == lane && it.y in 0.78f..0.95f }) {
                running = false
                if (score > best) best = score
            }

            speed = (speed + dt * 0.004f).coerceAtMost(0.05f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = p.textPrimary)
            }
            Spacer(Modifier.height(0.dp))
            Text("LastBot Dash", style = MaterialTheme.typography.titleLarge, color = p.textPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text("тапай слева / справа, чтобы увернуться", color = p.textSecondary, style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("score: $score", color = p.accent, fontWeight = FontWeight.Bold)
            Text("best: $best", color = p.textSecondary)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(p.surface)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val laneWidth = size.width / LANES
                // полосы
                for (i in 1 until LANES) {
                    drawRect(
                        color = p.surfaceVariant,
                        topLeft = Offset(laneWidth * i - 1.dp.toPx(), 0f),
                        size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height),
                    )
                }
                // препятствия
                obstacles.forEach { o ->
                    val cx = laneWidth * o.lane + laneWidth / 2f
                    val cy = o.y * size.height
                    drawCircle(color = p.danger, radius = laneWidth * 0.28f, center = Offset(cx, cy))
                }
                // игрок
                val px = laneWidth * lane + laneWidth / 2f
                val py = size.height * 0.87f
                drawCircle(color = p.accent, radius = laneWidth * 0.3f, center = Offset(px, py))
            }

            // зоны тапа влево/вправо
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize()
                        .clickable(enabled = running) { if (lane > 0) lane-- }
                )
                Box(
                    modifier = Modifier.weight(1f).fillMaxSize()
                        .clickable(enabled = running) { if (lane < LANES - 1) lane++ }
                )
            }

            if (!running) {
                Surface(
                    color = p.background.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("game over", style = MaterialTheme.typography.headlineSmall, color = p.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("счёт: $score", color = p.textSecondary)
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { restart() },
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Ещё раз") }
                    }
                }
            }
        }
    }
}
