package com.rudra.jarvis

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
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
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private val YOUTUBE_API_KEY = "PASTE_YOUR_YOUTUBE_API_KEY_HERE"

    private var isUnlocked = false
    private val unlockRequestCode = 777

    private lateinit var lockScreenLayout: LinearLayout
    private lateinit var lockStatusText: TextView
    private lateinit var lockOrb: TextView

    private lateinit var mainLayout: LinearLayout
    private lateinit var mainScrollView: ScrollView

    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var chatBox: TextView
    private lateinit var imageView: ImageView
    private lateinit var imagePromptText: TextView
    private lateinit var typedInput: EditText
    private lateinit var openRouterKeyInput: EditText
    private lateinit var groqKeyInput: EditText
    private lateinit var tts: TextToSpeech
    private lateinit var prefs: SharedPreferences

    private var lastImageUrl: String = ""
    private val imageHistory = mutableListOf<String>()

    private var continuousMode = false
    private var waitingForWakeCommand = false

    private val speechRequestCode = 101
    private val micPermissionCode = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)
        prefs = getSharedPreferences("jarvis_prefs", MODE_PRIVATE)

        // ================= LOCK SCREEN =================

        lockScreenLayout = LinearLayout(this)
        lockScreenLayout.orientation = LinearLayout.VERTICAL
        lockScreenLayout.gravity = Gravity.CENTER
        lockScreenLayout.setPadding(40, 40, 40, 40)
        lockScreenLayout.setBackgroundColor(Color.BLACK)

        val lockTitle = TextView(this)
        lockTitle.text = "JARVIS SECURITY"
        lockTitle.textSize = 34f
        lockTitle.setTextColor(Color.CYAN)
        lockTitle.gravity = Gravity.CENTER
        lockTitle.setPadding(0, 0, 0, 30)

        lockOrb = TextView(this)
        lockOrb.text = "🐷"
        lockOrb.textSize = 120f
        lockOrb.gravity = Gravity.CENTER
        lockOrb.setTextColor(Color.rgb(255, 90, 90))
        lockOrb.setShadowLayer(60f, 0f, 0f, Color.RED)

        lockStatusText = TextView(this)
        lockStatusText.text = "VOICE ID REQUIRED"
        lockStatusText.textSize = 22f
        lockStatusText.setTextColor(Color.WHITE)
        lockStatusText.gravity = Gravity.CENTER
        lockStatusText.setPadding(0, 35, 0, 20)

        val hint = TextView(this)
        hint.text = "AUTHORIZED USER ONLY\n\nSay: Rudransh"
        hint.textSize = 16f
        hint.setTextColor(Color.LTGRAY)
        hint.gravity = Gravity.CENTER
        hint.setPadding(0, 20, 0, 0)

        lockScreenLayout.addView(lockTitle)
        lockScreenLayout.addView(lockOrb)
        lockScreenLayout.addView(lockStatusText)
        lockScreenLayout.addView(hint)

        // ================= MAIN UI =================

        mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.gravity = Gravity.CENTER
        mainLayout.setPadding(45, 45, 45, 45)
        mainLayout.setBackgroundColor(Color.rgb(3, 5, 18))

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
        orb.setShadowLayer(40f, 0f, 0f, Color.CYAN)

        statusText = TextView(this)
        statusText.text = "SYSTEM ONLINE"
        statusText.textSize = 20f
        statusText.setTextColor(Color.WHITE)
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, 20, 0, 10)

        commandText = TextView(this)
        commandText.text = "Say: play channa mereya"
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
            if (!isUnlocked) {
                startVoicePassword()
                return@setOnClickListener
            }

            statusText.text = "LISTENING..."
            checkMicPermissionAndListen()
        }

        chatBox = TextView(this)
        chatBox.text = "Chat started...\n"
        chatBox.textSize = 14f
        chatBox.setTextColor(Color.LTGRAY)
        chatBox.setPadding(20, 25, 20, 20)
        chatBox.movementMethod = ScrollingMovementMethod()

        imagePromptText = TextView(this)
        imagePromptText.text = "AI Image Studio ready"
        imagePromptText.textSize = 14f
        imagePromptText.setTextColor(Color.CYAN)
        imagePromptText.gravity = Gravity.CENTER
        imagePromptText.setPadding(0, 20, 0, 10)

        imageView = ImageView(this)
        imageView.adjustViewBounds = true
        imageView.setBackgroundColor(Color.rgb(10, 12, 30))
        imageView.setPadding(8, 8, 8, 8)

        val openImageButton = Button(this)
        openImageButton.text = "OPEN FULL IMAGE"
        openImageButton.setOnClickListener {
            if (lastImageUrl.isNotBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lastImageUrl)))
            } else {
                speak("No image generated yet")
            }
        }

        val downloadImageButton = Button(this)
        downloadImageButton.text = "DOWNLOAD IMAGE"
        downloadImageButton.setOnClickListener {
            if (lastImageUrl.isNotBlank()) {
                downloadLastImage()
            } else {
                speak("No image generated yet")
            }
        }

        val historyButton = Button(this)
        historyButton.text = "IMAGE HISTORY"
        historyButton.setOnClickListener {
            if (imageHistory.isEmpty()) {
                addChat("Jarvis", "No image history yet.")
            } else {
                addChat("Image History", imageHistory.joinToString("\n\n"))
            }
        }

        typedInput = EditText(this)
        typedInput.hint = "Type message..."
        typedInput.setTextColor(Color.WHITE)
        typedInput.setHintTextColor(Color.GRAY)
        typedInput.inputType = InputType.TYPE_CLASS_TEXT

        val sendButton = Button(this)
        sendButton.text = "SEND TO JARVIS"
        sendButton.setOnClickListener {
            if (!isUnlocked) {
                startVoicePassword()
                return@setOnClickListener
            }

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
            if (!isUnlocked) {
                startVoicePassword()
                return@setOnClickListener
            }

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
        settingsButton.text = "⚙ AI KEY SETTINGS"
        settingsButton.setTextColor(Color.WHITE)
        settingsButton.background = roundedBg(Color.rgb(20, 25, 45), Color.DKGRAY, 35f)

        openRouterKeyInput = EditText(this)
        openRouterKeyInput.hint = "Paste OpenRouter API Key"
        openRouterKeyInput.setText(prefs.getString("openrouter_key", ""))
        openRouterKeyInput.setTextColor(Color.WHITE)
        openRouterKeyInput.setHintTextColor(Color.GRAY)
        openRouterKeyInput.inputType = InputType.TYPE_CLASS_TEXT
        openRouterKeyInput.visibility = View.GONE

        groqKeyInput = EditText(this)
        groqKeyInput.hint = "Paste Groq API Key"
        groqKeyInput.setText(prefs.getString("groq_key", ""))
        groqKeyInput.setTextColor(Color.WHITE)
        groqKeyInput.setHintTextColor(Color.GRAY)
        groqKeyInput.inputType = InputType.TYPE_CLASS_TEXT
        groqKeyInput.visibility = View.GONE

        val saveKeyButton = Button(this)
        saveKeyButton.text = "SAVE AI KEYS"
        saveKeyButton.visibility = View.GONE

        settingsButton.setOnClickListener {
            val show = openRouterKeyInput.visibility == View.GONE
            openRouterKeyInput.visibility = if (show) View.VISIBLE else View.GONE
            groqKeyInput.visibility = if (show) View.VISIBLE else View.GONE
            saveKeyButton.visibility = if (show) View.VISIBLE else View.GONE
        }

        saveKeyButton.setOnClickListener {
            prefs.edit()
                .putString("openrouter_key", openRouterKeyInput.text.toString().trim())
                .putString("groq_key", groqKeyInput.text.toString().trim())
                .apply()

            statusText.text = "AI KEYS SAVED"
            speak("AI keys saved")
            openRouterKeyInput.visibility = View.GONE
            groqKeyInput.visibility = View.GONE
            saveKeyButton.visibility = View.GONE
        }

        val helpText = TextView(this)
        helpText.text = """
            Try:
            
            play channa mereya
            play perfect on spotify
            google pe motu patlu search karo
            open whatsapp
            camera kholo
            take selfie
            maps pe lucknow railway station dikhao
            dog image banao
        """.trimIndent()
        helpText.textSize = 14f
        helpText.setTextColor(Color.GRAY)
        helpText.gravity = Gravity.CENTER
        helpText.setPadding(0, 35, 0, 0)

        mainLayout.addView(title)
        mainLayout.addView(orb)
        mainLayout.addView(statusText)
        mainLayout.addView(commandText)
        mainLayout.addView(micButton)
        mainLayout.addView(chatBox)
        mainLayout.addView(imagePromptText)
        mainLayout.addView(imageView)
        mainLayout.addView(openImageButton)
        mainLayout.addView(downloadImageButton)
        mainLayout.addView(historyButton)
        mainLayout.addView(typedInput)
        mainLayout.addView(sendButton)
        mainLayout.addView(continuousButton)
        mainLayout.addView(serviceButton)
        mainLayout.addView(accessibilityButton)
        mainLayout.addView(settingsButton)
        mainLayout.addView(openRouterKeyInput)
        mainLayout.addView(groqKeyInput)
        mainLayout.addView(saveKeyButton)
        mainLayout.addView(helpText)

        mainScrollView = ScrollView(this)
        mainScrollView.addView(mainLayout)

        setContentView(lockScreenLayout)

        startVoicePassword()
    }
    // =========================
