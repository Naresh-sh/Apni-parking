package com.surajpole.parkingfinder.data.model

import com.google.firebase.firestore.PropertyName

data class Booking(
    val id: String = "",
    @get:PropertyName("user_id")
    @set:PropertyName("user_id")
    var user_id: String = "",
    @get:PropertyName("parking_id")
    @set:PropertyName("parking_id")
    var parking_id: String = "",
    @get:PropertyName("parking_name")
    @set:PropertyName("parking_name")
    var parking_name: String = "",
    @get:PropertyName("car_number")
    @set:PropertyName("car_number")
    var car_number: String = "",
    @get:PropertyName("booked_at")
    @set:PropertyName("booked_at")
    var booked_at: Long = 0L,
    @get:PropertyName("expires_at")
    @set:PropertyName("expires_at")
    var expires_at: Long = 0L,
    val status: String = "active" // "active", "completed", "cancelled"
)
