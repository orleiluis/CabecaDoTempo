package com.orleiluis.cabecadotempo

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class Stage(val text: String, val guitar: Boolean, val headsOnly: Boolean, val factor: Float, val bars: Int)

val STAGES = listOf(
    Stage("Etapa 1/5 · Só escute e bata o pé em cada CABEÇA (o 1 é o mais forte)", false, false, 0.7f, 2),
    Stage("Etapa 2/5 · Toque ↓ só nas cabeças do tempo", true, true, 0.7f, 4),
    Stage("Etapa 3/5 · Ritmo completo, devagar", true, false, 0.6f, 4),
    Stage("Etapa 4/5 · Ritmo completo, quase na velocidade", true, false, 0.8f, 4),
    Stage("Etapa 5/5 · Velocidade alvo — segure a cabeça do tempo!", true, false, 1.0f, -1)
)

class StepEvent(
    val frame: Long,
    val absStep: Long,
    val patternStep: Int,
    val beatInBar: Int,
    val isHead: Boolean,
    val isDownbeat: Boolean,
    val countIn: Boolean,
    val chord: String,
    val nextChord: String,
    val bpm: Int,
    val stageIndex: Int,
    val headsOnly: Boolean
)

/**
 * Sequenciador + síntese em tempo real. O tempo é contado em amostras de áudio,
 * então o clique e o violão ficam exatamente na cabeça do tempo.
 */
class RhythmEngine {
    @Volatile var bpm = 80
    @Volatile var clickOn = true
    @Volatile var subdivOn = false
    @Volatile var guitarOn = true
    @Volatile var countInOn = true
    @Volatile var rampOn = false
    @Volatile var guided = false

    @Volatile private var running = false
    @Volatile private var track: AudioTrack? = null
    private var thread: Thread? = null

    private val lock = Any()
    private val ring = arrayOfNulls<StepEvent>(96)
    private var ringPos = 0

    val isRunning: Boolean get() = running

    fun start(r: Rhythm) {
        stop()
        running = true
        val t = Thread({ loop(r) }, "rhythm-audio")
        t.priority = Thread.MAX_PRIORITY
        thread = t
        t.start()
    }

    fun stop() {
        running = false
        try {
            thread?.join(800)
        } catch (_: InterruptedException) {
        }
        thread = null
        synchronized(lock) {
            for (i in ring.indices) ring[i] = null
            ringPos = 0
        }
    }

    fun playedFrames(): Long {
        val t = track ?: return -1
        return try {
            t.playbackHeadPosition.toLong() and 0xffffffffL
        } catch (e: Exception) {
            -1
        }
    }

    fun eventAt(frame: Long): StepEvent? {
        synchronized(lock) {
            var best: StepEvent? = null
            for (e in ring) {
                if (e != null && e.frame <= frame && (best == null || e.frame > best.frame)) best = e
            }
            return best
        }
    }

    private fun record(e: StepEvent) {
        synchronized(lock) {
            ring[ringPos] = e
            ringPos = (ringPos + 1) % ring.size
        }
    }