// PART 2
// LOCK SCREEN + VOICE PASSWORD + MAIN UI FIX
// =========================


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
private fun startVoicePassword() {

    isUnlocked = false

    lockOrb.setTextColor(Color.RED)

    lockOrb.setShadowLayer(
        50f,
        0f,
        0f,
        Color.RED
    )

    lockStatusText.text =
        "VOICE ID REQUIRED"

    speak("Apna naam batiye")

    Handler(Looper.getMainLooper())
        .postDelayed({

            startUnlockVoiceInput()

        }, 1500)
}

private fun startUnlockVoiceInput() {

    val intent =
        Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        )

    intent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
    )

    intent.putExtra(
        RecognizerIntent.EXTRA_LANGUAGE,
        "en-IN"
    )

    intent.putExtra(
        RecognizerIntent.EXTRA_PROMPT,
        "Say your name"
    )

    try {

        startActivityForResult(
            intent,
            unlockRequestCode
        )

    } catch (e: Exception) {

        lockStatusText.text =
            "VOICE ENGINE FAILED"

        speak("Voice recognition failed")
    }
}

override fun onNewIntent(intent: Intent?) {

    super.onNewIntent(intent)

    setIntent(intent)

    val launchedAsAssistant =

        intent?.action ==
            Intent.ACTION_ASSIST ||

        intent?.action ==
            Intent.ACTION_VOICE_COMMAND ||

        intent?.getBooleanExtra(
            "wake_word_detected",
            false
        ) == true

    if (launchedAsAssistant) {

        statusText.text =
            "Jarvis activated"

        addChat(
            "Jarvis",
            "Yes boss, I am listening."
        )

        speak("Yes boss")

        checkMicPermissionAndListen()
    }
}

