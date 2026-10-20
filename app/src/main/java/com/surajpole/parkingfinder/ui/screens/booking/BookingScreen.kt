package com.surajpole.parkingfinder.ui.screens.booking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.surajpole.parkingfinder.data.local.Strings
import com.surajpole.parkingfinder.data.model.Booking
import com.surajpole.parkingfinder.data.model.ParkingLot
import com.surajpole.parkingfinder.data.repository.AuthRepository
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyPrimary
import com.surajpole.parkingfinder.ui.theme.RedDanger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    parkingId: String,
    parkingRepository: ParkingRepository,
    authRepository: AuthRepository,
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val parkingLot by parkingRepository.observeParkingLot(parkingId).collectAsState(initial = null)
    val currentUser = authRepository.currentUser

    var carNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var activeBooking by remember { mutableStateOf<Booking?>(null) }
    var remainingMillis by remember { mutableLongStateOf(0L) }
    var isCancelledOrExpired by remember { mutableStateOf(false) }

    // Live 1-second countdown ticker
    LaunchedEffect(activeBooking) {
        val booking = activeBooking ?: return@LaunchedEffect
        while (true) {
            val now = System.currentTimeMillis()
            val diff = booking.expires_at - now
            if (diff <= 0) {
                remainingMillis = 0L
                isCancelledOrExpired = true
                // Auto-cancel in Firestore when timer hits 0
                parkingRepository.cancelBooking(booking.id, booking.parking_id)
                snackbarHostState.showSnackbar("Reservation time expired! Slot has been released.")
                break
            } else {
                remainingMillis = diff
            }
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (activeBooking == null) Strings.get("book_title") else Strings.get("active_reservation"), color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = Strings.get("back"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val lot = parkingLot

        if (lot == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NavyPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (activeBooking == null) {
                    // BOOKING FORM VIEW
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = lot.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${Strings.get("slots_available")}: ${lot.availableSlots}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = carNumber,
                                onValueChange = { carNumber = it.uppercase() },
                                label = { Text(Strings.get("car_number")) },
                                leadingIcon = {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Note: Confirming will hold 1 slot in real-time for exactly 15 minutes. Once confirmed, you must arrive before expiry.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = {
                            if (carNumber.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please enter vehicle plate number")
                                }
                                return@Button
                            }
                            val uid = currentUser?.uid ?: "anonymous"
                            isLoading = true
                            coroutineScope.launch {
                                val result = parkingRepository.createBooking(lot, uid, carNumber)
                                isLoading = false
                                result.fold(
                                    onSuccess = { booking ->
                                        activeBooking = booking
                                        remainingMillis = booking.expires_at - System.currentTimeMillis()
                                    },
                                    onFailure = { error ->
                                        snackbarHostState.showSnackbar(
                                            error.localizedMessage ?: "Booking failed"
                                        )
                                    }
                                )
                            }
                        },
                        enabled = !isLoading && !lot.isFull,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text(Strings.get("confirm_booking"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // ACTIVE BOOKING / COUNTDOWN VIEW
                    val booking = activeBooking!!

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        if (isCancelledOrExpired) RedDanger.copy(alpha = 0.1f) else EmeraldGreen.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCancelledOrExpired) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCancelledOrExpired) RedDanger else EmeraldGreen,
                                    modifier = Modifier.size(38.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = if (isCancelledOrExpired) "Booking Expired / Cancelled" else "Slot Reserved Successfully!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelledOrExpired) RedDanger else EmeraldGreen
                            )

                            Text(
                                text = lot.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Timer Display
                            val minutes = (remainingMillis / 1000) / 60
                            val seconds = (remainingMillis / 1000) % 60
                            val formattedTime = String.format("%02d:%02d", minutes, seconds)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isCancelledOrExpired) Color(0xFFFEE2E2) else Color(0xFFEFF6FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isCancelledOrExpired) "RESERVATION EXPIRED" else Strings.get("expires_in"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCancelledOrExpired) RedDanger else NavyPrimary
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = formattedTime,
                                        fontSize = 44.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isCancelledOrExpired) RedDanger else NavyPrimary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Vehicle: ${booking.car_number}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            if (!isCancelledOrExpired) {
                                Button(
                                    onClick = {
                                        isLoading = true
                                        coroutineScope.launch {
                                            parkingRepository.cancelBooking(booking.id, booking.parking_id)
                                            isLoading = false
                                            isCancelledOrExpired = true
                                            snackbarHostState.showSnackbar("Booking cancelled successfully")
                                        }
                                    },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = RedDanger),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(Strings.get("cancel_booking"), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            OutlinedButton(
                                onClick = onNavigateToHome,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Return to Map", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
