package com.auratune.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

data class M3Palette(
    val id: String,
    val name: String,
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outlineVariant: Color
)

val MONET_THEMES = mapOf(
    "lavender" to M3Palette(
        "lavender", "Лаванда",
        Color(0xFF141318), Color(0xFF1C1B20), Color(0xFF211F26),
        Color(0xFF2B2931), Color(0xFF36343C), Color(0xFFD0BCFF),
        Color(0xFF381E72), Color(0xFF4F378B), Color(0xFFEADDFF),
        Color(0xFF4A4458), Color(0xFFE8DEF8), Color(0xFFE6E1E5),
        Color(0xFFCAC4D0), Color(0xFF49454F)
    ),
    "sage" to M3Palette(
        "sage", "Шалфей",
        Color(0xFF121412), Color(0xFF1A1C1A), Color(0xFF1E211E),
        Color(0xFF282B28), Color(0xFF333633), Color(0xFFB4D0B8),
        Color(0xFF213626), Color(0xFF374D3B), Color(0xFFD0ECD3),
        Color(0xFF3D4A3F), Color(0xFFD7E6D8), Color(0xFFE2E3DF),
        Color(0xFFC2C8C1), Color(0xFF424842)
    ),
    "peach" to M3Palette(
        "peach", "Терракота",
        Color(0xFF171312), Color(0xFF201B19), Color(0xFF251F1D),
        Color(0xFF302927), Color(0xFF3B3331), Color(0xFFF5B8A7),
        Color(0xFF4C251B), Color(0xFF663B2F), Color(0xFFFFDBD1),
        Color(0xFF56423C), Color(0xFFF5DDD6), Color(0xFFECE0DD),
        Color(0xFFD7C2BC), Color(0xFF52443F)
    ),
    "sky" to M3Palette(
        "sky", "Дымчатый",
        Color(0xFF121417), Color(0xFF1A1C20), Color(0xFF1E2025),
        Color(0xFF282A2F), Color(0xFF33353B), Color(0xFFAFC8E8),
        Color(0xFF19324B), Color(0xFF314863), Color(0xFFD4E4FF),
        Color(0xFF3E4756), Color(0xFFDAE3F3), Color(0xFFE2E2E6),
        Color(0xFFC3C6CF), Color(0xFF43474E)
    )
)

data class TrackItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val source: String,
    val sourceBadge: String,
    val codec: String,
    val duration: Int,
    val cached: Boolean,
    val bpm: Int,
    val rootFreq: Double,
    val monetSeed: String,
    val coverColors: List<Color>,
    val lyrics: List<String>,
    val streamUrl: String? = null
)

val INITIAL_TRACKS = listOf(
    TrackItem(
        "live-synth", "Nightride FM — 24/7 Synthwave & Retrowave", "Nightride Radio",
        "Прямой эфир • 320 kbps Icecast Stream", "ytmusic", "Live Stream",
        "MP3 320 kbps • Настоящий 24/7 поток", 3600, false, 118, 55.0, "lavender",
        listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
        listOf("♪ Прямой эфир Nightride FM Synthwave ♪", "Реальный интернет-поток без заглушек"),
        streamUrl = "https://stream.nightride.fm/nightride.m4a"
    ),
    TrackItem(
        "live-chillsynth", "Chillsynth FM — Lo-Fi & Ambient Synth", "Chillsynth Radio",
        "Прямой эфир • Audio-Only Stream", "soundcloud", "SoundCloud Live",
        "AAC+ • Реальный поток", 3600, false, 95, 58.27, "sky",
        listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
        listOf("♪ Прямой эфир Chillsynth FM ♪"),
        streamUrl = "https://stream.nightride.fm/chillsynth.m4a"
    ),
    TrackItem(
        "tg-1", "Сияние Неона (DSP Synth VIP)", "Klangwerk & Криоген",
        "Telegram • Канал «Lossless Synth & Retrowave»", "telegram", "Telegram FLAC",
        "FLAC 1140 kbps • Встроенный DSP-синтезатор", 214, true, 118, 55.0, "sky",
        listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
        listOf(
            "♪ Встроенный процедурный 5-полосный синтезатор ♪",
            "Откройте вкладку «Источники» для импорта или выберите MP3 с телефона"
        ),
        streamUrl = null
    )
)

data class EqPreset(val name: String, val gains: List<Float>, val bassBoost: Float, val loudness: Float)

