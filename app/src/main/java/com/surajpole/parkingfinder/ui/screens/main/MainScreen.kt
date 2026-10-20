package com.surajpole.parkingfinder.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.surajpole.parkingfinder.data.repository.AuthRepository
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.ui.screens.home.HomeScreen
import com.surajpole.parkingfinder.ui.screens.profile.ProfileScreen
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyPrimary

@Composable
fun MainScreen(
    authRepository: AuthRepository,
    parkingRepository: ParkingRepository,
    initialTab: Int = 0,
    onNavigateToDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(initialTab) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                // Home (Live Map) Item
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Explore else Icons.Outlined.Explore,
                            contentDescription = "Home / Live Map"
                        )
                    },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyPrimary,
                        selectedTextColor = NavyPrimary,
                        indicatorColor = NavyPrimary.copy(alpha = 0.12f)
                    )
                )

                // Profile Item
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NavyPrimary,
                        selectedTextColor = NavyPrimary,
                        indicatorColor = NavyPrimary.copy(alpha = 0.12f)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> {
                    HomeScreen(
                        parkingRepository = parkingRepository,
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToProfile = { selectedTab = 1 }
                    )
                }
                1 -> {
                    ProfileScreen(
                        authRepository = authRepository,
                        parkingRepository = parkingRepository,
                        onNavigateBack = { selectedTab = 0 },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
