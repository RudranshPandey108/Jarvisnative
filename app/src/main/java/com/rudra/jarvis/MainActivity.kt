package com.rudra.jarvis
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread
import android.content.SharedPreferences
import android.text.InputType

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
    
private lateinit var prefs: SharedPreferences
private lateinit var geminiKeyInput: EditText
    private val speechRequestCode = 101
    private val micPermissionCode = 201

    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)

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
        geminiKeyInput = EditText(this)
geminiKeyInput.hint = "Paste Gemini API Key here"
geminiKeyInput.setText(prefs.getString("gemini_key", ""))
geminiKeyInput.setTextColor(Color.WHITE)
geminiKeyInput.setHintTextColor(Color.GRAY)
geminiKeyInput.inputType = InputType.TYPE_CLASS_TEXT
geminiKeyInput.setPadding(20, 20, 20, 20)

val saveKeyButton = Button(this)
saveKeyButton.text = "Save Gemini Key"

saveKeyButton.setOnClickListener {
    prefs.edit()
        .putString("gemini_key", geminiKeyInput.text.toString().trim())
        .apply()

    speak("Gemini key saved")
    statusText.text = "Gemini key saved"
}
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
        layout.addView(geminiKeyInput)
        layout.addView(saveKeyButton)
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

    askGeminiForAction(normalized)
    }
    private fun askGeminiForAction(command: String) {

    val geminiKey = prefs.getString("gemini_key", "") ?: ""

    if (geminiKey.isBlank()) {
        speak("Please save Gemini API key first")
        statusText.text = "Gemini API key missing"
        return
    }

    statusText.text = "Thinking..."

    thread {
        try {
            val prompt = """
                You are Jarvis, an Android assistant.
                Convert the user's command into ONLY valid JSON.
                
                Possible actions:
                1. youtube
                2. spotify
                3. open_app
                4. selfie
                5. chat
                
                Rules:
                - If user wants to play song/music/video, use youtube unless Spotify is mentioned.
                - If user wants to open app, use open_app.
                - If user wants selfie/photo, use selfie.
                - If normal talk, use chat.
                - Return ONLY JSON. No markdown.
                
                JSON format:
                {"action":"youtube","query":"song name","reply":"Playing song"}
                
                User command: $command
            """.trimIndent()

            val requestJson = JSONObject()
            val contents = org.json.JSONArray()
            val contentObj = JSONObject()
            val parts = org.json.JSONArray()
            val partObj = JSONObject()

            partObj.put("text", prompt)
            parts.put(partObj)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            requestJson.put("contents", contents)

            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$geminiKey"
            )

            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use {
                it.write(requestJson.toString().toByteArray())
            }

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)

            var text = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()

            text = text
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val actionJson = JSONObject(text)

            runOnUiThread {
                runGeminiAction(actionJson)
            }

        } catch (e: Exception) {
            runOnUiThread {
                statusText.text = "Gemini error"
                speak("Sorry, Gemini se response nahi aaya")
            }
        }
    }
    }
    private fun runGeminiAction(json: JSONObject) {

    val action = json.optString("action", "chat")
    val query = json.optString("query", "")
    val reply = json.optString("reply", "")

    if (reply.isNotBlank()) {
        speak(reply)
    }

    when (action) {

        "youtube" -> {
            playOnYouTube(query)
        }

        "spotify" -> {
            playOnSpotify(query)
        }

        "open_app" -> {
            openApp(query)
        }

        "selfie" -> {
            openCameraSelfie()
        }

        "chat" -> {
            statusText.text = reply.ifBlank { "Jarvis ready" }
            speak(reply.ifBlank { "I am ready" })
        }

        else -> {
            statusText.text = "Unknown action"
            speak("I did not understand the action")
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
