package com.rudra.jarvis
import java.net.URL
import org.json.JSONObject
import kotlin.concurrent.thread
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.content.SharedPreferences
import android.text.InputType
import android.text.method.ScrollingMovementMethod
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
    private lateinit var chatBox: TextView
private lateinit var typedInput: EditText
private var continuousMode = false
private var waitingForWakeCommand = false
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
    layout.setPadding(45, 45, 45, 45)
    layout.setBackgroundColor(Color.rgb(3, 5, 18))

    val title = TextView(this)
    title.text = "JARVIS"
    title.textSize = 42f
    title.setTextColor(Color.CYAN)
    title.gravity = Gravity.CENTER
    title.setPadding(0, 0, 0, 20)

    val orb = TextView(this)
    orb.text = "◉"
    orb.textSize = 90f
    orb.setTextColor(Color.CYAN)
    orb.gravity = Gravity.CENTER
    orb.setShadowLayer(35f, 0f, 0f, Color.CYAN)

    statusText = TextView(this)
    statusText.text = "SYSTEM ONLINE"
    statusText.textSize = 20f
    statusText.setTextColor(Color.WHITE)
    statusText.gravity = Gravity.CENTER
    statusText.setPadding(0, 15, 0, 10)

    commandText = TextView(this)
    commandText.text = "Say: play kesariya on youtube"
    commandText.textSize = 15f
    commandText.setTextColor(Color.LTGRAY)
    commandText.gravity = Gravity.CENTER
    commandText.setPadding(0, 0, 0, 35)

    chatBox = TextView(this)
chatBox.text = "Chat started...\n"
chatBox.textSize = 14f
chatBox.setTextColor(Color.LTGRAY)
chatBox.setPadding(20, 20, 20, 20)
chatBox.movementMethod = ScrollingMovementMethod()

    val micButton = Button(this)
    micButton.text = "🎙  SPEAK TO JARVIS"
    micButton.textSize = 18f
    micButton.setTextColor(Color.WHITE)
    micButton.setPadding(30, 22, 30, 22)
    typedInput = EditText(this)
typedInput.hint = "Type your message..."
typedInput.setTextColor(Color.WHITE)
typedInput.setHintTextColor(Color.GRAY)
typedInput.inputType = InputType.TYPE_CLASS_TEXT

val sendButton = Button(this)
sendButton.text = "SEND TO JARVIS"

sendButton.setOnClickListener {
    val text = typedInput.text.toString().trim()

    if (text.isNotBlank()) {
        addChat("You", text)
        typedInput.setText("")
        handleCommand(text)
    }
}

val continuousButton = Button(this)
continuousButton.text = "🎧 CONTINUOUS MODE: OFF"

continuousButton.setOnClickListener {
    continuousMode = !continuousMode

    if (continuousMode) {
        continuousButton.text = "🎧 CONTINUOUS MODE: ON"
        statusText.text = "Say Hey Jarvis"
        speak("Continuous mode on. Say Hey Jarvis")
        startVoiceInput()
    } else {
        continuousButton.text = "🎧 CONTINUOUS MODE: OFF"
        statusText.text = "Continuous mode off"
        speak("Continuous mode off")
    }
}
    val btnBg = GradientDrawable()
    btnBg.cornerRadius = 45f
    btnBg.setColor(Color.rgb(0, 120, 140))
    btnBg.setStroke(3, Color.CYAN)
    micButton.background = btnBg

    micButton.setOnClickListener {
        statusText.text = "LISTENING..."
        checkMicPermissionAndListen()
    }

    val settingsButton = Button(this)
    settingsButton.text = "⚙ GEMINI KEY SETTINGS"
    settingsButton.textSize = 14f
    settingsButton.setTextColor(Color.WHITE)

    val settingsBg = GradientDrawable()
    settingsBg.cornerRadius = 35f
    settingsBg.setColor(Color.rgb(20, 25, 45))
    settingsBg.setStroke(2, Color.DKGRAY)
    settingsButton.background = settingsBg

    geminiKeyInput = EditText(this)
    geminiKeyInput.hint = "Paste Gemini API Key"
    geminiKeyInput.setText(prefs.getString("gemini_key", ""))
    geminiKeyInput.setTextColor(Color.WHITE)
    geminiKeyInput.setHintTextColor(Color.GRAY)
    geminiKeyInput.inputType = InputType.TYPE_CLASS_TEXT
    geminiKeyInput.visibility = View.GONE

    val saveKeyButton = Button(this)
    saveKeyButton.text = "SAVE GEMINI KEY"
    saveKeyButton.visibility = View.GONE

    settingsButton.setOnClickListener {
        if (geminiKeyInput.visibility == View.GONE) {
            geminiKeyInput.visibility = View.VISIBLE
            saveKeyButton.visibility = View.VISIBLE
        } else {
            geminiKeyInput.visibility = View.GONE
            saveKeyButton.visibility = View.GONE
        }
    }

    saveKeyButton.setOnClickListener {
        prefs.edit()
            .putString("gemini_key", geminiKeyInput.text.toString().trim())
            .apply()

        speak("Gemini key saved")
        statusText.text = "GEMINI KEY SAVED"
        geminiKeyInput.visibility = View.GONE
        saveKeyButton.visibility = View.GONE
    }

    val helpText = TextView(this)
    helpText.text = """
        Try:
        
        play arijit singh sad song
        open whatsapp
        camera kholo
        take selfie
        hello jarvis
    """.trimIndent()
    helpText.textSize = 14f
    helpText.setTextColor(Color.GRAY)
    helpText.gravity = Gravity.CENTER
    helpText.setPadding(0, 35, 0, 0)

    layout.addView(title)
    layout.addView(orb)
    layout.addView(statusText)
    layout.addView(commandText)
    layout.addView(micButton)
    layout.addView(chatBox)