override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
) {

    // =========================
    // VOICE PASSWORD RESULT
    // =========================

    if (
        requestCode == unlockRequestCode &&
        resultCode == RESULT_OK
    ) {

        val result =
            data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

        val spoken =
            result?.get(0)
                ?.lowercase(
                    Locale.getDefault()
                ) ?: ""

        // =========================
        // ACCESS GRANTED
        // =========================

        if (
            spoken.contains("rudransh") ||
            spoken.contains("rudra")
        ) {

            isUnlocked = true

            lockOrb.setTextColor(
                Color.GREEN
            )

            lockOrb.setShadowLayer(
                60f,
                0f,
                0f,
                Color.GREEN
            )

            lockStatusText.text =
                "WELCOME BOSS"

            speak(
                "Welcome boss. Swaagat hai."
            )

            Handler(
                Looper.getMainLooper()
            ).postDelayed({

                setContentView(
                    mainScrollView
                )

            }, 1800)

        }

        // =========================
        // ACCESS DENIED
        // =========================

        else {

            isUnlocked = false

            lockOrb.setTextColor(
                Color.RED
            )

            lockOrb.setShadowLayer(
                60f,
                0f,
                0f,
                Color.RED
            )

            lockStatusText.text =
                "ACCESS DENIED"

            speak(
                "You are not boss. Please stay away."
            )

            Handler(
                Looper.getMainLooper()
            ).postDelayed({

                startVoicePassword()

            }, 2500)
        }

        return
    }

    // =========================
    // NORMAL VOICE COMMANDS
    // =========================

    super.onActivityResult(
        requestCode,
        resultCode,
        data
    )

    if (
        requestCode == speechRequestCode &&
        resultCode == RESULT_OK
    ) {

        val result =
            data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

        val command =
            result?.get(0)
                ?.lowercase(
                    Locale.getDefault()
                ) ?: ""

        commandText.text =
            "You said: $command"

        if (continuousMode) {

            handleContinuousCommand(
                command
            )

        } else {

            addChat(
                "You",
                command
            )

            handleCommand(command)
        }

    } else {

        statusText.text =
            "No command received"

        if (continuousMode) {
            startVoiceInput()
        }
    }
}
// =========================
// PART 3
// COMMAND SYSTEM + SPOTIFY AUTOPLAY
// =========================

