package com.example.liftlogic.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt

/** Weight conversion and formatting. The database always stores kilograms. */
object Units {
    const val KG = "kg"
    const val LB = "lb"
    private const val LB_PER_KG = 2.2046226218

    private val symbols = DecimalFormatSymbols(Locale.US)
    private val numberFormat = DecimalFormat("#.##", symbols)
    private val volumeFormat = DecimalFormat("#,###", symbols)

    fun fromKg(kg: Double, unit: String): Double = if (unit == LB) kg * LB_PER_KG else kg

    fun toKg(value: Double, unit: String): Double = if (unit == LB) value / LB_PER_KG else value

    /** "62.5" / "60" — used for text fields and compact labels. */
    fun number(value: Double): String = numberFormat.format(value)

    /** Display value (in the user's unit) of a stored kg weight, rounded to 0.01. */
    fun display(kg: Double, unit: String): Double = (fromKg(kg, unit) * 100).roundToInt() / 100.0

    /** "62.5 kg" */
    fun weight(kg: Double, unit: String): String = "${number(display(kg, unit))} $unit"

    /** "12,350 kg" */
    fun volume(kg: Double, unit: String): String = "${volumeFormat.format(fromKg(kg, unit))} $unit"

    /** Parses user input such as "62.5" or "62,5". */
    fun parse(text: CharSequence?): Double? =
        text?.toString()?.trim()?.replace(',', '.')?.takeIf { it.isNotEmpty() }?.toDoubleOrNull()
}
