package com.surajpole.parkingfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.surajpole.parkingfinder.data.local.AppPreferences
import com.surajpole.parkingfinder.data.repository.AuthRepository
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.ui.screens.auth.LoginScreen
import com.surajpole.parkingfinder.ui.screens.auth.SignUpScreen
import com.surajpole.parkingfinder.ui.screens.booking.BookingScreen
import com.surajpole.parkingfinder.ui.screens.detail.ParkingDetailScreen
import com.surajpole.parkingfinder.ui.screens.home.HomeScreen
import com.surajpole.parkingfinder.ui.screens.policy.PrivacyPolicyScreen
import com.surajpole.parkingfinder.ui.screens.profile.ProfileScreen
import com.surajpole.parkingfinder.ui.screens.splash.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository,
    parkingRepository: ParkingRepository
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                authRepository = authRepository,
                parkingRepository = parkingRepository,
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate(Screen.PrivacyPolicy.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Privacy Policy Screen
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(
                parkingRepository = parkingRepository,
                onAccept = {
                    val prefs = AppPreferences(context)
                    prefs.isPrivacyPolicyAccepted = true
                    if (authRepository.isUserLoggedIn) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.PrivacyPolicy.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.PrivacyPolicy.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Login Screen
        composable(Screen.Login.route) {
            LoginScreen(
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                }
            )
        }

        // Sign Up Screen
        composable(Screen.SignUp.route) {
            SignUpScreen(
                authRepository = authRepository,
                onSignUpSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Main Screen (Hosts Bottom Navigation: Home & Profile)
        composable(Screen.Home.route) {
            com.surajpole.parkingfinder.ui.screens.main.MainScreen(
                authRepository = authRepository,
                parkingRepository = parkingRepository,
                initialTab = 0,
                onNavigateToDetail = { parkingId ->
                    navController.navigate(Screen.ParkingDetail.createRoute(parkingId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Parking Detail Screen
        composable(
            route = Screen.ParkingDetail.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getString("parkingId") ?: ""
            ParkingDetailScreen(
                parkingId = parkingId,
                parkingRepository = parkingRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToBooking = { id ->
                    navController.navigate(Screen.Booking.createRoute(id))
                }
            )
        }

        // Booking Screen
        composable(
            route = Screen.Booking.route,
            arguments = listOf(navArgument("parkingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val parkingId = backStackEntry.arguments?.getString("parkingId") ?: ""
            BookingScreen(
                parkingId = parkingId,
                parkingRepository = parkingRepository,
                authRepository = authRepository,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        // Profile Screen (opens MainScreen with Tab 1 active)
        composable(Screen.Profile.route) {
            com.surajpole.parkingfinder.ui.screens.main.MainScreen(
                authRepository = authRepository,
                parkingRepository = parkingRepository,
                initialTab = 1,
                onNavigateToDetail = { parkingId ->
                    navController.navigate(Screen.ParkingDetail.createRoute(parkingId))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
