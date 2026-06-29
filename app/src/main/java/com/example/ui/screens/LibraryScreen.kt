package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.PlaylistEntity
import com.example.model.Song
import com.example.ui.HarmonyViewModel
import com.example.ui.components.DeveloperFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: HarmonyViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val songs by viewModel.filteredSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    
    val playlists by viewModel.playlists.collectAsState()
    val history by viewModel.history.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Songs, 1: Playlists, 2: History
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var activePlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }

    // Permission tracking
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            viewModel.scanDeviceMusic(context)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            viewModel.scanDeviceMusic(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tab Selector at the top
        TabRow(
            selectedTabIndex = selectedSubTab,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedSubTab == 0 && activePlaylist == null,
                onClick = { 
                    selectedSubTab = 0 
                    activePlaylist = null
                },
                text = { Text("آهنگ‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedSubTab == 1 || activePlaylist != null,
                onClick = { selectedSubTab = 1 },
                text = { Text("پلی‌لیست‌ها", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
            Tab(
                selected = selectedSubTab == 2 && activePlaylist == null,
                onClick = { 
                    selectedSubTab = 2 
                    activePlaylist = null
                },
                text = { Text("تاریخچه", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp)) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activePlaylist != null) {
            // PLAYLIST SONGS DETAIL VIEW
            val playlistSongs by viewModel.getSongsForPlaylist(activePlaylist!!.id).collectAsState(initial = emptyList())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activePlaylist = null }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activePlaylist!!.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playlistSongs.size} آهنگ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (playlistSongs.isNotEmpty()) {
                    Button(
                        onClick = { 
                            // Set playlist songs as active queue and play first
                            // Simply play first song in playlist
                            viewModel.selectSong(playlistSongs.first())
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "پخش")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("پخش همه")
                    }
                }
            }

            if (playlistSongs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "پلی‌لیست خالی",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "این پلی‌لیست خالی است",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "در زبانه آهنگ‌ها با زدن بر روی منوی سه نقطه هر آهنگ، آن را به این پلی‌لیست اضافه کنید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                    DeveloperFooter(modifier = Modifier.align(Alignment.BottomCenter))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlistSongs, key = { it.id }) { song ->
                        val isCurrent = currentSong?.id == song.id
                        val isFav by viewModel.isSongFavorite(song.id).collectAsState(initial = false)

                        SongRowItem(
                            song = song,
                            isCurrent = isCurrent,
                            isPlaying = isCurrent && isPlaying,
                            isFavorite = isFav,
                            playlists = playlists,
                            onRowClick = { viewModel.selectSong(song) },
                            onFavoriteToggle = { viewModel.toggleFavorite(song) },
                            onAddToPlaylist = { pId -> viewModel.addSongToPlaylist(pId, song) },
                            onAddToQueue = { viewModel.addToQueue(song) },
                            onRemoveFromPlaylist = { viewModel.removeSongFromPlaylist(activePlaylist!!.id, song.id) }
                        )
                    }
                    item {
                        DeveloperFooter(modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        } else {
            // MAIN TABS
            when (selectedSubTab) {
                0 -> {
                    // SONGS TAB
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("search_input"),
                        placeholder = { Text("جستجوی نام آهنگ، خواننده یا آلبوم...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "پاک کردن")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Local scan promotion bar if permission is not granted
                    if (!hasPermission) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "اسکن موسیقی‌های محلی",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "برای دسترسی و پخش موسیقی‌های ذخیره شده در دستگاه، مجوز مربوطه را تایید کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = { launcher.launch(permissionToRequest) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("مجوز")
                                }
                            }
                        }
                    }

                    // Header and Scan Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "کتابخانه موسیقی (${songs.size} آهنگ)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (hasPermission) {
                            TextButton(
                                onClick = { viewModel.scanDeviceMusic(context) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اسکن مجدد", fontSize = 12.sp)
                            }
                        }
                    }

                    // Songs List
                    if (songs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "آهنگی پیدا نشد",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "هیچ موسیقی یافت نشد",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "عبارت دیگری را جستجو کنید یا موسیقی‌های دستگاه را اسکن کنید.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            DeveloperFooter(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = songs,
                                key = { _, song -> song.id }
                            ) { _, song ->
                                val isCurrent = currentSong?.id == song.id
                                val isFav by viewModel.isSongFavorite(song.id).collectAsState(initial = false)

                                SongRowItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isFavorite = isFav,
                                    playlists = playlists,
                                    onRowClick = { viewModel.selectSong(song) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(song) },
                                    onAddToPlaylist = { pId -> viewModel.addSongToPlaylist(pId, song) },
                                    onAddToQueue = { viewModel.addToQueue(song) }
                                )
                            }
                            item {
                                DeveloperFooter(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }

                1 -> {
                    // PLAYLISTS TAB
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "پلی‌لیست‌های شما (${playlists.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "ایجاد")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پلی‌لیست جدید")
                        }
                    }

                    if (playlists.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = "بدون پلی‌لیست",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "هنوز پلی‌لیستی نساخته‌اید",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "با کلیک روی دکمه «پلی‌لیست جدید» اولین لیست موسیقی اختصاصی خود را بسازید.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            DeveloperFooter(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(playlists, key = { it.id }) { playlist ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activePlaylist = playlist },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.QueueMusic,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = playlist.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "پلی‌لیست موسیقی",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف پلی‌لیست",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                DeveloperFooter(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }

                2 -> {
                    // HISTORY TAB
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تاریخچه پخش اخیر",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (history.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearHistory() }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "پاک کردن")
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("پاک کردن تاریخچه", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (history.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "تاریخچه خالی",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "تاریخچه پخش خالی است",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "آهنگ‌هایی که پخش می‌کنید به صورت خودکار در این قسمت قرار خواهند گرفت.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                            DeveloperFooter(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(history, key = { idx, s -> "${s.id}_$idx" }) { _, song ->
                                val isCurrent = currentSong?.id == song.id
                                val isFav by viewModel.isSongFavorite(song.id).collectAsState(initial = false)

                                SongRowItem(
                                    song = song,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && isPlaying,
                                    isFavorite = isFav,
                                    playlists = playlists,
                                    onRowClick = { viewModel.selectSong(song) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(song) },
                                    onAddToPlaylist = { pId -> viewModel.addSongToPlaylist(pId, song) },
                                    onAddToQueue = { viewModel.addToQueue(song) }
                                )
                            }
                            item {
                                DeveloperFooter(modifier = Modifier.padding(top = 16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE PLAYLIST DIALOG
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("ساخت پلی‌لیست جدید") },
            text = {
                Column {
                    Text("نامی برای پلی‌لیست خود انتخاب کنید:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("مثلا: تمرکز، لوفای، خاطره‌انگیز") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName.trim())
                            newPlaylistName = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("ساختن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFavorite: Boolean,
    playlists: List<PlaylistEntity> = emptyList(),
    onRowClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToPlaylist: ((String) -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .testTag("song_item_${song.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art Thumbnail
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                        imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                        contentDescription = "آیکون موسیقی",
                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = song.durationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Favorite and Menu Options
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag("favorite_toggle_${song.id}")
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "افزودن به علاقه‌مندی‌ها",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "بیشتر",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (onAddToQueue != null) {
                        DropdownMenuItem(
                            text = { Text("افزودن به صف پخش") },
                            leadingIcon = { Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onAddToQueue()
                                showMenu = false
                            }
                        )
                    }

                    if (onAddToPlaylist != null && playlists.isNotEmpty()) {
                        HorizontalDivider()
                        // Sub-header for playlists
                        Text(
                            text = "افزودن به پلی‌لیست:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    onAddToPlaylist(playlist.id)
                                    showMenu = false
                                }
                            )
                        }
                    }

                    if (onRemoveFromPlaylist != null) {
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("حذف از این پلی‌لیست", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                            onClick = {
                                onRemoveFromPlaylist()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
