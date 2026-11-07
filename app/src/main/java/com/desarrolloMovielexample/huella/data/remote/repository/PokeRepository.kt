package com.desarrolloMovielexample.huella.data.remote.repository

import com.desarrolloMovielexample.huella.data.remote.api.PokeApiService
import com.desarrolloMovielexample.huella.data.remote.RetrofitClient
import com.desarrolloMovielexample.huella.data.remote.dto.PokemonDetailResponse
import com.desarrolloMovielexample.huella.data.remote.dto.PokemonSpeciesResponse
import com.desarrolloMovielexample.huella.data.remote.mapper.PokemonMapper
import com.desarrolloMovielexample.huella.data.remote.network.NetworkResult
import com.desarrolloMovielexample.huella.data.remote.network.safeRawApiCall
import com.desarrolloMovielexample.huella.domain.model.Publication

/**
 * Repositorio que consume PokéAPI para obtener publicaciones con imágenes reales.
 *
 * Flujo:
 * 1. Obtiene la lista paginada de Pokémon.
 * 2. Para cada uno, obtiene el detalle (sprites/imágenes) y la especie (descripción).
 * 3. Mapea ambos a [Publication] usando [PokemonMapper].
 */
class PokeRepository(
    private val api: PokeApiService = RetrofitClient.pokeApiService
) {

    /**
     * Obtiene una lista de publicaciones con imágenes reales desde PokéAPI.
     * @param limit Cantidad de elementos por página.
     * @param offset Desplazamiento para paginación.
     * @return [Result] con la lista de publicaciones o error.
     */
    suspend fun getPublicationsFromPokemon(
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<Publication>> {
        // 1. Obtener lista paginada
        val listResult = safeRawApiCall { api.getPokemonList(limit, offset) }

        return when (listResult) {
            is NetworkResult.Success -> {
                val items = listResult.data?.results ?: emptyList()
                val publications = items.mapNotNull { item ->
                    val pokemonId = item.id ?: return@mapNotNull null

                    // 2. Obtener detalle y especie en paralelo (secuencial por simplicidad)
                    val detail = fetchDetail(pokemonId.toString())
                    val species = fetchSpecies(pokemonId)

                    if (detail != null) {
                        // 3. Mapear a Publication
                        PokemonMapper.toPublication(detail, species)
                    } else null
                }
                Result.success(publications)
            }
            is NetworkResult.Error -> {
                Result.failure(Exception("Error ${listResult.code}: ${listResult.message}"))
            }
            is NetworkResult.Exception -> {
                Result.failure(listResult.throwable)
            }
        }
    }

    /**
     * Obtiene el detalle de un Pokémon específico (incluye imágenes).
     * @param idOrName ID numérico o nombre del Pokémon.
     * @return [Result] con la publicación mapeada o error.
     */
    suspend fun getPublicationDetail(idOrName: String): Result<Publication> {
        val detailResult = safeRawApiCall { api.getPokemonDetail(idOrName) }

        return when (detailResult) {
            is NetworkResult.Success -> {
                val detail = detailResult.data
                if (detail != null) {
                    val species = fetchSpecies(detail.id ?: 0)
                    Result.success(PokemonMapper.toPublication(detail, species))
                } else {
                    Result.failure(Exception("No se encontró el Pokémon"))
                }
            }
            is NetworkResult.Error -> {
                Result.failure(Exception("Error ${detailResult.code}: ${detailResult.message}"))
            }
            is NetworkResult.Exception -> {
                Result.failure(detailResult.throwable)
            }
        }
    }

    /** Helper: obtiene el detalle de un Pokémon o null si falla. */
    private suspend fun fetchDetail(idOrName: String): PokemonDetailResponse? {
        val result = safeRawApiCall { api.getPokemonDetail(idOrName) }
        return (result as? NetworkResult.Success)?.data
    }

    /** Helper: obtiene la especie de un Pokémon o null si falla. */
    private suspend fun fetchSpecies(id: Int): PokemonSpeciesResponse? {
        if (id <= 0) return null
        val result = safeRawApiCall { api.getPokemonSpecies(id) }
        return (result as? NetworkResult.Success)?.data
    }
}
