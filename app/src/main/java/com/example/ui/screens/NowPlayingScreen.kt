package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.RepeatMode
import com.example.model.Song
import com.example.ui.HarmonyViewModel
import com.example.ui.components.AudioVisualizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    viewModel: HarmonyViewModel,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    // Sleep Timer states
    val sleepTimerActive by viewModel.sleepTimerActive.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimerRemaining.collectAsState()
    val sleepTimerTotal by viewModel.sleepTimerTotal.collectAsState()

    var showSpeedMenu by remember { mutableStateOf(false) }
    var showSleepMenu by remember { mutableStateOf(false) }
    var showQueueOverlay by remember { mutableStateOf(false) }
    var showLyricsOverlay by remember { mutableStateOf(false) }

    if (currentSong == null) return
    val song = currentSong!!

    // 1. Animated Ambient Gradient Background (Lava Lamp Aesthetic)
    val transition = rememberInfiniteTransition(label = "ambient_background")
    val color1 by transition.animateColor(
        initialValue = MaterialTheme.colorScheme.surface,
        targetValue = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "c1"
    )
    val color2 by transition.animateColor(
        initialValue = MaterialTheme.colorScheme.surfaceVariant,
        targetValue = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "c2"
    )

    // 2. Pulse Animation on Album Art
    val scaleAnim by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val albumScale = if (isPlaying) scaleAnim else 1.0f

    val formattedProgress = remember(progress) {
        val totalSecs = progress / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Text(
                        text = "در حال پخش",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCollapse) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "بستن")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        showLyricsOverlay = !showLyricsOverlay
                        if (showLyricsOverlay) showQueueOverlay = false
                    }) {
                        Icon(
                            imageVector = if (showLyricsOverlay) Icons.Default.MusicNote else Icons.Default.Notes,
                            contentDescription = "متن آهنگ",
                            tint = if (showLyricsOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(onClick = { 
                        showQueueOverlay = !showQueueOverlay
                        if (showQueueOverlay) showLyricsOverlay = false
                    }) {
                        Icon(
                            imageVector = if (showQueueOverlay) Icons.Default.MusicNote else Icons.Default.QueueMusic,
                            contentDescription = "صف پخش",
                            tint = if (showQueueOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(color1, color2)))
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                if (!showQueueOverlay) {
                    // MAIN PLAYER BODY
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (showLyricsOverlay) {
                            val lyrics = remember(song.id) { getLyricsForSong(song.id) }
                            val activeLineIndex = remember(progress, lyrics) {
                                lyrics.indexOfLast { progress >= it.first }.coerceAtLeast(0)
                            }
                            val lazyListState = rememberLazyListState()

                            LaunchedEffect(activeLineIndex) {
                                if (lyrics.isNotEmpty()) {
                                    lazyListState.animateScrollToItem(activeLineIndex)
                                }
                            }

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                itemsIndexed(lyrics) { index, line ->
                                    val isActive = index == activeLineIndex
                                    Text(
                                        text = line.second,
                                        style = if (isActive) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                            .scale(if (isActive) 1.08f else 1f)
                                    )
                                }
                            }
                        } else {
                            // Album Art Card with dynamic beat scale pulse
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .scale(albumScale)
                                    .shadow(16.dp, RoundedCornerShape(24.dp))
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.albumArtUri != null && song.albumArtUri.startsWith("http")) {
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = "کاور آلبوم",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "موسیقی",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(100.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Title & Artist Metadata
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Audio Visualizer
                        AudioVisualizer(
                            isPlaying = isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 12.dp)
                        )
                    }
                } else {
                    // QUEUE OVERLAY PANEL
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "صف پخش موسیقی",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.clearQueue() }) {
                                Icon(Icons.Default.Delete, contentDescription = "پاک کردن صف", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن صف", fontSize = 12.sp)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(queue) { index, queueSong ->
                                val isNow = queueSong.id == song.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectSong(queueSong) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isNow) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(12.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isNow) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "NOW",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = queueSong.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = queueSong.artist,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(onClick = { viewModel.removeFromQueue(index) }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف از صف",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CONTROLLER CONTROLS AND ACTIONS DOCKED AT THE BASE
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Linear seek progress bar
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = progress.toFloat(),
                            onValueChange = { viewModel.seekTo(it.toLong()) },
                            valueRange = 0f..(song.duration.toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formattedProgress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = song.durationText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Media Action Row: Shuffle, Prev, Play/Pause, Next, Repeat
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleShuffle() },
                            modifier = Modifier.testTag("shuffle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "پخش تصادفی",
                                tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { viewModel.playPrevious() },
                            modifier = Modifier.testTag("prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "قبلی",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .testTag("play_pause_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "پخش/توقف",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.playNext() },
                            modifier = Modifier.testTag("next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "بعدی",
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleRepeatMode() },
                            modifier = Modifier.testTag("repeat_button")
                        ) {
                            Icon(
                                imageVector = when (repeatMode) {
                                    RepeatMode.ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                },
                                contentDescription = "تکرار",
                                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic Quick Tools: Playback Speed and Sleep Timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed button with quick Dialog
                        Box {
                            IconButton(onClick = { showSpeedMenu = true }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = "سرعت پخش", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${playbackSpeed}x",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            viewModel.setPlaybackSpeed(speed)
                                            showSpeedMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Sleep Timer button with countdown indicator
                        Box {
                            IconButton(
                                onClick = { showSleepMenu = true },
                                modifier = Modifier.testTag("sleep_timer_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "تایمر خواب",
                                        tint = if (sleepTimerActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    if (sleepTimerActive) {
                                        val m = sleepTimerRemaining / 60
                                        val s = sleepTimerRemaining % 60
                                        Text(
                                            text = String.format("%02d:%02d", m, s),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text("تایمر خواب", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = showSleepMenu,
                                onDismissRequest = { showSleepMenu = false }
                            ) {
                                if (sleepTimerActive) {
                                    DropdownMenuItem(
                                        text = { Text("لغو تایمر خواب", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            viewModel.stopSleepTimer()
                                            showSleepMenu = false
                                        }
                                    )
                                } else {
                                    listOf(15L, 30L, 45L, 60L, 90L).forEach { mins ->
                                        DropdownMenuItem(
                                            text = { Text("$mins دقیقه") },
                                            onClick = {
                                                viewModel.startSleepTimer(mins)
                                                showSleepMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getLyricsForSong(songId: String): List<Pair<Long, String>> {
    return when (songId) {
        "demo_1" -> listOf(
            0L to "🎶 صدای باران ملایم و شروع ریتم...",
            15000L to "رایحه باران پاییزی در فضا می‌پیچد 🌧️",
            30000L to "نت‌های آرام پیانو، ذهن را آزاد می‌کنند",
            45000L to "تمرکز بر روی یادگیری و کارهای بزرگ 📚",
            60000L to "هر نت، گامیست به سوی آرامش درونی",
            90000L to "ذهنی متمرکز، قلبی آرام و قلمی روان ✨",
            120000L to "لوفای، همدم تنهایی‌های سازنده تو",
            180000L to "ریتم ملایم همچنان ادامه دارد..."
        )
        "demo_2" -> listOf(
            0L to "🌊 صدای دوردست امواج ملایم اقیانوس...",
            20000L to "شناور در دریایی از فرکانس‌های عمیق 🌌",
            40000L to "تسکین ذهن و رهایی از دغدغه‌های روزمره",
            60000L to "غوطه‌ور در کهکشان رویاهای بی‌پایان 💫",
            90000L to "آرامشی عمیق مانند سکوت اقیانوس در شب",
            120000L to "تاریکی زیبا، خوابی آرام و روحی سبک"
        )
        "demo_3" -> listOf(
            0L to "⚡ ضربان ریتمیک بیس الکترونیک آغاز می‌شود...",
            15000L to "رانندگی در بزرگراه‌های درخشان نئونی شب 🏎️",
            30000L to "نورهای بنفش و صورتی در شیشه جلو می‌رقصند",
            45000L to "آینده متعلق به ماست، فراتر از زمان و مکان 🌌",
            60000L to "انرژی بی‌پایان موسیقی سنث‌ویو در رگ‌ها",
            90000L to "عبور از خط افق به سمت فردایی روشن‌تر 🚀",
            120000L to "سرعت، هیجان و ریتمی که متوقف نمی‌شود!"
        )
        "demo_4" -> listOf(
            0L to "🎸 آکوردهای زنده و دلنشین گیتار آکوستیک...",
            15000L to "غروب طلایی خورشید در افق دریا 🌅",
            30000L to "گرمای مطبوع آتش کمپ و جمع دوستانه 🔥",
            45000L to "خاطرات خوش گذشته در ملودی‌ها زنده می‌شوند",
            60000L to "لبخندها، نگاه‌ها و یک فنجان چای داغ ☕",
            90000L to "پایان روزی زیبا با آرامش مطلق و موسیقی"
        )
        else -> listOf(
            0L to "متن این آهنگ در دسترس نیست.",
            5000L to "می‌توانید موسیقی‌های دمو را پخش کنید تا متن هماهنگ‌شده را ببینید!"
        )
    }
}
