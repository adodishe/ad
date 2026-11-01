package com.auratune.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val lyrics: List<String>
)

val INITIAL_TRACKS = listOf(
    TrackItem(
        "tg-1", "Сияние Неона (Night Drive VIP)", "Klangwerk & Криоген",
        "Telegram • Канал «Lossless Synth & Retrowave»", "telegram", "Telegram FLAC",
        "FLAC 1140 kbps • 24-bit/48kHz", 214, true, 118, 55.0, "lavender",
        listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
        listOf(
            "♪ Инструментальное вступление (Аналоговый суб-бас) ♪",
            "Ночной проспект отражает огни витрин",
            "В потоке чистых частот мы летим одни",
            "Без сжатия и потерь — только тёплый звук",
            "Сквозь облака Телеграма летит сигнал"
        )
    ),
    TrackItem(
        "sc-1", "Submerged Echoes (3AM Warehouse Mix)", "velvet_shadow // Берлин",
        "SoundCloud • Underground Reposts", "soundcloud", "SoundCloud Go+",
        "AAC 256 kbps • HLS Direct", 248, true, 128, 49.0, "peach",
        listOf(Color(0xFF8A5749), Color(0xFF29201E)),
        listOf(
            "♪ Глубокий бочковой бит 128 BPM ♪",
            "Тени танцуют в бетонном зале",
            "Глубокий бас резонирует в грудной клетке",
            "SoundCloud хранит то, чего нет на радио"
        )
    ),
    TrackItem(
        "yt-1", "Космический Океан (Acoustic & Ambient)", "Аврора Севен",
        "YouTube Music • Opus 251 Audio Stream", "ytmusic", "YouTube Music",
        "Opus 160 kbps (itag 251 • Без видео)", 196, false, 92, 65.41, "sky",
        listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
        listOf(
            "♪ Пространственные аккорды и мягкий винил ♪",
            "Закрой глаза и отключи экран телефона",
            "Видеодекодер спит — работает только звук",
            "Невесомость между созвездиями лиры"
        )
    ),
    TrackItem(
        "tg-2", "Фонк Полуночи (Drift Tape 04)", "Kaito & Северный Ветер",
        "Telegram • Избранное (Saved Messages)", "telegram", "Telegram Cloud",
        "MP3 320 kbps • CBR ID3v2.4", 164, true, 140, 43.65, "sage",
        listOf(Color(0xFF4B6B51), Color(0xFF1B221C)),
        listOf(
            "♪ Перегруженный 808 суб-бас и ковбелл ♪",
            "Турбина свистит на пустой развязке",
            "Добавь +6 дБ на 60 Гц в эквалайзере",
            "Локальный кэш читается без участия сети"
        )
    ),
    TrackItem(
        "yt-2", "После Дождя в Токио (Lo-Fi Study)", "Mori & Sora Beats",
        "YouTube Music • Плейлист «Фокус и Код»", "ytmusic", "YouTube Music",
        "Opus 160 kbps (itag 251 • Audio Only)", 208, true, 84, 58.27, "lavender",
        listOf(Color(0xFF59507A), Color(0xFF1E2026)),
        listOf(
            "♪ Шум дождя и тёплое родес-пианино ♪",
            "Чашка чая остывает рядом с ноутбуком",
            "Плавный кроссфейд соединяет мгновения"
        )
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AuraTuneApp(synthEngine)
        }
    }

    override fun onDestroy() {
        synthEngine.stop()
        super.onDestroy()
    }
}

