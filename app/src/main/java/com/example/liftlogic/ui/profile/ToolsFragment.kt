package com.example.liftlogic.ui.profile

import android.os.Bundle
import android.view.View
import androidx.core.widget.doAfterTextChanged
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentToolsBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.util.OneRepMax
import com.example.liftlogic.util.PlateCalculator
import com.example.liftlogic.util.Units

/** One-rep-max estimator and barbell plate calculator. Works in the user's unit. */
class ToolsFragment : BaseFragment<FragmentToolsBinding>(FragmentToolsBinding::inflate) {

    private val unit: String get() = app.unit
    private val standardBar: Double get() = if (unit == Units.LB) 45.0 else 20.0
    private val lightBar: Double get() = if (unit == Units.LB) 35.0 else 15.0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.title_tools)

        binding.ormWeightLayout.hint = getString(R.string.format_weight_hint, unit)
        binding.plateTargetLayout.hint = getString(R.string.format_target_weight_hint, unit)
        binding.chipBarStandard.text = "${Units.number(standardBar)} $unit"
        binding.chipBarLight.text = "${Units.number(lightBar)} $unit"

        binding.ormWeightInput.doAfterTextChanged { updateOneRepMax() }
        binding.ormRepsInput.doAfterTextChanged { updateOneRepMax() }
        binding.plateTargetInput.doAfterTextChanged { updatePlates() }
        binding.barGroup.setOnCheckedStateChangeListener { _, _ -> updatePlates() }

        updateOneRepMax()
        updatePlates()
    }

    private fun updateOneRepMax() {
        val b = bindingOrNull ?: return
        val weight = Units.parse(b.ormWeightInput.text)
        val reps = b.ormRepsInput.text?.toString()?.toIntOrNull()
        if (weight == null || reps == null || reps <= 0 || weight <= 0) {
            b.ormResult.setText(R.string.text_1rm_prompt)
            b.ormTable.text = ""
            return
        }
        val max = OneRepMax.estimate(weight, reps)
        b.ormResult.text = getString(R.string.format_estimated_1rm, "${Units.number(max)} $unit")
        b.ormTable.text = OneRepMax.percentages.joinToString("\n") { (percent, repRange) ->
            val w = max * percent / 100.0
            "%-5s %-10s %s".format("$percent%", "${Units.number(w)} $unit", getString(R.string.format_approx_reps, repRange))
        }
    }

    private fun updatePlates() {
        val b = bindingOrNull ?: return
        val target = Units.parse(b.plateTargetInput.text)
        if (target == null || target <= 0) {
            b.plateResult.setText(R.string.text_plate_prompt)
            return
        }
        val bar = when (b.barGroup.checkedChipId) {
            R.id.chip_bar_light -> lightBar
            R.id.chip_bar_none -> 0.0
            else -> standardBar
        }
        val result = PlateCalculator.calculate(target, bar, unit)
        b.plateResult.text = when {
            result == null -> getString(R.string.error_target_below_bar)
            result.perSide.isEmpty() -> getString(R.string.text_just_the_bar)
            else -> {
                val plates = result.perSide.joinToString(", ") { (plate, count) -> "$count × ${Units.number(plate)}" }
                val text = getString(R.string.format_plates_per_side, plates, unit)
                if (result.remainder > 0.01) text + "\n" + getString(R.string.format_plate_remainder, "${Units.number(result.remainder)} $unit") else text
            }
        }
    }
}
