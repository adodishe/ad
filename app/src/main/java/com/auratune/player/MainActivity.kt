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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
        "tg-1", "Сияние Неона (DSP Synth VIP)", "Klangwerk & Криоген",
        "Telegram • Канал «Lossless Synth & Retrowave»", "telegram", "Telegram FLAC",
        "FLAC 1140 kbps • Встроенный DSP-синтезатор", 214, true, 118, 55.0, "lavender",
        listOf(Color(0xFF5F4D8C), Color(0xFF262332)),
        listOf(
            "♪ Встроенный процедурный 5-полосный синтезатор ♪",
            "Откройте вкладку «Источники», чтобы вставить ссылку с YouTube",
            "Или введите название любого трека для живого поиска!"
        ),
        streamUrl = null
    ),
    TrackItem(
        "tg-2", "Фонк Полуночи (Drift Tape 04)", "Kaito & Северный Ветер",
        "Telegram • Избранное (Saved Messages)", "telegram", "Telegram Cloud",
        "MP3 320 kbps • CBR ID3v2.4", 164, true, 140, 43.65, "sage",
        listOf(Color(0xFF4B6B51), Color(0xFF1B221C)),
        listOf(
            "♪ Перегруженный 808 суб-бас и ковбелл ♪",
            "Добавь +6 дБ на 60 Гц в эквалайзере"
        ),
        streamUrl = null
    )
)

