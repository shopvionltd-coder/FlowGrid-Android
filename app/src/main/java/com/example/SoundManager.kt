package com.shopvion.flowgrid

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R

/**
 * SoundManager:
 * High-performance audio engine managing background music (MediaPlayer),
 * sound effects (SoundPool), haptics (Vibrator), and real-time volume controls.
 */
class SoundManager(
    private val context: Context,
    private val prefsManager: PreferencesManager = PreferencesManager(context)
) {
    private var soundPool: SoundPool? = null
    private var popConnectId: Int = 0
    private var lineCutId: Int = 0
    private var levelWinId: Int = 0
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    // Volume states (0.0f to 1.0f)
    @Volatile
    private var musicVolume: Float = 0.6f

    @Volatile
    private var soundFxVolume: Float = 0.8f

    init {
        // 1. Restore persisted volume settings upon initial launch
        musicVolume = prefsManager.getMusicVolume().coerceIn(0f, 1f)
        soundFxVolume = prefsManager.getSoundFxVolume().coerceIn(0f, 1f)

        // 2. Initialize SoundPool for low-latency SFX playback
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        try {
            popConnectId = soundPool?.load(context, R.raw.pop_connect, 1) ?: 0
            lineCutId = soundPool?.load(context, R.raw.line_cut, 1) ?: 0
            levelWinId = soundPool?.load(context, R.raw.level_win, 1) ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Error loading raw audio resources", e)
        }

        // 3. Initialize Vibrator service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibrator = vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // -------------------------------------------------------------
    // REAL-TIME VOLUME CONTROLS
    // -------------------------------------------------------------

    /**
     * Dynamically sets background music volume (0.0 to 1.0).
     * Applies to MediaPlayer in real-time and persists value.
     */
    fun setMusicVolume(volume: Float, persist: Boolean = true) {
        val clamped = volume.coerceIn(0f, 1f)
        musicVolume = clamped
        if (persist) {
            prefsManager.setMusicVolume(clamped)
        }
        val effectiveVol = (clamped * 0.75f).coerceIn(0f, 1f)
        try {
            mediaPlayer?.setVolume(effectiveVol, effectiveVol)
        } catch (e: Exception) {
            Log.w(TAG, "Error adjusting music volume", e)
        }
    }

    fun getMusicVolume(): Float = musicVolume

    fun increaseMusicVolume(step: Float = 0.1f): Float {
        val newVol = (musicVolume + step).coerceIn(0f, 1f)
        setMusicVolume(newVol)
        return newVol
    }

    fun decreaseMusicVolume(step: Float = 0.1f): Float {
        val newVol = (musicVolume - step).coerceIn(0f, 1f)
        setMusicVolume(newVol)
        return newVol
    }

    /**
     * Dynamically sets sound FX volume (0.0 to 1.0).
     * Applies to all subsequent soundPool.play() calls and persists value.
     */
    fun setSoundFxVolume(volume: Float, persist: Boolean = true) {
        val clamped = volume.coerceIn(0f, 1f)
        soundFxVolume = clamped
        if (persist) {
            prefsManager.setSoundFxVolume(clamped)
        }
    }

    fun getSoundFxVolume(): Float = soundFxVolume

    fun increaseSoundFxVolume(step: Float = 0.1f): Float {
        val newVol = (soundFxVolume + step).coerceIn(0f, 1f)
        setSoundFxVolume(newVol)
        playPopConnect(true)
        return newVol
    }

    fun decreaseSoundFxVolume(step: Float = 0.1f): Float {
        val newVol = (soundFxVolume - step).coerceIn(0f, 1f)
        setSoundFxVolume(newVol)
        playPopConnect(true)
        return newVol
    }

    // -------------------------------------------------------------
    // MUSIC PLAYBACK
    // -------------------------------------------------------------

    fun playMusic(enabled: Boolean = prefsManager.isMusicEnabled()) {
        if (!enabled || musicVolume <= 0f) {
            pauseMusic()
            return
        }
        try {
            val effectiveVol = (musicVolume * 0.75f).coerceIn(0f, 1f)
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(context, R.raw.bg_music)?.apply {
                    isLooping = true
                    setVolume(effectiveVol, effectiveVol)
                }
            } else {
                mediaPlayer?.setVolume(effectiveVol, effectiveVol)
            }
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing background music", e)
        }
    }

    fun pauseMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing music", e)
        }
    }

    fun resumeMusic() {
        if (prefsManager.isMusicEnabled() && musicVolume > 0f) {
            playMusic(true)
        }
    }

    // -------------------------------------------------------------
    // SOUND EFFECTS (SOUNDPOOL)
    // -------------------------------------------------------------

    fun playPopConnect(enabled: Boolean = prefsManager.isSoundFxEnabled()) {
        if (enabled && popConnectId != 0 && soundFxVolume > 0f) {
            val vol = (0.95f * soundFxVolume).coerceIn(0f, 1f)
            soundPool?.play(popConnectId, vol, vol, 1, 0, 1.0f)
        }
        vibrate(30)
    }

    fun playLineCut(enabled: Boolean = prefsManager.isSoundFxEnabled()) {
        if (enabled && lineCutId != 0 && soundFxVolume > 0f) {
            val vol = (0.75f * soundFxVolume).coerceIn(0f, 1f)
            soundPool?.play(lineCutId, vol, vol, 1, 0, 1.0f)
        }
        vibrate(20)
    }

    fun playLevelWin(enabled: Boolean = prefsManager.isSoundFxEnabled()) {
        if (enabled && levelWinId != 0 && soundFxVolume > 0f) {
            val vol = (1.0f * soundFxVolume).coerceIn(0f, 1f)
            soundPool?.play(levelWinId, vol, vol, 2, 0, 1.0f)
        }
        vibrate(90)
    }

    // -------------------------------------------------------------
    // HAPTICS & LIFECYCLE
    // -------------------------------------------------------------

    fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            soundPool?.release()
            soundPool = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing audio resources", e)
        }
    }

    companion object {
        private const val TAG = "SoundManager"
    }
}

/**
 * Backward compatibility alias:
 * Allows all existing references to AudioEngine to work transparently with SoundManager.
 */
typealias AudioEngine = SoundManager