private fun handleContinuousCommand(command: String) {

    if (command.contains("hey jarvis") ||
        command.contains("hello jarvis")) {

        waitingForWakeCommand = true

        statusText.text = "Jarvis activated"

        speak("Yes boss")

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

    if (continuousMode) {
        startVoiceInput()
    }
}

private fun handleCommand(command: String) {

    val normalized = normalizeCommand(command)

    when {

        // ================= IMAGE GENERATION =================

        normalized.contains("image") ||
        normalized.contains("photo") ||
        normalized.contains("wallpaper") ||
        normalized.contains("picture") ||
        normalized.contains("generate") ||
        normalized.contains("banao") ||
        normalized.contains("bnado") -> {

            val prompt = normalized
                .replace("generate", "")
                .replace("image", "")
                .replace("photo", "")
                .replace("wallpaper", "")
                .replace("picture", "")
                .replace("banao", "")
                .replace("bnado", "")
                .replace("of", "")
                .trim()

            generateAIImage(prompt)

            return
        }

        // ================= SPOTIFY =================

        normalized.contains("spotify") -> {

            val q = normalized
                .replace("spotify", "")
                .replace("play", "")
                .replace("chalao", "")
                .replace("gana", "")
                .replace("song", "")
                .replace("music", "")
                .replace("on", "")
                .trim()

            playOnSpotifyAuto(q)

            return
        }

        // ================= YOUTUBE =================

        normalized.contains("youtube") ||
        normalized.contains("play") ||
        normalized.contains("song") ||
        normalized.contains("music") ||
        normalized.contains("gana") -> {

            val q = normalized
                .replace("youtube", "")
                .replace("play", "")
                .replace("song", "")
                .replace("music", "")
                .replace("gana", "")
                .replace("on", "")
                .trim()

            playOnYouTube(q)

            return
        }

        // ================= OPEN APPS =================

        normalized.startsWith("open ") -> {

            val app = normalized
                .replace("open", "")
                .trim()

            openApp(app)

            return
        }

        // ================= GOOGLE SEARCH =================

        normalized.contains("google") &&
        normalized.contains("search") -> {

            val q = normalized
                .replace("google", "")
                .replace("search", "")
                .replace("karo", "")
                .trim()

            openWebSearch(q)

            return
        }

        // ================= CAMERA =================

        normalized.contains("camera") -> {

            openApp("camera")

            return
        }

        normalized.contains("selfie") -> {

            openCameraSelfie()

            return
        }

        // ================= CALL =================

        normalized.startsWith("call ") ||
        normalized.contains("phone lagao") -> {

            val number = extractPhoneNumber(normalized)

            openDialer(number)

            return
        }

        // ================= SMS =================

        normalized.startsWith("message ") ||
        normalized.startsWith("sms ") -> {

            val text = normalized
                .replace("message", "")
                .replace("sms", "")
                .trim()

            openSms(text)

            return
        }

        // ================= WHATSAPP =================

        normalized.startsWith("whatsapp ") ||
        normalized.contains("whatsapp message") -> {

            val text = normalized
                .replace("whatsapp", "")
                .replace("message", "")
                .trim()

            openWhatsApp(text)

            return
        }

        // ================= MAPS =================

        normalized.contains("maps") ||
        normalized.contains("location") ||
        normalized.contains("route") -> {

            val q = normalized
                .replace("maps", "")
                .replace("location", "")
                .replace("route", "")
                .trim()

            openMaps(q)

            return
        }

        // ================= ACCESSIBILITY =================

        normalized.contains("go back") -> {

            JarvisAccessibilityService
                .instance
                ?.goBack()

            return
        }

        normalized.contains("go home") -> {

            JarvisAccessibilityService
                .instance
                ?.goHome()

            return
        }

        normalized.contains("recent apps") -> {

            JarvisAccessibilityService
                .instance
                ?.openRecents()

            return
        }

        normalized.contains("notifications") -> {

            JarvisAccessibilityService
                .instance
                ?.openNotifications()

            return
        }

        normalized.contains("scroll down") -> {

            JarvisAccessibilityService
                .instance
                ?.scrollDown()

            return
        }

        normalized.contains("scroll up") -> {

            JarvisAccessibilityService
                .instance
                ?.scrollUp()

            return
        }

        normalized.contains("tap center") -> {

            JarvisAccessibilityService
                .instance
                ?.tapCenter()

            return
        }
    }

    // ================= AI CHAT =================

    askAIChat(normalized)
}

// =========================
// SPOTIFY AUTO PLAY
// =========================

private fun playOnSpotifyAuto(song: String) {

    if (song.isBlank()) {

        speak("Please say the song name")

        return
    }

    val service = JarvisAccessibilityService.instance

    if (service == null) {

        speak("Enable accessibility service first")

        openApp("spotify")

        return
    }

    speak("Playing $song on Spotify")

    try {

        val intent =
            packageManager
                .getLaunchIntentForPackage(
                    "com.spotify.music"
                )

        if (intent == null) {

            speak("Spotify not installed")

            return
        }

        startActivity(intent)

    } catch (e: Exception) {

        speak("Spotify not installed")

        return
    }

    // OPEN SEARCH TAB

    Handler(Looper.getMainLooper())
        .postDelayed({

            service.tapPercent(
                0.50f,
                0.93f
            )

        }, 1800)

    // TAP SEARCH BAR

    Handler(Looper.getMainLooper())
        .postDelayed({

            service.tapPercent(
                0.50f,
                0.10f
            )

        }, 3200)

    // TYPE SONG

    Handler(Looper.getMainLooper())
        .postDelayed({

            service.typeText(song)

        }, 4500)

    // TAP FIRST RESULT

    Handler(Looper.getMainLooper())
        .postDelayed({

            service.tapPercent(
                0.50f,
                0.32f
            )

        }, 7000)
}

// =========================
// PART 4
// IMAGE + AI CHAT + UTILITY FUNCTIONS
// =========================

private fun generateAIImage(prompt: String) {

    if (prompt.isBlank()) {
        speak("Please say image prompt")
        return
    }

    statusText.text = "Generating image..."
    imagePromptText.text = "Prompt: $prompt"
    speak("Generating image")

    val cleanPrompt =
        "$prompt, ultra detailed, cinematic, high quality"

    val seed =
        System.currentTimeMillis() % 100000

    val imageUrl =
        "https://image.pollinations.ai/prompt/${Uri.encode(cleanPrompt)}?width=768&height=768&seed=$seed&nologo=true&safe=true"

    lastImageUrl = imageUrl
    imageHistory.add(0, prompt)

    thread {
        try {
            val input =
                URL(imageUrl).openStream()

            val bitmap =
                BitmapFactory.decodeStream(input)

            runOnUiThread {
                imageView.setImageBitmap(bitmap)
                statusText.text = "Image ready"
                addChat("Jarvis", "Image generated: $prompt")
                speak("Image ready")
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text = "Image error"
                addChat(
                    "Jarvis",
                    "Image generation failed. Opening in browser."
                )

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(imageUrl)
                    )
                )
            }
        }
    }
}

