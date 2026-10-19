package com.example.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class AudioVoiceHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null
    private var recordingJob: Job? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    // Real-time audio waveform amplitude history for visualization (0.0f .. 1.0f)
    private val _recordingAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val recordingAmplitudes: StateFlow<List<Float>> = _recordingAmplitudes.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    private val _isAudioPaused = MutableStateFlow(false)
    val isAudioPaused: StateFlow<Boolean> = _isAudioPaused.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _playbackPositionSeconds = MutableStateFlow(0)
    val playbackPositionSeconds: StateFlow<Int> = _playbackPositionSeconds.asStateFlow()

    fun startRecording(): Result<File> {
        return try {
            stopPlaying()
            val audioDir = File(context.filesDir, "voice_messages").apply {
                if (!exists()) mkdirs()
            }
            val audioFile = File(audioDir, "voice_${System.currentTimeMillis()}.m4a")
            currentRecordingFile = audioFile

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            _isRecording.value = true
            _recordingSeconds.value = 0
            _recordingAmplitudes.value = List(16) { 0.15f }

            // Amplitude and timer ticker loop
            recordingJob?.cancel()
            recordingJob = scope.launch {
                var ticks = 0
                while (_isRecording.value) {
                    delay(100)
                    ticks++
                    if (ticks % 10 == 0) {
                        _recordingSeconds.value = ticks / 10
                    }
                    val maxAmp = try {
                        mediaRecorder?.maxAmplitude ?: 0
                    } catch (_: Exception) { 0 }
                    val normalized = (maxAmp / 32767f).coerceIn(0.12f, 1f)
                    val currentList = _recordingAmplitudes.value.toMutableList()
                    if (currentList.size >= 24) {
                        currentList.removeAt(0)
                    }
                    currentList.add(normalized)
                    _recordingAmplitudes.value = currentList
                }
            }

            Result.success(audioFile)
        } catch (e: Exception) {
            Log.e("AudioVoiceHelper", "Failed to start recording: ${e.message}")
            _isRecording.value = false
            currentRecordingFile = null
            Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        if (!_isRecording.value) return null
        recordingJob?.cancel()
        return try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    Log.w("AudioVoiceHelper", "Recorder stop exception: ${e.message}")
                }
                release()
            }
            mediaRecorder = null
            _isRecording.value = false
            val file = currentRecordingFile
            currentRecordingFile = null
            file
        } catch (e: Exception) {
            Log.e("AudioVoiceHelper", "Failed to finalize audio recording: ${e.message}")
            _isRecording.value = false
            currentRecordingFile = null
            null
        }
    }

    fun cancelRecording() {
        recordingJob?.cancel()
        try {
            mediaRecorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
            mediaRecorder = null
            currentRecordingFile?.delete()
            currentRecordingFile = null
            _isRecording.value = false
            _recordingSeconds.value = 0
            _recordingAmplitudes.value = emptyList()
        } catch (e: Exception) {
            Log.w("AudioVoiceHelper", "Cancel recording error: ${e.message}")
        }
    }

    fun playVoiceMessage(messageId: String, filePath: String?, onComplete: () -> Unit = {}) {
        if (filePath.isNullOrEmpty()) {
            Log.w("AudioVoiceHelper", "No audio file path provided")
            return
        }

        // If toggling the same active audio message: handle pause/resume
        if (_playingMessageId.value == messageId) {
            val player = mediaPlayer
            if (player != null) {
                if (_isAudioPlaying.value) {
                    player.pause()
                    _isAudioPlaying.value = false
                    _isAudioPaused.value = true
                    progressJob?.cancel()
                    return
                } else if (_isAudioPaused.value) {
                    player.start()
                    _isAudioPlaying.value = true
                    _isAudioPaused.value = false
                    startProgressTracker(onComplete)
                    return
                }
            }
        }

        stopPlaying()

        val isRemoteUrl = filePath.startsWith("http://") || filePath.startsWith("https://")
        if (!isRemoteUrl) {
            val file = File(filePath)
            if (!file.exists()) {
                Log.w("AudioVoiceHelper", "Audio file does not exist at: $filePath")
                return
            }
        }

        try {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    _isAudioPlaying.value = true
                    _isAudioPaused.value = false
                    startProgressTracker(onComplete)
                }
                setOnCompletionListener {
                    progressJob?.cancel()
                    _playingMessageId.value = null
                    _isAudioPlaying.value = false
                    _isAudioPaused.value = false
                    _playbackProgress.value = 0f
                    _playbackPositionSeconds.value = 0
                    onComplete()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioVoiceHelper", "MediaPlayer error: what=$what extra=$extra")
                    stopPlaying()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
            _playingMessageId.value = messageId
        } catch (e: Exception) {
            Log.e("AudioVoiceHelper", "Failed to play voice audio: ${e.message}")
            stopPlaying()
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        val total = player.duration
        if (total > 0) {
            val targetMs = (progress * total).toInt().coerceIn(0, total)
            player.seekTo(targetMs)
            _playbackProgress.value = progress.coerceIn(0f, 1f)
            _playbackPositionSeconds.value = targetMs / 1000
        }
    }

    private fun startProgressTracker(onComplete: () -> Unit) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (mediaPlayer != null && _isAudioPlaying.value) {
                val current = mediaPlayer?.currentPosition ?: 0
                val total = mediaPlayer?.duration ?: 1
                if (total > 0) {
                    _playbackProgress.value = (current.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    _playbackPositionSeconds.value = current / 1000
                }
                delay(100)
            }
        }
    }

    fun stopPlaying() {
        progressJob?.cancel()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            _playingMessageId.value = null
            _isAudioPlaying.value = false
            _isAudioPaused.value = false
            _playbackProgress.value = 0f
            _playbackPositionSeconds.value = 0
        } catch (e: Exception) {
            Log.w("AudioVoiceHelper", "Stop playing error: ${e.message}")
        }
    }

    fun release() {
        cancelRecording()
        stopPlaying()
    }
}
