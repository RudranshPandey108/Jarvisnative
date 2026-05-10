package com.rudra.jarvis
import android.os.Build

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var chatBox: TextView
    private lateinit var typedInput: EditText
    private lateinit var geminiKeyInput: EditText
    private lateinit var youtubeKeyInput: EditText
    private lateinit var tts: TextToSpeech
    private lateinit var prefs: SharedPreferences

    private var continuousMode = false
    private var waitingForWakeCommand = false

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
        commandText.setPadding(0, 0, 0, 25)

        val micButton = Button(this)
        micButton.text = "🎙 SPEAK TO JARVIS"
        micButton.textSize = 18f
        micButton.setTextColor(Color.WHITE)
        micButton.setPadding(30, 22, 30, 22)
        micButton.background = roundedBg(Color.rgb(0, 120, 140), Color.CYAN, 45f)

        micButton.setOnClickListener {
            statusText.text = "LISTENING..."
            checkMicPermissionAndListen()
        }

        chatBox = TextView(this)
        chatBox.text = "Chat started...\n"
        chatBox.textSize = 14f
        chatBox.setTextColor(Color.LTGRAY)
        chatBox.setPadding(20, 25, 20, 20)
        chatBox.movementMethod = ScrollingMovementMethod()

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
                waitingForWakeCommand = false
                statusText.text = "Continuous mode off"
                speak("Continuous mode off")
            }
        }
        val serviceButton = Button(this)
serviceButton.text = "🚀 START JARVIS SERVICE"

serviceButton.setOnClickListener {
    val intent = Intent(this, JarvisService::class.java)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }

    statusText.text = "JARVIS service started"
    speak("Jarvis service started")
}
val accessibilityButton = Button(this)
accessibilityButton.text = "♿ ENABLE ACCESSIBILITY"

