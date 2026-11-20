package com.cinntra.ledurecare.feature.auth.domain

data class User(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val roleName: String = "",
    val roleCode: String = "",
    val mustChangePassword: Boolean = false,
)
