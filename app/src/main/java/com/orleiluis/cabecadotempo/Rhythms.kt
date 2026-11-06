package com.orleiluis.cabecadotempo

/**
 * Acorde em afinação padrão. strings[0] = 6ª corda (Mi grave) ... strings[5] = 1ª corda (Mi agudo).
 * Valor = nota MIDI, -1 = corda não tocada.
 */
class Chord(
    val name: String,
    val strings: IntArray,
    val bassString: Int,
    val altString: Int,
    val altMidi: Int
)

val CHORDS: Map<String, Chord> = listOf(
    Chord("E", intArrayOf(40, 47, 52, 56, 59, 64), 0, 1, 47),
    Chord("A", intArrayOf(-1, 45, 52, 57, 61, 64), 1, 0, 40),
    Chord("D", intArrayOf(-1, -1, 50, 57, 62, 66), 2, 1, 45),
    Chord("G", intArrayOf(43, 47, 50, 55, 59, 67), 0, 2, 50),
    Chord("C", intArrayOf(-1, 48, 52, 55, 60, 64), 1, 0, 43),
    Chord("F", intArrayOf(41, 48, 53, 57, 60, 65), 0, 1, 48),
    Chord("Am", intArrayOf(-1, 45, 52, 57, 60, 64), 1, 0, 40),
    Chord("Em", intArrayOf(40, 47, 52, 55, 59, 64), 0, 1, 47),
    Chord("Dm", intArrayOf(-1, -1, 50, 57, 62, 65), 2, 1, 45),
    Chord("Bm", intArrayOf(-1, 47, 54, 59, 62, 66), 1, 0, 42),
    Chord("E7", intArrayOf(40, 47, 50, 56, 59, 64), 0, 1, 47),
    Chord("A7", intArrayOf(-1, 45, 52, 55, 61, 64), 1, 0, 40),
    Chord("D7", intArrayOf(-1, -1, 50, 57, 60, 66), 2, 1, 45),
    Chord("G7", intArrayOf(43, 47, 50, 55, 59, 65), 0, 2, 50),
    Chord("B7", intArrayOf(-1, 47, 51, 57, 59, 66), 1, 0, 42),
    Chord("Am7", intArrayOf(-1, 45, 52, 55, 60, 64), 1, 0, 40),
    Chord("Dm7", intArrayOf(-1, -1, 50, 57, 60, 65), 2, 1, 45),
    Chord("Em7", intArrayOf(40, 47, 50, 55, 59, 64), 0, 1, 47),
    Chord("Cmaj7", intArrayOf(-1, 48, 52, 55, 59, 64), 1, 0, 43)
).associateBy { it.name }

/**
 * Símbolos do padrão (um por subdivisão):
 *  D = palhetada pra baixo   U = palhetada pra cima
 *  X = abafado pra baixo     x = abafado pra cima
 *  S = acorde seco (toca e abafa)
 *  B = baixo (polegar na tônica)   b = baixo alternado
 *  C = dedos (acorde pinçado)      P = baixo + dedos   Q = baixo alternado + dedos
 *  - = mão passa sem tocar
 */
class Rhythm(
    val id: String,
    val name: String,
    val category: String,
    val timeSig: String,
    val beatsPerBar: Int,
    val stepsPerBeat: Int,
    val pattern: String,
    val bpm: Int,
    val bpmMin: Int,
    val bpmMax: Int,
    val chords: List<String>,
    val chordEveryBars: Int,
    val description: String,
    val tip: String
) {
    val stepsPerBar: Int get() = beatsPerBar * stepsPerBeat
    val bars: Int get() = pattern.length / stepsPerBar

    /** Versão de treino: ↓ só na cabeça de cada tempo. */
    fun headsOnlyPattern(): String {
        val sb = StringBuilder()
        for (i in pattern.indices) sb.append(if (i % stepsPerBeat == 0) 'D' else '-')
        return sb.toString()
    }
}

val CATEGORIES = listOf(
    "Fundamentos (comece aqui)",
    "Pop e Rock",
    "Groove",
    "Brasileiros",
    "Ternários e compostos"
)

