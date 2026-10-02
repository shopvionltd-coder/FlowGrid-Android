package com.shopvion.flowgrid

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shopvion.flowgrid.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    audioEngine: AudioEngine,
    prefsManager: PreferencesManager,
    themeMode: String = PreferencesManager.THEME_SYSTEM,
    onThemeModeChange: (String) -> Unit = {},
    onBackClick: () -> Unit
) {
    val palette = cubixPalette

    // Audio persistence and reactive states
    var soundFxOn by remember { mutableStateOf(prefsManager.isSoundFxEnabled()) }
    var musicOn by remember { mutableStateOf(prefsManager.isMusicEnabled()) }
    var musicVolume by remember { mutableFloatStateOf(prefsManager.getMusicVolume()) }
    var soundFxVolume by remember { mutableFloatStateOf(prefsManager.getSoundFxVolume()) }
    var showResetDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Multi-plane cubist background
        CubistBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // -------------------------------------------------------------
            // TOP APP BAR (Sticky Header)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Cubix3DButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(46.dp),
                    shape = CircleShape,
                    depth = 4.dp
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = palette.textPrimary,
                    letterSpacing = 1.sp
                )
            }

            // -------------------------------------------------------------
            // SCROLLABLE SETTINGS CONTENT
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. VISUAL THEME SELECTOR
                Cubix3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(FlowPurple.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = FlowPurple,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Visual Theme",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "3D Cubism mode",
                                    fontSize = 12.sp,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        // 3-way Segmented Buttons: Light, Dark, System
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf(
                                Triple(PreferencesManager.THEME_LIGHT, "Light", Icons.Default.LightMode),
                                Triple(PreferencesManager.THEME_DARK, "Dark", Icons.Default.DarkMode),
                                Triple(PreferencesManager.THEME_SYSTEM, "System", Icons.Default.SettingsBrightness)
                            )

                            modes.forEach { (mode, label, icon) ->
                                val isSelected = themeMode == mode
                                Cubix3DButton(
                                    onClick = { onThemeModeChange(mode) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    backgroundColor = if (isSelected) FlowBlue else palette.surface,
                                    contentColor = if (isSelected) Color.White else palette.textSecondary,
                                    depth = if (isSelected) 5.dp else 2.5.dp
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) Color.White else palette.textSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. BACKGROUND MUSIC VOLUME CONTROL
                NeumorphicVolumeCard(
                    title = "Background Music",
                    subtitle = "Ambient soundtrack level",
                    accentColor = FlowGreen,
                    isEnabled = musicOn,
                    volume = musicVolume,
                    onToggleEnabled = { enabled ->
                        musicOn = enabled
                        prefsManager.setMusicEnabled(enabled)
                        audioEngine.playMusic(enabled)
                    },
                    onVolumeChange = { newVol ->
                        musicVolume = newVol
                        audioEngine.setMusicVolume(newVol)
                        // If turning volume up while music switch was off, turn on
                        if (newVol > 0f && !musicOn) {
                            musicOn = true
                            prefsManager.setMusicEnabled(true)
                            audioEngine.playMusic(true)
                        }
                    },
                    onVolumeChangeFinished = {
                        audioEngine.setMusicVolume(musicVolume, persist = true)
                    }
                )

                // 3. SOUND FX VOLUME CONTROL
                NeumorphicVolumeCard(
                    title = "Sound Effects",
                    subtitle = "Tactile clicks & win tones",
                    accentColor = FlowBlue,
                    isEnabled = soundFxOn,
                    volume = soundFxVolume,
                    onToggleEnabled = { enabled ->
                        soundFxOn = enabled
                        prefsManager.setSoundFxEnabled(enabled)
                        if (enabled) {
                            audioEngine.playPopConnect(true)
                        }
                    },
                    onVolumeChange = { newVol ->
                        soundFxVolume = newVol
                        audioEngine.setSoundFxVolume(newVol)
                        if (newVol > 0f && !soundFxOn) {
                            soundFxOn = true
                            prefsManager.setSoundFxEnabled(true)
                        }
                    },
                    onVolumeChangeFinished = {
                        audioEngine.setSoundFxVolume(soundFxVolume, persist = true)
                        // Play a pleasant feedback tone so user hears the new volume level
                        audioEngine.playPopConnect(true)
                    }
                )

                // 4. RESET PROGRESS CARD
                Cubix3DCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    elevation = 5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(FlowRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = FlowRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Reset All Progress",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Clears unlocked levels",
                                    fontSize = 13.sp,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        Cubix3DButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier
                                .width(82.dp)
                                .height(38.dp),
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = palette.surface,
                            contentColor = FlowRed,
                            depth = 4.dp
                        ) {
                            Text(
                                text = "RESET",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. APP METADATA INFO & BANNER AD
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = GameConfig.GAME_TITLE,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = "Version ${GameConfig.APP_VERSION}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    AdBannerView()
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // -------------------------------------------------------------
        // RESET CONFIRMATION MODAL
        // -------------------------------------------------------------
        if (showResetDialog) {
            Dialog(onDismissRequest = { showResetDialog = false }) {
                Cubix3DCard(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(26.dp),
                    elevation = 14.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(FlowRed.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = FlowRed,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Reset All Progress?",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = palette.textPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "This will lock all levels past Level 1, reset all earned stars, and clear best move records. This action cannot be undone.",
                            fontSize = 14.sp,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Cubix3DButton(
                                onClick = { showResetDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = palette.surface,
                                contentColor = palette.textSecondary,
                                depth = 4.dp
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Cubix3DButton(
                                onClick = {
                                    prefsManager.resetAllProgress()
                                    audioEngine.playLineCut(true)
                                    showResetDialog = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                backgroundColor = FlowRed,
                                contentColor = Color.White,
                                depth = 5.dp
                            ) {
                                Text(
                                    text = "Reset",
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * NeumorphicVolumeCard:
 * A Claymorphic 3D Card housing an interactive Volume Slider, Steppers (-10% / +10%),
 * visual percentage readout, dynamic speaker icons, and master ON/OFF toggle.
 */
@Composable
private fun NeumorphicVolumeCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    isEnabled: Boolean,
    volume: Float,
    onToggleEnabled: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onVolumeChangeFinished: () -> Unit
) {
    val palette = cubixPalette
    val effectiveVolume = if (isEnabled) volume else 0f
    val percentage = (effectiveVolume * 100).roundToInt()

    // Dynamic Volume Icon based on position and mute state
    val dynamicIcon: ImageVector = when {
        !isEnabled || effectiveVolume <= 0.01f -> Icons.AutoMirrored.Filled.VolumeOff
        effectiveVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
        else -> Icons.AutoMirrored.Filled.VolumeUp
    }

    Cubix3DCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Header Row: Icon, Title, Percentage Badge & ON/OFF Switch ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dynamicIcon,
                            contentDescription = null,
                            tint = if (isEnabled) accentColor else palette.textSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = palette.textSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Volume Percentage Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isEnabled && effectiveVolume > 0f) accentColor.copy(alpha = 0.16f)
                                else palette.surfaceShadow.copy(alpha = 0.35f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnabled && effectiveVolume > 0f) "$percentage%" else "MUTE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isEnabled && effectiveVolume > 0f) accentColor else palette.textSecondary
                        )
                    }

                    // ON / OFF Tactile Button
                    Cubix3DButton(
                        onClick = { onToggleEnabled(!isEnabled) },
                        modifier = Modifier
                            .width(62.dp)
                            .height(36.dp),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = if (isEnabled) accentColor else palette.surface,
                        contentColor = if (isEnabled) Color.White else palette.textSecondary,
                        depth = 3.5.dp
                    ) {
                        Text(
                            text = if (isEnabled) "ON" else "OFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // --- Neumorphic Slider & Stepper Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.background.copy(alpha = 0.65f))
                    .border(
                        width = 1.dp,
                        color = palette.border.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Minus Stepper (-10%)
                Cubix3DButton(
                    onClick = {
                        val stepped = ((volume - 0.1f).coerceIn(0f, 1f) * 10f).roundToInt() / 10f
                        onVolumeChange(stepped)
                        onVolumeChangeFinished()
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    backgroundColor = palette.surface,
                    contentColor = palette.textPrimary,
                    depth = 3.dp,
                    enabled = isEnabled && volume > 0f
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease Volume",
                        modifier = Modifier.size(16.dp),
                        tint = if (isEnabled && volume > 0f) palette.textPrimary else palette.textSecondary.copy(alpha = 0.4f)
                    )
                }

                // Interactive Neumorphic Slider
                Slider(
                    value = effectiveVolume,
                    onValueChange = { newVal ->
                        val rounded = (newVal * 100f).roundToInt() / 100f
                        onVolumeChange(rounded)
                    },
                    onValueChangeFinished = onVolumeChangeFinished,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    enabled = isEnabled,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = palette.surfaceShadow.copy(alpha = if (palette.isDark) 0.5f else 0.25f),
                        disabledThumbColor = palette.textSecondary.copy(alpha = 0.5f),
                        disabledActiveTrackColor = palette.textSecondary.copy(alpha = 0.25f),
                        disabledInactiveTrackColor = palette.surfaceShadow.copy(alpha = 0.15f)
                    )
                )

                // Plus Stepper (+10%)
                Cubix3DButton(
                    onClick = {
                        val stepped = ((volume + 0.1f).coerceIn(0f, 1f) * 10f).roundToInt() / 10f
                        onVolumeChange(stepped)
                        onVolumeChangeFinished()
                    },
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    backgroundColor = palette.surface,
                    contentColor = palette.textPrimary,
                    depth = 3.dp,
                    enabled = isEnabled && volume < 1f
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase Volume",
                        modifier = Modifier.size(16.dp),
                        tint = if (isEnabled && volume < 1f) palette.textPrimary else palette.textSecondary.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
