package com.rudra.jarvis

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

class JarvisService : Service() {

    private val channelId = "jarvis_service_channel"
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())
        startWakeListening()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startWakeListening()
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, channelId)
            .setContentTitle("JARVIS is listening")
            .setContentText("Say Hey Jarvis")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun startWakeListening() {
        if (isListening) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            return
        }

        isListening = true

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: android.os.Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                restartListening()
            }

            override fun onError(error: Int) {
                restartListening()
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val spoken = matches
                    ?.joinToString(" ")
                    ?.lowercase(Locale.getDefault())
                    ?: ""

                if (
                    spoken.contains("hey jarvis") ||
                    spoken.contains("hello jarvis") ||
                    spoken.contains("jarvis")
                ) {
                    openJarvisApp()
                }

                restartListening()
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val spoken = matches
                    ?.joinToString(" ")
                    ?.lowercase(Locale.getDefault())
                    ?: ""

                if (
                    spoken.contains("hey jarvis") ||
                    spoken.contains("hello jarvis") ||
                    spoken.contains("jarvis")
                ) {
                    openJarvisApp()
                }
            }

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        listenNow()
    }

    private fun listenNow() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            restartListening()
        }
    }

    private fun restartListening() {
        handler.postDelayed({
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
            }

            listenNow()
        }, 800)
    }

    private fun openJarvisApp() {
        val launchIntent = Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launchIntent.putExtra("wake_word_detected", true)

        startActivity(launchIntent)
    }

    override fun onDestroy() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        speechRecognizer = null
        isListening = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "JARVIS Service",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