val RHYTHMS: List<Rhythm> = listOf(
    // ---------- Fundamentos ----------
    Rhythm(
        "fund-seminima", "Semínimas — só a cabeça", CATEGORIES[0], "4/4", 4, 4,
        "D---D---D---D---", 70, 40, 140, listOf("G", "C", "D", "G"), 1,
        "Uma palhetada pra baixo em cada tempo. É o exercício mais importante: sua mão bate exatamente junto com o clique.",
        "Cada ↓ É a cabeça do tempo. Conte em voz alta: UM, dois, três, quatro — o UM é o mais forte."
    ),
    Rhythm(
        "fund-colcheia", "Colcheias ↓↑", CATEGORIES[0], "4/4", 4, 4,
        "D-U-D-U-D-U-D-U-", 70, 40, 140, listOf("G", "C", "D", "G"), 1,
        "Duas palhetadas por tempo: ↓ na cabeça, ↑ no contratempo (o \"e\").",
        "A mão desce SEMPRE na cabeça. Conte: 1 e 2 e 3 e 4 e — o número é o ↓."
    ),
    Rhythm(
        "fund-semicolcheia", "Semicolcheias ↓↑↓↑", CATEGORIES[0], "4/4", 4, 4,
        "DUDUDUDUDUDUDUDU", 60, 40, 120, listOf("Am", "F", "C", "G"), 1,
        "Quatro palhetadas por tempo. A mão vira um pêndulo que nunca para.",
        "De cada 4 palhetadas, só a primeira (↓) cai na cabeça. Acentue ela um pouco mais forte."
    ),
    Rhythm(
        "fund-contratempo", "Contratempo (só o \"e\")", CATEGORIES[0], "4/4", 4, 4,
        "--U---U---U---U-", 70, 40, 130, listOf("G", "C", "D", "G"), 1,
        "Você toca só ENTRE os tempos. O clique marca a cabeça, a guitarra responde no meio.",
        "Mova a mão pra baixo no vazio junto com o clique (sem tocar) e acerte a corda na subida."
    ),

    // ---------- Pop e Rock ----------
    Rhythm(
        "rock-basico", "Rock básico (tudo pra baixo)", CATEGORIES[1], "4/4", 4, 4,
        "D-D-D-D-D-D-D-D-", 110, 70, 170, listOf("A", "D", "E", "A"), 1,
        "Colcheias só pra baixo, firme, o motor do rock.",
        "Acentue as palhetadas que caem nos números (1, 2, 3, 4); as do meio vêm mais leves."
    ),
    Rhythm(
        "pop-folk", "Pop/Folk (↓ ↓↑ ↑↓↑)", CATEGORIES[1], "4/4", 4, 4,
        "D---D-U---U-D-U-", 90, 60, 140, listOf("G", "D", "Em", "C"), 1,
        "O ritmo mais usado em música pop e folk ao violão.",
        "No tempo 3 a mão desce SEM tocar — é aí que muita gente perde a cabeça do tempo."
    ),
    Rhythm(
        "balada", "Balada", CATEGORIES[1], "4/4", 4, 4,
        "D-----U-D-U-D-U-", 72, 50, 110, listOf("C", "G", "Am", "F"), 1,
        "Um ↓ longo no 1 que deixa soar, e o resto em colcheias.",
        "Depois do primeiro ↓, a mão continua balançando no ar no tempo 2 — não pare o braço."
    ),
    Rhythm(
        "galope", "Galope (rock pesado)", CATEGORIES[1], "4/4", 4, 4,
        "D-DUD-DUD-DUD-DU", 100, 60, 170, listOf("Em", "C", "D", "Em"), 1,
        "Colcheia + duas semicolcheias: o \"pa-ca-tá\" do metal e do rock.",
        "O primeiro ↓ de cada grupo é a cabeça. Pense \"UM-pa-ca, DOIS-pa-ca\"."
    ),
    Rhythm(
        "pop-16", "Pop dançante (semicolcheias)", CATEGORIES[1], "4/4", 4, 4,
        "D-DU-UDUD-DU-UDU", 96, 60, 130, listOf("Am", "F", "C", "G"), 1,
        "Padrão sincopado em semicolcheias, comum em pop atual.",
        "A mão faz ↓↑↓↑ o tempo todo; você só escolhe quando encosta nas cordas."
    ),
    Rhythm(
        "country", "Country (boom-chick)", CATEGORIES[1], "4/4", 4, 4,
        "B---D-U-b---D-U-", 100, 70, 160, listOf("G", "C", "G", "D7"), 1,
        "Baixo no 1, acorde no 2, baixo alternado no 3, acorde no 4.",
        "O baixo (B) marca as cabeças 1 e 3; o acorde marca 2 e 4."
    ),
    Rhythm(
        "blues-shuffle", "Blues shuffle", CATEGORIES[1], "4/4 swing", 4, 3,
        "D-UD-UD-UD-U", 80, 55, 140,
        listOf("A7", "A7", "A7", "A7", "D7", "D7", "A7", "A7", "E7", "D7", "A7", "E7"), 1,
        "Cada tempo é dividido em 3 (tercina); toca-se a 1ª e a 3ª: \"TUM-ca TUM-ca\". Blues de 12 compassos.",
        "O ↓ longo é a cabeça; o ↑ curtinho vem logo antes do próximo tempo."
    ),

    // ---------- Groove ----------
    Rhythm(
        "reggae", "Reggae", CATEGORIES[2], "4/4", 4, 4,
        "----S-------S---", 75, 60, 100, listOf("G", "C", "G", "D"), 1,
        "Acorde seco e curto só nos tempos 2 e 4.",
        "A guitarra fica calada no 1 — conte o 1 por dentro (o clique forte) e ataque no 2."
    ),
    Rhythm(
        "ska", "Ska", CATEGORIES[2], "4/4", 4, 4,
        "--S---S---S---S-", 140, 100, 190, listOf("C", "Am", "F", "G"), 1,
        "Acorde seco em todos os contratempos.",
        "Bata o pé na cabeça; a guitarra entra quando o pé sobe."
    ),
    Rhythm(
        "funk", "Funk (com abafados)", CATEGORIES[2], "4/4", 4, 4,
        "D-XxD-XUDxXUX-DU", 95, 70, 120, listOf("Em7", "A7"), 2,
        "Semicolcheias com cordas abafadas (✕) fazendo a percussão.",
        "Mesmo nos abafados a mão continua ↓↑↓↑; a cabeça é sempre um movimento pra baixo."
    ),

    // ---------- Brasileiros ----------
    Rhythm(
        "bossa", "Bossa nova", CATEGORIES[3], "2/4", 2, 4,
        "P--Cb-C-B-C-Q---", 70, 50, 100, listOf("Am7", "Dm7", "G7", "Cmaj7"), 2,
        "Polegar marca as cabeças (tônica e baixo alternado); os dedos pinçam o acorde nas síncopes.",
        "O polegar NUNCA sai das cabeças: ele é o seu metrônomo. Os dedos é que balançam."
    ),
    Rhythm(
        "samba", "Samba", CATEGORIES[3], "2/4", 2, 4,
        "P-CCbC-CB-CCbC-C", 90, 70, 130, listOf("C", "A7", "Dm", "G7"), 2,
        "Baixo em todas as cabeças e o acorde \"batucando\" nas semicolcheias.",
        "Pense no surdo: o baixo é o surdo marcando a cabeça de cada tempo."
    ),
    Rhythm(
        "pagode", "Pagode", CATEGORIES[3], "2/4", 2, 4,
        "P-C-bCC-B-C-bC-C", 85, 65, 115, listOf("Dm", "G7", "C", "A7"), 2,
        "Variação do samba com mais espaço, típica do pagode de roda.",
        "Baixo na cabeça, dedos respondendo. Se perder, toque só os baixos até voltar."
    ),
    Rhythm(
        "baiao", "Baião", CATEGORIES[3], "2/4", 2, 4,
        "B--bC-C-B--bC-C-", 90, 70, 130, listOf("G", "C", "D7", "G"), 2,
        "O baixo faz a célula do zabumba: \"TUM — tum TUM\".",
        "O primeiro baixo é a cabeça do 1; o segundo vem ANTES do 2 (síncope) e o acorde cai na cabeça do 2."
    ),
    Rhythm(
        "xote", "Xote", CATEGORIES[3], "4/4", 4, 4,
        "B--DD-U-b--DD-U-", 80, 60, 110, listOf("C", "G7", "G7", "C"), 1,
        "Baixo, acorde arrastado, baixo, acorde — o balanço do forró pé-de-serra.",
        "Os baixos ficam nas cabeças 1 e 3; o acorde \"arrasta\" um pouquinho antes do 2 e do 4."
    ),
    Rhythm(
        "arrasta-pe", "Arrasta-pé", CATEGORIES[3], "2/4", 2, 4,
        "B-D-b-D-", 120, 90, 160, listOf("D", "A7"), 2,
        "Forró rápido: baixo na cabeça, acorde no contratempo.",
        "Baixo = cabeça, acorde = meio. Simples, mas tem que ficar firme em velocidade."
    ),
    Rhythm(
        "marchinha", "Marchinha", CATEGORIES[3], "2/4", 2, 2,
        "BDbD", 110, 80, 150, listOf("C", "G7", "G7", "C"), 1,
        "Ritmo de carnaval: baixo e acorde alternando.",
        "Baixo no 1 e no 2 (cabeças), acorde no meio: \"tum-PÁ tum-PÁ\"."
    ),
    Rhythm(
        "vanerao", "Vanerão", CATEGORIES[3], "2/4", 2, 4,
        "B-DUb-DU", 110, 80, 150, listOf("G", "D7"), 2,
        "Ritmo gaúcho, rápido e saltitante.",
        "Baixo cravado em cada cabeça, ↓↑ do acorde logo depois."
    ),
    Rhythm(
        "sertanejo", "Sertanejo", CATEGORIES[3], "4/4", 4, 4,
        "B-DUD-DUb-DUD-DU", 90, 65, 130, listOf("G", "D", "Em", "C"), 1,
        "Batida do sertanejo atual: baixo + palhetada cheia.",
        "Baixo nas cabeças 1 e 3; nas cabeças 2 e 4 entra um ↓ do acorde."
    ),
    Rhythm(
        "arrocha", "Arrocha", CATEGORIES[3], "4/4", 4, 4,
        "B---D-U-b-D-D-U-", 70, 55, 95, listOf("Am", "F", "C", "G"), 1,
        "Lento e romântico, com baixo marcado.",
        "Deixe o baixo do 1 soar inteiro; ele é a âncora do compasso."
    ),

    // ---------- Ternários e compostos ----------
    Rhythm(
        "valsa", "Valsa", CATEGORIES[4], "3/4", 3, 4,
        "B---D---D---", 100, 70, 160, listOf("C", "C", "G7", "G7", "G7", "G7", "C", "C"), 1,
        "Três tempos: baixo forte no 1, acorde no 2 e no 3.",
        "Conte UM-dois-três. Só o 1 tem baixo — é ele que você sente como cabeça do compasso."
    ),
    Rhythm(
        "guarania", "Guarânia / Rasqueado", CATEGORIES[4], "3/4", 3, 2,
        "B-DUDU", 70, 55, 110, listOf("D", "A7", "A7", "D"), 1,
        "Ritmo ternário da música caipira e paraguaia.",
        "Baixo só no 1; as cabeças 2 e 3 são as palhetadas ↓."
    ),
    Rhythm(
        "seis-oito", "Balada 6/8", CATEGORIES[4], "6/8", 2, 3,
        "D-UDDU", 60, 45, 90, listOf("C", "Am", "F", "G"), 1,
        "Dois tempos por compasso, cada um dividido em três. Clássico das baladas de rock.",
        "Conte UM-dois-três DOIS-dois-três; as cabeças são o 1 e o 4 das colcheias."
    )
)