    private fun loop(r: Rhythm) {
        val sr = 44100
        val minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t: AudioTrack
        try {
            t = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sr)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(max(minBuf, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } catch (e: Exception) {
            running = false
            return
        }

        val strings = Array(6) { KsString(sr) }
        val mute = MuteHit(sr)
        val clickDown = Clicks.make(sr, 1800.0, 2700.0, 45, 1.0f, 70.0)
        val clickHead = Clicks.make(sr, 1150.0, 1720.0, 40, 0.62f, 80.0)
        val clickSub = Clicks.make(sr, 2400.0, 3600.0, 14, 0.16f, 300.0)
        var clickBuf: FloatArray? = null
        var clickPos = 0

        val spb = r.stepsPerBeat
        val spBar = r.stepsPerBar
        val full = r.pattern
        val heads = r.headsOnlyPattern()
        val chordNames = r.chords

        val countInTotal = if (countInOn) spBar * (if (r.beatsPerBar <= 2) 2 else 1) else 0
        var countInLeft = countInTotal
        var pStep = 0
        var barCounter = 0L
        var firstBar = true
        var absStep = 0L
        var stageIdx = 0
        var stageBars = 0
        var rampBars = 0
        var playBpm = bpm
        var wasRamp = rampOn

        val block = 192
        val out = ShortArray(block)
        var frame = 0L
        var nextStep = sr * 0.12

        fun strum(ch: Chord, down: Boolean, vel: Float, stepLen: Double, short: Boolean) {
            val spread = min(sr * 0.011, stepLen * 0.12).toInt()
            val stop = if (short) (sr * 0.085).toInt() else -1
            if (down) {
                var k = 0
                for (s in 0 until 6) {
                    val m = ch.strings[s]
                    if (m < 0) {
                        strings[s].damp()
                        continue
                    }
                    strings[s].pluck(midiToFreq(m), vel * 0.85f, 0.55f, k * spread, if (stop > 0) stop + k * spread else -1)
                    k++
                }
            } else {
                var k = 0
                for (s in 5 downTo 2) {
                    val m = ch.strings[s]
                    if (m < 0) continue
                    strings[s].pluck(midiToFreq(m), vel * 0.55f, 0.7f, k * (spread * 3 / 4), if (stop > 0) stop + k * spread else -1)
                    k++
                }
            }
        }

        fun fingers(ch: Chord, vel: Float) {
            var k = 0
            for (s in 5 downTo 0) {
                if (k >= 3) break
                val m = ch.strings[s]
                if (m < 0 || s == ch.bassString) continue
                strings[s].pluck(midiToFreq(m), vel * 0.6f, 0.4f, k * (sr / 600))
                k++
            }
        }

        fun bass(stringIdx: Int, midi: Int, vel: Float) {
            if (midi < 0) return
            if (stringIdx in 0..2) {
                for (s in 0..2) if (s != stringIdx) strings[s].damp()
            }
            strings[stringIdx].pluck(midiToFreq(midi), vel * 1.05f, 0.28f, 0, -1, 0.998f)
        }

        fun playChar(c: Char, ch: Chord, vel: Float, stepLen: Double) {
            when (c) {
                'D' -> strum(ch, true, vel, stepLen, false)
                'U' -> strum(ch, false, vel, stepLen, false)
                'S' -> strum(ch, true, vel, stepLen, true)
                'X' -> {
                    for (s in strings) s.damp(); mute.trigger(0.9f * vel)
                }
                'x' -> {
                    for (s in strings) s.damp(); mute.trigger(0.55f * vel)
                }
                'B' -> bass(ch.bassString, ch.strings[ch.bassString], vel)
                'b' -> bass(ch.altString, ch.altMidi, vel)
                'C' -> fingers(ch, vel)
                'P' -> {
                    bass(ch.bassString, ch.strings[ch.bassString], vel); fingers(ch, vel * 0.9f)
                }
                'Q' -> {
                    bass(ch.altString, ch.altMidi, vel); fingers(ch, vel * 0.9f)
                }
                else -> {}
            }
        }

        try {
            t.play()
            track = t
            while (running) {
                for (i in 0 until block) {
                    if (frame >= nextStep) {
                        val isCount = countInLeft > 0
                        val inBar: Int
                        val pIdx: Int
                        if (isCount) {
                            inBar = (countInTotal - countInLeft) % spBar
                            pIdx = -1
                        } else {
                            inBar = pStep % spBar
                            if (inBar == 0) {
                                if (!firstBar) {
                                    barCounter++
                                    if (guided) {
                                        val st = STAGES[stageIdx]
                                        stageBars++
                                        if (st.bars > 0 && stageBars >= st.bars && stageIdx < STAGES.size - 1) {
                                            stageIdx++
                                            stageBars = 0
                                            pStep = 0
                                        }
                                    }
                                    if (rampOn) {
                                        rampBars++
                                        if (rampBars % 4 == 0) playBpm = min(playBpm + 4, max(r.bpmMax, bpm))
                                    }
                                }
                                firstBar = false
                            }
                            pIdx = pStep
                        }

                        if (rampOn != wasRamp) {
                            wasRamp = rampOn
                            playBpm = bpm
                            rampBars = 0
                        }
                        if (!rampOn) playBpm = bpm

                        val stage = if (guided) STAGES[stageIdx] else null
                        val effBpm = if (stage != null) max(30, (playBpm * stage.factor).roundToInt()) else playBpm
                        val stepLen = sr * 60.0 / effBpm / spb

                        val head = inBar % spb == 0
                        val down = inBar == 0
                        val chordIdx = ((barCounter / r.chordEveryBars) % chordNames.size).toInt()
                        val chName = chordNames[chordIdx]
                        val nextName = chordNames[(chordIdx + 1) % chordNames.size]
                        val headsOnly = stage?.headsOnly == true

                        // Metrônomo: a cabeça sempre bem marcada (o 1 mais forte e mais agudo)
                        val forceClick = isCount || (stage != null && !stage.guitar)
                        if (clickOn || forceClick) {
                            if (head) {
                                clickBuf = if (down) clickDown else clickHead
                                clickPos = 0
                            } else if (subdivOn) {
                                clickBuf = clickSub
                                clickPos = 0
                            }
                        }

                        if (!isCount && guitarOn && (stage == null || stage.guitar)) {
                            val pat = if (headsOnly) heads else full
                            val c = pat[pStep % pat.length]
                            val ch = CHORDS[chName] ?: CHORDS.getValue("G")
                            val vel = if (down) 1.0f else if (head) 0.9f else 0.68f
                            playChar(c, ch, vel, stepLen)
                        }

                        record(
                            StepEvent(
                                frame, absStep, pIdx, inBar / spb, head, down, isCount,
                                chName, nextName, effBpm, if (guided) stageIdx else -1, headsOnly
                            )
                        )
                        absStep++
                        if (isCount) {
                            countInLeft--
                        } else {
                            pStep = (pStep + 1) % full.length
                        }
                        nextStep += stepLen
                    }

                    var g = 0f
                    for (s in strings) g += s.next()
                    g += mute.next()
                    var v = g * 0.2f
                    val cb = clickBuf
                    if (cb != null) {
                        v += cb[clickPos] * 0.9f
                        clickPos++
                        if (clickPos >= cb.size) clickBuf = null
                    }
                    if (v > 1f) v = 1f else if (v < -1f) v = -1f
                    out[i] = (v * 32000f).toInt().toShort()
                    frame++
                }
                t.write(out, 0, block)
            }
        } catch (e: Exception) {
            running = false
        } finally {
            track = null
            try {
                t.pause()
                t.flush()
                t.release()
            } catch (_: Exception) {
            }
        }
    }
}
