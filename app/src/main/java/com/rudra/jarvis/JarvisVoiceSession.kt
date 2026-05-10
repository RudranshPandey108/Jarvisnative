package com.rudra.jarvis

import android.content.Context
import android.content.Intent
import android.service.voice.VoiceInteractionSession

class JarvisVoiceSession(context: Context) : VoiceInteractionSession(context) {

    override fun onShow(args: android.os.Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)

        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra("wake_word_detected", true)

        context.startActivity(intent)
        finish()
    }
}
