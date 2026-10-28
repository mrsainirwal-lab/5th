package com.zara.floatai

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class Voice(private val ctx: Context, private val onSpeaking: (Boolean) -> Unit) : TextToSpeech.OnInitListener {
    companion object {
        val names = listOf("1. Sweet", "2. Chibi cute", "3. Calm", "4. Energetic", "5. Deep cool",
            "6. Soft", "7. Tsundere", "8. Mature", "9. Storyteller", "10. Robot")
        private val pitch = floatArrayOf(1.5f, 1.9f, 1.0f, 1.3f, 0.6f, 1.2f, 1.4f, 0.8f, 0.9f, 0.5f)
        private val rate = floatArrayOf(1.05f, 1.2f, 0.85f, 1.3f, 0.9f, 0.75f, 1.15f, 0.95f, 0.8f, 1.0f)
    }

    private val h = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = TextToSpeech(ctx, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        ready = true
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) { h.post { onSpeaking(true) } }
            override fun onDone(id: String?) { h.post { onSpeaking(false) } }
            @Suppress("OVERRIDE_DEPRECATION")
            override fun onError(id: String?) { h.post { onSpeaking(false) } }
        })
        pending?.let { speak(it) }
        pending = null
    }

    fun speak(text: String) {
        val e = tts
        if (!ready || e == null) { pending = text; return }
        val v = Prefs.geti(ctx, "voice").coerceIn(0, 9)
        val loc = Locale.forLanguageTag(Prefs.get(ctx, "lang", "hi-IN"))
        e.language = loc
        val vs = e.voices?.filter { it.locale.language == loc.language && !it.isNetworkConnectionRequired }?.sortedBy { it.name }
        if (!vs.isNullOrEmpty()) e.voice = vs[v % vs.size]
        e.setPitch(pitch[v])
        e.setSpeechRate(rate[v])
        e.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u")
    }

    fun stop() { tts?.stop(); onSpeaking(false) }
    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null }
}
