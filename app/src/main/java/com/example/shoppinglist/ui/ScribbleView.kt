package com.example.shoppinglist.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.shoppinglist.R
import kotlin.random.Random

/** Draws an irregular, hand-drawn looking line across the whole cell to cross a product out. */
class ScribbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * resources.displayMetrics.density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = ContextCompat.getColor(context, R.color.scribble)
    }
    private val path = Path()

    /** Same seed gives the same scribble, so a row keeps its drawing between redraws. */
    var seed: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val random = Random(seed)
        val margin = 6f * resources.displayMetrics.density
        val step = 18f * resources.displayMetrics.density
        val mid = h / 2f
        val amplitude = h * 0.32f

        path.reset()
        var x = margin
        var y = mid + (random.nextFloat() - 0.5f) * amplitude
        path.moveTo(x, y)
        while (x < w - margin) {
            val nextX = minOf(x + step * (0.7f + random.nextFloat() * 0.6f), w - margin)
            val nextY = mid + (random.nextFloat() - 0.5f) * 2f * amplitude
            val ctrlX = (x + nextX) / 2f
            val ctrlY = mid + (random.nextFloat() - 0.5f) * 2.4f * amplitude
            path.quadTo(ctrlX, ctrlY, nextX, nextY)
            x = nextX
            y = nextY
        }
        // A second, slightly offset stroke back across makes it look scribbled by hand.
        while (x > margin) {
            val nextX = maxOf(x - step * 1.6f * (0.7f + random.nextFloat() * 0.6f), margin)
            val nextY = mid + (random.nextFloat() - 0.5f) * 1.2f * amplitude
            path.quadTo((x + nextX) / 2f, y + (random.nextFloat() - 0.5f) * amplitude, nextX, nextY)
            x = nextX
            y = nextY
        }
        canvas.drawPath(path, paint)
    }
}
