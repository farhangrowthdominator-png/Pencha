package com.pencha.app

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import java.time.LocalDate
import java.time.ZoneId

object Notifier {
    fun ensure(c: Context) {
        val m = c.getSystemService(NotificationManager::class.java)
        m.createNotificationChannel(NotificationChannel("track", "Pencha chalu ache", NotificationManager.IMPORTANCE_LOW))
        m.createNotificationChannel(NotificationChannel("pop", "Pencha-r message", NotificationManager.IMPORTANCE_HIGH))
    }

    fun birthday(c: Context) {
        ensure(c)
        val msg = Messages.birthday(Prefs(c))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "pop")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(PenchaArt.bitmap(4, 256))
            .setContentTitle("Pencha-r shubhechha")
            .setContentText(msg)
            .setStyle(Notification.BigTextStyle().bigText(msg))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        c.getSystemService(NotificationManager::class.java).notify(3, n)
    }

    fun popup(c: Context, mark: Int) {
        ensure(c)
        val level = minOf(5, mark / 60)
        val msg = Messages.pick(level, Prefs(c))
        val time = if (mark % 60 == 0) "${mark / 60} ghonta" else "${mark / 60} ghonta 30 min"
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(c, "pop")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setLargeIcon(PenchaArt.bitmap(PenchaArt.moodFor(level), 256))
            .setContentTitle("Pencha bolche - $time")
            .setContentText(msg)
            .setStyle(Notification.BigTextStyle().bigText(msg))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        c.getSystemService(NotificationManager::class.java).notify(2, n)
    }
}

class TrackerService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private var lastEvt = System.currentTimeMillis() - 3_600_000L
    private var cur: String? = null

    private val tick = object : Runnable {
        override fun run() { step(); h.postDelayed(this, 30_000) }
    }

    override fun onCreate() {
        super.onCreate()
        Notifier.ensure(this)
        val n = Notification.Builder(this, "track")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Pencha pahara dichhe")
            .setContentText("Screen time gonona cholche")
            .build()
        if (Build.VERSION.SDK_INT >= 34)
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
        try { backfill() } catch (e: Exception) { }
        h.post(tick)
    }

    override fun onStartCommand(i: Intent?, f: Int, id: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
    override fun onDestroy() { h.removeCallbacks(tick); super.onDestroy() }

    @Suppress("DEPRECATION")
    private fun backfill() {
        val p = Prefs(this)
        p.rollDay()
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val ev = getSystemService(UsageStatsManager::class.java).queryEvents(start, now)
        val e = UsageEvents.Event()
        val skip = p.excluded
        var pkg: String? = null
        var since = 0L
        var total = 0L
        fun close(t: Long) {
            val c = pkg
            if (c != null && c != packageName && c !in skip) total += t - since
            pkg = null
        }
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> { close(e.timeStamp); pkg = e.packageName; since = e.timeStamp }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> if (e.packageName == pkg) close(e.timeStamp)
                16 -> close(e.timeStamp)
            }
        }
        if (getSystemService(PowerManager::class.java).isInteractive) close(now)
        if (total > p.todayMs) p.todayMs = total
        val min = (p.todayMs / 60_000).toInt()
        val mark = if (min < 300) min / 60 * 60 else 300 + (min - 300) / 30 * 30
        if (mark >= 60 && mark > p.lastMark) { p.lastMark = mark; Notifier.popup(this, mark) }
    }

    @Suppress("DEPRECATION")
    private fun step() {
        val p = Prefs(this)
        p.rollDay()
        val um = getSystemService(UsageStatsManager::class.java)
        val now = System.currentTimeMillis()
        val ev = um.queryEvents(lastEvt, now)
        val e = UsageEvents.Event()
        while (ev.hasNextEvent()) {
            ev.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) cur = e.packageName
            else if (e.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND && e.packageName == cur) cur = null
        }
        lastEvt = now
        val on = getSystemService(PowerManager::class.java).isInteractive
        val app = cur
        val today = LocalDate.now().toString()
        if (on && p.isBirthdayToday() && p.wishedDay != today) { p.wishedDay = today; Notifier.birthday(this) }
        if (on && app != null && app != packageName && app !in p.excluded) {
            p.todayMs += 30_000
            val min = (p.todayMs / 60_000).toInt()
            val mark = if (min < 300) min / 60 * 60 else 300 + (min - 300) / 30 * 30
            if (mark >= 60 && mark > p.lastMark) {
                p.lastMark = mark
                Notifier.popup(this, mark)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.startForegroundService(Intent(c, TrackerService::class.java))
    }
}
