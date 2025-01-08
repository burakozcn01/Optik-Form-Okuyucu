package com.reloading.optik_form.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * API istemcisi tekil nesnesi. Retrofit ve OkHttpClient yapılandırmalarını içerir.
 */
object ApiClient {
    // API'nin temel URL'si
    private const val BASE_URL = "http://3.74.41.200/api/"

    // HTTP istek ve yanıtlarını loglamak için kullanılan interceptor
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttpClient yapılandırması
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging) // Loglama interceptor'ını ekler
        .connectTimeout(30, TimeUnit.SECONDS) // Bağlantı zaman aşımı süresi
        .readTimeout(60, TimeUnit.SECONDS) // Okuma zaman aşımı süresi
        .writeTimeout(60, TimeUnit.SECONDS) // Yazma zaman aşımı süresi
        .build()

    // Moshi JSON dönüştürücüsü
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory()) // Kotlin için uyumlu adapter
        .build()

    // Retrofit yapılandırması
    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL) // Temel URL
        .addConverterFactory(MoshiConverterFactory.create(moshi)) // JSON dönüştürücü
        .client(client) // HTTP istemcisi
        .build()

    // Servis arayüzlerinin oluşturulması
    val answerKeysService: AnswerKeysService = retrofit.create(AnswerKeysService::class.java)
    val columnMappingsService: ColumnMappingsService = retrofit.create(ColumnMappingsService::class.java)
    val coursesService: CoursesService = retrofit.create(CoursesService::class.java)
    val processService: ProcessService = retrofit.create(ProcessService::class.java)
    val studentAnswersService: StudentAnswersService = retrofit.create(StudentAnswersService::class.java)
    val studentsService: StudentsService = retrofit.create(StudentsService::class.java)
}
