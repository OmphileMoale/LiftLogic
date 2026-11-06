package com.example.liftlogic

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.example.liftlogic.databinding.ActivityMainBinding
import com.example.liftlogic.util.AppPrefs


class MainActivity : AppCompatActivity() {

    // ViewBinding reference generated from activity_main.xml
    private lateinit var binding: ActivityMainBinding

    // Navigation Controller for handling screen transitions
    private lateinit var navController: NavController

    // Application-wide dependency injection container
    private val container: AppContainer get() = (application as LiftLogicApp).container

    // Set of top-level tab IDs corresponding to bottom navigation options
    private val tabDestinations = setOf(
        R.id.homeFragment,
        R.id.routinesFragment,
        R.id.exerciseLibraryFragment,
        R.id.progressFragment,
        R.id.profileFragment
    )

    // Activity result launcher for runtime notification permissions on Android 13+ (API 33+)
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* Permission handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Apply user preference accent theme overlay before layout inflation
        theme.applyStyle(AppPrefs.accentOverlay(AppPrefs.getAccent(this)), true)
        super.onCreate(savedInstanceState)

        // 2. Inflate views using ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 3. Initialize Navigation Controller from FragmentContainerView host
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHost.navController

        // 4. Dynamic Start Destination Logic:
        // Opens HomeFragment directly if session is active, otherwise opens LoginFragment
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            if (container.session.currentUserId != null) R.id.homeFragment else R.id.loginFragment
        )
        navController.setGraph(graph, null)

        // 5. Setup Bottom Navigation item selection listener
        binding.bottomNav.setOnItemSelectedListener { item ->
            navigateToTab(item.itemId)
            true
        }
        binding.bottomNav.setOnItemReselectedListener { /* Re-selection ignored */ }

        // 6. Manage Bottom Navigation Bar visibility dynamically depending on active screen
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            val isPickerMode = arguments?.getBoolean("pickMode") == true
            val isTopLevelTab = destination.id in tabDestinations && !isPickerMode

            // Display bottom navigation bar on top-level tabs only; hide on sub-screens
            binding.bottomNav.visibility = if (isTopLevelTab) View.VISIBLE else View.GONE
            if (isTopLevelTab) {
                binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
            }
        }

        // 7. Validate saved session user in database asynchronously using Coroutines
        container.session.currentUserId?.let { userId ->
            lifecycleScope.launch {
                if (container.userRepo.getUser(userId) == null) {
                    container.session.logout()
                    navController.navigate(R.id.action_global_logout)
                }
            }
        }

        // 8. Prompt for runtime POST_NOTIFICATIONS permission on Android 13+ (API 33+)
        if (savedInstanceState == null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !container.notifications.canPostNotifications() &&
            AppPrefs.shouldAskNotifications(this)
        ) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Handles navigation between top-level bottom navigation tabs.
     * Keeps HomeFragment as the base of the back stack so pressing Back returns Home.
     */
    fun navigateToTab(destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        if (destinationId == R.id.homeFragment) {
            if (!navController.popBackStack(R.id.homeFragment, false)) {
                navController.navigate(R.id.homeFragment)
            }
            return
        }

        navController.navigate(destinationId, null, navOptions {
            popUpTo(R.id.homeFragment) { inclusive = false }
            launchSingleTop = true
        })
    }
}
