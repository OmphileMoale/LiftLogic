package com.example.liftlogic.ui.settings

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.AuthError
import com.example.liftlogic.data.AuthResult
import com.example.liftlogic.databinding.FragmentChangePasswordBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.toMessage
import com.example.liftlogic.ui.visibleIf

class ChangePasswordFragment : BaseFragment<FragmentChangePasswordBinding>(FragmentChangePasswordBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.settings_change_password)

        binding.updateButton.setOnClickListener {
            val userId = app.session.currentUserId ?: return@setOnClickListener
            val current = binding.currentPasswordInput.text?.toString().orEmpty()
            val newPassword = binding.newPasswordInput.text?.toString().orEmpty()
            val confirm = binding.confirmPasswordInput.text?.toString().orEmpty()

            when {
                newPassword.length < 8 -> showError(getString(R.string.error_password_too_short))
                newPassword != confirm -> showError(getString(R.string.error_passwords_dont_match))
                else -> {
                    showError(null)
                    launchUi {
                        when (val result = app.userRepo.changePassword(userId, current, newPassword)) {
                            is AuthResult.Success -> {
                                toast(R.string.text_password_updated)
                                findNavController().navigateUp()
                            }
                            is AuthResult.Failure -> showError(
                                if (result.error == AuthError.EMPTY_FIELDS) getString(R.string.error_password_too_short)
                                else result.error.toMessage(requireContext())
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showError(message: String?) {
        val b = bindingOrNull ?: return
        b.errorText.visibleIf(message != null)
        b.errorText.text = message
    }
}
