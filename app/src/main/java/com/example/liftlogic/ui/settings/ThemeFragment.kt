package com.example.liftlogic.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentThemeBinding
import com.example.liftlogic.util.AppPrefs

class ThemeFragment : Fragment() {

    private var _binding: FragmentThemeBinding? = null
    private val binding get() = _binding!!

    /** Radio button id -> accent key stored in AppPrefs */
    private val accents = mapOf(
        R.id.accent_green to AppPrefs.ACCENT_GREEN,
        R.id.accent_blue to AppPrefs.ACCENT_BLUE,
        R.id.accent_purple to AppPrefs.ACCENT_PURPLE,
        R.id.accent_orange to AppPrefs.ACCENT_ORANGE,
        R.id.accent_red to AppPrefs.ACCENT_RED
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBar.toolbar.setTitle(R.string.title_app_theme)
        binding.appBar.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        if (savedInstanceState == null) {
            val current = AppPrefs.getAccent(requireContext())
            val checkedId = accents.entries.firstOrNull { it.value == current }?.key ?: R.id.accent_green
            binding.accentGroup.check(checkedId)
        }

        binding.applyButton.setOnClickListener {
            val accent = accents[binding.accentGroup.checkedRadioButtonId] ?: AppPrefs.ACCENT_GREEN
            val changed = accent != AppPrefs.getAccent(requireContext())
            AppPrefs.setAccent(requireContext(), accent)
            val activity = requireActivity()
            findNavController().navigateUp()
            // Recreate the activity so the new accent colour is applied everywhere
            if (changed) activity.recreate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
