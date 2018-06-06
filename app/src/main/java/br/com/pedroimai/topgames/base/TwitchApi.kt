package br.com.pedroimai.topgames.base

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

private const val BASE_URL = "https://api.twitch.tv/kraken/"


interface TwitchApi {
    @GET("getGameList/top")
    @Headers("Client-ID: fcrz6ui3e7k402mzr11b3224tgjx7p")
    fun getTopGames(): Call<GameListResponse>
}

fun createTwitchApi(): TwitchApi =
     Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(TwitchApi::class.java)



data class GamePayload(
    @SerializedName("_id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("box") val images: GameImagesPayload
)

data class GameImagesPayload(
    @SerializedName("small") val thumb: String,
    @SerializedName("large") val large: String
)

data class GameListResponse(
    @SerializedName("top") val games: List<GamePayload>
)
