package com.lorenzocalifano.shieldup.ui.followme

data class FollowMeSessionDto(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val destination: String = "",

    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    val active: Boolean = true,

    val startedAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)