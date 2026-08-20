package com.shahrafuking.kingassistant.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class BackendResultDto(
    val title: String? = null,
    val snippet: String? = null,
    val link: String? = null,
    val url: String? = null,
    val paragraphs: List<String>? = null,
    val source: String? = null
)

data class BackendSearchResponse(
    val provider: String? = null,
    val q: String? = null,
    val results: List<BackendResultDto> = emptyList()
)

data class BackendFetchResponse(
    val url: String,
    val parsed: BackendResultDto
)

interface BackendApi {
    @GET("api/v1/search")
    suspend fun search(
        @Query("provider") provider: String,
        @Query("q") q: String,
        @Query("feed") feed: String? = null,
        @Query("url") url: String? = null
    ): BackendSearchResponse

    @GET("api/v1/fetch")
    suspend fun fetch(
        @Query("url") url: String
    ): BackendFetchResponse
}
