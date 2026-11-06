package com.example.liftlogic.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

/**
 * Lightweight line chart drawn on a Canvas (no chart library needed).
 * Shows the min/max value on the left and the first/last label along the bottom.
 */
class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var values: List<Float> = emptyList()
    private var labels: List<String> = emptyList()
    private var valueFormatter: (Float) -> String = { it.toInt().toString() }

    private val density = resources.displayMetrics.density
    private val primary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
    private val onSurfaceVariant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)
    private val outline = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline)

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = primary
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ColorUtils.setAlphaComponent(primary, 40)
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = primary
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
        color = outline
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = onSurfaceVariant
        textSize = 11f * resources.displayMetrics.scaledDensity
    }

    fun setData(values: List<Float>, labels: List<String> = emptyList(), formatter: ((Float) -> String)? = null) {
        this.values = values
        this.labels = labels
        formatter?.let { valueFormatter = it }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.size < 2) return

        val textHeight = textPaint.textSize
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val axisLabelWidth = maxOf(textPaint.measureText(valueFormatter(maxV)), textPaint.measureText(valueFormatter(minV)))

        val left = paddingLeft + axisLabelWidth + 8 * density
        val top = paddingTop + textHeight / 2
        val right = (width - paddingRight).toFloat() - 4 * density
        val bottom = height - paddingBottom - textHeight - 8 * density
        val chartW = right - left
        val chartH = bottom - top

        // Grid + axis labels
        canvas.drawLine(left, top, right, top, gridPaint)
        canvas.drawLine(left, bottom, right, bottom, gridPaint)
        canvas.drawText(valueFormatter(maxV), paddingLeft.toFloat(), top + textHeight / 3, textPaint)
        canvas.drawText(valueFormatter(minV), paddingLeft.toFloat(), bottom + textHeight / 3, textPaint)

        val stepX = chartW / (values.size - 1)
        val xs = FloatArray(values.size) { left + it * stepX }
        val ys = FloatArray(values.size) { top + chartH - ((values[it] - minV) / range) * chartH }

        val line = Path().apply {
            moveTo(xs[0], ys[0])
            for (i in 1 until values.size) lineTo(xs[i], ys[i])
        }
        val fill = Path(line).apply {
            lineTo(xs.last(), bottom)
            lineTo(xs.first(), bottom)
            close()
        }
        canvas.drawPath(fill, fillPaint)
        canvas.drawPath(line, linePaint)
        val radius = if (values.size > 20) 2f * density else 3.5f * density
        for (i in values.indices) canvas.drawCircle(xs[i], ys[i], radius, pointPaint)

        if (labels.size == values.size) {
            val labelY = height - paddingBottom.toFloat() - 2 * density
            canvas.drawText(labels.first(), left, labelY, textPaint)
            val last = labels.last()
            canvas.drawText(last, right - textPaint.measureText(last), labelY, textPaint)
        }
    }
}
