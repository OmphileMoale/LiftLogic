package com.example.liftlogic.ui.auth

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.AuthResult
import com.example.liftlogic.databinding.FragmentRegisterBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.toMessage
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Units

class RegisterFragment : BaseFragment<FragmentRegisterBinding>(FragmentRegisterBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createButton.setOnClickListener { register() }
        binding.loginLink.setOnClickListener { findNavController().popBackStack() }
    }

    private fun register() {
        val name = binding.nameInput.text?.toString().orEmpty()
        val email = binding.emailInput.text?.toString().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()

        if (password != confirm) {
            showError(getString(R.string.error_passwords_dont_match))
            return
        }
        showError(null)
        val units = if (binding.unitsGroup.checkedChipId == R.id.chip_lb) Units.LB else Units.KG

        binding.createButton.isEnabled = false
        launchUi {
            when (val result = app.userRepo.register(name, email, password, units)) {
                is AuthResult.Success -> {
                    // Don't sign in automatically: go back to Sign In with the email filled in,
                    // so the user signs in with the email and password they just created.
                    // Their first sign-in then opens the profile setup (onboarding).
                    requireContext().getSharedPreferences(PREFS_ONBOARDING, Context.MODE_PRIVATE)
                        .edit().putBoolean(result.user.email, true).apply()
                    val nav = findNavController()
                    nav.previousBackStackEntry?.savedStateHandle?.set(KEY_REGISTERED_EMAIL, result.user.email)
                    if (!nav.popBackStack(R.id.loginFragment, false)) {
                        nav.navigate(R.id.loginFragment)
                    }
                }
                is AuthResult.Failure -> {
                    showError(result.error.toMessage(requireContext()))
                    binding.createButton.isEnabled = true
                }
            }
        }
    }

    private fun showError(message: String?) {
        val b = bindingOrNull ?: return
        b.errorText.visibleIf(message != null)
        b.errorText.text = message
    }

    companion object {
        /** Passed back to the Sign In screen so it can fill in the new account's email. */
        const val KEY_REGISTERED_EMAIL = "registered_email"
        /** Remembers which new accounts still need the profile setup on first sign-in. */
        const val PREFS_ONBOARDING = "liftlogic_onboarding"
    }
}
