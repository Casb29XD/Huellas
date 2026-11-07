package com.desarrolloMovielexample.huella.data.remote.dto

import com.google.gson.annotations.SerializedName

// ============================================================
// DTOs para PokéAPI — mapean la respuesta JSON a Kotlin
// Cada campo es nullable para evitar fallos por deserialización
// ============================================================

// ---------- GET /pokemon?limit=N&offset=N ----------

data class PokemonListResponse(
    @SerializedName("count")
    val count: Int? = null,

    @SerializedName("next")
    val next: String? = null,

    @SerializedName("previous")
    val previous: String? = null,

    @SerializedName("results")
    val results: List<PokemonListItem>? = null
)

data class PokemonListItem(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("url")
    val url: String? = null
) {
    /** Extrae el ID numérico de la URL: "https://pokeapi.co/api/v2/pokemon/25/" → 25 */
    val id: Int?
        get() = url?.trimEnd('/')?.substringAfterLast('/')?.toIntOrNull()
}

// ---------- GET /pokemon/{id} ----------

data class PokemonDetailResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("height")
    val height: Int? = null,

    @SerializedName("weight")
    val weight: Int? = null,

    @SerializedName("base_experience")
    val baseExperience: Int? = null,

    @SerializedName("sprites")
    val sprites: Sprites? = null,

    @SerializedName("types")
    val types: List<TypeSlot>? = null,

    @SerializedName("stats")
    val stats: List<StatSlot>? = null,

    @SerializedName("abilities")
    val abilities: List<AbilitySlot>? = null
)

data class Sprites(
    @SerializedName("front_default")
    val frontDefault: String? = null,

    @SerializedName("front_shiny")
    val frontShiny: String? = null,

    @SerializedName("other")
    val other: OtherSprites? = null
)

data class OtherSprites(
    @SerializedName("official-artwork")
    val officialArtwork: OfficialArtwork? = null,

    @SerializedName("home")
    val home: HomeSprites? = null
)

data class OfficialArtwork(
    @SerializedName("front_default")
    val frontDefault: String? = null,

    @SerializedName("front_shiny")
    val frontShiny: String? = null
)

data class HomeSprites(
    @SerializedName("front_default")
    val frontDefault: String? = null,

    @SerializedName("front_shiny")
    val frontShiny: String? = null
)

data class TypeSlot(
    @SerializedName("slot")
    val slot: Int? = null,

    @SerializedName("type")
    val type: NamedResource? = null
)

data class StatSlot(
    @SerializedName("base_stat")
    val baseStat: Int? = null,

    @SerializedName("effort")
    val effort: Int? = null,

    @SerializedName("stat")
    val stat: NamedResource? = null
)

data class AbilitySlot(
    @SerializedName("is_hidden")
    val isHidden: Boolean? = null,

    @SerializedName("slot")
    val slot: Int? = null,

    @SerializedName("ability")
    val ability: NamedResource? = null
)

/** Recurso genérico con nombre y URL (tipo, habilidad, stat, especie, color, hábitat, etc.) */
data class NamedResource(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("url")
    val url: String? = null
)

// ---------- GET /pokemon-species/{id} ----------

data class PokemonSpeciesResponse(
    @SerializedName("id")
    val id: Int? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("color")
    val color: NamedResource? = null,

    @SerializedName("habitat")
    val habitat: NamedResource? = null,

    @SerializedName("gender_rate")
    val genderRate: Int? = null,

    @SerializedName("capture_rate")
    val captureRate: Int? = null,

    @SerializedName("base_happiness")
    val baseHappiness: Int? = null,

    @SerializedName("is_legendary")
    val isLegendary: Boolean? = null,

    @SerializedName("is_mythical")
    val isMythical: Boolean? = null,

    @SerializedName("genera")
    val genera: List<Genus>? = null,

    @SerializedName("flavor_text_entries")
    val flavorTextEntries: List<FlavorTextEntry>? = null,

    @SerializedName("evolution_chain")
    val evolutionChain: NamedResource? = null
)

data class Genus(
    @SerializedName("genus")
    val genus: String? = null,

    @SerializedName("language")
    val language: NamedResource? = null
)

data class FlavorTextEntry(
    @SerializedName("flavor_text")
    val flavorText: String? = null,

    @SerializedName("language")
    val language: NamedResource? = null,

    @SerializedName("version")
    val version: NamedResource? = null
)
