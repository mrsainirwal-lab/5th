package com.zara.floatai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** On-device "self learning": remembers facts about the user + recent chat, sent as context to GPT/Gemini. */
object Memory {
    fun facts(c: Context): List<String> = Prefs.get(c, "facts").split("\n").filter { it.isNotBlank() }

    fun addFact(c: Context, f: String) {
        val l = facts(c).toMutableList()
        if (!l.contains(f)) l.add(f)
        Prefs.put(c, "facts", l.takeLast(40).joinToString("\n"))
    }

    fun history(c: Context): JSONArray =
        try { JSONArray(Prefs.get(c, "hist", "[]")) } catch (e: Exception) { JSONArray() }

    fun addTurn(c: Context, role: String, text: String) {
        val h = history(c)
        h.put(JSONObject().put("r", role).put("t", text))
        val out = JSONArray()
        for (i in maxOf(0, h.length() - 12) until h.length()) out.put(h.get(i))
        Prefs.put(c, "hist", out.toString())
    }

    fun clear(c: Context) { Prefs.put(c, "facts", ""); Prefs.put(c, "hist", "[]") }

    fun learn(c: Context, user: String) {
        val t = user.trim()
        if (Regex("(?i)(forget everything|sab bhool ja|sab bhul ja)").containsMatchIn(t)) { clear(c); return }
        Regex("(?i)^(?:remember|yaad rakh(?:na|o)?)[:, ]+(.+)$").find(t)?.let { addFact(c, it.groupValues[1]); return }
        Regex("(?i)\\bmy name is (\\p{L}+)").find(t)?.let { addFact(c, "User's name is ${it.groupValues[1]}") }
        Regex("(?i)\\bmera naam (\\p{L}+)").find(t)?.let { addFact(c, "User's name is ${it.groupValues[1]}") }
        Regex("(?i)\\bi (?:like|love) (.+)$").find(t)?.let { addFact(c, "User likes ${it.groupValues[1]}") }
        Regex("(?i)\\bmujhe (.+) pasand").find(t)?.let { addFact(c, "User likes ${it.groupValues[1]}") }
    }
}
