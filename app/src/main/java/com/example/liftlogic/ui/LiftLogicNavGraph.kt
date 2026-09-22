
package com.example.liftlogic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.liftlogic.ui.screens.*

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val ACTIVE_WORKOUT = "active_workout"
    const val SUMMARY = "summary"
    const val PROGRESS = "progress"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val EDIT_PROFILE = "edit_profile"
    const val EMAIL_USERNAME = "email_username"
    const val CHANGE_PASSWORD = "change_password"
    const val PROFILE_PICTURE = "profile_picture"
    const val THEME = "theme"
}

@Composable
fun LiftLogicNavGraph(viewModel: AppViewModel) {
    val navController: NavHostController = rememberNavController()
    val currentUser by viewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (currentUser == null) Routes.LOGIN else Routes.HOME
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = { navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = viewModel,
                onRegisterSuccess = { navController.navigate(Routes.LOGIN) { popUpTo(Routes.LOGIN) { inclusive = true } } },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = viewModel,
                onStartWorkout = { navController.navigate(Routes.LIBRARY) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenProgress = { navController.navigate(Routes.PROGRESS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.LIBRARY) {
            ExerciseLibraryScreen(
                viewModel = viewModel,
                onExercisePicked = { navController.navigate(Routes.ACTIVE_WORKOUT) },
                onSkipToWorkout = { navController.navigate(Routes.ACTIVE_WORKOUT) },
                onHome = { navController.navigate(Routes.HOME) },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onProfile = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.ACTIVE_WORKOUT) {
            ActiveWorkoutScreen(
                viewModel = viewModel,
                onFinish = { navController.navigate(Routes.SUMMARY) { popUpTo(Routes.LIBRARY) { inclusive = true } } }
            )
        }
        composable(Routes.SUMMARY) {
            WorkoutSummaryScreen(
                viewModel = viewModel,
                onBackToHome = { navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } }
            )
        }
        composable(Routes.PROGRESS) {
            ProgressScreen(
                viewModel = viewModel,
                onHome = { navController.navigate(Routes.HOME) },
                onLibrary = { navController.navigate(Routes.LIBRARY) },
                onProfile = { navController.navigate(Routes.PROFILE) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                viewModel = viewModel,
                onHome = { navController.navigate(Routes.HOME) },
                onLibrary = { navController.navigate(Routes.LIBRARY) },
                onProgress = { navController.navigate(Routes.PROGRESS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
                onLoggedOut = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
            )
        }
        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.EMAIL_USERNAME) {
            EmailUsernameScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.CHANGE_PASSWORD) {
            ChangePasswordScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE_PICTURE) {
            ProfilePictureScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.THEME) {
            ThemeScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
