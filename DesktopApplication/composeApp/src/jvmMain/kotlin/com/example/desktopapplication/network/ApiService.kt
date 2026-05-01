package com.example.desktopapplication.network

import com.example.desktopapplication.models.Nepremicnina
import retrofit2.http.*


interface NepremicnineApiService {

    @GET("nepremicnine")
    suspend fun getAll(): List<Nepremicnina>

    @POST("nepremicnine")
    suspend fun create(@Body nepremicnina: Nepremicnina): Nepremicnina

    @PUT("nepremicnine/{id}")
    suspend fun update(@Path("id") id: Int, @Body nepremicnina: Nepremicnina): Nepremicnina

    @DELETE("nepremicnine/{id}")
    suspend fun delete(@Path("id") id: Int): retrofit2.Response<Unit>
}