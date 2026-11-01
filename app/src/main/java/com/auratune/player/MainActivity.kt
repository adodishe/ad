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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
    val surfaceDim: Color,
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
        id = "lavender", name = "Лаванда",
        surface = Color(0xFF141318), surfaceDim = Color(0xFF0E0D12),
        surfaceContainerLow = Color(0xFF1C1B20), surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2931), surfaceContainerHighest = Color(0xFF36343C),
        primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
        secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
        onSurface = Color(0xFFE6E1E5), onSurfaceVariant = Color(0xFFCAC4D0),
        outlineVariant = Color(0xFF49454F)
    ),
    "sage" to M3Palette(
        id = "sage", name = "Шалфей",
        surface = Color(0xFF121412), surfaceDim = Color(0xFF0C0E0C),
        surfaceContainerLow = Color(0xFF1A1C1A), surfaceContainer = Color(0xFF1E211E),
        surfaceContainerHigh = Color(0xFF282B28), surfaceContainerHighest = Color(0xFF333633),
        primary = Color(0xFFB4D0B8), onPrimary = Color(0xFF213626),
        primaryContainer = Color(0xFF374D3B), onPrimaryContainer = Color(0xFFD0ECD3),
        secondaryContainer = Color(0xFF3D4A3F), onSecondaryContainer = Color(0xFFD7E6D8),
        onSurface = Color(0xFFE2E3DF), onSurfaceVariant = Color(0xFFC2C8C1),
        outlineVariant = Color(0xFF424842)
    ),
    "peach" to M3Palette(
        id = "peach", name = "Терракота",
        surface = Color(0xFF171312), surfaceDim = Color(0xFF110D0C),
        surfaceContainerLow = Color(0xFF201B19), surfaceContainer = Color(0xFF251F1D),
        surfaceContainerHigh = Color(0xFF302927), surfaceContainerHighest = Color(0xFF3B3331),
        primary = Color(0xFFF5B8A7), onPrimary = Color(0xFF4C251B),
        primaryContainer = Color(0xFF663B2F), onPrimaryContainer = Color(0xFFFFDBD1),
        secondaryContainer = Color(0xFF56423C), onSecondaryContainer = Color(0xFFF5DDD6),
        onSurface = Color(0xFFECE0DD), onSurfaceVariant = Color(0xFFD7C2BC),
        outlineVariant = Color(0xFF52443F)
    ),
    "sky" to M3Palette(
        id = "sky", name = "Дымчатый",
        surface = Color(0xFF121417), surfaceDim = Color(0xFF0B0E11),
        surfaceContainerLow = Color(0xFF1A1C20), surfaceContainer = Color(0xFF1E2025),
        surfaceContainerHigh = Color(0xFF282A2F), surfaceContainerHighest = Color(0xFF33353B),
        primary = Color(0xFFAFC8E8), onPrimary = Color(0xFF19324B),
        primaryContainer = Color(0xFF314863), onPrimaryContainer = Color(0xFFD4E4FF),
        secondaryContainer = Color(0xFF3E4756), onSecondaryContainer = Color(0xFFDAE3F3),
        onSurface = Color(0xFFE2E2E6), onSurfaceVariant = Color(0xFFC3C6CF),
        outlineVariant = Color(0xFF43474E)
    )
)

data class LyricLine(val time: Int, val text: String)

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
    val liked: Boolean,
    val bpm: Int,
    val rootFreq: Double,
    val monetSeed: String,
    val coverColors: List<Color>,
    val lyrics: List<LyricLine>
)

