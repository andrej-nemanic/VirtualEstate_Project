package com.example.desktopapplication.models

data class User(
    val id: Int? = null,
    val apiId: String? = null,
    val name: String,
    val email: String,
    val isAdmin: Boolean = false
)