private fun downloadLastImage() {

    if (lastImageUrl.isBlank()) {
        speak("No image generated yet")
        return
    }

    thread {
        try {
            val input =
                URL(lastImageUrl).openStream()

            val picturesDir =
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                )

            val file =
                File(
                    picturesDir,
                    "jarvis_image_${System.currentTimeMillis()}.png"
                )

            val output =
                FileOutputStream(file)

            input.copyTo(output)

            output.close()
            input.close()

            runOnUiThread {
                statusText.text = "Image downloaded"
                addChat(
                    "Jarvis",
                    "Saved image: ${file.absolutePath}"
                )
                speak("Image downloaded")
            }

        } catch (e: Exception) {

            runOnUiThread {
                statusText.text = "Download failed"
                speak("Download failed")
            }
        }
    }
}

// =========================
// AI CHAT: OPENROUTER + GROQ FALLBACK
// =========================

private fun askAIChat(command: String) {

    val openRouterKey =
        prefs.getString("openrouter_key", "") ?: ""

    val groqKey =
        prefs.getString("groq_key", "") ?: ""

    when {

        openRouterKey.isNotBlank() -> {
            askOpenRouter(command, openRouterKey)
        }

        groqKey.isNotBlank() -> {
            askGroq(command, groqKey)
        }

        else -> {
            val msg =
                "AI key missing. Basic commands still work."

            statusText.text = "AI key missing"

            addChat("Jarvis", msg)

            speak(msg)
        }
    }
}

