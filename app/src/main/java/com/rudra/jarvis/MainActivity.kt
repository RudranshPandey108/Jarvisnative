package com.rudra.jarvis
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread


import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val YOUTUBE_API_KEY = "AIzaSyAE7-4GLJQNAk5vxhPBCrRxB4pa85eg6gE"

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var tts: TextToSpeech

    private val speechRequestCode = 101
    private val micPermissionCode = 201

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(40, 40, 40, 40)
        layout.setBackgroundColor(Color.rgb(5, 5, 16))

        val title = TextView(this)
        title.text = "JARVIS NATIVE"
        title.textSize = 30f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER

        statusText = TextView(this)
        statusText.text = "Tap mic and speak a command"
        statusText.textSize = 18f
        statusText.setTextColor(Color.WHITE)
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, 40, 0, 20)

        commandText = TextView(this)
        commandText.text = "Try: play kesariya on youtube"
        commandText.textSize = 16f
        commandText.setTextColor(Color.LTGRAY)
        commandText.gravity = Gravity.CENTER
        commandText.setPadding(0, 20, 0, 40)

        val micButton = Button(this)
        micButton.text = "🎙 Speak to Jarvis"
        micButton.textSize = 20f
        micButton.setPadding(30, 20, 30, 20)

        micButton.setOnClickListener {
            checkMicPermissionAndListen()
        }

        val helpText = TextView(this)
        helpText.text = """
            Commands:
            
            open youtube
            open spotify
            open whatsapp
            play kesariya on youtube
            play arijit singh on spotify
            play perfect on youtube music
        """.trimIndent()
        helpText.textSize = 15f
        helpText.setTextColor(Color.GRAY)
        helpText.gravity = Gravity.CENTER
        helpText.setPadding(0, 45, 0, 0)

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(commandText)
        layout.addView(micButton)
        layout.addView(helpText)

        setContentView(layout)

        speak("Jarvis online")
    }

    private fun checkMicPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                micPermissionCode
            )
        } else {
            startVoiceInput()
        }
    }
    private fun openCameraSelfie() {
    statusText.text = "Opening selfie camera"
    speak("Opening selfie camera")

    val intent = Intent("android.media.action.IMAGE_CAPTURE")
    intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
    intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
    intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)

    try {
        startActivity(intent)
    } catch (e: Exception) {
        speak("Camera could not be opened")
    }
    }

    private fun startVoiceInput() {
        statusText.text = "Listening..."

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command")

        try {
            startActivityForResult(intent, speechRequestCode)
        } catch (e: ActivityNotFoundException) {
            statusText.text = "Speech recognition not available"
            speak("Speech recognition is not available on this device")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == speechRequestCode && resultCode == RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = result?.get(0)?.lowercase(Locale.getDefault()) ?: ""

            commandText.text = "You said: $command"
            handleCommand(command)
        } else {
            statusText.text = "No command received"
        }
    }

    private fun handleCommand(command: String) {
        val cmd = command.lowercase()

        val normalized = cmd
    .replace("ओपन", "open")
    .replace("व्हाट्सएप", "whatsapp")
    .replace("यूट्यूब", "youtube")
    .replace("स्पॉटिफाई", "spotify")
    .replace("कैमरा", "camera")
    .replace("सेटिंग", "settings")

    
    when {
        normalized.contains("take selfie") ||
normalized.contains("selfie lo") ||
normalized.contains("selfie le lo") ||
normalized.contains("photo lo") ||
normalized.contains("photo le lo") -> {
    openCameraSelfie()
}

        normalized.startsWith("open ") ||
        normalized.contains("khol") ||
        normalized.contains("khol do") -> {

            val appName = normalized
                .replace("open", "")
                .replace("khol do", "")
                .replace("khol", "")
                .trim()

            openApp(appName)
        }

        normalized.contains("play") ||
        normalized.contains("chalao") ||
        normalized.contains("bajao") ||
        normalized.contains("gana") -> {

            when {

                normalized.contains("spotify") -> {

                    val song = normalized
                        .replace("spotify", "")
                        .replace("play", "")
                        .replace("chalao", "")
                        .replace("gana", "")
                        .trim()

                    playOnSpotify(song)
                }

                normalized.contains("youtube music") -> {

                    val song = normalized
                        .replace("youtube music", "")
                        .replace("play", "")
                        .replace("chalao", "")
                        .replace("gana", "")
                        .trim()

                    playOnYouTubeMusic(song)
                }

                else -> {

                    val song = normalized
                        .replace("youtube", "")
                        .replace("play", "")
                        .replace("chalao", "")
                        .replace("gana", "")
                        .trim()

                    playOnYouTube(song)
                }
            }
        }

        normalized.contains("hello") ||
        normalized.contains("hi") ||
        normalized.contains("namaste") -> {

            statusText.text = "Hello sir"
            speak("Hello sir")
        }

        else -> {

            statusText.text = "Command not understood"
            speak("Sorry, command samajh nahi aaya")
        }
    }
    }

    private fun extractSongName(command: String, platform: String): String {
        return command
            .replace("play", "")
            .replace("on $platform", "")
            .replace("in $platform", "")
            .trim()
    }

    private fun openApp(appName: String) {

    val pm = packageManager

    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    var found = false

    for (app in apps) {

        val label = pm.getApplicationLabel(app)
            .toString()
            .lowercase()

        if (label.contains(appName.lowercase())) {

            val intent = pm.getLaunchIntentForPackage(app.packageName)

            if (intent != null) {

                found = true

                statusText.text = "Opening $label"

                speak("Opening $label")

                startActivity(intent)

                break
            }
        }
    }

    if (!found) {

        statusText.text = "App not found"

        speak("I could not find that app")
    }
    }
    private fun playOnYouTube(song: String) {

    if (song.isBlank()) {
        speak("Please say the song name")
        return
    }

    statusText.text = "Searching YouTube for $song"
    speak("Playing $song on YouTube")

    thread {

        try {

            val apiUrl =
                "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q=${
                    Uri.encode(song)
                }&key=$YOUTUBE_API_KEY"

            val response = URL(apiUrl).readText()

            val json = JSONObject(response)

            val items = json.getJSONArray("items")

            if (items.length() > 0) {

                val videoId = items
                    .getJSONObject(0)
                    .getJSONObject("id")
                    .getString("videoId")

                runOnUiThread {

                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("vnd.youtube:$videoId")
                    )

                    intent.setPackage("com.google.android.youtube")

                    try {
                        startActivity(intent)
                    } catch (e: Exception) {

                        val webUrl =
                            "https://www.youtube.com/watch?v=$videoId"

                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(webUrl)
                            )
                        )
                    }
                }

            } else {

                runOnUiThread {
                    speak("No video found")
                }
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text = "YouTube API Error"
                speak("Failed to fetch video")
            }
        }
    }
}

    private fun playOnYouTubeMusic(song: String) {
        if (song.isBlank()) {
            speak("Please say the song name")
            return
        }

        statusText.text = "Opening YouTube Music: $song"
        speak("Opening $song on YouTube Music")

        val url = "https://music.youtube.com/search?q=${Uri.encode(song)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.google.android.apps.youtube.music")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    private fun playOnSpotify(song: String) {
        if (song.isBlank()) {
            speak("Please say the song name")
            return
        }

        statusText.text = "Opening Spotify: $song"
        speak("Opening $song on Spotify")

        val url = "spotify:search:${Uri.encode(song)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.setPackage("com.spotify.music")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            val webUrl = "https://open.spotify.com/search/${Uri.encode(song)}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("en", "IN")
        }
    }

    private fun speak(text: String) {
        try {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
