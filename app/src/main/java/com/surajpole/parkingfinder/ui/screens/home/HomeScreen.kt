package com.surajpole.parkingfinder.ui.screens.home

import android.Manifest
import android.os.Looper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.surajpole.parkingfinder.data.model.ParkingLot
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.data.service.RoadRoutingService
import com.surajpole.parkingfinder.data.service.RouteResult
import com.surajpole.parkingfinder.ui.components.MapMarkerUtils
import com.surajpole.parkingfinder.ui.components.VehicleSelectionDialog
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyPrimary
import com.surajpole.parkingfinder.ui.theme.RedDanger
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    parkingRepository: ParkingRepository,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Surajpole, Udaipur default center
    val surajpoleLatLng = remember { LatLng(24.5804, 73.6967) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(surajpoleLatLng, 15f)
    }

    // Parking lots from Firestore
    val parkingLots by parkingRepository.observeParkingLots().collectAsState(initial = emptyList())

    // Map Type toggle: Normal, Satellite, Hybrid
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }

    // Live User Location state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Routing and vehicle state
    var pendingParkingForVehicle by remember { mutableStateOf<ParkingLot?>(null) }
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var navigatingLot by remember { mutableStateOf<ParkingLot?>(null) }
    var isTwoWheelerSelected by remember { mutableStateOf(false) }
    var isRoutingLoading by remember { mutableStateOf(false) }

    // Location permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Fast and smooth GPS polling: updates every 1.5 - 2 seconds as requested
    DisposableEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L // 2.0 sec interval
            ).apply {
                setMinUpdateIntervalMillis(1500L) // 1.5 sec fastest interval
                setMinUpdateDistanceMeters(1.0f) // 1 meter sensitivity
                setWaitForAccurateLocation(false)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        val newLatLng = LatLng(loc.latitude, loc.longitude)
                        userLocation = newLatLng
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )

                // Fast initial fix
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val initLatLng = LatLng(it.latitude, it.longitude)
                        userLocation = initLatLng
                    }
                }
            } catch (e: SecurityException) {
                // Permission not granted
            }

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) {
        locationPermissionsState.launchMultiplePermissionRequest()
        parkingRepository.seedInitialDataIfEmpty()
    }

    // Vehicle Selection Dialog
    pendingParkingForVehicle?.let { lot ->
        VehicleSelectionDialog(
            parkingLot = lot,
            onDismiss = { pendingParkingForVehicle = null },
            onVehicleSelected = { isTwoWheeler ->
                pendingParkingForVehicle = null
                isTwoWheelerSelected = isTwoWheeler
                navigatingLot = lot

                val startPoint = userLocation ?: surajpoleLatLng
                val destPoint = LatLng(lot.latitude, lot.longitude)

                isRoutingLoading = true
                coroutineScope.launch {
                    val route = RoadRoutingService.getRoadRoute(
                        origin = startPoint,
                        destination = destPoint,
                        isTwoWheeler = isTwoWheeler
                    )
                    activeRoute = route
                    isRoutingLoading = false

                    // Camera animate to show whole route smoothly
                    try {
                        val builder = LatLngBounds.Builder()
                        route.points.forEach { builder.include(it) }
                        builder.include(startPoint)
                        builder.include(destPoint)
                        val bounds = builder.build()
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 120),
                            durationMs = 900
                        )
                    } catch (e: Exception) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(startPoint, 15f),
                            durationMs = 800
                        )
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = com.surajpole.parkingfinder.R.drawable.app_logo),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Apni Parking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (userLocation != null) "GPS Live Sync (1.5s)" else "Smart Live Tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (userLocation != null) EmeraldGreen else Color.White.copy(alpha = 0.8f),
                                fontWeight = if (userLocation != null) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary),
                actions = {
                    // Recenter camera
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val target = userLocation ?: surajpoleLatLng
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 16f),
                                durationMs = 800
                            )
                        }
                    }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Recenter",
                            tint = Color.White
                        )
                    }
                    // Profile button
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map View (High FPS, smooth rendering)
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    mapType = currentMapType,
                    isMyLocationEnabled = false // Disabled default blue dot in favor of custom host radar dot
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = false,
                    zoomControlsEnabled = false,
                    compassEnabled = true
                )
            ) {
                // Red pins for all parking lots
                parkingLots.forEach { lot ->
                    val markerState = rememberMarkerState(
                        key = "lot_${lot.id}",
                        position = LatLng(lot.latitude, lot.longitude)
                    )
                    Marker(
                        state = markerState,
                        title = lot.name,
                        snippet = "${lot.availableSlots} / ${lot.total_slots} Slots Available (Tap to route)",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (lot.isFull) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_AZURE
                        ),
                        onClick = {
                            pendingParkingForVehicle = lot
                            true
                        }
                    )
                }

                // Custom Unique Host Dot Marker & Pulsing Radar Circle
                userLocation?.let { loc ->
                    val userMarkerState = rememberMarkerState(key = "user_gps_host", position = loc)
                    LaunchedEffect(loc) {
                        userMarkerState.position = loc
                    }

                    Marker(
                        state = userMarkerState,
                        title = "Your Live GPS Position",
                        snippet = "Accurate host position (Updated every 1.5s)",
                        icon = remember { MapMarkerUtils.getUserLocationDescriptor() },
                        anchor = Offset(0.5f, 0.5f)
                    )

                    // Electric Cyan pulse radar aura
                    Circle(
                        center = loc,
                        radius = 28.0,
                        fillColor = Color(0x3300E5FF),
                        strokeColor = Color(0xFF00E5FF),
                        strokeWidth = 2.5f
                    )
                }

                // Road-snapped Shortest Path (Magnet Effect)
                activeRoute?.let { route ->
                    Polyline(
                        points = route.points,
                        color = Color(0xFF00B0FF), // Electric Blue
                        width = 13f,
                        geodesic = true
                    )
                }
            }

            // Top Floating Controls: Satellite Toggle & Current Location FAB
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Map Mode Toggle FAB (Normal <-> Hybrid)
                FloatingActionButton(
                    onClick = {
                        currentMapType = if (currentMapType == MapType.NORMAL) {
                            MapType.HYBRID
                        } else {
                            MapType.NORMAL
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NavyPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Map View Mode",
                        tint = if (currentMapType != MapType.NORMAL) EmeraldGreen else NavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // My Location FAB
                FloatingActionButton(
                    onClick = {
                        if (locationPermissionsState.allPermissionsGranted) {
                            val target = userLocation ?: surajpoleLatLng
                            coroutineScope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(target, 16.5f),
                                    durationMs = 700
                                )
                            }
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NavyPrimary,
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Current Location",
                        tint = NavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Hybrid Mode Indicator Badge
            if (currentMapType == MapType.HYBRID) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                ) {
                    Text(
                        text = "🛰️ Hybrid View",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Active Route Navigation Bottom Card
            AnimatedVisibility(
                visible = activeRoute != null && navigatingLot != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                activeRoute?.let { route ->
                    navigatingLot?.let { lot ->
                        ActiveRouteCard(
                            parkingLot = lot,
                            route = route,
                            isTwoWheeler = isTwoWheelerSelected,
                            onClose = {
                                activeRoute = null
                                navigatingLot = null
                            },
                            onProceedToDetail = {
                                onNavigateToDetail(lot.id)
                            }
                        )
                    }
                }
            }



            // Route Calculation Progress
            if (isRoutingLoading) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Finding shortest road path...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveRouteCard(
    parkingLot: ParkingLot,
    route: RouteResult,
    isTwoWheeler: Boolean,
    onClose: () -> Unit,
    onProceedToDetail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (isTwoWheeler) EmeraldGreen else NavyPrimary).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isTwoWheeler) Icons.AutoMirrored.Filled.DirectionsBike else Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = if (isTwoWheeler) EmeraldGreen else NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = parkingLot.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1
                        )
                        Text(
                            text = if (isTwoWheeler) "🏍️ 2-Wheeler Optimal Route" else "🚗 4-Wheeler Road Route",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear Route", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("Distance", fontSize = 11.sp, color = Color.Gray)
                        Text(route.distanceText, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyPrimary)
                    }
                    Column {
                        Text("ETA", fontSize = 11.sp, color = Color.Gray)
                        Text(route.durationText, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = EmeraldGreen)
                    }
                    Column {
                        Text("Available", fontSize = 11.sp, color = Color.Gray)
                        Text("${parkingLot.availableSlots} slots", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Button(
                    onClick = onProceedToDetail,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Book Slot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}


