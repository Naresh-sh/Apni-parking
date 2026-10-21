package com.surajpole.parkingfinder.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.surajpole.parkingfinder.data.model.Booking
import com.surajpole.parkingfinder.data.model.ParkingLot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class ParkingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        val DEFAULT_UDAIPUR_LOTS = listOf(
            // City Center & Heritage Markets
            ParkingLot(
                id = "sp_surajpole",
                name = "Surajpole Circle Multi-Level Parking",
                latitude = 24.5804,
                longitude = 73.6967,
                total_slots = 60,
                filled_slots = 24
            ),
            ParkingLot(
                id = "sp_bapu_bazar",
                name = "Bapu Bazar Central Market Parking",
                latitude = 24.5818,
                longitude = 73.6945,
                total_slots = 45,
                filled_slots = 38
            ),
            ParkingLot(
                id = "sp_delhi_gate",
                name = "Delhi Gate Multi-Level Smart Parking",
                latitude = 24.5850,
                longitude = 73.6922,
                total_slots = 80,
                filled_slots = 32
            ),
            ParkingLot(
                id = "sp_hathipole",
                name = "Hathipole Artisan Market Parking",
                latitude = 24.5880,
                longitude = 73.6885,
                total_slots = 50,
                filled_slots = 29
            ),
            ParkingLot(
                id = "sp_town_hall",
                name = "Town Hall & Nagar Nigam Complex",
                latitude = 24.5833,
                longitude = 73.6938,
                total_slots = 70,
                filled_slots = 22
            ),
            ParkingLot(
                id = "sp_chetak",
                name = "Chetak Circle Public Parking",
                latitude = 24.5930,
                longitude = 73.6845,
                total_slots = 55,
                filled_slots = 19
            ),
            ParkingLot(
                id = "sp_court_circle",
                name = "Court Circle Collectorate Parking",
                latitude = 24.5872,
                longitude = 73.6955,
                total_slots = 65,
                filled_slots = 41
            ),
            
            // Heritage & Tourist Landmarks
            ParkingLot(
                id = "sp_city_palace",
                name = "City Palace (Badi Pol) Royal Visitor Parking",
                latitude = 24.5762,
                longitude = 73.6842,
                total_slots = 120,
                filled_slots = 78
            ),
            ParkingLot(
                id = "sp_jagdish_chowk",
                name = "Jagdish Chowk & Gangaur Ghat Parking",
                latitude = 24.5796,
                longitude = 73.6828,
                total_slots = 35,
                filled_slots = 30
            ),
            ParkingLot(
                id = "sp_dudh_talai",
                name = "Dudh Talai & Ropeway Base Parking",
                latitude = 24.5702,
                longitude = 73.6835,
                total_slots = 85,
                filled_slots = 44
            ),
            ParkingLot(
                id = "sp_gulab_bagh",
                name = "Gulab Bagh & Zoo North Gate Parking",
                latitude = 24.5748,
                longitude = 73.6942,
                total_slots = 50,
                filled_slots = 18
            ),
            ParkingLot(
                id = "sp_moti_magri",
                name = "Moti Magri & Maharana Pratap Memorial",
                latitude = 24.5985,
                longitude = 73.6812,
                total_slots = 70,
                filled_slots = 25
            ),
            ParkingLot(
                id = "sp_saheliyon_bari",
                name = "Saheliyon Ki Bari Tourist Parking",
                latitude = 24.6042,
                longitude = 73.6875,
                total_slots = 90,
                filled_slots = 49
            ),
            ParkingLot(
                id = "sp_sukhadia_circle",
                name = "Sukhadia Circle Chowpatty Parking",
                latitude = 24.6025,
                longitude = 73.6920,
                total_slots = 75,
                filled_slots = 36
            ),

            // Lakes & Scenic Hubs
            ParkingLot(
                id = "sp_fs_paal",
                name = "Fateh Sagar Paal Lakeside Parking",
                latitude = 24.5995,
                longitude = 73.6738,
                total_slots = 110,
                filled_slots = 82
            ),
            ParkingLot(
                id = "sp_fs_rani_road",
                name = "Rani Road Lakeview Sunset Parking",
                latitude = 24.6048,
                longitude = 73.6652,
                total_slots = 65,
                filled_slots = 28
            ),
            ParkingLot(
                id = "sp_sajjangarh_base",
                name = "Sajjangarh Biological Park Base Parking",
                latitude = 24.5935,
                longitude = 73.6420,
                total_slots = 95,
                filled_slots = 35
            ),

            // Transit, Hospitals & Institutions
            ParkingLot(
                id = "sp_railway_station",
                name = "Udaipur City Railway Station Main Gate",
                latitude = 24.5765,
                longitude = 73.7011,
                total_slots = 130,
                filled_slots = 88
            ),
            ParkingLot(
                id = "sp_ranapratap_station",
                name = "Rana Pratap Nagar Railway Station Parking",
                latitude = 24.5912,
                longitude = 73.7225,
                total_slots = 60,
                filled_slots = 21
            ),
            ParkingLot(
                id = "sp_mb_hospital",
                name = "MB Hospital (RNT College) Emergency Parking",
                latitude = 24.5898,
                longitude = 73.6958,
                total_slots = 100,
                filled_slots = 85
            ),

            // Shopping Malls & Suburbs
            ParkingLot(
                id = "sp_celebration_mall",
                name = "Nexus Celebration Mall Multi-Level",
                latitude = 24.6225,
                longitude = 73.7115,
                total_slots = 180,
                filled_slots = 92
            ),
            ParkingLot(
                id = "sp_urban_square",
                name = "Urban Square Mall Multi-Level Parking",
                latitude = 24.6360,
                longitude = 73.7250,
                total_slots = 250,
                filled_slots = 115
            ),
            ParkingLot(
                id = "sp_lake_city_mall",
                name = "Lake City Mall Parking (Durga Nursery)",
                latitude = 24.5905,
                longitude = 73.7050,
                total_slots = 90,
                filled_slots = 42
            ),
            ParkingLot(
                id = "sp_hiran_magri_sec4",
                name = "Hiran Magri Sector 4 Commercial Hub",
                latitude = 24.5720,
                longitude = 73.7180,
                total_slots = 50,
                filled_slots = 15
            ),
            ParkingLot(
                id = "sp_subhash_nagar",
                name = "Sevashram Bridge & Subhash Nagar Parking",
                latitude = 24.5775,
                longitude = 73.7150,
                total_slots = 40,
                filled_slots = 12
            ),
            ParkingLot(
                id = "sp_ahar_cenotaphs",
                name = "Ahar Cenotaphs Heritage Museum Parking",
                latitude = 24.5840,
                longitude = 73.7190,
                total_slots = 35,
                filled_slots = 8
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
    }.distinctUntilChanged()

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

    // Helper to seed initial Udaipur parking lots if collection is empty
    suspend fun seedInitialDataIfEmpty() {
        try {
            val snapshot = parkingCollection.limit(1).get().await()
            if (snapshot.isEmpty) {
                for (lot in DEFAULT_UDAIPUR_LOTS) {
                    parkingCollection.document(lot.id).set(lot).await()
                }
            }
        } catch (e: Exception) {
            // Silently ignore if offline or waiting for Firestore rules
        }
    }
}