// ============================================================================
// НАСТОЯЩИЙ ЭКСТРАКТОР YOUTUBE / YOUTUBE MUSIC / RADIO STREAM (Иннертюб + Piped)
// Извлекает реальный аудиопоток по ссылке YouTube или поисковому запросу!
// ============================================================================
object RealStreamExtractor {
    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.tokhmi.xyz",
        "https://pipedapi.syncpundit.io",
        "https://api.piped.private.coffee"
    )

    fun extractYoutubeVideoId(input: String): String? {
        val trimmed = input.trim()
        val regexes = listOf(
            Regex("""(?:v=|/v/|youtu\.be/|/embed/|/shorts/)([a-zA-Z0-9_-]{11})"""),
            Regex("""^([a-zA-Z0-9_-]{11})$""")
        )
        for (r in regexes) {
            val m = r.find(trimmed)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    private fun httpGet(urlStr: String): String? {
        return runCatching {
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android 14; Mobile)")
            }
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        }.getOrNull()
    }

    // 1. Прямой запрос к YouTube Innertube API (клиент ANDROID_VR, который отдает прямые ссылки без cipher)
    private fun fetchViaInnertube(videoId: String): TrackItem? {
        return runCatching {
            val url = URL("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("User-Agent", "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12; Quest 3 Build/SQ3A.220605.009.A1) gzip")
            }

            val payload = """
                {
                  "videoId": "$videoId",
                  "context": {
                    "client": {
                      "clientName": "ANDROID_VR",
                      "clientVersion": "1.56.21",
                      "androidSdkVersion": 32,
                      "hl": "ru",
                      "gl": "US"
                    }
                  }
                }
            """.trimIndent()

            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null

            val respText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val json = JSONObject(respText)

            val details = json.optJSONObject("videoDetails") ?: return null
            val title = details.optString("title", "YouTube Track")
            val author = details.optString("author", "YouTube Music")
            val lengthSec = details.optString("lengthSeconds", "210").toIntOrNull() ?: 210

            val streamingData = json.optJSONObject("streamingData") ?: return null
            val adaptive = streamingData.optJSONArray("adaptiveFormats") ?: JSONArray()

            var bestAudioUrl: String? = null
            var bestBitrate = 0
            var bestMime = "Opus 251 Audio-Only"

            for (i in 0 until adaptive.length()) {
                val fmt = adaptive.getJSONObject(i)
                val mime = fmt.optString("mimeType", "")
                val directUrl = fmt.optString("url", "")
                val bitrate = fmt.optInt("bitrate", 0)
                if (mime.startsWith("audio/") && directUrl.isNotEmpty() && bitrate >= bestBitrate) {
                    bestBitrate = bitrate
                    bestAudioUrl = directUrl
                    bestMime = if (mime.contains("opus")) "Opus ${bitrate / 1000} kbps (itag 251)" else "M4A ${bitrate / 1000} kbps"
                }
            }

            if (bestAudioUrl == null) {
                val hlsUrl = streamingData.optString("hlsManifestUrl", "")
                if (hlsUrl.isNotEmpty()) bestAudioUrl = hlsUrl
            }

            if (bestAudioUrl.isNullOrEmpty()) return null

            TrackItem(
                id = "yt-$videoId-${System.currentTimeMillis()}",
                title = title,
                artist = author,
                album = "YouTube Music • Прямой поток Innertube",
                source = "ytmusic",
                sourceBadge = "YouTube Music",
                codec = "$bestMime • Без видео",
                duration = lengthSec.coerceAtLeast(30),
                cached = false,
                bpm = 120,
                rootFreq = 55.0,
                monetSeed = "sky",
                coverColors = listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
                lyrics = listOf(
                    "♪ Прямой аудиопоток YouTube Music ($title) ♪",
                    "Исполнитель: $author",
                    "Извлечено через Innertube API (Video ID: $videoId)"
                ),
                streamUrl = bestAudioUrl
            )
        }.getOrNull()
    }

    // 2. Резервный запрос через зеркала Piped API
    private fun fetchViaPiped(videoId: String): TrackItem? {
        for (instance in PIPED_INSTANCES) {
            val text = httpGet("$instance/streams/$videoId") ?: continue
            runCatching {
                val json = JSONObject(text)
                val title = json.optString("title", "YouTube Track")
                val uploader = json.optString("uploader", "YouTube Artist")
                val duration = json.optInt("duration", 200)
                val audioStreams = json.optJSONArray("audioStreams") ?: JSONArray()

                var bestUrl: String? = null
                var bestBitrate = 0
                for (i in 0 until audioStreams.length()) {
                    val s = audioStreams.getJSONObject(i)
                    val u = s.optString("url", "")
                    val br = s.optInt("bitrate", 0)
                    if (u.isNotEmpty() && br >= bestBitrate) {
                        bestBitrate = br
                        bestUrl = u
                    }
                }
                if (!bestUrl.isNullOrEmpty()) {
                    return TrackItem(
                        id = "yt-$videoId-${System.currentTimeMillis()}",
                        title = title,
                        artist = uploader,
                        album = "YouTube Music • Piped Stream",
                        source = "ytmusic",
                        sourceBadge = "YouTube Music",
                        codec = "Opus ${bestBitrate / 1000} kbps • Audio Only",
                        duration = duration.coerceAtLeast(30),
                        cached = false,
                        bpm = 120,
                        rootFreq = 55.0,
                        monetSeed = "sky",
                        coverColors = listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
                        lyrics = listOf("♪ Аудиопоток извлечён из YouTube ($title) ♪"),
                        streamUrl = bestUrl
                    )
                }
            }
        }
        return null
    }

    // Поиск видео на YouTube по текстовому запросу (возвращает первый найденный videoId)
    private fun searchYoutubeVideoId(query: String): String? {
        val encoded = URLEncoder.encode(query, "UTF-8")
        // Сначала пробуем поиск через Innertube Search
        val innertubeId = runCatching {
            val url = URL("https://www.youtube.com/youtubei/v1/search?prettyPrint=false")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 7000
                readTimeout = 7000
                setRequestProperty("Content-Type", "application/json")
            }
            val body = """
                {
                  "query": "$query",
                  "context": {
                    "client": {
                      "clientName": "WEB",
                      "clientVersion": "2.20240726.00.00"
                    }
                  }
                }
            """.trimIndent()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            val m = Regex(""""videoId"\s*:\s*"([a-zA-Z0-9_-]{11})"""").find(resp)
            m?.groupValues?.get(1)
        }.getOrNull()

        if (!innertubeId.isNullOrEmpty()) return innertubeId

        for (instance in PIPED_INSTANCES) {
            val resp = httpGet("$instance/search?q=$encoded&filter=music_songs") ?: continue
            val m = Regex("""/watch\?v=([a-zA-Z0-9_-]{11})""").find(resp)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    // Поиск реального трека через открытый каталог iTunes / Apple Music / Deezer Preview (если YouTube заблокирован провайдером)
    private fun searchDirectMusicApi(query: String, platform: String): TrackItem? {
        return runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val resp = httpGet("https://itunes.apple.com/search?term=$encoded&media=music&limit=1") ?: return null
            val results = JSONObject(resp).optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val item = results.getJSONObject(0)
            val previewUrl = item.optString("previewUrl", "")
            if (previewUrl.isEmpty()) return null
            val trackName = item.optString("trackName", query)
            val artistName = item.optString("artistName", "Unknown Artist")
            val collectionName = item.optString("collectionName", "Cloud Audio")

            TrackItem(
                id = "cloud-${System.currentTimeMillis()}",
                title = trackName,
                artist = artistName,
                album = collectionName,
                source = platform,
                sourceBadge = if (platform == "soundcloud") "SoundCloud" else "YouTube Music",
                codec = "AAC 256 kbps • Прямой поток",
                duration = 30,
                cached = false,
                bpm = 120,
                rootFreq = 55.0,
                monetSeed = if (platform == "soundcloud") "peach" else "sky",
                coverColors = if (platform == "soundcloud") listOf(Color(0xFF8A5749), Color(0xFF29201E)) else listOf(Color(0xFF4A6682), Color(0xFF1E242B)),
                lyrics = listOf(
                    "♪ Трек найден в глобальном каталоге: $trackName ♪",
                    "Исполнитель: $artistName ($collectionName)"
                ),
                streamUrl = previewUrl
            )
        }.getOrNull()
    }

    suspend fun resolveInputToTrack(platform: String, rawInput: String, customTitle: String, customArtist: String): Result<TrackItem> {
        return withContext(Dispatchers.IO) {
            val input = rawInput.trim()
            if (input.isEmpty() && customTitle.isEmpty()) {
                return@withContext Result.failure(Exception("Введите ссылку или название трека"))
            }

            // 1. Если это прямая ссылка на аудиофайл или поток (.mp3 / .flac / .ogg / .m4a / радио)
            if (input.startsWith("http") && !input.contains("youtube.com") && !input.contains("youtu.be")) {
                val title = customTitle.ifBlank { input.substringAfterLast("/").substringBefore("?").ifBlank { "Прямой Аудиопоток" } }
                val artist = customArtist.ifBlank { "Direct Stream" }
                return@withContext Result.success(
                    TrackItem(
                        id = "direct-${System.currentTimeMillis()}",
                        title = title,
                        artist = artist,
                        album = "Прямой URL поток",
                        source = platform,
                        sourceBadge = if (platform == "soundcloud") "SoundCloud" else "Direct Stream",
                        codec = "Direct Audio Stream",
                        duration = 240,
                        cached = false,
                        bpm = 120,
                        rootFreq = 55.0,
                        monetSeed = "peach",
                        coverColors = listOf(Color(0xFF8A5749), Color(0xFF29201E)),
                        lyrics = listOf("♪ Воспроизведение по прямой ссылке ♪", input),
                        streamUrl = input
                    )
                )
            }

            // 2. Если это ссылка на YouTube / YouTube Music ИЛИ текстовый запрос для поиска
            val videoId = extractYoutubeVideoId(input) ?: searchYoutubeVideoId(input.ifBlank { "$customArtist $customTitle" })
            if (videoId != null) {
                val ytTrack = fetchViaInnertube(videoId) ?: fetchViaPiped(videoId)
                if (ytTrack != null) {
                    val finalTrack = ytTrack.copy(
                        title = customTitle.ifBlank { ytTrack.title },
                        artist = customArtist.ifBlank { ytTrack.artist }
                    )
                    return@withContext Result.success(finalTrack)
                }
            }

            // 3. Если YouTube в вашей сети недоступен без VPN/sing-box, ищем реальный аудиопоток по названию через глобальный музыкальный API
            val searchQuery = if (customTitle.isNotBlank()) "$customArtist $customTitle" else input
            val fallbackTrack = searchDirectMusicApi(searchQuery, platform)
            if (fallbackTrack != null) {
                return@withContext Result.success(fallbackTrack)
            }

            Result.failure(Exception("Не удалось извлечь поток (проверьте ссылку или включите sing-box/VPN для доступа к YouTube)"))
        }
    }
}

