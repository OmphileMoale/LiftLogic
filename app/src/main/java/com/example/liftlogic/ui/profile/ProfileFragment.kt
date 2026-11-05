package com.example.liftlogic.ui.profile

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.Badge
import com.example.liftlogic.data.Experience
import com.example.liftlogic.data.FitnessGoal
import com.example.liftlogic.databinding.FragmentProfileBinding
import com.example.liftlogic.databinding.ItemBadgeBinding
import com.example.liftlogic.databinding.ItemSettingsRowBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.auth.ProfileSetupFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.util.AppPrefs
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Stats
import com.example.liftlogic.util.Units
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class ProfileFragment : BaseFragment<FragmentProfileBinding>(FragmentProfileBinding::inflate) {

    private val badgeAdapter = BadgeAdapter { badge, unlocked ->
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(badge.title)
            .setMessage(getString(badge.description) + if (unlocked) "\n\n" + getString(R.string.text_badge_unlocked) else "")
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val nav = findNavController()

        binding.settingsButton.setOnClickListener { nav.navigate(R.id.settingsFragment) }
        bindRow(binding.rowBodyWeight, R.drawable.ic_monitor_weight, R.string.title_body_weight) { nav.navigate(R.id.bodyWeightFragment) }
        bindRow(binding.rowHistory, R.drawable.ic_history, R.string.title_history) { nav.navigate(R.id.historyFragment) }
        bindRow(binding.rowTools, R.drawable.ic_calculate, R.string.title_tools) { nav.navigate(R.id.toolsFragment) }
        bindRow(binding.rowEditProfile, R.drawable.ic_person, R.string.settings_edit_profile) {
            nav.navigate(R.id.profileSetupFragment, bundleOf(ProfileSetupFragment.ARG_ONBOARDING to false))
        }
        bindRow(binding.rowSettings, R.drawable.ic_settings, R.string.settings_title) { nav.navigate(R.id.settingsFragment) }

        binding.logoutButton.setOnClickListener {
            app.restTimer.skip()
            app.session.logout()
            nav.navigate(R.id.action_global_logout)
        }

        binding.metricWorkouts.label.setText(R.string.label_workouts)
        binding.metricVolume.label.setText(R.string.label_total_volume)
        binding.metricSince.label.setText(R.string.label_member_since)

        binding.badgeList.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.badgeList.adapter = badgeAdapter

        val data = app.session.userId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else combine(app.currentUser, app.workoutRepo.observeFinished(id), app.workoutRepo.observeBadges(id)) { user, workouts, badges ->
                Triple(user, workouts, badges.map { it.badgeKey }.toSet())
            }
        }

        observe(data) { triple ->
            val (user, workouts, unlocked) = triple ?: return@observe
            if (user == null) return@observe
            val unit = user.units

            binding.nameText.text = user.name
            binding.goalText.text = getString(
                R.string.format_two_parts,
                getString(FitnessGoal.fromKey(user.goal).label),
                getString(Experience.fromKey(user.experience).label)
            )
            val level = Stats.levelForXp(user.xp)
            binding.levelText.text = getString(R.string.format_level, level)
            binding.xpText.text = getString(R.string.format_xp_progress, Stats.xpIntoLevel(user.xp), Stats.XP_PER_LEVEL)
            binding.xpProgress.max = Stats.XP_PER_LEVEL
            binding.xpProgress.setProgressCompat(Stats.xpIntoLevel(user.xp), true)

            binding.metricWorkouts.value.text = workouts.size.toString()
            binding.metricVolume.value.text = Units.volume(workouts.sumOf { it.totalVolumeKg }, unit)
            binding.metricSince.value.text = Formatters.monthYear(user.createdAt)

            badgeAdapter.unlocked = unlocked
            binding.badgesTitle.text = getString(R.string.format_badges_title, unlocked.size, Badge.entries.size)

            showAvatar(user.userId)
        }
    }

    private fun showAvatar(userId: String) {
        val image = binding.avatarImage
        val uri = AppPrefs.getAvatarUri(requireContext(), userId)
        if (uri != null) {
            image.imageTintList = null
            image.scaleType = ImageView.ScaleType.CENTER_CROP
            image.setPadding(0, 0, 0, 0)
            image.setImageURI(Uri.parse(uri))
        }
        if (uri == null || image.drawable == null) {
            val pad = (14 * resources.displayMetrics.density).toInt()
            image.setPadding(pad, pad, pad, pad)
            image.scaleType = ImageView.ScaleType.FIT_CENTER
            image.setImageResource(R.drawable.ic_person)
            image.imageTintList = ColorStateList.valueOf(MaterialColors.getColor(image, com.google.android.material.R.attr.colorPrimary))
        }
    }

    private fun bindRow(row: ItemSettingsRowBinding, @DrawableRes icon: Int, @StringRes title: Int, onClick: () -> Unit) {
        row.icon.setImageResource(icon)
        row.title.setText(title)
        row.root.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        binding.badgeList.adapter = null
        super.onDestroyView()
    }

    /** Grid of all badges; locked ones are faded. */
    private class BadgeAdapter(private val onClick: (Badge, Boolean) -> Unit) : RecyclerView.Adapter<BadgeAdapter.VH>() {
        var unlocked: Set<String> = emptySet()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        private val badges = Badge.entries

        class VH(val b: ItemBadgeBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount() = badges.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val badge = badges[position]
            val has = badge.key in unlocked
            val context = holder.itemView.context
            val color = if (has) ContextCompat.getColor(context, R.color.amber_badge)
            else MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOutline)
            holder.b.icon.setImageResource(badge.icon)
            holder.b.icon.imageTintList = ColorStateList.valueOf(color)
            holder.b.iconBackground.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 50))
            holder.b.title.setText(badge.title)
            holder.itemView.alpha = if (has) 1f else 0.55f
            holder.itemView.setOnClickListener { onClick(badge, has) }
        }
    }
}
