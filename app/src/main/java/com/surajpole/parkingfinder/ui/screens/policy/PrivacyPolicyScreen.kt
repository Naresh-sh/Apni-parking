package com.surajpole.parkingfinder.ui.screens.policy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surajpole.parkingfinder.R
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.data.service.MapPreloader
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyDark
import com.surajpole.parkingfinder.ui.theme.NavyPrimary

@Composable
fun PrivacyPolicyScreen(
    parkingRepository: ParkingRepository,
    onAccept: () -> Unit
) {
    val context = LocalContext.current

    // Preload Google Maps and Parking cache in the background while user reads policy
    LaunchedEffect(Unit) {
        MapPreloader.preload(context, parkingRepository)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        NavyDark,
                        NavyPrimary,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Logo
            Surface(
                shape = RoundedCornerShape(26.dp),
                shadowElevation = 8.dp,
                color = Color.White,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Apni Parking Logo",
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(22.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Apni Parking",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EmeraldGreen.copy(alpha = 0.2f),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "Transparency & Location Privacy",
                    color = EmeraldGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Explanatory Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Why We Request Your Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "To deliver live turn-by-turn routing and connect you to the nearest available parking space in Udaipur, Apni Parking uses your device's high-precision GPS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Feature Highlights
            PolicyFeatureItem(
                icon = Icons.Default.MyLocation,
                iconColor = EmeraldGreen,
                title = "Nearest Slot Detection",
                description = "Instantly calculates distances from your current spot to active parking lots around Surajpole, Delhi Gate, and Bapu Bazar."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PolicyFeatureItem(
                icon = Icons.Default.Navigation,
                iconColor = NavyPrimary,
                title = "Smart Road-Only Routing",
                description = "Magnet road-matching ensures your 2-wheeler or 4-wheeler route stays on motorable streets, avoiding pedestrian lanes and shops."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PolicyFeatureItem(
                icon = Icons.Default.Timer,
                iconColor = EmeraldGreen,
                title = "Live ETA & Real-Time Sync",
                description = "Smooth 1.5s coordinate sync keeps your ETA accurate so you reach before your 15-minute slot reservation hold expires."
            )

            Spacer(modifier = Modifier.height(12.dp))

            PolicyFeatureItem(
                icon = Icons.Default.Lock,
                iconColor = NavyPrimary,
                title = "100% Privacy & Zero Tracking",
                description = "Your location is processed locally in real-time during navigation only. It is never sold, shared, or stored on ad servers."
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Accept & Continue Button
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Accept & Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "By continuing, you agree to enable live navigation and real-time slot tracking.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp),
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PolicyFeatureItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}
