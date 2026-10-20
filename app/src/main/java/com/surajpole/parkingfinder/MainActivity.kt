package com.surajpole.parkingfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.surajpole.parkingfinder.data.repository.AuthRepository
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.ui.navigation.AppNavHost
import com.surajpole.parkingfinder.ui.theme.SurajpoleParkingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase if not already initialized
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            // Already initialized or waiting for config
        }

        val authRepository = AuthRepository()
        val parkingRepository = ParkingRepository()

        // Background asynchronous preload of Maps engine, location, and parking cache
        com.surajpole.parkingfinder.data.service.MapPreloader.preload(this, parkingRepository)

        setContent {
            SurajpoleParkingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        authRepository = authRepository,
                        parkingRepository = parkingRepository
                    )
                }
            }
        }
    }
}
