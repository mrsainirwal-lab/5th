package com.zara.floatai

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class Look(val name: String, val hair: Int, val hair2: Int, val eye: Int, val outfit: Int, val acc: Int, val longHair: Boolean)

object Looks {
    val all = listOf(
        Look("Sakura", 0xFFFF8FB8.toInt(), 0xFFFF6FA0.toInt(), 0xFFE0457B.toInt(), 0xFFFFFFFF.toInt(), 0, true),
        Look("Yuki", 0xFFE8F1FF.toInt(), 0xFFC9DCF5.toInt(), 0xFF4DA3FF.toInt(), 0xFF5B7DB1.toInt(), 1, true),
        Look("Hana", 0xFF9B6BFF.toInt(), 0xFF7F4FE0.toInt(), 0xFFFFC857.toInt(), 0xFF3B2A6B.toInt(), 2, true),
        Look("Rin", 0xFFFFD84D.toInt(), 0xFFF2B600.toInt(), 0xFF2EC4A0.toInt(), 0xFFFF7A59.toInt(), 3, false),
        Look("Kai", 0xFF2B2F55.toInt(), 0xFF1B1E3A.toInt(), 0xFFFF4D4D.toInt(), 0xFF222222.toInt(), 4, false)
    )
}

/** Original anime-style characters drawn in code (no copyrighted art). */
class CharacterView(ctx: Context) : View(ctx) {
    var style = 0
        set(v) { field = v.coerceIn(0, 4); invalidate() }
    var speaking = false
    var listening = false

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2400
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { t = it.animatedValue as Float; invalidate() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); anim.start() }
    override fun onDetachedFromWindow() { anim.cancel(); super.onDetachedFromWindow() }

    private fun fill(c: Int) { p.style = Paint.Style.FILL; p.color = c }

    override fun onDraw(c: Canvas) {
        val L = Looks.all[style]
        val u = width / 100f
        val cx = width / 2f
        val bob = sin(t * 2 * PI).toFloat() * 3f * u
        val blink = if (t > 0.93f || (t > 0.45f && t < 0.48f)) 0.1f else 1f
        val mouth = if (speaking) abs(sin(t * 2 * PI * 6)).toFloat() else 0f
        c.save(); c.translate(0f, bob)

        if (listening) {
            p.style = Paint.Style.STROKE; p.strokeWidth = 2.5f * u; p.color = 0xAAFF4081.toInt()
            c.drawCircle(cx, 48 * u, (33 + 4 * abs(sin(t * 2 * PI * 2)).toFloat()) * u, p)
        }
        // back hair
        fill(L.hair)
        if (L.longHair) c.drawRoundRect(RectF(cx - 34 * u, 12 * u, cx + 34 * u, 108 * u), 30 * u, 30 * u, p)
        else c.drawRoundRect(RectF(cx - 32 * u, 12 * u, cx + 32 * u, 70 * u), 28 * u, 28 * u, p)
        // body
        fill(L.outfit)
        val body = Path().apply {
            moveTo(cx - 17 * u, 76 * u); lineTo(cx + 17 * u, 76 * u)
            lineTo(cx + 26 * u, 126 * u); lineTo(cx - 26 * u, 126 * u); close()
        }
        c.drawPath(body, p)
        fill(L.eye); c.drawCircle(cx, 88 * u, 3 * u, p) // chest gem / button
        // face
        fill(0xFFFFE4D2.toInt()); c.drawCircle(cx, 48 * u, 28 * u, p)
        // bangs
        fill(L.hair2)
        val bangs = Path().apply {
            moveTo(cx - 30 * u, 48 * u)
            quadTo(cx - 30 * u, 13 * u, cx, 13 * u)
            quadTo(cx + 30 * u, 13 * u, cx + 30 * u, 48 * u)
            lineTo(cx + 22 * u, 30 * u); lineTo(cx + 10 * u, 40 * u); lineTo(cx - 2 * u, 28 * u)
            lineTo(cx - 14 * u, 40 * u); lineTo(cx - 23 * u, 30 * u); close()
        }
        c.drawPath(bangs, p)
        drawAccessory(c, L, u, cx)
        // eyes
        for (ex in floatArrayOf(cx - 12 * u, cx + 12 * u)) {
            c.save(); c.scale(1f, blink, ex, 51 * u)
            fill(Color.WHITE); c.drawOval(RectF(ex - 8 * u, 42 * u, ex + 8 * u, 60 * u), p)
            fill(L.eye); c.drawCircle(ex, 51 * u, 6.5f * u, p)
            fill(0xFF1A1A2E.toInt()); c.drawCircle(ex, 51 * u, 3.5f * u, p)
            fill(Color.WHITE); c.drawCircle(ex - 2.5f * u, 48.5f * u, 2f * u, p)
            c.restore()
        }
        // blush
        fill(0x55FF6B8A.toInt())
        c.drawCircle(cx - 18 * u, 62 * u, 4 * u, p); c.drawCircle(cx + 18 * u, 62 * u, 4 * u, p)
        // mouth
        if (mouth > 0.05f) {
            fill(0xFFB5334F.toInt())
            c.drawOval(RectF(cx - 4 * u, 66 * u, cx + 4 * u, (66 + 1 + 6 * mouth) * u), p)
        } else {
            p.style = Paint.Style.STROKE; p.strokeWidth = 1.6f * u; p.color = 0xFFB5334F.toInt()
            c.drawArc(RectF(cx - 5 * u, 62 * u, cx + 5 * u, 70 * u), 20f, 140f, false, p)
        }
        c.restore()
    }

    private fun drawAccessory(c: Canvas, L: Look, u: Float, cx: Float) {
        when (L.acc) {
            0 -> { // ribbon
                fill(0xFFFF3B5C.toInt())
                val x = cx - 22 * u; val y = 20 * u
                c.drawPath(Path().apply { moveTo(x, y); lineTo(x - 10 * u, y - 7 * u); lineTo(x - 10 * u, y + 7 * u); close() }, p)
                c.drawPath(Path().apply { moveTo(x, y); lineTo(x + 10 * u, y - 7 * u); lineTo(x + 10 * u, y + 7 * u); close() }, p)
                c.drawCircle(x, y, 3 * u, p)
            }
            1 -> { // cat ears
                fill(L.hair)
                c.drawPath(Path().apply { moveTo(cx - 26 * u, 24 * u); lineTo(cx - 26 * u, 2 * u); lineTo(cx - 8 * u, 15 * u); close() }, p)
                c.drawPath(Path().apply { moveTo(cx + 26 * u, 24 * u); lineTo(cx + 26 * u, 2 * u); lineTo(cx + 8 * u, 15 * u); close() }, p)
                fill(0xFFFFB6C9.toInt())
                c.drawPath(Path().apply { moveTo(cx - 23 * u, 19 * u); lineTo(cx - 23 * u, 8 * u); lineTo(cx - 14 * u, 15 * u); close() }, p)
                c.drawPath(Path().apply { moveTo(cx + 23 * u, 19 * u); lineTo(cx + 23 * u, 8 * u); lineTo(cx + 14 * u, 15 * u); close() }, p)
            }
            2 -> { // flower
                val x = cx + 20 * u; val y = 20 * u
                fill(0xFFFFFFFF.toInt())
                for (i in 0 until 5) {
                    val a = i * 2 * PI / 5
                    c.drawCircle(x + (5 * u * kotlin.math.cos(a)).toFloat(), y + (5 * u * sin(a)).toFloat(), 3.2f * u, p)
                }
                fill(0xFFFFD84D.toInt()); c.drawCircle(x, y, 3 * u, p)
            }
            3 -> { // hairpins
                p.style = Paint.Style.STROKE; p.strokeCap = Paint.Cap.ROUND; p.strokeWidth = 2.6f * u; p.color = 0xFF00C2FF.toInt()
                c.drawLine(cx + 12 * u, 22 * u, cx + 24 * u, 30 * u, p)
                c.drawLine(cx + 10 * u, 28 * u, cx + 22 * u, 36 * u, p)
            }
            else -> { // headband
                p.style = Paint.Style.STROKE; p.strokeWidth = 4f * u; p.color = 0xFFFF4D4D.toInt()
                c.drawArc(RectF(cx - 29 * u, 14 * u, cx + 29 * u, 60 * u), 205f, 130f, false, p)
            }
        }
    }
}
