package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.model.AlarmSoundOption
import com.example.domain.model.VibrationPatternOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Plays alarm tones and vibration strictly on AudioAttributes.USAGE_ALARM so alarms ring
 * even when the phone is in Silent or Vibrate mode.
 */
class AlarmSoundPlayer(private val context: Context) {

  private var mediaPlayer: MediaPlayer? = null
  private var audioTrack: AudioTrack? = null
  private var previewJob: Job? = null

  private val alarmAttributes: AudioAttributes =
    AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

  @Synchronized
  fun start(sound: AlarmSoundOption, vibration: VibrationPatternOption) {
    stop()
    startVibration(vibration)
    if (sound == AlarmSoundOption.SYSTEM_ALARM) {
      val startedSystem = startSystemAlarmSound()
      if (!startedSystem) {
        startSynthesizedTone(AlarmSoundOption.MORNING_CHIME)
      }
    } else {
      startSynthesizedTone(sound)
    }
  }

  fun preview(
    sound: AlarmSoundOption,
    vibration: VibrationPatternOption,
    scope: CoroutineScope,
    durationMillis: Long = 2200L,
  ) {
    previewJob?.cancel()
    start(sound, vibration)
    previewJob =
      scope.launch(Dispatchers.Main) {
        delay(durationMillis)
        stop()
      }
  }

  @Synchronized
  fun stop() {
    previewJob?.cancel()
    previewJob = null

    runCatching {
      mediaPlayer?.let { mp ->
        if (mp.isPlaying) mp.stop()
        mp.release()
      }
    }
    mediaPlayer = null

    runCatching {
      audioTrack?.let { track ->
        track.pause()
        track.flush()
        track.release()
      }
    }
    audioTrack = null

    stopVibration()
  }

  private fun startSystemAlarmSound(): Boolean {
    return runCatching {
      val uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
          ?: return false

      val player =
        MediaPlayer().apply {
          setAudioAttributes(alarmAttributes)
          setDataSource(context, uri)
          isLooping = true
          prepare()
          start()
        }
      mediaPlayer = player
      true
    }.getOrDefault(false)
  }

  private fun startSynthesizedTone(option: AlarmSoundOption) {
    runCatching {
      val sampleRate = 22_050
      val pcmData = generateTonePcm(option, sampleRate)
      val bufferBytes = pcmData.size * 2

      val track =
        AudioTrack.Builder()
          .setAudioAttributes(alarmAttributes)
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferBytes)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

      track.write(pcmData, 0, pcmData.size)
      track.setLoopPoints(0, pcmData.size, -1)
      track.play()
      audioTrack = track
    }
  }

  private fun generateTonePcm(option: AlarmSoundOption, sampleRate: Int): ShortArray {
    val durationSec =
      when (option) {
        AlarmSoundOption.MORNING_CHIME -> 2.4
        AlarmSoundOption.CRISP_BELL -> 1.8
        AlarmSoundOption.CLASSIC_TONE -> 1.2
        AlarmSoundOption.URGENT_RING -> 1.0
        AlarmSoundOption.SYSTEM_ALARM -> 2.0
      }
    val totalSamples = (sampleRate * durationSec).toInt()
    val samples = ShortArray(totalSamples)

    for (i in 0 until totalSamples) {
      val t = i.toDouble() / sampleRate
      val value =
        when (option) {
          AlarmSoundOption.MORNING_CHIME -> {
            // Two soft ascending warm bell notes (C5 + E5 + G5)
            val note1 = chimeNote(t, 0.0, 523.25) + 0.45 * chimeNote(t, 0.0, 1046.5)
            val note2 = chimeNote(t, 0.45, 659.25) + 0.45 * chimeNote(t, 0.45, 1318.5)
            val note3 = chimeNote(t, 0.90, 783.99) + 0.35 * chimeNote(t, 0.90, 1567.98)
            0.38 * (note1 + note2 + note3)
          }
          AlarmSoundOption.CRISP_BELL -> {
            val bell1 = chimeNote(t, 0.0, 880.0, decay = 4.2) + 0.35 * chimeNote(t, 0.0, 1760.0, decay = 5.5)
            val bell2 = chimeNote(t, 0.35, 1174.66, decay = 4.0) + 0.3 * chimeNote(t, 0.35, 2349.32, decay = 5.5)
            0.42 * (bell1 + bell2)
          }
          AlarmSoundOption.CLASSIC_TONE -> {
            val gate =
              if ((t in 0.0..0.22) || (t in 0.34..0.56)) 1.0 else 0.0
            0.48 * gate * (sin(2 * PI * 800.0 * t) + 0.25 * sin(2 * PI * 1600.0 * t))
          }
          AlarmSoundOption.URGENT_RING,
          AlarmSoundOption.SYSTEM_ALARM -> {
            val pulse =
              if ((t in 0.0..0.14) || (t in 0.20..0.34) || (t in 0.40..0.54)) 1.0 else 0.0
            0.55 * pulse * (sin(2 * PI * 987.77 * t) + 0.4 * sin(2 * PI * 1479.98 * t))
          }
        }
      val clamped = value.coerceIn(-0.95, 0.95)
      samples[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
    }
    return samples
  }

  private fun chimeNote(
    t: Double,
    start: Double,
    freq: Double,
    decay: Double = 3.2,
  ): Double {
    if (t < start) return 0.0
    val dt = t - start
    val attack = (dt / 0.012).coerceAtMost(1.0)
    val envelope = attack * exp(-decay * dt)
    return envelope * sin(2 * PI * freq * dt)
  }

  private fun startVibration(option: VibrationPatternOption) {
    if (option == VibrationPatternOption.OFF || option.pattern.isEmpty()) return
    val vibrator = getVibrator() ?: return
    if (!vibrator.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val effect = VibrationEffect.createWaveform(option.pattern, 0)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        vibrator.vibrate(
          effect,
          android.os.VibrationAttributes.Builder()
            .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
            .build(),
        )
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(effect, alarmAttributes)
      }
    } else {
      @Suppress("DEPRECATION")
      vibrator.vibrate(option.pattern, 0, alarmAttributes)
    }
  }

  private fun stopVibration() {
    runCatching { getVibrator()?.cancel() }
  }

  private fun getVibrator(): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