accessibilityButton.setOnClickListener {
    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
    startActivity(intent)
    speak("Enable Jarvis accessibility service")
}

        val settingsButton = Button(this)
        settingsButton.text = "⚙ API KEY SETTINGS"
        settingsButton.setTextColor(Color.WHITE)
        settingsButton.background = roundedBg(Color.rgb(20, 25, 45), Color.DKGRAY, 35f)

        geminiKeyInput = EditText(this)
        geminiKeyInput.hint = "Paste Gemini API Key"
        geminiKeyInput.setText(prefs.getString("gemini_key", ""))
        geminiKeyInput.setTextColor(Color.WHITE)
        geminiKeyInput.setHintTextColor(Color.GRAY)
        geminiKeyInput.inputType = InputType.TYPE_CLASS_TEXT
        geminiKeyInput.visibility = View.GONE

        youtubeKeyInput = EditText(this)
        youtubeKeyInput.hint = "Paste YouTube API Key"
        youtubeKeyInput.setText(prefs.getString("youtube_key", ""))
        youtubeKeyInput.setTextColor(Color.WHITE)
        youtubeKeyInput.setHintTextColor(Color.GRAY)
        youtubeKeyInput.inputType = InputType.TYPE_CLASS_TEXT
        youtubeKeyInput.visibility = View.GONE

        val saveKeyButton = Button(this)
        saveKeyButton.text = "SAVE API KEYS"
        saveKeyButton.visibility = View.GONE

        settingsButton.setOnClickListener {
            val show = geminiKeyInput.visibility == View.GONE
            geminiKeyInput.visibility = if (show) View.VISIBLE else View.GONE
            youtubeKeyInput.visibility = if (show) View.VISIBLE else View.GONE
            saveKeyButton.visibility = if (show) View.VISIBLE else View.GONE
        }

        saveKeyButton.setOnClickListener {
            prefs.edit()
                .putString("gemini_key", geminiKeyInput.text.toString().trim())
                .putString("youtube_key", youtubeKeyInput.text.toString().trim())
                .apply()

            statusText.text = "API KEYS SAVED"
            speak("API keys saved")
            geminiKeyInput.visibility = View.GONE
            youtubeKeyInput.visibility = View.GONE
            saveKeyButton.visibility = View.GONE
        }

        val helpText = TextView(this)
        helpText.text = """
            Try:
            
            hi hello
            google pe motu patlu search karo
            play arijit singh sad song
            open whatsapp
            camera kholo
            take selfie
            maps pe lucknow railway station dikhao
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
        layout.addView(serviceButton)
        layout.addView(accessibilityButton)
        layout.addView(settingsButton)
        layout.addView(geminiKeyInput)
        layout.addView(youtubeKeyInput)
        layout.addView(saveKeyButton)
        layout.addView(helpText)

        setContentView(layout)

if (intent.getBooleanExtra("wake_word_detected", false)) {
    statusText.text = "Jarvis activated"
    addChat("Jarvis", "Yes sir, I am listening.")
    speak("Yes sir, I am listening.")
} else {
    speak("Jarvis online")
}
    }

    private fun roundedBg(color: Int, strokeColor: Int, radius: Float): GradientDrawable {
        val bg = GradientDrawable()
        bg.cornerRadius = radius
        bg.setColor(color)
        bg.setStroke(3, strokeColor)
        return bg
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
            speak("Speech recognition is not available")
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
            if (continuousMode) startVoiceInput()
        }
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
            return
        }

        statusText.text = "Waiting for Hey Jarvis"
        if (continuousMode) startVoiceInput()
    }

private fun handleCommand(command: String) {
    val normalized = command.lowercase()
        .replace("ओपन", "open")
        .replace("व्हाट्सएप", "whatsapp")
        .replace("यूट्यूब", "youtube")
        .replace("स्पॉटिफाई", "spotify")
        .replace("कैमरा", "camera")
        .replace("सेटिंग", "settings")

    // Basic offline commands first
    when {
        normalized.startsWith("open ") -> {
            val app = normalized.replace("open", "").trim()
            openApp(app)
            return
        }

        normalized.contains("google") && normalized.contains("search") -> {
            val q = normalized
                .replace("google", "")
                .replace("pe", "")
                .replace("par", "")
                .replace("search", "")
                .replace("karo", "")
                .replace("kar", "")
                .trim()

            openWebSearch(q)
            return
        }

        normalized.contains("youtube") || normalized.contains("chalao") || normalized.contains("gana") -> {
            val q = normalized
                .replace("youtube", "")
                .replace("pe", "")
                .replace("par", "")
                .replace("play", "")
                .replace("chalao", "")
                .replace("gana", "")
                .trim()

            playOnYouTube(q)
            return
        }

        normalized.contains("camera") || normalized.contains("कैमरा") -> {
            openApp("camera")
            return
        }

        normalized.contains("selfie") -> {
            openCameraSelfie()
            return
        }

        normalized.contains("go back") ||
        normalized.contains("back jao") ||
        normalized.contains("peeche jao") -> {
            JarvisAccessibilityService.instance?.goBack()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("go home") ||
        normalized.contains("home jao") ||
        normalized.contains("home screen") -> {
            JarvisAccessibilityService.instance?.goHome()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("recent") ||
        normalized.contains("recent apps") -> {
            JarvisAccessibilityService.instance?.openRecents()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("notification") ||
        normalized.contains("notifications kholo") -> {
            JarvisAccessibilityService.instance?.openNotifications()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("scroll down") ||
        normalized.contains("neeche scroll") ||
        normalized.contains("niche scroll") -> {
            JarvisAccessibilityService.instance?.scrollDown()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("scroll up") ||
        normalized.contains("upar scroll") -> {
            JarvisAccessibilityService.instance?.scrollUp()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }

        normalized.contains("tap center") ||
        normalized.contains("beech mein tap") -> {
            JarvisAccessibilityService.instance?.tapCenter()
                ?: speak("Please enable Jarvis accessibility service")
            return
        }
    }

    // Only normal chat goes to Gemini
    askGeminiForAction(normalized)
}
    private fun addChat(sender: String, message: String) {
        chatBox.append("\n$sender: $message\n")
    }

    private fun askGeminiForAction(command: String) {
        val geminiKey = prefs.getString("gemini_key", "") ?: ""

        if (geminiKey.isBlank()) {
            statusText.text = "Gemini API key missing"
            speak("Please save Gemini API key first")
            return
        }

        statusText.text = "Thinking..."

        thread {
            try {
                val prompt = """
                    You are Jarvis, a smart Android assistant.

                    Convert the user's command into ONLY valid JSON.

                    Available actions:
                    open_app, web_search, open_url, youtube, spotify, maps, selfie, camera, chat

                    Use:
                    - open_app for opening installed apps
                    - web_search for Google/browser search
                    - open_url for websites
                    - youtube for playing/searching video or song
                    - spotify only if Spotify is mentioned
                    - maps for location/navigation/nearby places
                    - selfie for taking selfie/photo
                    - camera for opening camera
                    - chat for normal conversation

                    Rules:
                    - Return ONLY JSON.
                    - No markdown.
                    - No explanation.
                    - For Hinglish/Hindi, understand meaning naturally.
                    - Keep query short.

                    JSON format:
                    {"action":"youtube","query":"Arijit Singh sad song","reply":"Playing Arijit Singh sad song on YouTube"}

                    User command: $command
                """.trimIndent()

                val requestJson = JSONObject()
                val contents = JSONArray()
                val contentObj = JSONObject()
                val parts = JSONArray()
                val partObj = JSONObject()

                partObj.put("text", prompt)
                parts.put(partObj)
                contentObj.put("parts", parts)
                contents.put(contentObj)
                requestJson.put("contents", contents)

                val url = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$geminiKey"
                )

                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use {
                    it.write(requestJson.toString().toByteArray())
                }

                val responseCode = connection.responseCode

                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    val err = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                    throw Exception("HTTP $responseCode")
                }

                val json = JSONObject(responseText)

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
                    statusText.text = "Gemini error"
                    addChat("Jarvis", "Gemini error. Check API key, internet, or model access.")
                    speak("Gemini error. Check API key or internet.")
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
            "youtube" -> playOnYouTube(query)
            "spotify" -> playOnSpotify(query)
            "open_app" -> openApp(query)
            "web_search" -> openWebSearch(query)
            "open_url" -> openUrl(query)
            "maps" -> openMaps(query)
            "camera" -> openApp("camera")
            "selfie" -> openCameraSelfie()
            "chat" -> {
                val message = reply.ifBlank { "I am here, sir." }
                if (reply.isBlank()) {
                    addChat("Jarvis", message)
                    speak(message)
                }
                statusText.text = "Jarvis replied"
            }
            else -> {
                statusText.text = "Unknown action"
                speak("I did not understand that action")
            }
        }
    }

    private fun openWebSearch(query: String) {
        val url = "https://www.google.com/search?q=${Uri.encode(query)}"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun openUrl(query: String) {
        var url = query.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun openMaps(query: String) {
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun openApp(appName: String) {
        val cleanedName = appName.lowercase().trim()

        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = packageManager.queryIntentActivities(mainIntent, 0)

        for (app in apps) {
            val label = app.loadLabel(packageManager).toString().lowercase()
            val packageName = app.activityInfo.packageName

            if (label.contains(cleanedName) || cleanedName.contains(label)) {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

                if (launchIntent != null) {
                    statusText.text = "Opening $label"
                    speak("Opening $label")
                    startActivity(launchIntent)
                    return
                }
            }
        }

        statusText.text = "App not found: $cleanedName"
        speak("I could not find $cleanedName")
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

    private fun playOnYouTube(song: String) {
        val youtubeKey = prefs.getString("youtube_key", "") ?: ""

        if (song.isBlank()) {
            speak("Please say the song name")
            return
        }

        if (youtubeKey.isBlank()) {
            val url = "https://www.youtube.com/results?search_query=${Uri.encode(song)}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        }

        statusText.text = "Searching YouTube for $song"

        thread {
            try {
                val apiUrl =
                    "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q=${
                        Uri.encode(song)
                    }&key=$youtubeKey"

                val response = URL(apiUrl).readText()
                val json = JSONObject(response)
                val items = json.getJSONArray("items")

                if (items.length() > 0) {
                    val videoId = items
                        .getJSONObject(0)
                        .getJSONObject("id")
                        .getString("videoId")

                    runOnUiThread {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
                        intent.setPackage("com.google.android.youtube")

                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            val webUrl = "https://www.youtube.com/watch?v=$videoId"
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)))
                        }
                    }
                } else {
                    runOnUiThread { speak("No video found") }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    val url = "https://www.youtube.com/results?search_query=${Uri.encode(song)}"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
        }
    }

    private fun playOnSpotify(song: String) {
        if (song.isBlank()) {
            speak("Please say the song name")
            return
        }

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