@Composable
fun AuraTuneApp(synth: SynthAudioEngine) {
    var activeTab by remember { mutableStateOf("home") }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var monetKey by remember { mutableStateOf("auto") }

    var tracks by remember { mutableStateOf(INITIAL_TRACKS) }
    var sourceFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var currentTrackId by remember { mutableStateOf(INITIAL_TRACKS[0].id) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(42) }
    var volume by remember { mutableStateOf(82f) }

    var eqEnabled by remember { mutableStateOf(true) }
    var activePreset by remember { mutableStateOf("electronic") }
    var eqGains by remember { mutableStateOf(EQ_PRESETS["electronic"]!!.gains) }
    var bassBoost by remember { mutableStateOf(55f) }
    var loudness by remember { mutableStateOf(35f) }
    var crossfadeSec by remember { mutableStateOf(4f) }

    var amoledBlack by remember { mutableStateOf(false) }
    var disableVideo by remember { mutableStateOf(true) }
    var smartBurst by remember { mutableStateOf(true) }
    var zeroWakeLock by remember { mutableStateOf(true) }
    var hwOffload by remember { mutableStateOf(true) }

    var toastText by remember { mutableStateOf<String?>(null) }

    val currentTrack = remember(tracks, currentTrackId) {
        tracks.find { it.id == currentTrackId } ?: tracks[0]
    }

    val m3 = remember(monetKey, currentTrack) {
        val key = if (monetKey == "auto") currentTrack.monetSeed else monetKey
        MONET_THEMES[key] ?: MONET_THEMES["lavender"]!!
    }

    val drainPerHour = remember(amoledBlack, disableVideo, smartBurst, zeroWakeLock, hwOffload) {
        var d = 7.8f
        if (disableVideo) d -= 2.6f
        if (smartBurst) d -= 1.5f
        if (amoledBlack) d -= 1.2f
        if (zeroWakeLock) d -= 0.7f
        if (hwOffload) d -= 0.4f
        d.coerceAtLeast(1.3f)
    }

    LaunchedEffect(isPlaying, currentTrack, eqEnabled, eqGains, bassBoost, volume) {
        synth.rootFreq = currentTrack.rootFreq
        synth.bpm = currentTrack.bpm
        synth.eqEnabled = eqEnabled
        synth.gains = eqGains.toFloatArray()
        synth.bassBoost = bassBoost
        synth.masterVol = volume / 100f
        if (isPlaying) synth.start() else synth.stop()
    }

    LaunchedEffect(isPlaying, currentTrackId) {
        while (isPlaying) {
            delay(1000L)
            if (currentTime + 1 >= currentTrack.duration) {
                val idx = tracks.indexOfFirst { it.id == currentTrackId }
                currentTrackId = tracks[(idx + 1) % tracks.size].id
                currentTime = 0
            } else {
                currentTime++
            }
        }
    }

    LaunchedEffect(toastText) {
        if (toastText != null) {
            delay(2500L)
            toastText = null
        }
    }

    val bgSurface = if (amoledBlack) Color(0xFF070709) else m3.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgSurface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Панель переключения тем Monet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "auto" to "Авто M3",
                        "lavender" to "Лаванда",
                        "sage" to "Шалфей",
                        "peach" to "Персик",
                        "sky" to "Небо"
                    ).forEach { (key, label) ->
                        val selected = monetKey == key
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) m3.secondaryContainer else m3.surfaceContainer)
                                .clickable { monetKey = key }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) m3.onSecondaryContainer else m3.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "~${String.format("%.1f", drainPerHour)}%/ч",
                        color = m3.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    "home" -> HomeTab(
                        m3 = m3,
                        tracks = tracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        sourceFilter = sourceFilter,
                        searchQuery = searchQuery,
                        activePresetName = EQ_PRESETS[activePreset]?.name ?: "Свой",
                        drainPerHour = drainPerHour,
                        onSearchChange = { searchQuery = it },
                        onFilterChange = { sourceFilter = it },
                        onSelectTrack = { t ->
                            if (t.id == currentTrackId) isPlaying = !isPlaying
                            else {
                                currentTrackId = t.id
                                currentTime = 0
                                isPlaying = true
                            }
                        },
                        onTogglePlay = { isPlaying = !isPlaying },
                        onOpenNowPlaying = { nowPlayingOpen = true },
                        onOpenSources = { activeTab = "sources" },
                        onToggleCache = { id ->
                            tracks = tracks.map {
                                if (it.id == id) {
                                    toastText = if (!it.cached) "Сохранено в кэш ОЗУ" else "Удалено из кэша"
                                    it.copy(cached = !it.cached)
                                } else it
                            }
                        }
                    )

                    "sources" -> SourcesTab(
                        m3 = m3,
                        onImportTrack = { platform ->
                            val newTrack = when (platform) {
                                "ytmusic" -> TrackItem(
                                    "yt-${System.currentTimeMillis()}",
                                    "Горизонт Событий (Opus 251 Remaster)",
                                    "Solaris Echo",
                                    "YouTube Music • Импорт",
                                    "ytmusic", "YouTube Music",
                                    "Opus 160 kbps (Без видео)",
                                    202, false, 112, 61.74, "sky",
                                    listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
                                    listOf("♪ Импортировано через Innertube без видеопотока ♪")
                                )
                                "soundcloud" -> TrackItem(
                                    "sc-${System.currentTimeMillis()}",
                                    "Tokyo Underground Dub (VIP)",
                                    "dj_kuroshio",
                                    "SoundCloud • Свежий репост",
                                    "soundcloud", "SoundCloud",
                                    "AAC 256 kbps • HLS Direct",
                                    189, true, 132, 49.0, "peach",
                                    listOf(Color(0xFF8A5749), Color(0xFF29201E)),
                                    listOf("♪ Загружено из SoundCloud Reposts ♪")
                                )
                                else -> TrackItem(
                                    "tg-${System.currentTimeMillis()}",
                                    "Белые Ночи (Lossless Vinyl Rip)",
                                    "Синтез-84",
                                    "Telegram • Избранное",
                                    "telegram", "Telegram FLAC",
                                    "FLAC 1085 kbps • TDLib",
                                    225, true, 122, 55.0, "lavender",
                                    listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
                                    listOf("♪ Найдено в Избранном Telegram ♪")
                                )
                            }
                            tracks = listOf(newTrack) + tracks
                            toastText = "Импортирован трек: ${newTrack.title}"
                        }
                    )

                    "eq" -> EqTab(
                        m3 = m3,
                        eqEnabled = eqEnabled,
                        isPlaying = isPlaying,
                        activePreset = activePreset,
                        eqGains = eqGains,
                        bassBoost = bassBoost,
                        loudness = loudness,
                        crossfadeSec = crossfadeSec,
                        onToggleEq = {
                            eqEnabled = !eqEnabled
                            toastText = if (eqEnabled) "5-полосный EQ включён" else "Эквалайзер: Bypass"
                        },
                        onTogglePlay = { isPlaying = !isPlaying },
                        onSelectPreset = { key ->
                            EQ_PRESETS[key]?.let { p ->
                                activePreset = key
                                eqGains = p.gains
                                bassBoost = p.bassBoost
                                loudness = p.loudness
                                toastText = "Пресет: ${p.name}"
                            }
                        },
                        onBandChange = { idx, v ->
                            val m = eqGains.toMutableList()
                            m[idx] = v
                            eqGains = m
                            activePreset = "custom"
                        },
                        onBassChange = { bassBoost = it },
                        onLoudnessChange = { loudness = it },
                        onCrossfadeChange = { crossfadeSec = it }
                    )

                    "battery" -> BatteryTab(
                        m3 = m3,
                        drainPerHour = drainPerHour,
                        amoledBlack = amoledBlack,
                        disableVideo = disableVideo,
                        smartBurst = smartBurst,
                        zeroWakeLock = zeroWakeLock,
                        hwOffload = hwOffload,
                        onToggleAmoled = { amoledBlack = !amoledBlack },
                        onToggleVideo = { disableVideo = !disableVideo },
                        onToggleBurst = { smartBurst = !smartBurst },
                        onToggleWake = { zeroWakeLock = !zeroWakeLock },
                        onToggleHw = { hwOffload = !hwOffload }
                    )
                }
            }

            MiniPlayerBar(
                m3 = m3,
                track = currentTrack,
                isPlaying = isPlaying,
                currentTime = currentTime,
                onOpen = { nowPlayingOpen = true },
                onTogglePlay = { isPlaying = !isPlaying },
                onNext = {
                    val idx = tracks.indexOfFirst { it.id == currentTrackId }
                    currentTrackId = tracks[(idx + 1) % tracks.size].id
                    currentTime = 0
                }
            )

            BottomNavBar(
                m3 = m3,
                activeTab = activeTab,
                onSelectTab = {
                    activeTab = it
                    nowPlayingOpen = false
                }
            )
        }

        if (toastText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 56.dp, start = 20.dp, end = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(m3.surfaceContainerHighest)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(text = toastText!!, color = m3.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        AnimatedVisibility(
            visible = nowPlayingOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            FullNowPlayingSheet(
                m3 = m3,
                track = currentTrack,
                isPlaying = isPlaying,
                currentTime = currentTime,
                volume = volume,
                showLyrics = showLyrics,
                presetName = EQ_PRESETS[activePreset]?.name ?: "Свой",
                onClose = { nowPlayingOpen = false },
                onToggleLyrics = { showLyrics = !showLyrics },
                onSeek = { currentTime = it },
                onVolume = { volume = it },
                onTogglePlay = { isPlaying = !isPlaying },
                onPrev = {
                    val idx = tracks.indexOfFirst { it.id == currentTrackId }
                    currentTrackId = tracks[(idx - 1 + tracks.size) % tracks.size].id
                    currentTime = 0
                },
                onNext = {
                    val idx = tracks.indexOfFirst { it.id == currentTrackId }
                    currentTrackId = tracks[(idx + 1) % tracks.size].id
                    currentTime = 0
                },
                onOpenEq = {
                    nowPlayingOpen = false
                    activeTab = "eq"
                }
            )
        }
    }
}

