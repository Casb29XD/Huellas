package com.desarrolloMovielexample.huella.data.remote.mapper

import com.desarrolloMovielexample.huella.data.remote.dto.PokemonDetailResponse
import com.desarrolloMovielexample.huella.data.remote.dto.PokemonSpeciesResponse
import com.desarrolloMovielexample.huella.domain.model.Author
import com.desarrolloMovielexample.huella.domain.model.Category
import com.desarrolloMovielexample.huella.domain.model.Level
import com.desarrolloMovielexample.huella.domain.model.PostStatus
import com.desarrolloMovielexample.huella.domain.model.Publication

/**
 * Mapea las respuestas de PokéAPI a los modelos de dominio de Huella.
 *
 * Convenciones de mapeo:
 * - Imagen: sprites → other → official-artwork → front_default (alta resolución)
 * - Especie: tipos de Pokémon → especie animal ("fire" → "Perro", "water" → "Gato", etc.)
 * - Tamaño: basado en height (< 7 → Pequeño, < 15 → Mediano, ≥ 15 → Grande)
 * - Sexo: basado en gender_rate (-1 → Desconocido, 0 → Macho, 8 → Hembra, otro → Macho/Hembra)
 * - Descripción: flavor_text_entries filtrado por idioma español, fallback a inglés
 */
object PokemonMapper {

    /** Colores base de tono para las tarjetas (ARGB). */
    private val toneColors = listOf(
        0xFFD8E8DD, 0xFFE8DAD6, 0xFFD8E0EC, 0xFFE4DCEC,
        0xFFD6ECE8, 0xFFE7E1D3, 0xFFEEE3D2, 0xFFEFE0CF
    )

    /** Ciudades aleatorias de Colombia para asignar a publicaciones. */
    private val cities = listOf(
        "Medellín", "Bogotá", "Cali", "Barranquilla",
        "Bucaramanga", "Pereira", "Manizales", "Cartagena"
    )

    /** Autores ficticios para poblar el feed. */
    private val fakeAuthors = listOf(
        Author("u_camila", "Camila Restrepo", Level.PROTECTOR, "CR"),
        Author("u_andres", "Andrés Gómez", Level.AMIGO_ANIMAL, "AG"),
        Author("u_valentina", "Valentina Ruiz", Level.GUARDIAN, "VR"),
        Author("u_juanpablo", "Juan Pablo Mora", Level.HEROE, "JM"),
        Author("u_patitas", "Fundación Patitas", Level.HEROE, "FP"),
        Author("u_refugio", "Refugio Segunda Oportunidad", Level.GUARDIAN, "RS"),
        Author("u_sofia", "Sofía Arango", Level.AMIGO_ANIMAL, "SA"),
        Author("u_mateo", "Mateo Zapata", Level.PROTECTOR, "MZ"),
    )

    /**
     * Convierte un detalle de Pokémon + especie en una [Publication] del dominio de Huella.
     */
    fun toPublication(
        detail: PokemonDetailResponse,
        species: PokemonSpeciesResponse? = null
    ): Publication {
        val pokemonId = detail.id ?: 0
        val name = detail.name?.replaceFirstChar { it.uppercase() } ?: "Desconocido"

        // Imagen: prioridad official-artwork > home > front_default
        val imageUrl = detail.sprites?.other?.officialArtwork?.frontDefault
            ?: detail.sprites?.other?.home?.frontDefault
            ?: detail.sprites?.frontDefault

        // Especie animal basada en el tipo principal del Pokémon
        val primaryType = detail.types?.firstOrNull()?.type?.name ?: "normal"
        val animalSpecies = mapTypeToSpecies(primaryType)

        // Tamaño basado en height (en decímetros)
        val height = detail.height ?: 5
        val size = when {
            height < 7 -> "Pequeño"
            height < 15 -> "Mediano"
            else -> "Grande"
        }

        // Sexo basado en gender_rate de la especie
        val sex = when (species?.genderRate) {
            -1 -> "Desconocido"
            0 -> "Macho"
            8 -> "Hembra"
            else -> if ((pokemonId % 2) == 0) "Hembra" else "Macho"
        }

        // Descripción en español, fallback a inglés
        val description = species?.flavorTextEntries
            ?.firstOrNull { it.language?.name == "es" }?.flavorText
            ?: species?.flavorTextEntries
                ?.firstOrNull { it.language?.name == "en" }?.flavorText
            ?: "Un compañero leal que busca un hogar lleno de amor."

        // Limpiar saltos de línea y caracteres de control del flavor text
        val cleanDescription = description
            .replace("\n", " ")
            .replace("\u000c", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        // Categoría cíclica para variedad en el feed
        val category = when (pokemonId % 5) {
            0 -> Category.ADOPCION
            1 -> Category.PERDIDOS
            2 -> Category.ENCONTRADOS
            3 -> Category.TEMPORAL
            else -> Category.VETERINARIA
        }

        // Vacunación basada en base_experience
        val vaccinated = (detail.baseExperience ?: 0) > 100

        // Raza aproximada usando la categoría de género (genus) de PokéAPI
        val breed = species?.genera
            ?.firstOrNull { it.language?.name == "es" }?.genus
            ?: species?.genera
                ?.firstOrNull { it.language?.name == "en" }?.genus
            ?: "Criollo"

        // Edad estimada basada en base_experience
        val ageYears = ((detail.baseExperience ?: 64) / 64).coerceIn(1, 10)
        val age = if (ageYears == 1) "1 año" else "$ageYears años"

        val city = cities[pokemonId % cities.size]
        val author = fakeAuthors[pokemonId % fakeAuthors.size]
        val tone = toneColors[pokemonId % toneColors.size]

        return Publication(
            id = "poke_$pokemonId",
            category = category,
            title = "$name busca un hogar",
            species = animalSpecies,
            breed = breed,
            size = size,
            vaccinated = vaccinated,
            sterilized = pokemonId % 3 == 0,
            age = age,
            sex = sex,
            city = city,
            neighborhood = "$city Centro",
            timeAgo = "hace ${(pokemonId % 12) + 1} h",
            description = cleanDescription,
            imageUrl = imageUrl,
            tone = tone,
            photoLabel = name,
            author = author,
            status = PostStatus.APROBADA,
        )
    }

    /**
     * Mapea el tipo principal de Pokémon a una especie animal para Huella.
     */
    private fun mapTypeToSpecies(type: String): String = when (type) {
        "fire", "fighting", "ground" -> "Perro"
        "water", "ice", "dark" -> "Gato"
        "flying", "fairy" -> "Ave"
        "grass", "bug" -> "Conejo"
        "electric", "steel" -> "Roedor"
        "normal", "poison", "rock", "ghost",
        "psychic", "dragon" -> "Perro"
        else -> "Otro"
    }
}
