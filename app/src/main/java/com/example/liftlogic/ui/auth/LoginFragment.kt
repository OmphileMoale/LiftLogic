package com.example.liftlogic.ui.auth

import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.liftlogic.R
import com.example.liftlogic.data.AuthResult
import com.example.liftlogic.databinding.FragmentLoginBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.toMessage
import com.example.liftlogic.ui.visibleIf
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject

class LoginFragment : BaseFragment<FragmentLoginBinding>(FragmentLoginBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.emailInput.doAfterTextChanged { showError(null) }
        binding.passwordInput.doAfterTextChanged { showError(null) }
        binding.passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) { attemptLogin(); true } else false
        }

        binding.loginButton.setOnClickListener { attemptLogin() }
        binding.googleSignInButton.setOnClickListener { attemptGoogleSignIn() }
        binding.registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        listenForNewAccount()
    }

    /**
     * After "Create account" the register screen comes back here with the email it used.
     * Fill it in so the user signs in with the same email and password they just chose.
     */
    private fun listenForNewAccount() {
        val handle = findNavController().currentBackStackEntry?.savedStateHandle ?: return
        handle.getLiveData<String>(RegisterFragment.KEY_REGISTERED_EMAIL)
            .observe(viewLifecycleOwner) { email ->
                if (email.isNullOrBlank()) return@observe
                handle.remove<String>(RegisterFragment.KEY_REGISTERED_EMAIL)
                binding.emailInput.setText(email)
                binding.passwordInput.text = null
                binding.passwordInput.requestFocus()
                showError(null)
                Snackbar.make(
                    binding.root,
                    "Account created! Sign in with the email and password you just used.",
                    Snackbar.LENGTH_LONG
                ).show()
            }
    }

    private fun attemptLogin() {
        val email = binding.emailInput.text?.toString().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        binding.loginButton.isEnabled = false
        launchUi {
            when (val result = app.userRepo.login(email, password)) {
                is AuthResult.Success -> {
                    app.session.login(result.user.userId)
                    openAppAfterLogin(result.user.email)
                }
                is AuthResult.Failure -> {
                    showError(result.error.toMessage(requireContext()))
                    binding.loginButton.isEnabled = true
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // Sign in with Google (Android Credential Manager)
    // ---------------------------------------------------------------------------------

    private fun attemptGoogleSignIn() {
        showError(null)
        binding.googleSignInButton.isEnabled = false

        val googleOption = GetSignInWithGoogleOption.Builder(getString(R.string.google_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        launchUi {
            try {
                val result = CredentialManager.create(requireContext())
                    .getCredential(requireActivity(), request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val google = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = google.id // Google returns the account's email address here
                    val name = google.displayName
                        ?: google.givenName
                        ?: email.substringBefore('@')
                    val googleId = googleSubject(google.idToken) ?: email
                    performGoogleLogin(email, name, googleId)
                } else {
                    googleFailed("Google Sign-In returned an unexpected credential.")
                }
            } catch (e: GetCredentialCancellationException) {
                googleFailed(null) // user closed the Google sheet
            } catch (e: NoCredentialException) {
                googleFailed("No Google account found on this phone. Add one in Settings > Accounts, then try again.")
            } catch (e: GoogleIdTokenParsingException) {
                googleFailed("Google Sign-In failed: could not read the Google account.")
            } catch (e: GetCredentialException) {
                googleFailed("Google Sign-In failed: ${e.message ?: e.type}")
            }
        }
    }

    private fun performGoogleLogin(email: String, name: String, googleId: String) {
        launchUi {
            when (val result = app.userRepo.loginOrRegisterWithGoogle(email, name, googleId)) {
                is AuthResult.Success -> {
                    app.session.login(result.user.userId)
                    findNavController().navigate(R.id.action_login_to_home)
                }
                is AuthResult.Failure -> googleFailed(result.error.toMessage(requireContext()))
            }
        }
    }

    private fun googleFailed(message: String?) {
        showError(message)
        bindingOrNull?.googleSignInButton?.isEnabled = true
    }

    /** The stable Google account id ("sub") from the ID token's payload. */
    private fun googleSubject(idToken: String): String? = try {
        val payload = idToken.split(".")[1]
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        JSONObject(json).optString("sub").takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }

    // ---------------------------------------------------------------------------------

    /** First sign-in after creating an account shows the profile setup, as before. */
    private fun openAppAfterLogin(email: String) {
        val prefs = requireContext().getSharedPreferences(RegisterFragment.PREFS_ONBOARDING, Context.MODE_PRIVATE)
        val key = email.trim().lowercase()
        if (prefs.getBoolean(key, false)) {
            prefs.edit().remove(key).apply()
            findNavController().navigate(
                R.id.profileSetupFragment,
                bundleOf(ProfileSetupFragment.ARG_ONBOARDING to true),
                navOptions { popUpTo(R.id.loginFragment) { inclusive = true } }
            )
        } else {
            findNavController().navigate(R.id.action_login_to_home)
        }
    }

    private fun showError(message: String?) {
        val b = bindingOrNull ?: return
        b.errorText.visibleIf(message != null)
        b.errorText.text = message
    }
}
