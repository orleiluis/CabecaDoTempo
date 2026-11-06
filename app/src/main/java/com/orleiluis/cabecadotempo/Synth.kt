package com.orleiluis.cabecadotempo

import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

fun midiToFreq(m: Int): Float = (440.0 * 2.0.pow((m - 69) / 12.0)).toFloat()

/** Corda sintetizada (Karplus-Strong com afinação fracionária por all-pass). */
class KsString(private val sr: Int) {
    private val buf = FloatArray(sr / 30 + 4)
    private var n = 2
    private var idx = 0
    private var c = 0f
    private var apX1 = 0f
    private var apY1 = 0f
    private var decay = 0.996f
    private var gain = 1f
    private var damping = false
    private var stopAfter = -1
    private var active = false

    private var pendingDelay = -1
    private var pFreq = 110f
    private var pAmp = 0f
    private var pBright = 0.5f
    private var pDecay = 0.996f
    private var pStopAfter = -1

    private val rnd = Random()

    fun pluck(freq: Float, amp: Float, bright: Float, delaySamples: Int, stopAfterSamples: Int = -1, decayFactor: Float = 0.997f) {
        pFreq = freq
        pAmp = amp
        pBright = bright
        pDecay = decayFactor
        pStopAfter = stopAfterSamples
        pendingDelay = max(0, delaySamples)
    }

    fun damp() {
        pendingDelay = -1
        if (active) damping = true
    }

    private fun begin() {
        val period = sr / pFreq.toDouble()
        var len = floor(period - 0.5).toInt()
        var d = period - 0.5 - len
        if (d < 0.1) {
            len -= 1
            d += 1.0
        }
        len = len.coerceIn(2, buf.size)
        n = len
        c = ((1.0 - d) / (1.0 + d)).toFloat()
        apX1 = 0f
        apY1 = 0f
        var y = 0f
        var mean = 0f
        for (i in 0 until n) {
            val x = rnd.nextFloat() * 2f - 1f
            y += pBright * (x - y)
            buf[i] = y
            mean += y
        }
        mean /= n
        var peak = 0.0001f
        for (i in 0 until n) {
            buf[i] -= mean
            val a = if (buf[i] < 0) -buf[i] else buf[i]
            if (a > peak) peak = a
        }
        val norm = pAmp / peak
        for (i in 0 until n) buf[i] *= norm
        idx = 0
        decay = pDecay
        gain = 1f
        damping = false
        stopAfter = pStopAfter
        active = true
    }

    fun next(): Float {
        if (pendingDelay >= 0) {
            if (pendingDelay == 0) {
                pendingDelay = -1
                begin()
            } else {
                pendingDelay--
            }
        }
        if (!active) return 0f
        val i1 = if (idx + 1 >= n) 0 else idx + 1
        val out = buf[idx]
        val x = (buf[idx] + buf[i1]) * 0.5f * decay
        val ap = c * x + apX1 - c * apY1
        apX1 = x
        apY1 = ap
        buf[idx] = ap
        idx = i1
        if (stopAfter > 0) {
            stopAfter--
            if (stopAfter == 0) damping = true
        }
        if (damping) {
            gain *= 0.9965f
            if (gain < 0.0005f) active = false
        }
        return out * gain
    }
}

/** Som de corda abafada (chuck): ruído filtrado + "tum" grave curto. */
class MuteHit(private val sr: Int) {
    private val rnd = Random()
    private var left = 0
    private var total = 1
    private var amp = 0f
    private var lp = 0f
    private var phase = 0.0

    fun trigger(a: Float) {
        amp = a
        total = (sr * 0.045).toInt()
        left = total
        phase = 0.0
    }

    fun next(): Float {
        if (left <= 0) return 0f
        val t = (total - left).toFloat() / sr
        left--
        val env = exp(-t * 90f)
        val x = rnd.nextFloat() * 2f - 1f
        lp += 0.45f * (x - lp)
        phase += 2.0 * PI * 140.0 / sr
        return (lp * 0.8f + sin(phase).toFloat() * 0.6f) * env * amp
    }
}

/** Cliques do metrônomo pré-calculados. */
object Clicks {
    fun make(sr: Int, f1: Double, f2: Double, ms: Int, amp: Float, decay: Double): FloatArray {
        val n = sr * ms / 1000
        val out = FloatArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sr
            val env = exp(-t * decay)
            val attack = if (i < 40) i / 40.0 else 1.0
            val s = sin(2 * PI * f1 * t) * 0.7 + sin(2 * PI * f2 * t) * 0.3
            out[i] = (s * env * attack * amp).toFloat()
        }
        return out
    }
}