private fun askOpenRouter(
    command: String,
    key: String
) {

    statusText.text =
        "Thinking with OpenRouter..."

    thread {
        try {

            val reply =
                callChatApi(
                    endpoint =
                        "https://openrouter.ai/api/v1/chat/completions",
                    key = key,
                    model = "openrouter/auto",
                    command = command,
                    isOpenRouter = true
                )

            runOnUiThread {
                statusText.text = "Jarvis replied"
                addChat("Jarvis", reply)
                speak(reply)
            }

        } catch (e: Exception) {

            val groqKey =
                prefs.getString("groq_key", "") ?: ""

            if (groqKey.isNotBlank()) {

                askGroq(command, groqKey)

            } else {

                runOnUiThread {
                    val msg =
                        "OpenRouter error. Add Groq key as backup."

                    statusText.text =
                        "AI chat error"

                    addChat("Jarvis", msg)

                    speak(msg)
                }
            }
        }
    }
}

private fun askGroq(
    command: String,
    key: String
) {

    statusText.text =
        "Thinking with Groq..."

    thread {
        try {

            val reply =
                callChatApi(
                    endpoint =
                        "https://api.groq.com/openai/v1/chat/completions",
                    key = key,
                    model = "llama-3.3-70b-versatile",
                    command = command,
                    isOpenRouter = false
                )

            runOnUiThread {
                statusText.text = "Jarvis replied"
                addChat("Jarvis", reply)
                speak(reply)
            }

        } catch (e: Exception) {

            runOnUiThread {
                val msg =
                    "AI chat error. Basic commands still work."

                statusText.text =
                    "AI chat error"

                addChat("Jarvis", msg)

                speak(msg)
            }
        }
    }
}

