package com.template

import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

object RetrofitService {
    private var retrofit: Retrofit? = null

    fun getClient(baseUrl: String, userAgent: String): Retrofit {
        if (retrofit == null) {
            val okHttpClient = OkHttpClient
                .Builder()
                .addInterceptor(UserAgentInterceptor(userAgent))
                .build()
            retrofit = Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(baseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}

interface RetrofitClient {
    @GET(".")
    fun getUrl(
        @Query("packageid") packageName: String,
        @Query("usserid") userId: String,
        @Query("getz") timeZone: String,
        @Query("getr") getr: String = "utm_source=google-play&utm_medium=organic"
    ): Call<String>
}
