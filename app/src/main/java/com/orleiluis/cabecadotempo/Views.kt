package com.orleiluis.cabecadotempo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import kotlin.math.min

object Palette {
    val BG = Color.parseColor("#14141A")
    val CARD = Color.parseColor("#1F1F28")
    val CARD2 = Color.parseColor("#2A2A36")
    val TEXT = Color.parseColor("#F2F0EA")
    val MUTED = Color.parseColor("#9C9AA6")
    val DOWN = Color.parseColor("#FF5A36")   // o "1" do compasso
    val HEAD = Color.parseColor("#FFC531")   // demais cabeças
    val SUB = Color.parseColor("#4FC3F7")    // subdivisões
    val DOWN_DIM = Color.parseColor("#4A2219")
    val HEAD_DIM = Color.parseColor("#41361A")
    val SUB_DIM = Color.parseColor("#262631")
}

fun symbolFor(c: Char): String = when (c) {
    'D' -> "↓"
    'U' -> "↑"
    'X' -> "✕↓"
    'x' -> "✕↑"
    'S' -> "↓•"
    'B' -> "B"
    'b' -> "b"
    'C' -> "C"
    'P' -> "B+C"
    'Q' -> "b+C"
    else -> ""
}

fun subLabel(stepsPerBeat: Int, sub: Int): String = when (stepsPerBeat) {
    2 -> "e"
    4 -> if (sub == 2) "e" else "·"
    else -> "·"
}

/** Grade do padrão: cada tempo é um bloco; a cabeça do tempo é destacada. */
class BeatGridView(ctx: Context) : View(ctx) {
    private var rhythm: Rhythm? = null
    private var pattern = ""
    var current = -1
        set(v) {
            field = v
            invalidate()
        }

    private val d = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val rect = RectF()

    fun setup(r: Rhythm, pat: String) {
        rhythm = r
        pattern = pat
        current = -1
        requestLayout()
        invalidate()
    }

    private val rowH get() = 78 * d
    private val rowGap get() = 10 * d

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val bars = rhythm?.bars ?: 1
        val h = (bars * rowH + (bars - 1) * rowGap + 4 * d).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        val r = rhythm ?: return
        val spb = r.stepsPerBeat
        val spBar = r.stepsPerBar
        val beatGap = 8 * d
        val pad = 2 * d
        val cellW = (width - 2 * pad - (r.beatsPerBar - 1) * beatGap) / spBar
        for (step in pattern.indices) {
            val row = step / spBar
            val col = step % spBar
            val beat = col / spb
            val sub = col % spb
            val head = sub == 0
            val down = col == 0
            val left = pad + col * cellW + beat * beatGap
            val top = pad + row * (rowH + rowGap)
            val inset = 1.5f * d
            rect.set(left + inset, top, left + cellW - inset, top + rowH)
            val isCur = step == current
            fill.color = when {
                isCur && down -> Palette.DOWN
                isCur && head -> Palette.HEAD
                isCur -> Palette.SUB
                down -> Palette.DOWN_DIM
                head -> Palette.HEAD_DIM
                else -> Palette.SUB_DIM
            }
            canvas.drawRoundRect(rect, 6 * d, 6 * d, fill)
            if (head) {
                stroke.strokeWidth = if (down) 3.5f * d else 2.5f * d
                stroke.color = if (down) Palette.DOWN else Palette.HEAD
                canvas.drawRoundRect(rect, 6 * d, 6 * d, stroke)
            }
            // contagem
            val dark = isCur
            txt.typeface = Typeface.DEFAULT_BOLD
            if (head) {
                txt.textSize = min(cellW * 0.7f, 22 * d)
                txt.color = if (dark) Palette.BG else if (down) Palette.DOWN else Palette.HEAD
                canvas.drawText((beat + 1).toString(), rect.centerX(), top + 24 * d, txt)
            } else {
                txt.textSize = min(cellW * 0.55f, 14 * d)
                txt.color = if (dark) Palette.BG else Palette.MUTED
                canvas.drawText(subLabel(spb, sub), rect.centerX(), top + 21 * d, txt)
            }
            // símbolo
            val sym = symbolFor(pattern[step])
            if (sym.isNotEmpty()) {
                var size = min(cellW * 0.8f, 28 * d)
                txt.textSize = size
                val maxW = cellW - 3 * d
                val w = txt.measureText(sym)
                if (w > maxW) {
                    size *= maxW / w
                    txt.textSize = size
                }
                txt.color = if (dark) Palette.BG else Palette.TEXT
                canvas.drawText(sym, rect.centerX(), top + rowH - 16 * d, txt)
            }
        }
    }
}

