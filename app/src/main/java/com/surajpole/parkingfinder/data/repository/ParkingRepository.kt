package com.surajpole.parkingfinder.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.surajpole.parkingfinder.data.model.Booking
import com.surajpole.parkingfinder.data.model.ParkingLot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ParkingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        val DEFAULT_UDAIPUR_LOTS = listOf(
            ParkingLot(
                id = "sp_main",
                name = "Surajpole Main Circle Parking",
                latitude = 24.5804,
                longitude = 73.6967,
                total_slots = 40,
                filled_slots = 18
            ),
            ParkingLot(
                id = "sp_bapu_bazar",
                name = "Bapu Bazar Entry Parking",
                latitude = 24.5818,
                longitude = 73.6945,
                total_slots = 25,
                filled_slots = 21
            ),
            ParkingLot(
                id = "sp_delhigate",
                name = "Delhi Gate Connector Parking",
                latitude = 24.5850,
                longitude = 73.6922,
                total_slots = 50,
                filled_slots = 12
            ),
            ParkingLot(
                id = "sp_railway_link",
                name = "Station Road Multilevel Parking",
                latitude = 24.5765,
                longitude = 73.7011,
                total_slots = 80,
                filled_slots = 35
            ),
            ParkingLot(
                id = "sp_hathipole",
                name = "Hathipole Market Parking",
                latitude = 24.5880,
                longitude = 73.6885,
                total_slots = 45,
                filled_slots = 28
            ),
            ParkingLot(
                id = "sp_chetak_circle",
                name = "Chetak Circle Public Parking",
                latitude = 24.5930,
                longitude = 73.6845,
                total_slots = 60,
                filled_slots = 15
            ),
            ParkingLot(
                id = "sp_town_hall",
                name = "Town Hall Link Parking",
                latitude = 24.5833,
                longitude = 73.6938,
                total_slots = 35,
                filled_slots = 10
            )
        )
    }

    private val parkingCollection = firestore.collection("parking_lots")
    private val bookingsCollection = firestore.collection("bookings")

    // Real-time flow of all parking lots (instant cached emission for high-FPS launch)
    fun observeParkingLots(): Flow<List<ParkingLot>> = callbackFlow {
        trySend(DEFAULT_UDAIPUR_LOTS)
        val listener = parkingCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val lots = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ParkingLot::class.java)?.copy(id = doc.id)
                }
                if (lots.isNotEmpty()) {
                    trySend(lots)
                }
            }
        }
        awaitClose { listener.remove() }
    }

    // Real-time flow of a single parking lot
    fun observeParkingLot(lotId: String): Flow<ParkingLot?> = callbackFlow {
        val fallback = DEFAULT_UDAIPUR_LOTS.find { it.id == lotId }
        if (fallback != null) {
            trySend(fallback)
        }
        val listener = parkingCollection.document(lotId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            val lot = snapshot?.toObject(ParkingLot::class.java)?.copy(id = snapshot.id)
            if (lot != null) {
                trySend(lot)
            }
        }
        awaitClose { listener.remove() }
    }

    // Real-time flow of user bookings
    fun observeUserBookings(userId: String): Flow<List<Booking>> = callbackFlow {
        val listener = bookingsCollection
            .whereEqualTo("user_id", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val bookings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Booking::class.java)?.copy(id = doc.id)
                    }.sortedByDescending { it.booked_at }
                    trySend(bookings)
                }
            }
        awaitClose { listener.remove() }
    }

    // Create a 15-minute booking with atomic slot decrement/increment
    suspend fun createBooking(
        lot: ParkingLot,
        userId: String,
        carNumber: String
    ): Result<Booking> {
        return try {
            val bookingDocRef = bookingsCollection.document()
            val now = System.currentTimeMillis()
            val expiresAt = now + 15 * 60 * 1000L // 15 minutes

            val newBooking = Booking(
                id = bookingDocRef.id,
                user_id = userId,
                parking_id = lot.id,
                parking_name = lot.name,
                car_number = carNumber.trim().uppercase(),
                booked_at = now,
                expires_at = expiresAt,
                status = "active"
            )

            // Run atomic transaction to ensure slots don't oversell
            firestore.runTransaction { transaction ->
                val lotRef = parkingCollection.document(lot.id)
                val lotSnap = transaction.get(lotRef)
                val currentFilled = lotSnap.getLong("filled_slots") ?: 0L
                val total = lotSnap.getLong("total_slots") ?: 0L

                if (currentFilled >= total) {
                    throw IllegalStateException("Parking lot is full!")
                }

                transaction.update(lotRef, "filled_slots", currentFilled + 1)
                transaction.set(bookingDocRef, newBooking)
            }.await()

            Result.success(newBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Auto-cancel or user cancel booking
    suspend fun cancelBooking(bookingId: String, lotId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingRef = bookingsCollection.document(bookingId)
                val bookingSnap = transaction.get(bookingRef)
                val currentStatus = bookingSnap.getString("status") ?: ""

                // Only cancel if active
                if (currentStatus == "active") {
                    transaction.update(bookingRef, "status", "cancelled")
                    if (lotId.isNotEmpty()) {
                        val lotRef = parkingCollection.document(lotId)
                        val lotSnap = transaction.get(lotRef)
                        val currentFilled = lotSnap.getLong("filled_slots") ?: 1L
                        if (currentFilled > 0) {
                            transaction.update(lotRef, "filled_slots", currentFilled - 1)
                        }
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper to seed initial Surajpole parking lots if collection is empty
    suspend fun seedInitialDataIfEmpty() {
        try {
            val snapshot = parkingCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                val samples = listOf(
                    ParkingLot(
                        id = "sp_main",
                        name = "Surajpole Main Circle Parking",
                        latitude = 24.5804,
                        longitude = 73.6967,
                        total_slots = 40,
                        filled_slots = 18
                    ),
                    ParkingLot(
                        id = "sp_bapu_bazar",
                        name = "Bapu Bazar Entry Parking",
                        latitude = 24.5818,
                        longitude = 73.6945,
                        total_slots = 25,
                        filled_slots = 21
                    ),
                    ParkingLot(
                        id = "sp_delhigate",
                        name = "Delhi Gate Connector Parking",
                        latitude = 24.5850,
                        longitude = 73.6922,
                        total_slots = 50,
                        filled_slots = 12
                    ),
                    ParkingLot(
                        id = "sp_railway_link",
                        name = "Station Road Multilevel Parking",
                        latitude = 24.5765,
                        longitude = 73.7011,
                        total_slots = 80,
                        filled_slots = 35
                    ),
                    ParkingLot(
                        id = "sp_hathipole",
                        name = "Hathipole Market Parking",
                        latitude = 24.5880,
                        longitude = 73.6885,
                        total_slots = 45,
                        filled_slots = 28
                    ),
                    ParkingLot(
                        id = "sp_chetak_circle",
                        name = "Chetak Circle Public Parking",
                        latitude = 24.5930,
                        longitude = 73.6845,
                        total_slots = 60,
                        filled_slots = 15
                    ),
                    ParkingLot(
                        id = "sp_town_hall",
                        name = "Town Hall Link Parking",
                        latitude = 24.5833,
                        longitude = 73.6938,
                        total_slots = 35,
                        filled_slots = 10
                    )
                )

                for (lot in samples) {
                    parkingCollection.document(lot.id).set(lot).await()
                }
            }
        } catch (e: Exception) {
            // Silently ignore if offline or waiting for Firestore rules
        }
    }
}
