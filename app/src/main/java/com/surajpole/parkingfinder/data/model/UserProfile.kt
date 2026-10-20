package com.surajpole.parkingfinder.data.model

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val loginMethod: String = "email",
    val createdAt: Long = System.currentTimeMillis()
)
