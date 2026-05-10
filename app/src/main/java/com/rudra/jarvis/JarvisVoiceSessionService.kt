package com.rudra.jarvis

import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class JarvisVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: android.os.Bundle?): VoiceInteractionSession {
        return JarvisVoiceSession(this)
    }
}