/** Círculo grande que pisca em cada cabeça do tempo; o 1 pisca em vermelho. */
class PulseView(ctx: Context) : View(ctx) {
    var beats = 4
        set(v) {
            field = v
            invalidate()
        }
    private var active = -1
    private var down = false
    private var count = false
    private var flash = 0f
    private var anim: ValueAnimator? = null
    private val d = resources.displayMetrics.density
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    fun pulse(beatIndex: Int, isDown: Boolean, isCountIn: Boolean, beatMs: Long) {
        active = beatIndex
        down = isDown
        count = isCountIn
        anim?.cancel()
        anim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = (beatMs * 0.7).toLong().coerceIn(80L, 600L)
            addUpdateListener {
                flash = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun reset() {
        anim?.cancel()
        active = -1
        flash = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val dotsH = 34 * d
        val cx = width / 2f
        val cy = (height - dotsH) / 2f
        val base = min(width.toFloat(), height - dotsH) / 2f * 0.86f
        val color = if (down) Palette.DOWN else Palette.HEAD

        // halo
        if (active >= 0 && flash > 0f) {
            p.style = Paint.Style.FILL
            p.color = color
            p.alpha = (90 * flash).toInt()
            canvas.drawCircle(cx, cy, base * (1f + 0.14f * flash), p)
        }
        // disco
        p.style = Paint.Style.FILL
        p.color = Palette.CARD
        p.alpha = 255
        canvas.drawCircle(cx, cy, base, p)
        if (active >= 0) {
            p.color = color
            p.alpha = (255 * (0.25f + 0.75f * flash)).toInt()
            canvas.drawCircle(cx, cy, base * (0.92f + 0.08f * flash), p)
        }
        p.style = Paint.Style.STROKE
        p.strokeWidth = 4 * d
        p.color = if (active >= 0) color else Palette.CARD2
        p.alpha = 255
        canvas.drawCircle(cx, cy, base, p)

        // número
        txt.textSize = base * 0.95f
        txt.color = if (active >= 0 && flash > 0.35f) Palette.BG else Palette.TEXT
        val label = if (active >= 0) (active + 1).toString() else "▶"
        val fm = txt.fontMetrics
        canvas.drawText(label, cx, cy - (fm.ascent + fm.descent) / 2f, txt)
        txt.textSize = 12 * d
        txt.color = if (active >= 0 && flash > 0.35f) Palette.BG else Palette.MUTED
        val cap = when {
            active < 0 -> "toque em Tocar"
            count -> "CONTAGEM"
            down -> "CABEÇA DO 1"
            else -> "CABEÇA"
        }
        canvas.drawText(cap, cx, cy + base * 0.58f, txt)

        // pontos dos tempos
        val gap = 26 * d
        val startX = cx - (beats - 1) * gap / 2f
        val dy = height - dotsH / 2f
        for (b in 0 until beats) {
            val on = b == active
            p.style = Paint.Style.FILL
            p.color = when {
                on && b == 0 -> Palette.DOWN
                on -> Palette.HEAD
                b == 0 -> Palette.DOWN_DIM
                else -> Palette.CARD2
            }
            canvas.drawCircle(startX + b * gap, dy, if (b == 0) 8 * d else 6 * d, p)
        }
    }
}