val EQ_PRESETS = mapOf(
    "flat" to EqPreset("Плоский", listOf(0f, 0f, 0f, 0f, 0f), 0f, 0f),
    "deep_bass" to EqPreset("Глубокий Бас", listOf(7.5f, 5.0f, -1.5f, 1.5f, 3.0f), 65f, 25f),
    "electronic" to EqPreset("Электроника / Phonk", listOf(6.5f, 3.0f, -2.5f, 4.0f, 6.0f), 55f, 35f),
    "vocal" to EqPreset("Вокал", listOf(-3.0f, -1.0f, 4.5f, 5.0f, 1.5f), 0f, 40f),
    "night" to EqPreset("Ночной", listOf(-4.5f, 1.0f, 2.0f, -2.0f, -4.0f), 10f, 50f)
)

val EQ_BAND_LABELS = listOf(
    "60 Гц (Суб-бас)",
    "230 Гц (Низкие)",
    "910 Гц (Средние)",
    "3.6 кГц (Верх. ср.)",
    "14 кГц (Высокие)"
)

fun formatSec(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    val m = s / 60
    val rem = s % 60
    return "$m:${if (rem < 10) "0" else ""}$rem"
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    var name = "Локальный аудиофайл"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) {
            name = cursor.getString(idx).substringBeforeLast(".")
        }
    }
    return name
}

class SynthAudioEngine {
    @Volatile var isRunning = false
    @Volatile var rootFreq = 55.0
    @Volatile var bpm = 120
    @Volatile var eqEnabled = true
    @Volatile var gains = floatArrayOf(6.5f, 3.0f, -2.5f, 4.0f, 6.0f)
    @Volatile var bassBoost = 55f
    @Volatile var masterVol = 0.82f

    fun start() {
        if (isRunning) return
        isRunning = true
        thread(start = true) {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf.coerceAtLeast(2048))
                .build()

            track.play()
            val buf = ShortArray(1024)
            var sampleIdx = 0L

            while (isRunning) {
                val f0 = rootFreq
                val stepSamples = ((60.0 / bpm / 2.0) * sampleRate).toInt().coerceAtLeast(1000)

                val g0 = if (eqEnabled) 10.0.pow((gains[0] + (bassBoost / 100f) * 6f) / 20.0) else 1.0
                val g1 = if (eqEnabled) 10.0.pow(gains[1] / 20.0) else 1.0
                val g2 = if (eqEnabled) 10.0.pow(gains[2] / 20.0) else 1.0
                val g3 = if (eqEnabled) 10.0.pow(gains[3] / 20.0) else 1.0
                val g4 = if (eqEnabled) 10.0.pow(gains[4] / 20.0) else 1.0

                for (i in buf.indices) {
                    val t = sampleIdx.toDouble() / sampleRate
                    val step = (sampleIdx / stepSamples).toInt()
                    val posInStep = (sampleIdx % stepSamples).toDouble() / stepSamples
                    val env = (1.0 - posInStep).pow(1.8)

                    val chordMult = when ((step / 8) % 4) {
                        0 -> 1.0
                        1 -> 1.1892
                        2 -> 1.3348
                        else -> 1.4983
                    }
                    val base = f0 * chordMult

                    val sub = sin(2.0 * PI * base * t) * env * 0.22 * g0
                    val low = sin(2.0 * PI * (base * 2.0) * t) * 0.12 * g1
                    val mid = sin(2.0 * PI * (base * 4.0) * t) * 0.08 * g2
                    val arpFreq = base * (if (step % 2 == 0) 6.0 else 8.0)
                    val highMid = sin(2.0 * PI * arpFreq * t) * env * 0.06 * g3
                    val hat = if (posInStep < 0.15) (Random.nextDouble(-1.0, 1.0) * 0.03 * g4) else 0.0

                    val mix = ((sub + low + mid + highMid + hat) * masterVol).coerceIn(-0.95, 0.95)
                    buf[i] = (mix * Short.MAX_VALUE).toInt().toShort()
                    sampleIdx++
                }
                track.write(buf, 0, buf.size)
            }
            track.stop()
            track.release()
        }
    }

    fun stop() {
        isRunning = false
    }
}

class MainActivity : ComponentActivity() {
    private val synthEngine = SynthAudioEngine()
    private lateinit var exoPlayer: ExoPlayer
    private var systemEq: Equalizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId != 0) {
                    runCatching {
                        systemEq?.release()
                        systemEq = Equalizer(0, audioSessionId).apply { enabled = true }
                    }
                }
            }
        })

        setContent {
            AuraTuneApp(
                synth = synthEngine,
                exoPlayer = exoPlayer,
                onApplyHardwareEq = { enabled, gains ->
                    runCatching {
                        systemEq?.let { eq ->
                            eq.enabled = enabled
