package com.zmstore.projectr.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zmstore.projectr.ui.MainViewModel
import com.zmstore.projectr.ui.camera.CameraScreen
import com.zmstore.projectr.ui.home.HomeScreen
import com.zmstore.projectr.ui.medication.MedicationDetailScreen
import com.zmstore.projectr.ui.login.LoginScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import com.zmstore.projectr.ui.home.HistoryScreen
import com.zmstore.projectr.ui.medication.MedicationListScreen
import com.zmstore.projectr.ui.alarms.AlarmManagementScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object MedicationList : Screen("medication_list")
    object Camera : Screen("camera")
    object History : Screen("history")
    object Profile : Screen("profile")
    object Help : Screen("help")
    object Alarms : Screen("alarms")
    object Detail : Screen("detail/{medicationName}?id={id}") {
        fun createRoute(name: String, id: Int = -1) = 
            "detail/${URLEncoder.encode(name, StandardCharsets.UTF_8.toString())}?id=$id"
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    onCheckCameraPermission: () -> Boolean,
    onOpenDrawer: () -> Unit = {},
    onRequestAlarmPermission: () -> Unit = {}
) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            enterTransition = { fadeIn(animationSpec = tween(400)) },
            exitTransition = { fadeOut(animationSpec = tween(400)) },
            popEnterTransition = { fadeIn(animationSpec = tween(400)) },
            popExitTransition = { fadeOut(animationSpec = tween(400)) }
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        onRequestAlarmPermission() // Solicita permissão de alarme APÓS o login
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    onNavigateToCamera = { 
                        if (onCheckCameraPermission()) {
                            navController.navigate(Screen.Camera.route)
                        }
                    },
                    onNavigateToDetail = { id: Int, name: String -> navController.navigate(Screen.Detail.createRoute(name, id)) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) },
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToMedicationList = { navController.navigate(Screen.MedicationList.route) },
                    onNavigateToAlarms = { navController.navigate(Screen.Alarms.route) }
                )
            }
            composable(Screen.MedicationList.route) {
                MedicationListScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id: Int, name: String -> navController.navigate(Screen.Detail.createRoute(name, id)) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Alarms.route) {
                AlarmManagementScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Camera.route) {
                CameraScreen(
                    onTextDetected = { text ->
                        navController.navigate(Screen.Detail.createRoute(text)) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("medicationName") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType; defaultValue = -1 }
                )
            ) { backStackEntry ->
                val medicationName = backStackEntry.arguments?.getString("medicationName")
                val medicationId = backStackEntry.arguments?.getInt("id") ?: -1
                MedicationDetailScreen(
                    viewModel = viewModel,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@composable,
                    medicationId = medicationId,
                    medicationName = medicationName,
                    onBack = { navController.popBackStack() },
                    onNavigateToCamera = {
                        if (onCheckCameraPermission()) {
                            navController.navigate(Screen.Camera.route)
                        }
                    },
                    onSave = { medication ->
                        if (medication.id == 0) {
                            viewModel.insertMedication(medication)
                        } else {
                            viewModel.updateMedication(medication)
                        }
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.Profile.route) {
                com.zmstore.projectr.ui.profile.ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Help.route) {
                com.zmstore.projectr.ui.help.HelpScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
