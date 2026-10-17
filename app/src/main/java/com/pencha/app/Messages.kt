package com.pencha.app

object Messages {
    private val all = mapOf(
        1 to listOf(
            "Hey, ekhon ektu boi poro!",
            "Ek ghonta hoye gelo, ektu chokh jirie nao",
            "Pani kheyecho? Phone rekhe ek glass pani khao",
            "Ei je bondhu, ektu janla diye bahire tako to",
            "Pencha bolche: ektu break nile kono khoti nei!"
        ),
        2 to listOf(
            "Duo ghonta! Ebar ektu hat-pa chhoriye nao",
            "Uff, ar koto mobile chalabe!",
            "Ami ekhono hashchhi, kintu beshi dir hoy na",
            "Ek chhoto walk diye ashle kemon hoy?",
            "Boi-ta tomar jonno oppekkha korche go"
        ),
        3 to listOf(
            "Chokh ta ki nosto korbe naki? Dekho, amar chokh lal hoye gelo!",
            "Dohai tomar, ebar thamo!",
            "Tin ghonta! Amar chokh jalche, tomar-o jalche",
            "Ei dekho, ghuriye dekhao je tumi phone chhara thakte paro",
            "Pencha ekhon rege jachhe, kintu mone mone tomake bhalobashe"
        ),
        4 to listOf(
            "Ami klanto... tumio kintu klanto. Ghum pachhe na?",
            "Ar pari na go, phone ta rakho na please",
            "Char ghonta! Matha byatha korche na?",
            "Ektu ghumiye nao, phone kothao palabe na",
            "Pencha-r pakha byatha korche, tomar chokh-o"
        ),
        5 to listOf(
            "Ami oshustho hoye porechi, tomar jonnoi!",
            "Dohai tmr, ebar to thamo!",
            "Pnach ghonta! Ebar doctor dakte hobe naki?",
            "Phone rakho, tahole ami bhalo hobo",
            "Pencha-r jor eshe gechhe... ebar to rakho, please!"
        )
    )

    fun birthday(p: Prefs): String {
        val n = p.name.ifBlank { "bondhu" }
        val age = p.age()
        val l = mutableListOf(
            "Shubho jonmodin, $n! Aaj Pencha-r pokkho theke ek rashi bhalobasha",
            "Happy birthday $n! Aaj phone-er cheye keke-r dike beshi mon dao",
            "Pencha tomar jonno party hat porechhe! Shubho jonmodin $n!"
        )
        if (age > 0) l.add("Shubho jonmodin $n! Aaj tomar $age tomo jonmodin, party hobe na?")
        return l.random()
    }

    fun pick(level: Int, p: Prefs): String {
        val l = all.getValue(level.coerceIn(1, 5))
        val last = p.lastIdx(level)
        var i: Int
        do { i = l.indices.random() } while (i == last && l.size > 1)
        p.setLastIdx(level, i)
        val n = p.name
        return if (n.isNotBlank() && Math.random() < 0.4) "$n! ${l[i]}" else l[i]
    }
}
