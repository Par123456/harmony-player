package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HarmonyViewModel
import com.example.ui.components.DeveloperFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: HarmonyViewModel,
    modifier: Modifier = Modifier
) {
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqPreset by viewModel.eqPreset.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()

    val presets = listOf(
        "Flat", "Bass Boost", "Treble Boost", "Vocal",
        "Rock", "Pop", "Jazz", "Classical"
    )
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header with toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "اکولایزر",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "اکولایزر صوتی (مخصوص اندروید)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            Switch(
                checked = eqEnabled,
                onCheckedChange = { viewModel.setEqualizerEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val panelAlpha by animateFloatAsState(targetValue = if (eqEnabled) 1.0f else 0.4f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .alpha(panelAlpha)
        ) {
            // Presets Header
            Text(
                text = "انتخاب پریست آماده",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Right
            )

            // Presets Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 130.dp)
            ) {
                items(presets) { preset ->
                    val isSelected = eqPreset == preset
                    Card(
                        onClick = { if (eqEnabled) viewModel.setEqPreset(preset) },
                        enabled = eqEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sliders Header
            Text(
                text = "تنظیم فرکانس‌ها (دستی)",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp),
                textAlign = TextAlign.Right
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 5 frequency band sliders (vertical-styled layout or horizontal)
            // A horizontal card with label + value + slider is extremely readable!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                eqBands.forEachIndexed { index, gain ->
                    if (index < bandLabels.size) {
                        val label = bandLabels[index]
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Label (e.g. 60Hz)
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.width(60.dp),
                                    textAlign = TextAlign.Left
                                )

                                // Slider
                                Slider(
                                    value = gain.toFloat(),
                                    onValueChange = { newValue ->
                                        if (eqEnabled) viewModel.setBandGain(index, newValue.toInt())
                                    },
                                    valueRange = -12f..12f,
                                    steps = 24,
                                    enabled = eqEnabled,
                                    modifier = Modifier.weight(1f)
                                )

                                // Gain Value Display (e.g. +4 dB)
                                val sign = if (gain > 0) "+" else ""
                                Text(
                                    text = "$sign$gain dB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (gain != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(64.dp),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }
            }
        }

        // Android hardware equalizer disclaimer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "راهنما",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تغییرات اکولایزر بر روی خروجی صوتی پیش‌فرض اندروید اعمال شده و بر کیفیت نهایی صدا تاثیر مستقیم دارد.",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
        
        DeveloperFooter()
    }
}
