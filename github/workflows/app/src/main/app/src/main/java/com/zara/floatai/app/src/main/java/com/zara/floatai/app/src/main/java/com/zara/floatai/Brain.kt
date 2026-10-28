package com.zara.floatai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Brain {
    private fun system(c: Context): String {
        val name = Looks.all[Prefs.geti(c, "char").coerceIn(0, 4)].name
        val facts = Memory.facts(c).joinToString("; ").ifEmpty { "nothing yet" }
        return "You are $name, a cute anime-style floating companion on the user's phone. " +
            "Reply in the same language the user speaks (Hindi, Hinglish or English). Be warm and playful, " +
            "maximum 2 short sentences, no emojis, no markdown, because your reply is spoken aloud. " +
            "What you have learned about the user so far: $facts."
    }

    /** Blocking. Call from a background thread. */
    fun ask(c: Context, text: String): String {
        Memory.learn(c, text)
        return try {
            val reply = when (Prefs.get(c, "provider", "offline")) {
                "openai" -> openai(c, text)
                "gemini" -> gemini(c, text)
                else -> offline(c, text)
            }
            Memory.addTurn(c, "u", text)
            Memory.addTurn(c, "a", reply)
            reply
        } catch (e: Exception) {
            "Connection me dikkat aa gayi. API key aur internet check karo. ${e.message ?: ""}"
        }
    }

    private fun openai(c: Context, text: String): String {
        val key = Prefs.get(c, "openai_key").trim()
        if (key.isEmpty()) return "Settings me pehle OpenAI API key daalo."
        val model = Prefs.get(c, "openai_model", "gpt-4o-mini").ifBlank { "gpt-4o-mini" }
        val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system(c)))
        val h = Memory.history(c)
        for (i in 0 until h.length()) {
            val o = h.getJSONObject(i)
            msgs.put(JSONObject().put("role", if (o.getString("r") == "u") "user" else "assistant").put("content", o.getString("t")))
        }
        msgs.put(JSONObject().put("role", "user").put("content", text))
        val body = JSONObject().put("model", model).put("messages", msgs)
        val res = post("https://api.openai.com/v1/chat/completions", body.toString(), mapOf("Authorization" to "Bearer $key"))
        return JSONObject(res).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim()
    }

    private fun gemini(c: Context, text: String): String {
        val key = Prefs.get(c, "gemini_key").trim()
        if (key.isEmpty()) return "Settings me pehle Gemini API key daalo."
        val model = Prefs.get(c, "gemini_model", "gemini-2.0-flash").ifBlank { "gemini-2.0-flash" }
        val contents = JSONArray()
        val h = Memory.history(c)
        for (i in 0 until h.length()) {
            val o = h.getJSONObject(i)
            contents.put(JSONObject().put("role", if (o.getString("r") == "u") "user" else "model")
                .put("parts", JSONArray().put(JSONObject().put("text", o.getString("t")))))
        }
        contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", text))))
        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system(c)))))
            .put("contents", contents)
        val res = post("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            body.toString(), mapOf("x-goog-api-key" to key))
        return JSONObject(res).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim()
    }

    private fun offline(c: Context, text: String): String {
        val t = text.lowercase()
        val name = Memory.facts(c).firstOrNull { it.startsWith("User's name is") }?.removePrefix("User's name is ")
        return when {
            Regex("hello|hi\\b|hey|namaste|hii").containsMatchIn(t) -> "Namaste${if (name != null) " $name" else ""}! Main yahin hoon, bolo kya karna hai?"
            "time" in t || "samay" in t || "baje" in t -> "Abhi " + SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) + " ho raha hai."
            "naam" in t || "name" in t -> if (name != null) "Tumhara naam $name hai, mujhe yaad hai!" else "Main ${Looks.all[Prefs.geti(c, "char").coerceIn(0, 4)].name} hoon. Tumhara naam kya hai?"
            "yaad" in t || "remember" in t -> "Theek hai, yaad rakh liya."
            else -> "Abhi main offline mode me hoon. App ki settings me ChatGPT ya Gemini chalu karo, phir poori baat karenge."
        }
    }

    private fun post(url: String, body: String, headers: Map<String, String>): String {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.connectTimeout = 15000
        con.readTimeout = 40000
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json")
        headers.forEach { (k, v) -> con.setRequestProperty(k, v) }
        con.outputStream.use { it.write(body.toByteArray()) }
        val code = con.responseCode
        val stream = if (code >= 400) con.errorStream else con.inputStream
        val txt = stream?.bufferedReader()?.use { it.readText() } ?: ""
        if (code >= 400) throw Exception("HTTP $code")
        return txt
    }
}
