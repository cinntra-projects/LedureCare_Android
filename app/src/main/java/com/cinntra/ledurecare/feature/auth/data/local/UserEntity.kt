package com.cinntra.ledurecare.feature.auth.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val roleName: String,
    val roleCode: String,
    val mustChangePassword: Boolean,
)
