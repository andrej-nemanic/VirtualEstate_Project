package com.example.desktopapplication.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private val BASE_URL: String = System.getenv("VIRTUALESTATE_API_URL")
        ?.takeIf { it.isNotBlank() }
        ?.let { if (it.endsWith("/")) it else "$it/" }
        ?: "http://localhost:3000/api/"

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val properties: PropertyApiService = retrofit.create(PropertyApiService::class.java)
    val users: UserApiService = retrofit.create(UserApiService::class.java)
}