val INITIAL_TRACKS = listOf(
    TrackItem(
        id = "tg-1",
        title = "Сияние Неона (Night Drive VIP)",
        artist = "Klangwerk & Криоген",
        album = "Telegram • Канал «Lossless Synth & Retrowave»",
        source = "telegram",
        sourceBadge = "Telegram FLAC",
        codec = "FLAC 1140 kbps • 24-bit/48kHz",
        duration = 214,
        cached = true,
        liked = true,
        bpm = 118,
        rootFreq = 55.0,
        monetSeed = "lavender",
        coverColors = listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
        lyrics = listOf(
            LyricLine(0, "♪ Инструментальное вступление (Аналоговый суб-бас) ♪"),
            LyricLine(18, "Ночной проспект отражает огни витрин"),
            LyricLine(36, "В потоке чистых частот мы летим одни"),
            LyricLine(58, "Без сжатия и потерь — только тёплый звук"),
            LyricLine(82, "Экран погас, но ритм продолжает стук"),
            LyricLine(115, "Сквозь облака Телеграма летит сигнал"),
            LyricLine(148, "На частоте шестьдесят герц задрожит квартал")
        )
    ),
    TrackItem(
        id = "sc-1",
        title = "Submerged Echoes (3AM Warehouse Mix)",
        artist = "velvet_shadow // Берлин",
        album = "SoundCloud • Underground Reposts",
        source = "soundcloud",
        sourceBadge = "SoundCloud Go+",
        codec = "AAC 256 kbps • HLS Direct",
        duration = 248,
        cached = true,
        liked = true,
        bpm = 128,
        rootFreq = 49.0,
        monetSeed = "peach",
        coverColors = listOf(Color(0xFF8A5749), Color(0xFF29201E)),
        lyrics = listOf(
            LyricLine(0, "♪ Глубокий бочковой бит 128 BPM ♪"),
            LyricLine(24, "Тени танцуют в бетонном зале"),
            LyricLine(52, "Глубокий бас резонирует в грудной клетке"),
            LyricLine(90, "SoundCloud хранит то, чего нет на радио")
        )
    ),
    TrackItem(
        id = "yt-1",
        title = "Космический Океан (Acoustic & Ambient)",
        artist = "Аврора Севен",
        album = "YouTube Music • Opus 251 Audio Stream",
        source = "ytmusic",
        sourceBadge = "YouTube Music",
        codec = "Opus 160 kbps (itag 251 • Без видео)",
        duration = 196,
        cached = false,
        liked = true,
        bpm = 92,
        rootFreq = 65.41,
        monetSeed = "sky",
        coverColors = listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
        lyrics = listOf(
            LyricLine(0, "♪ Пространственные аккорды и мягкий винил ♪"),
            LyricLine(20, "Закрой глаза и отключи экран телефона"),
            LyricLine(45, "Видеодекодер спит — работает только звук"),
            LyricLine(78, "Невесомость между созвездиями лиры")
        )
    ),
    TrackItem(
        id = "tg-2",
        title = "Фонк Полуночи (Drift Tape 04)",
        artist = "Kaito & Северный Ветер",
        album = "Telegram • Избранное (Saved Messages)",
        source = "telegram",
        sourceBadge = "Telegram Cloud",
        codec = "MP3 320 kbps • CBR ID3v2.4",
        duration = 164,
        cached = true,
        liked = false,
        bpm = 140,
        rootFreq = 43.65,
        monetSeed = "sage",
        coverColors = listOf(Color(0xFF4B6B51), Color(0xFF1B221C)),
        lyrics = listOf(
            LyricLine(0, "♪ Перегруженный 808 суб-бас и ковбелл ♪"),
            LyricLine(16, "Турбина свистит на пустой развязке"),
            LyricLine(40, "Добавь +6 дБ на 60 Гц в эквалайзере"),
            LyricLine(75, "Локальный кэш читается без участия сети")
        )
    ),
    TrackItem(
        id = "yt-2",
        title = "После Дождя в Токио (Lo-Fi Study Session)",
        artist = "Mori & Sora Beats",
        album = "YouTube Music • Плейлист «Фокус и Код»",
        source = "ytmusic",
        sourceBadge = "YouTube Music",
        codec = "Opus 160 kbps (itag 251 • Audio Only)",
        duration = 208,
        cached = true,
        liked = true,
        bpm = 84,
        rootFreq = 58.27,
        monetSeed = "lavender",
        coverColors = listOf(Color(0xFF59507A), Color(0xFF1E2026)),
        lyrics = listOf(
            LyricLine(0, "♪ Шум дождя и тёплое родес-пианино ♪"),
            LyricLine(30, "Чашка чая остывает рядом с ноутбуком"),
            LyricLine
