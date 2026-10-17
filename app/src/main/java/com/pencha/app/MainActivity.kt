package com.pencha.app

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.time.LocalDate

class BarChartView(c: Context, private val hours: List<Float>, private val labels: List<String>) : View(c) {
    private val dp = resources.displayMetrics.density
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF666666.toInt(); textSize = 11 * dp }
    private val grid = Paint().apply { color = 0x22000000; strokeWidth = 1f }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(cv: Canvas) {
        val left = 34 * dp; val top = 20 * dp; val bottom = height - 24 * dp
        val w = width - left - 4 * dp
        val maxH = maxOf(5f, Math.ceil((hours.maxOrNull() ?: 0f).toDouble()).toFloat())
        val step = if (maxH <= 6f) 1 else 2
        var v = 0
        tp.textAlign = Paint.Align.RIGHT
        while (v <= maxH) {
            val y = bottom - (bottom - top) * v / maxH
            cv.drawLine(left, y, width.toFloat(), y, grid)
            cv.drawText("${v}h", left - 4 * dp, y + 4 * dp, tp)
            v += step
        }
        val n = hours.size
        val slot = w / n
        val bw = slot * 0.62f
        tp.textAlign = Paint.Align.CENTER
        for (i in 0 until n) {
            val x = left + slot * i + (slot - bw) / 2
            val y = bottom - (bottom - top) * hours[i] / maxH
            barPaint.color = when {
                hours[i] < 3f -> 0xFF4CAF50.toInt()
                hours[i] < 4f -> 0xFFFFB300.toInt()
                hours[i] < 5f -> 0xFFFB8C00.toInt()
                else -> 0xFFE53935.toInt()
            }
            cv.drawRoundRect(RectF(x, y, x + bw, bottom), 4 * dp, 4 * dp, barPaint)
            if (n <= 7) cv.drawText(String.format("%.1f", hours[i]), x + bw / 2, y - 4 * dp, tp)
            if (n <= 7 || i % 5 == 0 || i == n - 1) cv.drawText(labels[i], x + bw / 2, bottom + 16 * dp, tp)
        }
    }
}

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private var tab = 0
    private var range = 7
    private var pendingBday = ""
    private val bn = arrayOf("Shom", "Mongol", "Budh", "Brihosh", "Shukro", "Shoni", "Rob")

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        if (hasUsage()) {
            try { startForegroundService(Intent(this, TrackerService::class.java)) } catch (e: Exception) { }
        }
        build()
    }

    @Suppress("DEPRECATION")
    private fun hasUsage() = getSystemService(AppOpsManager::class.java)
        .checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED

    private fun txt(t: String, size: Float, center: Boolean = true) = TextView(this).apply {
        text = t; textSize = size; if (center) gravity = Gravity.CENTER
    }

    private fun btn(t: String, a: () -> Unit) = Button(this).apply {
        text = t; isAllCaps = false; setOnClickListener { a() }
    }

    private fun weight() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

    private fun build() {
        root.removeAllViews()
        val p = Prefs(this)
        p.rollDay()
        val bar = LinearLayout(this)
        listOf("Dashboard", "Profile", "Settings").forEachIndexed { i, t ->
            bar.addView(Button(this).apply {
                text = t; isAllCaps = false; isEnabled = tab != i
                setOnClickListener { tab = i; if (i == 1) pendingBday = p.birthday; build() }
            }, weight())
        }
        root.addView(bar)
        when (tab) { 0 -> dashboard(p); 1 -> profile(p); else -> settings(p) }
    }

    private fun dashboard(p: Prefs) {
        val min = (p.todayMs / 60_000).toInt()
        val level = minOf(5, min / 60)
        val bday = p.isBirthdayToday()
        root.addView(txt(if (p.name.isBlank()) "Hi!" else "Hi, ${p.name}!", 20f).apply { setPadding(0, 30, 0, 0) })
        if (bday) root.addView(txt("Aaj tomar jonmodin! Shubho jonmodin!", 16f))
        root.addView(ImageView(this).apply {
            setImageBitmap(PenchaArt.bitmap(if (bday) 4 else PenchaArt.moodFor(level), 460))
            layoutParams = LinearLayout.LayoutParams(460, 460).apply { gravity = Gravity.CENTER_HORIZONTAL }
        })
        root.addView(txt("Aaj: ${min / 60} ghonta ${min % 60} min", 22f))

        val row = LinearLayout(this)
        row.addView(btn("7 din") { range = 7; build() }.apply { isEnabled = range != 7 }, weight())
        row.addView(btn("30 din") { range = 30; build() }.apply { isEnabled = range != 30 }, weight())
        root.addView(row)

        val days = (range - 1 downTo 0).toList()
        val hours = days.map { p.dayMs(it) / 3_600_000f }
        val labels = days.map {
            val d = LocalDate.now().minusDays(it.toLong())
            if (range == 7) bn[d.dayOfWeek.value - 1] else d.dayOfMonth.toString()
        }
        root.addView(BarChartView(this, hours, labels).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (260 * resources.displayMetrics.density).toInt())
        })
        root.addView(txt("Gorod: ${String.format("%.1f", hours.average())} ghonta/din  |  Shobcheye beshi: ${String.format("%.1f", hours.max())} ghonta", 14f))
    }

    private fun profile(p: Prefs) {
        root.addView(txt("Tomar profile", 20f).apply { setPadding(0, 30, 0, 20) })
        val nameEt = EditText(this).apply { hint = "Tomar naam"; setText(p.name); setSingleLine() }
        root.addView(nameEt)
        root.addView(txt("Gender", 16f, false).apply { setPadding(0, 30, 0, 0) })
        val gs = listOf("Chhele", "Meye", "Bolbo na")
        val rg = RadioGroup(this)
        gs.forEachIndexed { i, g -> rg.addView(RadioButton(this).apply { text = g; id = 100 + i; isChecked = p.gender == g }) }
        root.addView(rg)
        root.addView(txt("Jonmodin", 16f, false).apply { setPadding(0, 30, 0, 0) })
        val dateBtn = Button(this).apply { isAllCaps = false; text = if (pendingBday.isEmpty()) "Jonmodin bachho" else pendingBday }
        dateBtn.setOnClickListener {
            val d = try { LocalDate.parse(pendingBday) } catch (e: Exception) { LocalDate.of(2000, 1, 1) }
            DatePickerDialog(this, { _, y, m, day ->
                pendingBday = LocalDate.of(y, m + 1, day).toString()
                dateBtn.text = pendingBday
            }, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }
        root.addView(dateBtn)
        root.addView(btn("Save koro") {
            p.name = nameEt.text.toString().trim()
            p.gender = gs.getOrNull(rg.checkedRadioButtonId - 100) ?: ""
            p.birthday = pendingBday
            Toast.makeText(this, "Save hoyeche!", Toast.LENGTH_SHORT).show()
        })
    }

    private fun settings(p: Prefs) {
        if (!hasUsage()) root.addView(btn("1. Usage Access on koro") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            root.addView(btn("2. Notification allow koro") {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            })
        }
        if (!getSystemService(android.os.PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) {
            root.addView(btn("3. Battery 'Unrestricted' koro (pop up jate bondho na hoy)") {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:$packageName")))
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            })
        }
        root.addView(btn("4. Pencha-ke chalu koro") {
            startForegroundService(Intent(this, TrackerService::class.java))
            Toast.makeText(this, "Pencha ekhon pahara dichhe!", Toast.LENGTH_SHORT).show()
        })
        root.addView(txt("Test koro", 16f).apply { setPadding(0, 30, 0, 0) })
        val row = LinearLayout(this)
        for (hr in 1..5) row.addView(btn("${hr}h") { Notifier.popup(this, hr * 60) }, weight())
        root.addView(row)
        root.addView(btn("Birthday wish test") { Notifier.birthday(this) })

        root.addView(txt("Gonona-r baire thakbe (tick koro):", 16f, false).apply { setPadding(0, 40, 0, 10) })
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(q, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .filter { it.first != packageName }
            .sortedBy { it.second.lowercase() }
            .forEach { (pkg, name) ->
                root.addView(CheckBox(this).apply {
                    text = name
                    isChecked = pkg in p.excluded
                    setOnCheckedChangeListener { _, on -> p.excluded = if (on) p.excluded + pkg else p.excluded - pkg }
                })
            }
    }
}
