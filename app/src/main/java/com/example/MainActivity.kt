package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.HarmonyTab
import com.example.ui.HarmonyViewModel
import com.example.ui.components.MiniPlayer
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Instantiate our shared ViewModel with Context Factory
                val viewModel: HarmonyViewModel = viewModel(
                    factory = HarmonyViewModel.Factory(LocalContext.current)
                )

                val activeTab by viewModel.activeTab.collectAsState()
                val nowPlayingExpanded by viewModel.nowPlayingExpanded.collectAsState()
                val currentSong by viewModel.currentSong.collectAsState()
                val isPlaying by viewModel.isPlaying.collectAsState()
                val progress by viewModel.progress.collectAsState()
                val updateState by viewModel.updateState.collectAsState()

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    bottomBar = {
                        Column {
                            // Docked Mini Player
                            if (currentSong != null && !nowPlayingExpanded) {
                                MiniPlayer(
                                    song = currentSong,
                                    isPlaying = isPlaying,
                                    progress = progress,
                                    onPlayPauseClick = { viewModel.togglePlayPause() },
                                    onSkipClick = { viewModel.playNext() },
                                    onClick = { viewModel.setNowPlayingExpanded(true) }
                                )
                            }

                            // Material 3 Navigation Bar (Persian localized labels)
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == HarmonyTab.LIBRARY,
                                    onClick = { viewModel.setTab(HarmonyTab.LIBRARY) },
                                    icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "کتابخانه") },
                                    label = { Text("کتابخانه") },
                                    modifier = Modifier.testTag("nav_library")
                                )
                                NavigationBarItem(
                                    selected = activeTab == HarmonyTab.FAVORITES,
                                    onClick = { viewModel.setTab(HarmonyTab.FAVORITES) },
                                    icon = { Icon(Icons.Default.Favorite, contentDescription = "علاقه‌مندی‌ها") },
                                    label = { Text("محبوب‌ها") },
                                    modifier = Modifier.testTag("nav_favorites")
                                )
                                NavigationBarItem(
                                    selected = activeTab == HarmonyTab.EQUALIZER,
                                    onClick = { viewModel.setTab(HarmonyTab.EQUALIZER) },
                                    icon = { Icon(Icons.Default.Equalizer, contentDescription = "اکولایزر") },
                                    label = { Text("اکولایزر") },
                                    modifier = Modifier.testTag("nav_equalizer")
                                )
                                NavigationBarItem(
                                    selected = activeTab == HarmonyTab.SETTINGS,
                                    onClick = { viewModel.setTab(HarmonyTab.SETTINGS) },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                                    label = { Text("تنظیمات") },
                                    modifier = Modifier.testTag("nav_settings")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Display correct screen based on tab selection
                        when (activeTab) {
                            HarmonyTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                            HarmonyTab.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                            HarmonyTab.EQUALIZER -> EqualizerScreen(viewModel = viewModel)
                            HarmonyTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                        }

                        // Sliding Slide-Up Now Playing Full-Screen Overlay panel
                        AnimatedVisibility(
                            visible = nowPlayingExpanded,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            ),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        ) {
                            NowPlayingScreen(
                                viewModel = viewModel,
                                onCollapse = { viewModel.setNowPlayingExpanded(false) }
                            )
                        }

                        // Auto-update interactive popup dialog
                        UpdateDialog(
                            state = updateState,
                            onDownloadClick = { viewModel.startUpdateDownload(it) },
                            onInstallClick = { viewModel.installUpdate(it) },
                            onDismiss = { viewModel.resetUpdateState() }
                        )
                    }
                }
            }
        }
    }
}