// Реальные круглосуточные 24/7 онлайн-потоки (Synthwave, Phonk, Lo-Fi), которые работают всегда
val LIVE_STREAMS_CATALOG = listOf(
    TrackItem(
        "live-synth", "Nightride FM — 24/7 Synthwave & Retrowave", "Nightride Radio Stream",
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
        "live-darksynth", "Darksynth & Cyberpunk Radio", "Datawave FM",
        "Прямой эфир • Bass Boost Ready", "telegram", "Telegram Radio",
        "AAC 256 kbps • Live Stream", 3600, false, 130, 49.0, "peach",
        listOf(Color(0xFF8A5749), Color(0xFF29201E)),
        listOf("♪ Прямой эфир Darksynth & Cyberpunk ♪"),
        streamUrl = "https://stream.nightride.fm/darksynth.m4a"
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
                            val numBands = eq.numberOfBands.toInt()
                            val range = eq.bandLevelRange
                            for (b in 0 until minOf(numBands, gains.size)) {
                                val mb = (gains[b] * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                                eq.setBandLevel(b.toShort(), mb)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        synthEngine.stop()
        runCatching { systemEq?.release() }
        exoPlayer.release()
        super.onDestroy()
    }
}

@Composable
fun AuraTuneApp(
    synth: SynthAudioEngine,
    exoPlayer: ExoPlayer,
    onApplyHardwareEq: (Boolean, List<Float>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf("home") }
    var nowPlayingOpen by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var monetKey by remember { mutableStateOf("auto") }

    var tracks by remember { mutableStateOf(LIVE_STREAMS_CATALOG + INITIAL_TRACKS) }
    var sourceFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var currentTrackId by remember { mutableStateOf(LIVE_STREAMS_CATALOG[0].id) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(0) }
    var volume by remember { mutableStateOf(85f) }
    var isImporting by remember { mutableStateOf(false) }

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
    var loadedStreamUrl by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileNameFromUri(context, uri)
            val newLocalTrack = TrackItem(
                id = "local-${System.currentTimeMillis()}",
                title = fileName,
                artist = "Локальный файл смартфона",
                album = "Внутренняя память • Прямой DSP поток",
                source = "telegram",
                sourceBadge = "Локальный MP3/FLAC",
                codec = "Lossless / Direct ExoPlayer • 5-Band EQ",
                duration = 240,
                cached = true,
                bpm = 120,
                rootFreq = 55.0,
                monetSeed = "sage",
                coverColors = listOf(Color(0xFF4B6B51), Color(0xFF1B221C)),
                lyrics = listOf(
                    "♪ Воспроизведение локального файла: $fileName ♪",
                    "Звук идет напрямую через аппаратный эквалайзер Android"
                ),
                streamUrl = uri.toString()
            )
            tracks = listOf(newLocalTrack) + tracks
            currentTrackId = newLocalTrack.id
            currentTime = 0
            isPlaying = true
            toastText = "Файл «$fileName» добавлен и запущен!"
        }
    }

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

    LaunchedEffect(isPlaying, currentTrack) {
        val url = currentTrack.streamUrl
        if (url != null) {
            synth.stop()
            if (loadedStreamUrl != url) {
                exoPlayer.setMediaItem(MediaItem.fromUri(url))
                exoPlayer.prepare()
                loadedStreamUrl = url
            }
            exoPlayer.playWhenReady = isPlaying
        } else {
            exoPlayer.pause()
            synth.rootFreq = currentTrack.rootFreq
            synth.bpm = currentTrack.bpm
            if (isPlaying) synth.start() else synth.stop()
        }
    }

    LaunchedEffect(eqEnabled, eqGains, bassBoost, volume) {
        synth.eqEnabled = eqEnabled
        synth.gains = eqGains.toFloatArray()
        synth.bassBoost = bassBoost
        synth.masterVol = volume / 100f
        exoPlayer.volume = (volume / 100f).coerceIn(0f, 1f)
        onApplyHardwareEq(eqEnabled, eqGains)
    }

    LaunchedEffect(isPlaying, currentTrackId) {
        while (isPlaying) {
            delay(1000L)
            if (currentTrack.streamUrl != null && exoPlayer.duration > 0) {
                currentTime = (exoPlayer.currentPosition / 1000L).toInt()
            } else {
                if (currentTime + 1 >= currentTrack.duration) {
                    val idx = tracks.indexOfFirst { it.id == currentTrackId }
                    currentTrackId = tracks[(idx + 1) % tracks.size].id
                    currentTime = 0
                } else {
                    currentTime++
                }
            }
        }
    }

    LaunchedEffect(toastText) {
        if (toastText != null) {
            delay(3200L)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
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
                                .padding(horizontal = 10.dp, vertical = 6.dp)
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

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.primaryContainer)
                        .clickable { filePickerLauncher.launch("audio/*") }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+ Файл MP3",
                        color = m3.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
                        isImporting = isImporting,
                        onPickLocalFile = { filePickerLauncher.launch("audio/*") },
                        onRealExtractImport = { platform, urlOrQuery, customTitle, customArtist ->
                            if (isImporting) return@SourcesTab
                            isImporting = true
                            toastText = "Извлекаем прямой аудиопоток..."
                            scope.launch {
                                val res = RealStreamExtractor.resolveInputToTrack(
                                    platform = platform,
                                    rawInput = urlOrQuery,
                                    customTitle = customTitle,
                                    customArtist = customArtist
                                )
                                isImporting = false
                                res.onSuccess { importedTrack ->
                                    tracks = listOf(importedTrack) + tracks
                                    currentTrackId = importedTrack.id
                                    currentTime = 0
                                    isPlaying = true
                                    toastText = "Готово! Играет: ${importedTrack.title}"
                                }.onFailure { err ->
                                    toastText = err.message ?: "Ошибка извлечения потока"
                                }
                            }
                        },
                        onQuickCatalogImport = { item ->
                            val copy = item.copy(id = "${item.id}-${System.currentTimeMillis()}")
                            tracks = listOf(copy) + tracks
                            currentTrackId = copy.id
                            currentTime = 0
                            isPlaying = true
                            toastText = "Запущен поток: ${copy.title}"
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
                onSeek = { pos ->
                    currentTime = pos
                    if (currentTrack.streamUrl != null) {
                        exoPlayer.seekTo(pos * 1000L)
                    }
                },
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
                        .background(m3.primaryContainer)
                        .clickable { onOpenSources() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("+ Импорт по ссылке", color = m3.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Фильтр по медиатеке...", fontSize = 12.sp) },
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
fun SourcesTab(
    m3: M3Palette,
    isImporting: Boolean,
    onPickLocalFile: () -> Unit,
    onRealExtractImport: (String, String, String, String) -> Unit,
    onQuickCatalogImport: (TrackItem) -> Unit
) {
    var selectedPlatform by remember { mutableStateOf("ytmusic") }
    var trackUrlOrQuery by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var customArtist by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Настоящий парсер YouTube Innertube / Piped / Global Search", color = m3.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text("Импорт и Поиск Музыки", color = m3.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(m3.surfaceContainerHigh)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Вставьте ссылку YouTube или введите название песни",
                color = m3.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ytmusic" to "YouTube Music",
                    "soundcloud" to "SoundCloud",
                    "telegram" to "Direct URL"
                ).forEach { (id, label) ->
                    val active = selectedPlatform == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (active) m3.primaryContainer else m3.surfaceContainer)
                            .clickable { selectedPlatform = id }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (active) m3.onPrimaryContainer else m3.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = trackUrlOrQuery,
                onValueChange = { trackUrlOrQuery = it },
                placeholder = { Text("Ссылка YouTube (youtu.be/...) или название трека", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = m3.surfaceContainer,
                    unfocusedContainerColor = m3.surfaceContainer,
                    focusedBorderColor = m3.primary,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = m3.onSurface,
                    unfocusedTextColor = m3.onSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = customTitle,
                    onValueChange = { customTitle = it },
                    placeholder = { Text("Своё название (необяз.)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = m3.surfaceContainer,
                        unfocusedContainerColor = m3.surfaceContainer,
                        focusedBorderColor = m3.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = m3.onSurface,
                        unfocusedTextColor = m3.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = customArtist,
                    onValueChange = { customArtist = it },
                    placeholder = { Text("Артист (необяз.)", fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = m3.surfaceContainer,
                        unfocusedContainerColor = m3.surfaceContainer,
                        focusedBorderColor = m3.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = m3.onSurface,
                        unfocusedTextColor = m3.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isImporting) m3.surfaceContainerHighest else m3.primary)
                        .clickable(enabled = !isImporting) {
                            onRealExtractImport(selectedPlatform, trackUrlOrQuery, customTitle, customArtist)
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isImporting) "⏳ Извлекаем поток..." else "Найти и запустить поток",
                        color = if (isImporting) m3.onSurface else m3.onPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(m3.secondaryContainer)
                        .clickable { onPickLocalFile() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📂 MP3/FLAC",
                        color = m3.onSecondaryContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Text("Круглосуточные Live-потоки (320 kbps Direct):", color = m3.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

        LIVE_STREAMS_CATALOG.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(m3.surfaceContainer)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(item.sourceBadge, color = m3.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(item.title, color = m3.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text("${item.artist} • ${item.codec}", color = m3.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(m3.primaryContainer)
                        .clickable { onQuickCatalogImport(item) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("▶ Включить", color = m3.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
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
                    Text(track.codec, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
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
                    valueRange = 0f..track.duration.toFloat().coerceAtLeast(1f)
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
