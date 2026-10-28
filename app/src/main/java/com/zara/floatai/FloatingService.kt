package com.zara.floatai

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var root: LinearLayout
    private lateinit var bubble: TextView
    private lateinit var ch: CharacterView
    private lateinit var lp: WindowManager.LayoutParams
    private var voice: Voice? = null
    private var rec: SpeechRecognizer? = null
    private val h = Handler(Looper.getMainLooper())
    private val hideBubble = Runnable { if (::bubble.isInitialized) bubble.visibility = View.GONE }
    private var busy = false
    private var built = false

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startFg()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        if (!built) build() else ch.style = Prefs.geti(this, "char")
        return START_STICKY
    }

    private fun startFg() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("zara", "Zara", NotificationManager.IMPORTANCE_LOW))
        val n = Notification.Builder(this, "zara")
            .setContentTitle("Zara is floating")
            .setContentText("Tap her to talk. Hold 2 sec to close.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now).build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        else startForeground(1, n)
    }

    private fun build() {
        built = true
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val d = resources.displayMetrics.density
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        bubble = TextView(this).apply {
            setTextColor(Color.BLACK); textSize = 13f
            setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
            maxWidth = (230 * d).toInt()
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 16 * d; setStroke((2 * d).toInt(), 0xFFFFB6C1.toInt()) }
            visibility = View.GONE
        }
        ch = CharacterView(this).apply { style = Prefs.geti(this@FloatingService, "char") }
        root.addView(bubble)
        root.addView(ch, LinearLayout.LayoutParams((110 * d).toInt(), (130 * d).toInt()))
        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = (16 * d).toInt(); y = (260 * d).toInt() }

        voice = Voice(this) { ch.speaking = it }

        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        val closeRun = Runnable { stopSelf() }
        ch.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false; h.postDelayed(closeRun, 2000); true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (abs(dx) > 10 * d || abs(dy) > 10 * d) { moved = true; h.removeCallbacks(closeRun) }
                    if (moved) { lp.x = (ox + dx).toInt(); lp.y = (oy + dy).toInt(); wm.updateViewLayout(root, lp) }
                    true
                }
                MotionEvent.ACTION_UP -> { h.removeCallbacks(closeRun); if (!moved) onTap(); true }
                MotionEvent.ACTION_CANCEL -> { h.removeCallbacks(closeRun); true }
                else -> false
            }
        }
        wm.addView(root, lp)
        show("Namaste! Mujhe tap karke baat karo. 2 sec dabaoge toh main chali jaungi.", 6000)
    }

    private fun show(t: String, ms: Long = 0) {
        bubble.text = t; bubble.visibility = View.VISIBLE
        h.removeCallbacks(hideBubble)
        if (ms > 0) h.postDelayed(hideBubble, ms)
    }

    private fun say(t: String) {
        show(t, maxOf(5000L, t.length * 80L))
        voice?.speak(t)
    }

    private fun onTap() {
        if (ch.speaking) { voice?.stop(); return }
        listen()
    }

    private fun listen() {
        if (busy) return
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            show("Pehle app khol kar mic permission do.", 4000); return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            show("Is phone me speech recognition available nahi hai.", 4000); return
        }
        rec?.destroy()
        rec = SpeechRecognizer.createSpeechRecognizer(this)
        rec?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
            override fun onError(p0: Int) { ch.listening = false; show("Samajh nahi aaya, dobara tap karo.", 3000) }
            override fun onResults(b: Bundle?) {
                ch.listening = false
                val text = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text.isNullOrBlank()) show("Kuch sunai nahi diya.", 3000) else think(text)
            }
        })
        rec?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Prefs.get(this, "lang", "hi-IN")))
        ch.listening = true
        show("Sun rahi hoon...")
    }

    private fun think(text: String) {
        busy = true
        show("...")
        Thread {
            val reply = Brain.ask(this, text)
            h.post { busy = false; say(reply) }
        }.start()
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        if (built) try { wm.removeView(root) } catch (_: Exception) {}
        rec?.destroy()
        voice?.shutdown()
        super.onDestroy()
    }
}
