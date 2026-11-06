package com.example.liftlogic.ui.settings

import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentProfilePictureBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.AppPrefs
import com.google.android.material.color.MaterialColors

class ProfilePictureFragment : BaseFragment<FragmentProfilePictureBinding>(FragmentProfilePictureBinding::inflate) {

    /** Opens the system picker; the URI permission is kept so the picture still shows after a restart. */
    private val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        try {
            requireContext().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Some providers don't offer persistable permissions; the picture still shows for now.
        }
        val userId = app.session.currentUserId ?: return@registerForActivityResult
        AppPrefs.setAvatarUri(requireContext(), userId, uri.toString())
        showAvatar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.settings_profile_picture)

        binding.changePhotoButton.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        binding.removePhotoButton.setOnClickListener {
            val userId = app.session.currentUserId ?: return@setOnClickListener
            AppPrefs.setAvatarUri(requireContext(), userId, null)
            showAvatar()
        }
        showAvatar()
    }

    private fun showAvatar() {
        val b = bindingOrNull ?: return
        val userId = app.session.currentUserId
        val saved = userId?.let { AppPrefs.getAvatarUri(requireContext(), it) }

        if (saved != null) {
            b.avatarImage.scaleType = ImageView.ScaleType.CENTER_CROP
            b.avatarImage.imageTintList = null
            b.avatarImage.setPadding(0, 0, 0, 0)
            try {
                b.avatarImage.setImageURI(Uri.parse(saved))
            } catch (e: Exception) {
                b.avatarImage.setImageDrawable(null)
            }
        }
        if (saved == null || b.avatarImage.drawable == null) {
            val padding = (35 * resources.displayMetrics.density).toInt()
            b.avatarImage.scaleType = ImageView.ScaleType.FIT_CENTER
            b.avatarImage.setPadding(padding, padding, padding, padding)
            b.avatarImage.setImageResource(R.drawable.ic_person)
            b.avatarImage.imageTintList = ColorStateList.valueOf(
                MaterialColors.getColor(b.avatarImage, com.google.android.material.R.attr.colorPrimary)
            )
        }
        b.removePhotoButton.visibleIf(saved != null)
    }
}
