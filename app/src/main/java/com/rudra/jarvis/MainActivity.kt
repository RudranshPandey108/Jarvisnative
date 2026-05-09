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

    private val appPackages = mapOf(
        "youtube" to "com.google.android.youtube",
        "spotify" to "com.spotify.music",
        "youtube music" to "com.google.android.apps.youtube.music",
        "whatsapp" to "com.whatsapp",
        "instagram" to "com.instagram.android",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps"
    )

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
        when {
            command.startsWith("open ") -> {
                val appName = command.removePrefix("open ").trim()
                openApp(appName)
            }

            command.contains("play") && command.contains("youtube music") -> {
                val song = extractSongName(command, "youtube music")
                playOnYouTubeMusic(song)
            }

            command.contains("play") && command.contains("youtube") -> {
                val song = extractSongName(command, "youtube")
                playOnYouTube(song)
            }

            command.contains("play") && command.contains("spotify") -> {
                val song = extractSongName(command, "spotify")
                playOnSpotify(song)
            }

            command.contains("hello") || command.contains("hi") -> {
                statusText.text = "Hello sir"
                speak("Hello sir, I am ready")
            }

            else -> {
                statusText.text = "Command not understood"
                speak("Sorry, I did not understand that command")
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
        val key = appPackages.keys.firstOrNull { appName.contains(it) }

        if (key == null) {
            statusText.text = "App not added yet: $appName"
            speak("This app is not added yet")
            return
        }

        val packageName = appPackages[key]
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName!!)

        if (launchIntent != null) {
            statusText.text = "Opening $key"
            speak("Opening $key")
            startActivity(launchIntent)
        } else {
            statusText.text = "$key is not installed"
            speak("$key is not installed")
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
