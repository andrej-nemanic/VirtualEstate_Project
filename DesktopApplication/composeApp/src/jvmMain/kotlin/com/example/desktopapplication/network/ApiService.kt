package com.example.desktopapplication.network

import com.example.desktopapplication.models.Property
import retrofit2.http.*


interface NepremicnineApiService {

    @GET("nepremicnine")
    suspend fun getAll(): List<Property>

    @POST("nepremicnine")
    suspend fun create(@Body nepremicnina: Property): Property

    @PUT("nepremicnine/{id}")
    suspend fun update(@Path("id") id: Int, @Body nepremicnina: Property): Property

    @DELETE("nepremicnine/{id}")
    suspend fun delete(@Path("id") id: Int): retrofit2.Response<Unit>
}