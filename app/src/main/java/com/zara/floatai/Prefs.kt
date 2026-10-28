package com.zara.floatai

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("zara", Context.MODE_PRIVATE)
    fun get(c: Context, k: String, d: String = ""): String = sp(c).getString(k, d) ?: d
    fun put(c: Context, k: String, v: String) = sp(c).edit().putString(k, v).apply()
    fun geti(c: Context, k: String, d: Int = 0): Int = sp(c).getInt(k, d)
    fun puti(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()
}
