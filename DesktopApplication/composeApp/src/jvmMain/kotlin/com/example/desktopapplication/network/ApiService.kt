package com.example.desktopapplication.network

import com.example.desktopapplication.models.PropertyIngestDto
import com.example.desktopapplication.models.PropertyResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PropertyApiService {

    @GET("properties")
    suspend fun list(): List<PropertyResponseDto>

    @POST("properties/ingest")
    suspend fun create(@Body property: PropertyIngestDto): PropertyResponseDto

    @PUT("properties/ingest/{id}")
    suspend fun update(@Path("id") id: String, @Body property: PropertyIngestDto): PropertyResponseDto

    @DELETE("properties/ingest/{id}")
    suspend fun delete(@Path("id") id: String): Response<Unit>
}