layout.addView(typedInput)
layout.addView(sendButton)
layout.addView(continuousButton)
    layout.addView(settingsButton)
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

        if (continuousMode) {
            handleContinuousCommand(command)
        } else {
            addChat("You", command)
            handleCommand(command)
        }

    } else {
        statusText.text = "No command received"

        if (continuousMode) {
            startVoiceInput()
        }
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
    private fun addChat(sender: String, message: String) {
    chatBox.append("\n$sender: $message\n")
    }
    private fun handleContinuousCommand(command: String) {

    if (command.contains("hey jarvis") || command.contains("hello jarvis")) {
        waitingForWakeCommand = true
        statusText.text = "Jarvis activated"
        speak("Yes sir")
        startVoiceInput()
        return
    }

    if (waitingForWakeCommand) {
        waitingForWakeCommand = false
        addChat("You", command)
        handleCommand(command)

        if (continuousMode) {
            startVoiceInput()
        }

        return
    }

    statusText.text = "Waiting for Hey Jarvis"

    if (continuousMode) {
        startVoiceInput()
    }
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
You are Jarvis, a smart Android assistant.

Convert the user's command into ONLY valid JSON.

Available actions:

1. open_app
Use when user wants to open any installed app.
Example: open whatsapp, camera kholo, open physics wallah

2. web_search
Use when user wants to search anything on Google/browser.
Example: google pe motu patlu search karo, search weather in lucknow

3. open_url
Use when user wants to open a website.
Example: open google.com, open youtube.com

4. youtube
Use when user wants to play/search a song/video on YouTube.
Example: kesariya chalao, play arijit singh sad song

5. spotify
Use only when Spotify is clearly mentioned.
Example: play perfect on spotify

6. maps
Use when user asks for location, route, navigation, nearby places.
Example: maps pe Lucknow railway station dikhao

7. selfie
Use when user wants selfie/photo.
Example: selfie le lo, take selfie

8. camera
Use when user wants to open camera.
Example: camera kholo

9. chat
Use for normal conversation.

Rules:
- Return ONLY JSON.
- No markdown.
- No explanation.
- If it is a normal conversation, use chat.
- For Hinglish/Hindi, understand meaning naturally.
- Keep query short and useful.

JSON format:
{"action":"youtube","query":"Arijit Singh sad song","reply":"Playing Arijit Singh sad song on YouTube"}

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
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$geminiKey"
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

            try {
    val actionJson = JSONObject(text)

    runOnUiThread {
        runGeminiAction(actionJson)
    }

} catch (e: Exception) {

    runOnUiThread {
        statusText.text = "Jarvis replied"
        addChat("Jarvis", text)
        speak(text)
    }
}

        } catch (e: Exception) {
    runOnUiThread {
        statusText.text = "Gemini error: ${e.message}"
        addChat("Jarvis", "Gemini error: ${e.message}")
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
        addChat("Jarvis", reply)
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

        "web_search" -> {
            val url = "https://www.google.com/search?q=${Uri.encode(query)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        "open_url" -> {
            var url = query.trim()

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://$url"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        "maps" -> {
            val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            }
        }

        "camera" -> {
            openApp("camera")
        }

        "selfie" -> {
            openCameraSelfie()
        }

        "chat" -> {
            val message = reply.ifBlank { "I am here, sir." }
            statusText.text = "Jarvis replied"

            if (reply.isBlank()) {
                addChat("Jarvis", message)
                speak(message)
            }
        }

        else -> {
            val fallback = reply.ifBlank { "I did not understand that action." }
            statusText.text = "Unknown action"
            addChat("Jarvis", fallback)
            speak(fallback)
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
