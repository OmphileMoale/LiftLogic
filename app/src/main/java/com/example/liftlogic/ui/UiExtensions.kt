package com.example.liftlogic.ui

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.viewbinding.ViewBinding
import com.example.liftlogic.AppContainer
import com.example.liftlogic.LiftLogicApp
import com.example.liftlogic.R
import com.example.liftlogic.data.AuthError
import com.example.liftlogic.databinding.DialogTextInputBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.navigation.fragment.findNavController

/**
 * Base class for every screen. Inflates the screen's XML layout through ViewBinding and
 * clears the binding when the view is destroyed (avoids memory leaks).
 */
abstract class BaseFragment<B : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> B
) : Fragment() {

    private var _binding: B? = null
    protected val binding: B get() = _binding!!
    protected val bindingOrNull: B? get() = _binding

    /** Repositories and app-wide services. */
    protected val app: AppContainer get() = (requireActivity().application as LiftLogicApp).container

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: android.os.Bundle?): View {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /** Back arrow + title for screens that have a toolbar. */
    protected fun setupToolbar(toolbar: MaterialToolbar, @StringRes title: Int? = null) {
        title?.let { toolbar.setTitle(it) }
        toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    /** Runs a coroutine tied to the view's lifecycle. */
    protected fun launchUi(block: suspend CoroutineScope.() -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch(block = block)
    }

    protected fun toast(@StringRes message: Int) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    protected fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as LiftLogicApp).container

/** Creates a ViewModel that receives the AppContainer (simple manual dependency injection). */
inline fun <reified VM : ViewModel> Fragment.appViewModels(noinline create: (AppContainer) -> VM): Lazy<VM> =
    viewModels {
        viewModelFactory {
            initializer { create(appContainer) }
        }
    }

/**
 * Collects a Flow while the fragment's view is visible (STARTED) and stops when it is not.
 * This is the XML/View equivalent of `collectAsState()` in Compose.
 */
fun <T> Fragment.observe(flow: Flow<T>, action: (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { action(it) }
        }
    }
}

/** Turns an AuthError into the user-facing (translated) message. */
fun AuthError.toMessage(context: Context): String = when (this) {
    AuthError.EMPTY_FIELDS -> context.getString(R.string.error_empty_fields)
    AuthError.EMAIL_TAKEN -> context.getString(R.string.error_email_taken)
    AuthError.NO_ACCOUNT -> context.getString(R.string.error_no_account)
    AuthError.WRONG_PASSWORD -> context.getString(R.string.error_wrong_password)
    AuthError.INVALID_EMAIL -> context.getString(R.string.error_invalid_email)
}

fun View.visibleIf(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/** Shows a dialog with one text field (rename workout, log body weight, ...). */
fun Fragment.showTextInputDialog(
    @StringRes title: Int,
    @StringRes hint: Int,
    initial: String = "",
    numeric: Boolean = false,
    onSave: (String) -> Unit
) {
    val dialogBinding = DialogTextInputBinding.inflate(layoutInflater)
    dialogBinding.inputLayout.setHint(hint)
    dialogBinding.input.inputType = if (numeric) {
        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    } else {
        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }
    dialogBinding.input.setText(initial)
    dialogBinding.input.setSelection(initial.length)
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setView(dialogBinding.root)
        .setPositiveButton(R.string.action_save) { _, _ -> onSave(dialogBinding.input.text?.toString().orEmpty()) }
        .setNegativeButton(R.string.action_cancel, null)
        .show()
}

/** Simple yes/no confirmation dialog. */
fun Fragment.confirm(
    @StringRes title: Int,
    message: String,
    @StringRes positive: Int,
    onConfirm: () -> Unit
) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(positive) { _, _ -> onConfirm() }
        .setNegativeButton(R.string.action_cancel, null)
        .show()
}