private fun callChatApi(
    endpoint: String,
    key: String,
    model: String,
    command: String,
    isOpenRouter: Boolean
): String {

    val requestJson =
        JSONObject()

    requestJson.put(
        "model",
        model
    )

    val messages =
        JSONArray()

    val systemMsg =
        JSONObject()

    systemMsg.put(
        "role",
        "system"
    )

    systemMsg.put(
        "content",
        "You are JARVIS, a helpful Hinglish Android assistant. Reply short, natural, friendly, and useful."
    )

    val userMsg =
        JSONObject()

    userMsg.put(
        "role",
        "user"
    )

    userMsg.put(
        "content",
        command
    )

    messages.put(systemMsg)
    messages.put(userMsg)

    requestJson.put(
        "messages",
        messages
    )

    requestJson.put(
        "temperature",
        0.7
    )

    val url =
        URL(endpoint)

    val connection =
        url.openConnection() as HttpURLConnection

    connection.requestMethod =
        "POST"

    connection.setRequestProperty(
        "Content-Type",
        "application/json"
    )

    connection.setRequestProperty(
        "Authorization",
        "Bearer $key"
    )

    if (isOpenRouter) {

        connection.setRequestProperty(
            "HTTP-Referer",
            "https://github.com/RudranshPandey108/Jarvisnative"
        )

        connection.setRequestProperty(
            "X-Title",
            "JARVIS Android"
        )
    }

    connection.doOutput =
        true

    connection.outputStream.use {
        it.write(
            requestJson.toString().toByteArray()
        )
    }

    val responseCode =
        connection.responseCode

    val responseText =
        if (responseCode in 200..299) {

            connection.inputStream
                .bufferedReader()
                .readText()

        } else {

            throw Exception(
                "HTTP $responseCode"
            )
        }

    val json =
        JSONObject(responseText)

    return json
        .getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString("content")
        .trim()
}

// =========================
// NORMALIZATION + CHAT UI
// =========================

private fun normalizeCommand(
    command: String
): String {

    return command
        .lowercase()
        .replace("ओपन", "open")
        .replace("व्हाट्सएप", "whatsapp")
        .replace("यूट्यूब", "youtube")
        .replace("स्पॉटिफाई", "spotify")
        .replace("कैमरा", "camera")
        .replace("सेटिंग", "settings")
        .replace("गूगल", "google")
        .replace("मैप", "maps")
        .trim()
}

private fun addChat(
    sender: String,
    message: String
) {

    chatBox.append(
        "\n$sender: $message\n"
    )
}
// =========================
// PART 5
// ANDROID ACTIONS + YOUTUBE + LIFECYCLE
// =========================

private fun extractPhoneNumber(text: String): String {
    val regex = Regex("\\d{10,13}")
    return regex.find(text)?.value ?: ""
}

private fun openDialer(number: String) {
    if (number.isBlank()) {
        speak("Please say the phone number")
        return
    }

    val intent = Intent(Intent.ACTION_DIAL)
    intent.data = Uri.parse("tel:$number")
    startActivity(intent)
}

private fun openSms(text: String) {
    val number = extractPhoneNumber(text)
    val message = text.replace(number, "").trim()

    if (number.isBlank()) {
        speak("Please say the phone number")
        return
    }

    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("smsto:$number")
    intent.putExtra("sms_body", message)

    startActivity(intent)
}

private fun openWhatsApp(text: String) {
    val number = extractPhoneNumber(text)
    val message = text.replace(number, "").trim()

    if (number.isBlank()) {
        openApp("whatsapp")
        return
    }

    val cleanNumber =
        if (number.length == 10) {
            "91$number"
        } else {
            number
        }

    val url =
        "https://wa.me/$cleanNumber?text=${Uri.encode(message)}"

    val intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url))

    try {
        intent.setPackage("com.whatsapp")
        startActivity(intent)
    } catch (e: Exception) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )
    }
}

