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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
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
import com.surajpole.parkingfinder.data.local.Strings
import com.surajpole.parkingfinder.data.model.ParkingLot
import com.surajpole.parkingfinder.data.repository.ParkingRepository
import com.surajpole.parkingfinder.data.service.RoadRoutingService
import com.surajpole.parkingfinder.data.service.RouteResult
import com.surajpole.parkingfinder.ui.components.VehicleSelectionDialog
import com.surajpole.parkingfinder.ui.theme.EmeraldGreen
import com.surajpole.parkingfinder.ui.theme.NavyPrimary
import com.surajpole.parkingfinder.ui.theme.RedDanger
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
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

    // Parking lots from Firestore (with immediate default fallback cache)
    val parkingLots by parkingRepository.observeParkingLots().collectAsState(
        initial = ParkingRepository.DEFAULT_UDAIPUR_LOTS
    )

    // Map Type toggle: Only Default (NORMAL) and HYBRID as requested
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }

    // Live User Location state
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Routing and vehicle state
    var pendingParkingForVehicle by remember { mutableStateOf<ParkingLot?>(null) }
    var activeRoute by remember { mutableStateOf<RouteResult?>(null) }
    var navigatingLot by remember { mutableStateOf<ParkingLot?>(null) }
    var isTwoWheelerSelected by remember { mutableStateOf(false) }
    var isRoutingLoading by remember { mutableStateOf(false) }
    var is3DCameraMode by remember { mutableStateOf(false) }

    // Location permissions
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Stable GPS polling: updates when moving (5m threshold)
    DisposableEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                5000L
            ).apply {
                setMinUpdateIntervalMillis(3000L)
                setMinUpdateDistanceMeters(5.0f)
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
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        val initLatLng = LatLng(it.latitude, it.longitude)
                        userLocation = initLatLng
                    }
                }
            } catch (_: SecurityException) {}

            onDispose {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } else {
            onDispose { }
        }
    }

    // Vehicle Selection Dialog
    pendingParkingForVehicle?.let { lot ->
        VehicleSelectionDialog(
            parkingLot = lot,
            onDismiss = { pendingParkingForVehicle = null },
            onVehicleSelected = { isTwoWheeler ->
                isTwoWheelerSelected = isTwoWheeler
                val targetLot = pendingParkingForVehicle ?: lot
                pendingParkingForVehicle = null

                val startPoint = userLocation ?: surajpoleLatLng
                val endPoint = LatLng(targetLot.latitude, targetLot.longitude)

                isRoutingLoading = true
                coroutineScope.launch {
                    val route = RoadRoutingService.getRoadRoute(
                        origin = startPoint,
                        destination = endPoint,
                        isTwoWheeler = isTwoWheeler
                    )
                    activeRoute = route
                    navigatingLot = targetLot
                    isRoutingLoading = false

                    // Auto-frame route smoothly
                    try {
                        val boundsBuilder = LatLngBounds.builder()
                        boundsBuilder.include(startPoint)
                        boundsBuilder.include(endPoint)
                        route.points.forEach { boundsBuilder.include(it) }
                        val bounds = boundsBuilder.build()

                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 140),
                            durationMs = 800
                        )
                    } catch (_: Exception) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(startPoint, 15.5f),
                            durationMs = 700
                        )
                    }
                }
            }
        )
    }

    // Clean, Full-Screen Map (Top bar removed per user request!)
    Box(modifier = Modifier.fillMaxSize()) {
        // Google Map View (High FPS with Native Vector Renderer)
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = currentMapType,
                isMyLocationEnabled = locationPermissionsState.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            )
        ) {
            val azureMarker = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) }
            val redMarker = remember { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) }

            // Rock-solid, non-blinking, high-performance parking pins across Udaipur
            parkingLots.forEach { lot ->
                val markerState = rememberMarkerState(
                    key = "lot_${lot.id}",
                    position = LatLng(lot.latitude, lot.longitude)
                )

                Marker(
                    state = markerState,
                    title = lot.name,
                    snippet = "${lot.availableSlots} / ${lot.total_slots} ${Strings.get("slots_available")}",
                    icon = if (lot.isFull) redMarker else azureMarker,
                    onClick = {
                        pendingParkingForVehicle = lot
                        true
                    }
                )
            }

            // Road-snapped Shortest Path (Magnet Effect)
            activeRoute?.let { route ->
                Polyline(
                    points = route.points,
                    color = Color(0xFF2563EB), // Vibrant Electric Royal Blue
                    width = 14f,
                    geodesic = true
                )
            }
        }

        // Floating Minimal Top Status Bar (Translucent glass pill, replaces heavy top app bar)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = com.surajpole.parkingfinder.R.drawable.app_logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("app_name"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (userLocation != null) EmeraldGreen else Color.Gray,
                    modifier = Modifier.size(7.dp)
                ) {}
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (userLocation != null) Strings.get("gps_live") else Strings.get("smart_tracking"),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (userLocation != null) EmeraldGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Floating Side Controls: Map Layer Toggle & Current Location FAB
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Profile Quick Shortcut
            FloatingActionButton(
                onClick = onNavigateToProfile,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = NavyPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Map Layer Toggle FAB: Only Normal <-> Hybrid as requested
            FloatingActionButton(
                onClick = {
                    currentMapType = if (currentMapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Map Layer",
                    tint = if (currentMapType == MapType.HYBRID) EmeraldGreen else NavyPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Recenter Live Location FAB
            FloatingActionButton(
                onClick = {
                    if (locationPermissionsState.allPermissionsGranted) {
                        val target = userLocation ?: surajpoleLatLng
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(target, 16.5f),
                                durationMs = 600
                            )
                        }
                    } else {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary,
                shape = CircleShape,
                modifier = Modifier.size(46.dp),
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = NavyPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Active Route Card with 3D Navigation Angles
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
                        is3DMode = is3DCameraMode,
                        onToggle3D = {
                            is3DCameraMode = !is3DCameraMode
                            coroutineScope.launch {
                                val current = userLocation ?: surajpoleLatLng
                                val target = LatLng(lot.latitude, lot.longitude)
                                val bearing = calculateBearing(current, target)

                                if (is3DCameraMode) {
                                    // 3D Driver Perspective view (55° tilt, destination-facing bearing)
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition(current, 17.5f, 55f, bearing)
                                        ),
                                        durationMs = 800
                                    )
                                } else {
                                    // 2D Top-Down View
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newCameraPosition(
                                            CameraPosition(current, 16f, 0f, 0f)
                                        ),
                                        durationMs = 700
                                    )
                                }
                            }
                        },
                        onClose = {
                            activeRoute = null
                            navigatingLot = null
                            is3DCameraMode = false
                        },
                        onProceedToDetail = {
                            onNavigateToDetail(lot.id)
                        }
                    )
                }
            }
        }

        // Nearby Parking Carousel (visible when no route active)
        AnimatedVisibility(
            visible = activeRoute == null && parkingLots.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(parkingLots) { lot ->
                    ParkingQuickCard(
                        lot = lot,
                        onClick = {
                            pendingParkingForVehicle = lot
                        }
                    )
                }
            }
        }

        // Route Calculation Spinner
        if (isRoutingLoading) {
            Surface(
                shape = RoundedCornerShape(14.dp),
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

@Composable
fun ActiveRouteCard(
    parkingLot: ParkingLot,
    route: RouteResult,
    isTwoWheeler: Boolean,
    is3DMode: Boolean,
    onToggle3D: () -> Unit,
    onClose: () -> Unit,
    onProceedToDetail: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
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
                        shape = RoundedCornerShape(10.dp),
                        color = (if (isTwoWheeler) EmeraldGreen else NavyPrimary).copy(alpha = 0.15f),
                        modifier = Modifier.size(38.dp)
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
                            text = if (isTwoWheeler) Strings.get("two_wheeler") else Strings.get("four_wheeler"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
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
                        Text("Slots", fontSize = 11.sp, color = Color.Gray)
                        Text("${parkingLot.availableSlots}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 3D / 2D Perspective Toggle Button as requested!
                    OutlinedButton(
                        onClick = onToggle3D,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (is3DMode) Icons.Default.Explore else Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (is3DMode) EmeraldGreen else NavyPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (is3DMode) Strings.get("navigate_2d") else Strings.get("navigate_3d"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (is3DMode) EmeraldGreen else NavyPrimary
                        )
                    }

                    // Book Slot CTA
                    Button(
                        onClick = onProceedToDetail,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(Strings.get("reserve_slot"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingQuickCard(
    lot: ParkingLot,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NavyPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalParking,
                        contentDescription = null,
                        tint = NavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = lot.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = Strings.get("slots_available"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${lot.availableSlots} / ${lot.total_slots}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lot.isFull) RedDanger else EmeraldGreen
                    )
                }

                Surface(
                    color = (if (lot.isFull) RedDanger else EmeraldGreen).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (lot.isFull) Strings.get("full") else Strings.get("start_navigation"),
                        color = if (lot.isFull) RedDanger else EmeraldGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Calculates bearing between two coordinates for dynamic 3D camera orientation
 */
private fun calculateBearing(start: LatLng, end: LatLng): Float {
    val lat1 = Math.toRadians(start.latitude)
    val lon1 = Math.toRadians(start.longitude)
    val lat2 = Math.toRadians(end.latitude)
    val lon2 = Math.toRadians(end.longitude)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

    var bearing = Math.toDegrees(atan2(y, x)).toFloat()
    if (bearing < 0) bearing += 360f
    return bearing
}
