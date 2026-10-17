package com.pencha.app

import android.content.Context
import java.time.LocalDate
import java.time.Period

class Prefs(c: Context) {
    private val sp = c.getSharedPreferences("pencha", Context.MODE_PRIVATE)

    var excluded: Set<String>
        get() = sp.getStringSet("ex", null)?.toSet() ?: setOf("com.fiverr.fiverr")
        set(v) { sp.edit().putStringSet("ex", HashSet(v)).apply() }

    fun dayMs(daysAgo: Int): Long = sp.getLong("d_${LocalDate.now().minusDays(daysAgo.toLong())}", 0L)

    var todayMs: Long
        get() = dayMs(0)
        set(v) { sp.edit().putLong("d_${LocalDate.now()}", v).apply() }

    var lastMark: Int
        get() = sp.getInt("mark", 0)
        set(v) { sp.edit().putInt("mark", v).apply() }

    var name: String
        get() = sp.getString("name", "") ?: ""
        set(v) { sp.edit().putString("name", v).apply() }

    var gender: String
        get() = sp.getString("gender", "") ?: ""
        set(v) { sp.edit().putString("gender", v).apply() }

    var birthday: String
        get() = sp.getString("bday", "") ?: ""
        set(v) { sp.edit().putString("bday", v).apply() }

    var wishedDay: String
        get() = sp.getString("wished", "") ?: ""
        set(v) { sp.edit().putString("wished", v).apply() }

    fun rollDay() {
        val d = LocalDate.now().toString()
        if (sp.getString("day", "") == d) return
        val old = LocalDate.now().minusDays(120).toString()
        val e = sp.edit().putString("day", d).putInt("mark", 0)
        sp.all.keys.filter { it.startsWith("d_") && it.substring(2) < old }.forEach { e.remove(it) }
        e.apply()
    }

    private fun birthdayDate(): LocalDate? = try { LocalDate.parse(birthday) } catch (e: Exception) { null }

    fun age(): Int = birthdayDate()?.let { Period.between(it, LocalDate.now()).years } ?: 0

    fun isBirthdayToday(): Boolean {
        val b = birthdayDate() ?: return false
        val t = LocalDate.now()
        return if (b.monthValue == 2 && b.dayOfMonth == 29 && !t.isLeapYear)
            t.monthValue == 2 && t.dayOfMonth == 28
        else b.monthValue == t.monthValue && b.dayOfMonth == t.dayOfMonth
    }

    fun lastIdx(level: Int) = sp.getInt("li$level", -1)
    fun setLastIdx(level: Int, i: Int) = sp.edit().putInt("li$level", i).apply()
}
