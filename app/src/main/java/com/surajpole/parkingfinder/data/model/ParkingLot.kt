package com.surajpole.parkingfinder.data.model

import com.google.firebase.firestore.PropertyName

data class ParkingLot(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @get:PropertyName("total_slots")
    @set:PropertyName("total_slots")
    var total_slots: Long = 0L,
    @get:PropertyName("filled_slots")
    @set:PropertyName("filled_slots")
    var filled_slots: Long = 0L
) {
    val availableSlots: Long
        get() = (total_slots - filled_slots).coerceAtLeast(0L)

    val isFull: Boolean
        get() = availableSlots <= 0L
}
