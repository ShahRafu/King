package com.shahrafuking.kingassistant.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitFactory {
    fun create(baseUrl: String, bearerToken: String?): BackendApi {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val builder: Request.Builder = original.newBuilder()
            if (!bearerToken.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $bearerToken")
            }
            builder.header("Accept", "application/json")
            chain.proceed(builder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        val retrofit = Retrofit.Builder()
            .baseUrl(url)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(BackendApi::class.java)
    }
}
