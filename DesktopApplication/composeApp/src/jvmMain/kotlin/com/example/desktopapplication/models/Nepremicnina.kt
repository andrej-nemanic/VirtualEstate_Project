package com.example.desktopapplication.models

import kotlinx.serialization.Serializable

@Serializable
data class Nepremicnina(
    val id: Int? = null,
    val naslov: String,
    val kraj: String,
    val vrsta: String,
    val velikost: Double,
    val cena: Double,
    val letoIzgradnje: Int,
    val opis: String? = null
)