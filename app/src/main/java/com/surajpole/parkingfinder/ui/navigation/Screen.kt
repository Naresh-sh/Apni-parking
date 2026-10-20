package com.surajpole.parkingfinder.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object PrivacyPolicy : Screen("privacy_policy")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Home : Screen("home")
    object ParkingDetail : Screen("parking_detail/{parkingId}") {
        fun createRoute(parkingId: String) = "parking_detail/$parkingId"
    }
    object Booking : Screen("booking/{parkingId}") {
        fun createRoute(parkingId: String) = "booking/$parkingId"
    }
    object Profile : Screen("profile")
}
