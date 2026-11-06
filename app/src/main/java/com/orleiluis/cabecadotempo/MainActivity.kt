package com.orleiluis.cabecadotempo

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private val engine = RhythmEngine()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private var vibrator: Vibrator? = null

    private var rhythm: Rhythm? = null
    private var onPlayer = false
    private var bpm = 80
    private var vibrateOn = false

    // views do player
    private var pulse: PulseView? = null
    private var grid: BeatGridView? = null
    private var bpmText: TextView? = null
    private var bpmSeek: SeekBar? = null
    private var playBtn: Button? = null
    private var stageText: TextView? = null
    private var chordText: TextView? = null
    private var modeFree: Button? = null
    private var modeGuided: Button? = null

    private var lastAbs = -1L
    private var gridHeadsOnly = false
    private val taps = ArrayList<Long>()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("cabeca", MODE_PRIVATE)
        vibrator = getSystemService(Vibrator::class.java)
        window.statusBarColor = Palette.BG
        window.navigationBarColor = Palette.BG
        engine.clickOn = prefs.getBoolean("click", true)
        engine.subdivOn = prefs.getBoolean("subdiv", false)
        engine.guitarOn = prefs.getBoolean("guitar", true)
        engine.countInOn = prefs.getBoolean("countin", true)
        vibrateOn = prefs.getBoolean("vibrate", false)
        showList()
    }

    override fun onPause() {
        super.onPause()
        stopPlay()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (onPlayer) {
            stopPlay()
            showList()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    // ---------------- helpers de UI ----------------

    private fun rounded(color: Int, radiusDp: Int = 14, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeColor != null) setStroke(dp(2), strokeColor)
        }

    private fun text(s: String, sizeSp: Float, color: Int = Palette.TEXT, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s
            textSize = sizeSp
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun button(label: String, bg: Int, fg: Int = Palette.TEXT, onClick: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 16f
            setTextColor(fg)
            background = rounded(bg, 12)
            minHeight = dp(48)
            minimumHeight = dp(48)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { onClick() }
        }

    private fun lp(w: Int, h: Int, weight: Float = 0f, top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h, weight).apply { topMargin = dp(top) }

    private fun topBar(title: String, subtitle: String?, back: (() -> Unit)?): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Palette.CARD)
        }
        if (back != null) {
            bar.addView(button("← Ritmos", Palette.CARD2) { back() }, lp(ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)))
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        col.addView(text(title, 19f, Palette.TEXT, true))
        if (subtitle != null) col.addView(text(subtitle, 12f, Palette.MUTED))
        bar.addView(col, lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return bar
    }

    private fun previewOf(r: Rhythm): String {
        val sb = StringBuilder()
        for (i in r.pattern.indices) {
            if (i > 0 && i % r.stepsPerBar == 0) sb.append("  ‖  ")
            else if (i > 0 && i % r.stepsPerBeat == 0) sb.append("  ")
            val s = symbolFor(r.pattern[i])
            sb.append(if (s.isEmpty()) "·" else s)
        }
        return sb.toString()
    }

    // ---------------- lista de ritmos ----------------

    private fun showList() {
        onPlayer = false
        pulse = null
        grid = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.BG)
            fitsSystemWindows = true
        }
        root.addView(topBar("Cabeça do Tempo", "Ritmos de guitarra com o tempo bem marcado", null))

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(32))
        }
        list.addView(
            text(
                "Escolha um ritmo. O clique forte (vermelho) é o 1 do compasso; os amarelos são as outras cabeças do tempo.",
                13f, Palette.MUTED
            ), lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6)
        )
        for (cat in CATEGORIES) {
            list.addView(
                text(cat.uppercase(), 13f, Palette.HEAD, true),
                lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 20)
            )
            for (r in RHYTHMS.filter { it.category == cat }) {
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    background = rounded(Palette.CARD)
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    isClickable = true
                    setOnClickListener { showPlayer(r) }
                }
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                row.addView(text(r.name, 17f, Palette.TEXT, true), lp(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                val saved = prefs.getInt("bpm_" + r.id, r.bpm)
                row.addView(text("${r.timeSig} · $saved BPM", 13f, Palette.MUTED))
                card.addView(row)
                card.addView(
                    text(previewOf(r), 14f, Palette.SUB).apply { typeface = Typeface.MONOSPACE },
                    lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 6)
                )
                list.addView(card, lp(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0f, 8))
            }
        }
        scroll.addView(list)
        root.addView(scroll, lp(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    // ---------------- player ----------------

    private fun showPlayer(r: Rhythm) {
        stopPlay()
        rhythm = r
        onPlayer = true
        bpm = prefs.getInt("bpm_" + r.id, r.bpm)
        engine.guided = false
        engine.rampOn = false

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.BG)
            fitsSystemWindows = true
        }
        root.addView(topBar(r.name, "${r.timeSig} · ${r.category}") {
            stopPlay()
            showList()
        })

        val scroll = ScrollView(this)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(40))
        }
        val full = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        val pv = PulseView(this).apply { beats = r.beatsPerBar }
        pulse = pv
        col.addView(pv, lp(full, dp(230)))

        val st = text("", 14f, Palette.HEAD, true).apply { gravity = Gravity.CENTER }
        stageText = st
        col.addView(st, lp(full, wrap, 0f, 4))

        val ct = text("", 16f, Palette.TEXT).apply { gravity = Gravity.CENTER }
        chordText = ct
        col.addView(ct, lp(full, wrap, 0f, 4))

        val g = BeatGridView(this)
        g.setup(r, r.pattern)
        gridHeadsOnly = false
        grid = g
        col.addView(g, lp(full, wrap, 0f, 10))

        val pb = button("▶  Tocar", Palette.DOWN, Palette.BG) { togglePlay() }
        pb.textSize = 20f
        pb.typeface = Typeface.DEFAULT_BOLD
        playBtn = pb
        col.addView(pb, lp(full, dp(60), 0f, 14))

        // BPM
        val bpmRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        bpmRow.addView(button("−5", Palette.CARD2) { setBpm(bpm - 5) }, lp(0, dp(48), 1f))
        bpmRow.addView(button("−1", Palette.CARD2) { setBpm(bpm - 1) }, lp(0, dp(48), 1f).apply { leftMargin = dp(6) })
        val bt = text("", 26f, Palette.TEXT, true).apply { gravity = Gravity.CENTER }
        bpmText = bt
        bpmRow.addView(bt, lp(0, wrap, 1.6f))
        bpmRow.addView(button("+1", Palette.CARD2) { setBpm(bpm + 1) }, lp(0, dp(48), 1f))
        bpmRow.addView(button("+5", Palette.CARD2) { setBpm(bpm + 5) }, lp(0, dp(48), 1f).apply { leftMargin = dp(6) })
        col.addView(bpmRow, lp(full, wrap, 0f, 14))

        val sk = SeekBar(this).apply {
            max = 240 - 30
            progress = bpm - 30
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) setBpm(p + 30)
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        bpmSeek = sk
        col.addView(sk, lp(full, dp(40), 0f, 6))
        col.addView(
            text("Faixa sugerida: ${r.bpmMin}–${r.bpmMax} BPM · alvo ${r.bpm}", 12f, Palette.MUTED).apply { gravity = Gravity.CENTER },
            lp(full, wrap)
        )
        col.addView(button("Tap tempo (toque no ritmo)", Palette.CARD2) { tap() }, lp(full, dp(48), 0f, 10))

        // modo
        col.addView(text("MODO", 13f, Palette.HEAD, true), lp(full, wrap, 0f, 20))
        val modeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val mf = button("Livre", Palette.CARD2) { setGuided(false) }
        val mg = button("Treino guiado", Palette.CARD2) { setGuided(true) }
        modeFree = mf
        modeGuided = mg
        modeRow.addView(mf, lp(0, dp(48), 1f))
        modeRow.addView(mg, lp(0, dp(48), 1f).apply { leftMargin = dp(8) })
        col.addView(modeRow, lp(full, wrap, 0f, 6))
        col.addView(
            text(
                "Treino guiado: 1) só o clique, você bate o pé · 2) ↓ só nas cabeças · 3) ritmo a 60% · 4) 80% · 5) velocidade alvo.",
                12f, Palette.MUTED
            ), lp(full, wrap, 0f, 6)
        )

        // opções
        col.addView(text("SOM E AJUDAS", 13f, Palette.HEAD, true), lp(full, wrap, 0f, 20))
        col.addView(switch("Metrônomo marcando a cabeça", engine.clickOn) {
            engine.clickOn = it; prefs.edit().putBoolean("click", it).apply()
        })
        col.addView(switch("Clique nas subdivisões também", engine.subdivOn) {
            engine.subdivOn = it; prefs.edit().putBoolean("subdiv", it).apply()
        })
        col.addView(switch("Violão tocando o ritmo", engine.guitarOn) {
            engine.guitarOn = it; prefs.edit().putBoolean("guitar", it).apply()
        })
        col.addView(switch("Contagem antes de começar", engine.countInOn) {
            engine.countInOn = it; prefs.edit().putBoolean("countin", it).apply()
        })
        col.addView(switch("Vibrar em cada cabeça", vibrateOn) {
            vibrateOn = it; prefs.edit().putBoolean("vibrate", it).apply()
        })
        col.addView(switch("Acelerar aos poucos (+4 BPM a cada 4 compassos)", engine.rampOn) {
            engine.rampOn = it
        })

        // explicação
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.CARD)
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }
        info.addView(text("Como é o ritmo", 15f, Palette.TEXT, true))
        info.addView(text(r.description, 14f, Palette.MUTED), lp(full, wrap, 0f, 4))
        info.addView(text("Onde está a cabeça do tempo", 15f, Palette.HEAD, true), lp(full, wrap, 0f, 12))
        info.addView(text(r.tip, 14f, Palette.TEXT), lp(full, wrap, 0f, 4))
        val every = if (r.chordEveryBars > 1) "  (um acorde a cada ${r.chordEveryBars} compassos)" else "  (um por compasso)"
        info.addView(
            text("Progressão: " + r.chords.joinToString("  ") + every, 13f, Palette.MUTED),
            lp(full, wrap, 0f, 12)
        )
        col.addView(info, lp(full, wrap, 0f, 20))

        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Palette.CARD)
            setPadding(dp(14), dp(12), dp(14), dp(14))
        }
        legend.addView(text("Legenda", 15f, Palette.TEXT, true))
        legend.addView(
            text(
                "↓ palhetada pra baixo   ↑ pra cima\n" +
                    "✕ corda abafada (mão esquerda alivia)\n" +
                    "↓• acorde seco (toca e abafa logo)\n" +
                    "B baixo com o polegar (tônica)   b baixo alternado\n" +
                    "C dedos pinçando o acorde   B+C juntos\n" +
                    "Quadro com borda vermelha = 1 do compasso · amarela = cabeça do tempo\n" +
                    "Número = cabeça · \"e\" = contratempo · \"·\" = semicolcheia/tercina",
                13f, Palette.MUTED
            ), lp(full, wrap, 0f, 6)
        )
        col.addView(legend, lp(full, wrap, 0f, 12))

        scroll.addView(col)
        root.addView(scroll, lp(full, 0, 1f))
        setContentView(root)

        updateBpmUi(bpm)
        updateModeUi()
        stageText?.text = ""
        chordText?.text = "Acorde: ${r.chords[0]}"
    }

    private fun switch(label: String, value: Boolean, onChange: (Boolean) -> Unit): View =
        Switch(this).apply {
            text = label
            textSize = 15f
            setTextColor(Palette.TEXT)
            isChecked = value
            minHeight = dp(48)
            setOnCheckedChangeListener { _, b -> onChange(b) }
        }

    private fun setBpm(v: Int) {
        val r = rhythm ?: return
        bpm = v.coerceIn(30, 240)
        engine.bpm = bpm
        prefs.edit().putInt("bpm_" + r.id, bpm).apply()
        updateBpmUi(bpm)
    }

    private fun updateBpmUi(shown: Int) {
        bpmText?.text = "$shown BPM"
        bpmSeek?.progress = bpm - 30
    }

    private fun setGuided(g: Boolean) {
        if (engine.guided == g) return
        engine.guided = g
        updateModeUi()
        if (engine.isRunning) {
            stopPlay()
            startPlay()
        }
    }

    private fun updateModeUi() {
        val g = engine.guided
        modeFree?.background = rounded(if (!g) Palette.HEAD else Palette.CARD2, 12)
        modeFree?.setTextColor(if (!g) Palette.BG else Palette.TEXT)
        modeGuided?.background = rounded(if (g) Palette.HEAD else Palette.CARD2, 12)
        modeGuided?.setTextColor(if (g) Palette.BG else Palette.TEXT)
    }

    private fun tap() {
        val now = SystemClock.elapsedRealtime()
        if (taps.isNotEmpty() && now - taps.last() > 2000) taps.clear()
        taps.add(now)
        while (taps.size > 5) taps.removeAt(0)
        if (taps.size >= 2) {
            val avg = (taps.last() - taps.first()).toDouble() / (taps.size - 1)
            if (avg > 0) setBpm(Math.round(60000.0 / avg).toInt())
        }
    }

    private fun togglePlay() {
        if (engine.isRunning) stopPlay() else startPlay()
    }

    private fun startPlay() {
        val r = rhythm ?: return
        engine.bpm = bpm
        lastAbs = -1L
        engine.start(r)
        playBtn?.text = "■  Parar"
        playBtn?.background = rounded(Palette.CARD2, 12)
        playBtn?.setTextColor(Palette.TEXT)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    private fun stopPlay() {
        handler.removeCallbacks(ticker)
        engine.stop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playBtn?.text = "▶  Tocar"
        playBtn?.background = rounded(Palette.DOWN, 12)
        playBtn?.setTextColor(Palette.BG)
        grid?.current = -1
        pulse?.reset()
        val r = rhythm
        if (r != null && gridHeadsOnly) {
            grid?.setup(r, r.pattern)
            gridHeadsOnly = false
        }
        if (onPlayer) {
            updateBpmUi(bpm)
            stageText?.text = ""
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (!engine.isRunning) {
                if (onPlayer && playBtn?.text?.startsWith("■") == true) stopPlay()
                return
            }
            val f = engine.playedFrames()
            if (f >= 0) {
                val e = engine.eventAt(f)
                if (e != null && e.absStep != lastAbs) {
                    lastAbs = e.absStep
                    onStep(e)
                }
            }
            handler.postDelayed(this, 6)
        }
    }

    private fun onStep(e: StepEvent) {
        val r = rhythm ?: return
        if (e.headsOnly != gridHeadsOnly) {
            gridHeadsOnly = e.headsOnly
            grid?.setup(r, if (e.headsOnly) r.headsOnlyPattern() else r.pattern)
        }
        grid?.current = if (e.countIn) -1 else e.patternStep
        if (e.isHead) {
            val beatMs = (60000.0 / e.bpm).toLong()
            pulse?.pulse(e.beatInBar, e.isDownbeat, e.countIn, beatMs)
            if (vibrateOn) {
                try {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(if (e.isDownbeat) 45L else 22L, if (e.isDownbeat) 255 else 140)
                    )
                } catch (_: Exception) {
                }
            }
        }
        bpmText?.text = "${e.bpm} BPM"
        stageText?.text = when {
            e.countIn -> "Contagem… prepare a mão"
            e.stageIndex >= 0 -> STAGES[e.stageIndex].text
            engine.rampOn -> "Acelerando aos poucos"
            else -> ""
        }
        chordText?.text = "Acorde: ${e.chord}    →  próximo: ${e.nextChord}"
    }
}