private fun openWebSearch(query: String) {
    val url =
        "https://www.google.com/search?q=${Uri.encode(query)}"

    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
    )
}

private fun openUrl(query: String) {
    var url = query.trim()

    if (
        !url.startsWith("http://") &&
        !url.startsWith("https://")
    ) {
        url = "https://$url"
    }

    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        )
    )
}

private fun openMaps(query: String) {
    val uri =
        Uri.parse("geo:0,0?q=${Uri.encode(query)}")

    val intent =
        Intent(Intent.ACTION_VIEW, uri)

    intent.setPackage(
        "com.google.android.apps.maps"
    )

    try {
        startActivity(intent)
    } catch (e: Exception) {
        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                uri
            )
        )
    }
}

private fun openApp(appName: String) {
    val cleanedName =
        appName.lowercase().trim()

    val mainIntent =
        Intent(Intent.ACTION_MAIN, null)

    mainIntent.addCategory(
        Intent.CATEGORY_LAUNCHER
    )

    val apps =
        packageManager.queryIntentActivities(
            mainIntent,
            0
        )

    for (app in apps) {

        val label =
            app.loadLabel(packageManager)
                .toString()
                .lowercase()

        val packageName =
            app.activityInfo.packageName

        if (
            label.contains(cleanedName) ||
            cleanedName.contains(label)
        ) {

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (launchIntent != null) {

                statusText.text =
                    "Opening $label"

                speak("Opening $label")

                startActivity(launchIntent)

                return
            }
        }
    }

    statusText.text =
        "App not found: $cleanedName"

    speak("I could not find $cleanedName")
}

private fun openCameraSelfie() {
    statusText.text =
        "Opening selfie camera"

    speak("Opening selfie camera")

    val intent =
        Intent("android.media.action.IMAGE_CAPTURE")

    intent.putExtra(
        "android.intent.extras.CAMERA_FACING",
        1
    )

    intent.putExtra(
        "android.intent.extras.LENS_FACING_FRONT",
        1
    )

    intent.putExtra(
        "android.intent.extra.USE_FRONT_CAMERA",
        true
    )

    try {
        startActivity(intent)
    } catch (e: Exception) {
        speak("Camera could not be opened")
    }
}

// =========================
// YOUTUBE DIRECT PLAY
// =========================

private fun playOnYouTube(song: String) {
    if (song.isBlank()) {
        speak("Please say the song name")
        return
    }

    if (
        YOUTUBE_API_KEY ==
        "PASTE_YOUR_YOUTUBE_API_KEY_HERE"
    ) {
        val url =
            "https://www.youtube.com/results?search_query=${Uri.encode(song)}"

        startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )
        )

        return
    }

    statusText.text =
        "Searching YouTube for $song"

    thread {
        try {
            val apiUrl =
                "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=1&q=${
                    Uri.encode(song)
                }&key=$YOUTUBE_API_KEY"

            val response =
                URL(apiUrl).readText()

            val json =
                JSONObject(response)

            val items =
                json.getJSONArray("items")

            if (items.length() > 0) {

                val videoId =
                    items
                        .getJSONObject(0)
                        .getJSONObject("id")
                        .getString("videoId")

                runOnUiThread {

                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "vnd.youtube:$videoId"
                            )
                        )

                    intent.setPackage(
                        "com.google.android.youtube"
                    )

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

                val url =
                    "https://www.youtube.com/results?search_query=${Uri.encode(song)}"

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(url)
                    )
                )
            }
        }
    }
}

// Spotify fallback wrapper
private fun playOnSpotify(song: String) {
    playOnSpotifyAuto(song)
}

// =========================
// TTS + LIFECYCLE
// =========================

override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale("en", "IN")
    }
}

private fun speak(text: String) {
    try {
        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    } catch (_: Exception) {
    }
}

override fun onDestroy() {
    tts.stop()
    tts.shutdown()

    super.onDestroy()
}
}
