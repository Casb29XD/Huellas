package com.desarrolloMovielexample.huella.data.remote.api

import com.desarrolloMovielexample.huella.data.remote.dto.PokemonDetailResponse
import com.desarrolloMovielexample.huella.data.remote.dto.PokemonListResponse
import com.desarrolloMovielexample.huella.data.remote.dto.PokemonSpeciesResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Interfaz Retrofit para consumir PokéAPI (https://pokeapi.co/api/v2/).
 *
 * Se usa como fuente de datos real para obtener "animales" con imágenes HD.
 * Las imágenes provienen de sprites.other.official-artwork.front_default.
 */
interface PokeApiService {

    /**
     * Lista paginada de Pokémon (usada para el feed de publicaciones).
     * Ejemplo: GET pokemon?limit=20&offset=0
     */
    @GET("pokemon")
    suspend fun getPokemonList(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<PokemonListResponse>

    /**
     * Detalle completo de un Pokémon por ID o nombre.
     * Incluye sprites (imágenes), tipos, stats, habilidades, peso y altura.
     * Ejemplo: GET pokemon/25 → Pikachu
     */
    @GET("pokemon/{idOrName}")
    suspend fun getPokemonDetail(
        @Path("idOrName") idOrName: String
    ): Response<PokemonDetailResponse>

    /**
     * Información de especie: descripción (flavor text), color, hábitat,
     * género, tasa de captura, si es legendario/mítico.
     * Ejemplo: GET pokemon-species/25
     */
    @GET("pokemon-species/{id}")
    suspend fun getPokemonSpecies(
        @Path("id") id: Int
    ): Response<PokemonSpeciesResponse>
}