@Composable
fun HomeTab(
    m3: M3Palette,
    tracks: List<TrackItem>,
    currentTrack: TrackItem,
    isPlaying: Boolean,
    sourceFilter: String,
    searchQuery: String,
    activePresetName: String,
    drainPerHour: Float,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onSelectTrack: (TrackItem) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenSources: () -> Unit,
    onToggleCache: (String) -> Unit
) {
    val filtered = remember(tracks, sourceFilter, searchQuery) {
        tracks.filter { t ->
            val matchSource = when (sourceFilter) {
                "all" -> true
                "cached" -> t.cached
                else -> t.source == sourceFilter
            }
            val q = searchQuery.trim().lowercase()
            val matchQ = q.isEmpty() || t.title.lowercase().contains(q) || t.artist.lowercase().contains(q)
            matchSource && matchQ
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ArchiveTune • Material You", color = m3.onSurfaceVariant, fontSize = 11.sp)
                    Text("Моя медиатека", color = m3.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.surfaceContainerHigh)
                        .clickable { onOpenSources() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("☁ 3 облака", color = m3.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Поиск в YT Music, SoundCloud и Telegram...", fontSize = 12.sp) },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = m3.surfaceContainerHigh,
                    unfocusedContainerColor = m3.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = m3.onSurface,
                    unfocusedTextColor = m3.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(m3.surfaceContainer)
                    .clickable { onOpenNowPlaying() }
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(currentTrack.coverColors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💿", fontSize = 28.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(currentTrack.sourceBadge, color = m3.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(currentTrack.title, color = m3.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentTrack.artist, color = m3.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(currentTrack.codec, color = m3.onSurfaceVariant.copy(alpha = 0.75f), fontSize = 10.sp, maxLines = 1)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(m3.primaryContainer)
                            .clickable { onTogglePlay() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isPlaying) "⏸" else "▶", color = m3.onPrimaryContainer, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Профиль EQ: $activePresetName", color = m3.onSurfaceVariant, fontSize = 11.sp)
                    val saved = (((7.8f - drainPerHour) / 7.8f) * 100).toInt()
                    Text("⚡ -$saved% расхода АКБ", color = m3.primary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "all" to "Все потоки (${tracks.size})",
                    "ytmusic" to "YouTube Music",
                    "soundcloud" to "SoundCloud",
                    "telegram" to "Telegram",
                    "cached" to "Офлайн-кэш"
                ).forEach { (id, title) ->
                    val active = sourceFilter == id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) m3.secondaryContainer else m3.surfaceContainerLow)
                            .clickable { onFilterChange(id) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (active) m3.onSecondaryContainer else m3.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        items(filtered, key = { it.id }) { track ->
            val isCurr = track.id == currentTrack.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isCurr) m3.surfaceContainerHigh else m3.surfaceContainerLow)
                    .clickable { onSelectTrack(track) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(track.coverColors)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isCurr && isPlaying) "♫" else "♪", color = Color.White, fontSize = 18.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = if (isCurr) m3.primary else m3.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(track.artist, color = m3.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.sourceBadge} • ${track.codec}", color = m3.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp, maxLines = 1)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (track.cached) m3.secondaryContainer else m3.surfaceContainer)
                        .clickable { onToggleCache(track.id) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (track.cached) "✓ Кэш" else "↓",
                        color = if (track.cached) m3.onSecondaryContainer else m3.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }

                Text(formatSec(track.duration), color = m3.onSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun SourcesTab(m3: M3Palette, onImportTrack: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Облачный мост • Прямой аудиопоток", color = m3.onSurfaceVariant, fontSize = 11.sp)
        Text("Источники музыки", color = m3.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        SourceCard(
            m3 = m3,
            title = "YouTube Music",
            subtitle = "Innertube OAuth2 • Opus 251 Audio-Only (Без видео)",
            stats = "418 треков синхронизировано • SponsorBlock активен",
            onSync = { onImportTrack("ytmusic") }
        )
        SourceCard(
            m3 = m3,
            title = "SoundCloud",
            subtitle = "API v2 HLS Direct • Лайки и андеграунд-репосты",
            stats = "194 трека в медиатеке • Прямое кэширование",
            onSync = { onImportTrack("soundcloud") }
        )
        SourceCard(
            m3 = m3,
            title = "Telegram Cloud Audio",
            subtitle = "TDLib Мост • Избранное (Saved Messages) + Каналы",
            stats = "Каналы: @LosslessSynth, @PhonkArchiveRU, @IndieFlacCloud",
            onSync = { onImportTrack("telegram") }
        )
    }
}

@Composable
fun SourceCard(m3: M3Palette, title: String, subtitle: String, stats: String, onSync: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(m3.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = m3.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = m3.onSurfaceVariant, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(m3.primaryContainer)
                    .clickable { onSync() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Импорт +1", color = m3.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(stats, color = m3.primary, fontSize = 11.sp)
    }
}

@Composable
fun EqTab(
    m3: M3Palette,
    eqEnabled: Boolean,
    isPlaying: Boolean,
    activePreset: String,
    eqGains: List<Float>,
    bassBoost: Float,
    loudness: Float,
    crossfadeSec: Float,
    onToggleEq: () -> Unit,
    onTogglePlay: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onBassChange: (Float) -> Unit,
    onLoudnessChange: (Float) -> Unit,
    onCrossfadeChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Аппаратный DSP • 64-bit Float", color = m3.onSurfaceVariant, fontSize = 11.sp)
                Text("5-полосный Эквалайзер", color = m3.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.primary)
                        .clickable { onTogglePlay() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(if (isPlaying) "⏸ Пауза" else "▶ Тест звука", color = m3.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (eqEnabled) m3.primaryContainer else m3.surfaceContainerHigh)
                        .clickable { onToggleEq() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (eqEnabled) "Активен" else "Bypass",
                        color = if (eqEnabled) m3.onPrimaryContainer else m3.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EQ_PRESETS.forEach { (key, preset) ->
                val selected = activePreset == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) m3.secondaryContainer else m3.surfaceContainerLow)
                        .clickable { onSelectPreset(key) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = preset.name,
                        color = if (selected) m3.onSecondaryContainer else m3.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(m3.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Кривая отклика частот (АЧХ)", color = m3.onSurfaceVariant, fontSize = 11.sp)
                Text("-12 дБ … +12 дБ", color = m3.primary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 2f
                )

                val pts = eqGains.mapIndexed { idx, gain ->
                    val x = 24f + idx * ((w - 48f) / 4f)
                    val y = midY - (gain / 12f) * (midY - 12f)
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(pts.first().x, pts.first().y)
                    for (i in 0 until pts.size - 1) {
                        val curr = pts[i]
                        val next = pts[i + 1]
                        val cx = (curr.x + next.x) / 2f
                        cubicTo(cx, curr.y, cx, next.y, next.x, next.y)
                    }
                }

                drawPath(
                    path = path,
                    color = if (eqEnabled) m3.primary else m3.outlineVariant,
                    style = Stroke(width = 6f)
                )

                pts.forEach { pt ->
                    drawCircle(
                        color = if (eqEnabled) m3.primary else m3.onSurfaceVariant,
                        radius = 10f,
                        center = pt
                    )
                }
            }

            EQ_BAND_LABELS.forEachIndexed { idx, label ->
                val g = eqGains[idx]
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, color = m3.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${if (g > 0) "+" else ""}${String.format("%.1f", g)} дБ",
                            color = m3.primary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = g,
                        onValueChange = { onBandChange(idx, it) },
                        valueRange = -12f..12f,
                        colors = SliderDefaults.colors(
                            thumbColor = m3.primary,
                            activeTrackColor = m3.primary,
                            inactiveTrackColor = m3.surfaceContainerHighest
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(m3.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Динамическая обработка (DSP)", color = m3.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text("Усиление суб-баса (85 Гц): ${bassBoost.toInt()}%", color = m3.onSurface, fontSize = 12.sp)
            Slider(value = bassBoost, onValueChange = onBassChange, valueRange = 0f..100f)

            Text("Тонкомпенсация (Loudness): ${loudness.toInt()}%", color = m3.onSurface, fontSize = 12.sp)
            Slider(value = loudness, onValueChange = onLoudnessChange, valueRange = 0f..100f)

            Text("Кроссфейд между треками: ${crossfadeSec.toInt()} сек", color = m3.onSurface, fontSize = 12.sp)
            Slider(value = crossfadeSec, onValueChange = onCrossfadeChange, valueRange = 0f..12f)
        }
    }
}

@Composable
fun BatteryTab(
    m3: M3Palette,
    drainPerHour: Float,
    amoledBlack: Boolean,
    disableVideo: Boolean,
    smartBurst: Boolean,
    zeroWakeLock: Boolean,
    hwOffload: Boolean,
    onToggleAmoled: () -> Unit,
    onToggleVideo: () -> Unit,
    onToggleBurst: () -> Unit,
    onToggleWake: () -> Unit,
    onToggleHw: () -> Unit
) {
    val mah = (drainPerHour * 48).toInt()
    val hours = (100f / drainPerHour).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Android Media3 ExoPlayer • Low-Power", color = m3.onSurfaceVariant, fontSize = 11.sp)
        Text("Энергоэффективность", color = m3.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(m3.surfaceContainerHigh)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Текущее потребление в фоне", color = m3.primary, fontSize = 12.sp)
            Text("~${String.format("%.1f", drainPerHour)}% / час ($mah мА·ч)", color = m3.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text("Прогноз автономности: до $hours часов на 1 заряде", color = m3.onSurfaceVariant, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(m3.surfaceContainer)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BatterySwitchRow(m3, "Приглушённый AMOLED-режим (Pure Black)", "Выключает пиксели OLED (-1.2%/ч)", amoledBlack, onToggleAmoled)
            BatterySwitchRow(m3, "Блокировка видеопотока (Opus 251)", "Видеодекодер спит (-2.6%/ч)", disableVideo, onToggleVideo)
            BatterySwitchRow(m3, "Пакетная буферизация в ОЗУ", "Модем спит 88% времени (-1.5%/ч)", smartBurst, onToggleBurst)
            BatterySwitchRow(m3, "Заморозка UI при выключенном экране", "0 FPS при блокировке (-0.7%/ч)", zeroWakeLock, onToggleWake)
            BatterySwitchRow(m3, "Аппаратный Audio DSP Offload", "Расчёт 5 полос EQ на чипе (-0.4%/ч)", hwOffload, onToggleHw)
        }
    }
}

@Composable
fun BatterySwitchRow(m3: M3Palette, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = m3.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = m3.onSurfaceVariant, fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}

@Composable
fun MiniPlayerBar(
    m3: M3Palette,
    track: TrackItem,
    isPlaying: Boolean,
    currentTime: Int,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(m3.surfaceContainerHigh)
            .clickable { onOpen() }
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(track.coverColors)),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", color = Color.White, fontSize = 18.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = m3.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${track.artist} • ${track.sourceBadge}", color = m3.primary, fontSize = 11.sp, maxLines = 1)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(m3.primaryContainer)
                    .clickable { onTogglePlay() },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isPlaying) "⏸" else "▶", color = m3.onPrimaryContainer, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Text("⏭", color = m3.onSurfaceVariant, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun BottomNavBar(m3: M3Palette, activeTab: String, onSelectTab: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(m3.surfaceContainer)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(
            Triple("home", "Главная", "🎵"),
            Triple("sources", "Источники", "☁"),
            Triple("eq", "Звук / EQ", "🎛"),
            Triple("battery", "Батарея", "🔋")
        ).forEach { (id, label, icon) ->
            val selected = activeTab == id
            Column(
                modifier = Modifier
                    .clickable { onSelectTab(id) }
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(30.dp)
                        .clip(CircleShape)
                        .background(if (selected) m3.secondaryContainer else Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 14.sp)
                }
                Text(
                    text = label,
                    color = if (selected) m3.onSurface else m3.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FullNowPlayingSheet(
    m3: M3Palette,
    track: TrackItem,
    isPlaying: Boolean,
    currentTime: Int,
    volume: Float,
    showLyrics: Boolean,
    presetName: String,
    onClose: () -> Unit,
    onToggleLyrics: () -> Unit,
    onSeek: (Int) -> Unit,
    onVolume: (Float) -> Unit,
    onTogglePlay: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onOpenEq: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(m3.surface)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(m3.surfaceContainerHigh)
                    .clickable { onClose() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("▼ Свернуть", color = m3.onSurface, fontSize = 12.sp)
            }

            Text(track.sourceBadge, color = m3.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (showLyrics) m3.secondaryContainer else m3.surfaceContainerHigh)
                    .clickable { onToggleLyrics() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("Текст", color = if (showLyrics) m3.onSecondaryContainer else m3.onSurfaceVariant, fontSize = 12.sp)
            }
        }

        if (!showLyrics) {
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Brush.linearGradient(track.coverColors)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💿", fontSize = 60.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(track.codec, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(m3.surfaceContainerLow)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                track.lyrics.forEach { line ->
                    Text(
                        text = line,
                        color = m3.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text(track.title, color = m3.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(track.artist, color = m3.onSurfaceVariant, fontSize = 14.sp, maxLines = 1)
                Text(track.album, color = m3.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
            }

            Column {
                Slider(
                    value = currentTime.toFloat(),
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..track.duration.toFloat()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatSec(currentTime), color = m3.onSurfaceVariant, fontSize = 11.sp)
                    Text("EQ: $presetName", color = m3.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("-${formatSec(track.duration - currentTime)}", color = m3.onSurfaceVariant, fontSize = 11.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(m3.surfaceContainerHigh)
                        .clickable { onPrev() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏮", color = m3.onSurface, fontSize = 20.sp)
                }

                Box(
                    modifier = Modifier
                        .width(84.dp)
                        .height(64.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(m3.primaryContainer)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isPlaying) "⏸" else "▶", color = m3.onPrimaryContainer, fontSize = 26.sp)
                }

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(m3.surfaceContainerHigh)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏭", color = m3.onSurface, fontSize = 20.sp)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔊", fontSize = 14.sp)
                Slider(
                    value = volume,
                    onValueChange = onVolume,
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.secondaryContainer)
                        .clickable { onOpenEq() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🎛 5-EQ", color = m3.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
