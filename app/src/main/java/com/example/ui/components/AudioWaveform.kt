package com.example.ui.components

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BlueAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

@Composable
fun AudioWaveformPlayer(
    durationSeconds: Int,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    // Tone synthesizer for realistic audio feedback
    fun playPreviewTone() {
        scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 8000
                val numSamples = sampleRate / 2
                val sample = DoubleArray(numSamples)
                val generatedSnd = ByteArray(2 * numSamples)
                val freq = 440.0 // A4 tone pitch

                for (i in 0 until numSamples) {
                    sample[i] = sin(2 * Math.PI * i / (sampleRate / freq))
                }

                var idx = 0
                for (dVal in sample) {
                    val valShort = (dVal * 32767).toInt().toShort()
                    generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
                    generatedSnd[idx++] = (valShort.toInt() and 0xff00 ushr 8).toByte()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(generatedSnd.size)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                delay(400)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            playPreviewTone()
            val totalSteps = 100
            val stepDelay = (durationSeconds * 1000L) / totalSteps
            while (isPlaying && currentProgress < 1f) {
                delay(stepDelay.coerceAtLeast(30L))
                currentProgress += 0.01f
            }
            if (currentProgress >= 1f) {
                isPlaying = false
                currentProgress = 0f
            }
        }
    }

    val bars = remember(durationSeconds) {
        val random = java.util.Random(durationSeconds.toLong())
        List(barCount) { 0.2f + random.nextFloat() * 0.8f }
    }

    val animatedHeights = bars.mapIndexed { index, baseHeight ->
        val barPos = index.toFloat() / barCount
        val isPassed = barPos <= currentProgress
        if (isPlaying && isPassed) {
            val infiniteTransition = rememberInfiniteTransition(label = "bar_$index")
            val scale by infiniteTransition.animateFloat(
                initialValue = baseHeight * 0.7f,
                targetValue = (baseHeight * 1.3f).coerceAtMost(1f),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300 + (index * 40) % 200, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            scale
        } else {
            baseHeight
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                if (currentProgress >= 1f) currentProgress = 0f
                isPlaying = !isPlaying
            },
            modifier = Modifier
                .size(42.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(IndigoPrimary, VioletSecondary)
                    ),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clickable {
                    isPlaying = false
                    currentProgress = (currentProgress + 0.25f) % 1f
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = (size.width / barCount) * 0.65f
                val gap = (size.width / barCount) * 0.35f

                animatedHeights.forEachIndexed { i, h ->
                    val x = i * (barWidth + gap)
                    val barHeight = size.height * h
                    val y = (size.height - barHeight) / 2
                    val progressPos = currentProgress * barCount

                    val barColor = if (i <= progressPos) {
                        BlueAccent
                    } else {
                        Color.Gray.copy(alpha = 0.35f)
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        val currentSec = (currentProgress * durationSeconds).toInt()
        Text(
            text = "${currentSec / 60}:${(currentSec % 60).toString().padStart(2, '0')} / ${durationSeconds / 60}:${(durationSeconds % 60).toString().padStart(2, '0')}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
