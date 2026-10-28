package com.zara.floatai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private var testVoice: Voice? = null

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val d = resources.displayMetrics.density
        fun dp(x: Int) = (x * d).toInt()
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(40)) }

        fun label(t: String) = TextView(this).apply {
            text = t; textSize = 14f; setTypeface(null, Typeface.BOLD); setPadding(0, dp(16), 0, dp(2))
        }
        fun edit(key: String, hint: String, pwd: Boolean = false, def: String = "") = EditText(this).apply {
            this.hint = hint; setText(Prefs.get(this@MainActivity, key, def)); setSingleLine()
            if (pwd) inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(e: Editable?) { Prefs.put(this@MainActivity, key, e.toString()) }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
        }
        fun spin(items: List<String>, sel: Int, onSel: (Int) -> Unit) = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(sel)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { onSel(pos) }
                override fun onNothingSelected(p: AdapterView<*>?) {}
            }
        }

        col.addView(TextView(this).apply { text = "Zara Float AI"; textSize = 24f; setTypeface(null, Typeface.BOLD) })
        col.addView(TextView(this).apply { text = "Floating anime assistant. Tap = baat karo, 2 sec dabao = band." })

        // Character
        col.addView(label("Character"))
        val preview = CharacterView(this).apply { style = Prefs.geti(this@MainActivity, "char") }
        col.addView(spin(Looks.all.map { it.name }, Prefs.geti(this, "char")) {
            Prefs.puti(this, "char", it); preview.style = it
        })
        col.addView(preview, LinearLayout.LayoutParams(dp(110), dp(130)))

        // Voice
        col.addView(label("Voice / tone"))
        col.addView(spin(Voice.names, Prefs.geti(this, "voice")) { Prefs.puti(this, "voice", it) })
        col.addView(Button(this).apply {
            text = "Test voice"
            setOnClickListener {
                if (testVoice == null) testVoice = Voice(this@MainActivity) {}
                testVoice?.speak("Namaste! Main tumhari floating assistant hoon.")
            }
        })

        // Language
        col.addView(label("Language"))
        val langs = listOf("hi-IN", "en-US", "en-IN")
        col.addView(spin(listOf("Hindi / Hinglish", "English (US)", "English (India)"), langs.indexOf(Prefs.get(this, "lang", "hi-IN")).coerceAtLeast(0)) {
            Prefs.put(this, "lang", langs[it])
        })

        // AI provider
        col.addView(label("AI brain"))
        val provs = listOf("offline", "openai", "gemini")
        col.addView(spin(listOf("Offline (basic)", "ChatGPT (OpenAI)", "Gemini (Google)"), provs.indexOf(Prefs.get(this, "provider", "offline")).coerceAtLeast(0)) {
            Prefs.put(this, "provider", provs[it])
        })
        col.addView(edit("openai_key", "OpenAI API key", true))
        col.addView(edit("openai_model", "OpenAI model", false, "gpt-4o-mini"))
        col.addView(edit("gemini_key", "Gemini API key", true))
        col.addView(edit("gemini_model", "Gemini model", false, "gemini-2.0-flash"))

        // Memory
        col.addView(Button(this).apply {
            text = "Reset what Zara learned"
            setOnClickListener { Memory.clear(this@MainActivity); Toast.makeText(this@MainActivity, "Memory cleared", Toast.LENGTH_SHORT).show() }
        })

        // Start / stop
        col.addView(label("Control"))
        col.addView(Button(this).apply { text = "START floating assistant"; setOnClickListener { start() } })
        col.addView(Button(this).apply {
            text = "STOP"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingService::class.java)) }
        })
        col.addView(TextView(this).apply {
            text = "Tip: bolo \"yaad rakh <baat>\" ya \"my name is ...\" taaki Zara tumhe yaad rakhe. \"sab bhool ja\" se memory clear."
            setPadding(0, dp(16), 0, 0)
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun start() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO, "android.permission.POST_NOTIFICATIONS"), 1)
            Toast.makeText(this, "Mic permission do, phir START dobara dabao", Toast.LENGTH_LONG).show()
            return
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "\"Display over other apps\" allow karo", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startForegroundService(Intent(this, FloatingService::class.java))
        Toast.makeText(this, "Zara chalu!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() { testVoice?.shutdown(); super.onDestroy() }
}
