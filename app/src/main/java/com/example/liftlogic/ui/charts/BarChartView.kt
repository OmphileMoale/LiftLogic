package com.example.liftlogic.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

/** Simple bar chart drawn on a Canvas. The last bar (current period) is highlighted. */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()

    private val density = resources.displayMetrics.density
    private val primary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ColorUtils.setAlphaComponent(primary, 110) }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = primary }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = MaterialColors.getColor(this@BarChartView, com.google.android.material.R.attr.colorOnSurfaceVariant)
        textSize = 10f * resources.displayMetrics.scaledDensity
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    fun setData(values: List<Float>, labels: List<String>) {
        this.values = values
        this.labels = labels
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) return

        val labelSpace = textPaint.textSize + 6 * density
        val left = paddingLeft.toFloat()
        val top = paddingTop.toFloat()
        val chartW = (width - paddingLeft - paddingRight).toFloat()
        val chartH = height - paddingTop - paddingBottom - labelSpace
        val max = values.max().takeIf { it > 0f } ?: 1f

        val slot = chartW / values.size
        val barW = slot * 0.6f
        val radius = 4 * density

        values.forEachIndexed { i, v ->
            val cx = left + slot * i + slot / 2
            val h = (v / max) * chartH
            rect.set(cx - barW / 2, top + chartH - h.coerceAtLeast(if (v > 0) 2 * density else 0f), cx + barW / 2, top + chartH)
            canvas.drawRoundRect(rect, radius, radius, if (i == values.lastIndex) highlightPaint else barPaint)
            labels.getOrNull(i)?.let { canvas.drawText(it, cx, height - paddingBottom.toFloat() - 2 * density, textPaint) }
        }
    }
}
