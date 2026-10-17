package com.pencha.app

import android.graphics.*

object PenchaArt {
    fun moodFor(level: Int) = when { level <= 2 -> 0; level == 3 -> 1; level == 4 -> 2; else -> 3 }

    fun bitmap(look: Int, px: Int): Bitmap {
        val mood = if (look == 4) 0 else look
        val bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.scale(px / 200f, px / 200f)
        val f = Paint(Paint.ANTI_ALIAS_FLAG)
        val s = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = 0xFF7A4412.toInt(); strokeCap = Paint.Cap.ROUND
        }
        val bag = Paint(s).apply { color = 0xFF8A5BA8.toInt() }
        val body = intArrayOf(0xFFF2A93B.toInt(), 0xFFF2A93B.toInt(), 0xFFE3B15F.toInt(), 0xFFA9CC84.toInt())[mood]
        val belly = intArrayOf(0xFFFFE3AE.toInt(), 0xFFFFE3AE.toInt(), 0xFFF3E2BC.toInt(), 0xFFE4EFCF.toInt())[mood]
        val wing = Color.rgb((Color.red(body) * 0.88).toInt(), (Color.green(body) * 0.88).toInt(), (Color.blue(body) * 0.88).toInt())
        val dark = 0xFF2B1B0E.toInt()
        val orange = 0xFFFF7A45.toInt()

        fun oval(cx: Float, cy: Float, rx: Float, ry: Float, col: Int, stroke: Boolean = true) {
            f.color = col
            val r = RectF(cx - rx, cy - ry, cx + rx, cy + ry)
            c.drawOval(r, f)
            if (stroke) c.drawOval(r, s)
        }
        fun tri(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, col: Int) {
            val p = Path().apply { moveTo(x1, y1); lineTo(x2, y2); lineTo(x3, y3); close() }
            f.color = col; c.drawPath(p, f); c.drawPath(p, s)
        }

        oval(82f, 188f, 14f, 7f, orange); oval(118f, 188f, 14f, 7f, orange)
        tri(48f, 52f, 42f, 14f, 82f, 38f, body); tri(152f, 52f, 158f, 14f, 118f, 38f, body)
        oval(100f, 110f, 70f, 76f, body)
        oval(100f, 140f, 44f, 44f, belly, false)
        oval(32f, 125f, 14f, 40f, wing); oval(168f, 125f, 14f, 40f, wing)

        for (x in floatArrayOf(78f, 122f)) {
            oval(x, 90f, 22f, 22f, if (mood == 1) 0xFFFFC9C9.toInt() else Color.WHITE)
            when (mood) {
                0 -> { oval(x + 2, 92f, 11f, 11f, dark, false); oval(x + 6, 88f, 4f, 4f, Color.WHITE, false) }
                1 -> { oval(x, 92f, 14f, 14f, 0xFFE5262B.toInt(), false); oval(x, 92f, 6f, 6f, dark, false) }
                2 -> {
                    oval(x + 1, 102f, 8f, 8f, dark, false)
                    f.color = body
                    val r = RectF(x - 22, 68f, x + 22, 112f)
                    c.drawArc(r, 180f, 180f, true, f); c.drawArc(r, 180f, 180f, false, s)
                    c.drawArc(RectF(x - 20, 106f, x + 20, 126f), 20f, 140f, false, bag)
                }
                else -> { c.drawLine(x - 10, 80f, x + 10, 100f, s); c.drawLine(x + 10, 80f, x - 10, 100f, s) }
            }
        }
        if (mood == 1) { c.drawLine(58f, 64f, 90f, 74f, s); c.drawLine(142f, 64f, 110f, 74f, s) }
        oval(52f, 116f, 10f, 6f, if (mood == 3) 0xFFC9E0A8.toInt() else 0xFFFF8FA3.toInt(), false)
        oval(148f, 116f, 10f, 6f, if (mood == 3) 0xFFC9E0A8.toInt() else 0xFFFF8FA3.toInt(), false)
        tri(93f, 108f, 107f, 108f, 100f, 121f, orange)
        when (mood) {
            0 -> c.drawArc(RectF(82f, 118f, 118f, 142f), 20f, 140f, false, s)
            1 -> c.drawArc(RectF(84f, 130f, 116f, 150f), 200f, 140f, false, s)
            2 -> oval(100f, 140f, 7f, 9f, 0xFF7A4412.toInt(), false)
            else -> oval(152f, 50f, 6f, 8f, 0xFF8AD3F0.toInt())
        }
        if (look == 4) {
            tri(82f, 46f, 118f, 46f, 100f, 4f, 0xFFFF6B9D.toInt())
            oval(100f, 5f, 5f, 5f, 0xFFFFD25E.toInt())
            oval(20f, 30f, 4f, 4f, 0xFF8AD3F0.toInt(), false); oval(180f, 40f, 4f, 4f, 0xFFFF8FA3.toInt(), false)
            oval(25f, 170f, 4f, 4f, 0xFFFFD25E.toInt(), false); oval(178f, 160f, 4f, 4f, 0xFF4CAF50.toInt(), false)
        }
        return bmp
    }
}
