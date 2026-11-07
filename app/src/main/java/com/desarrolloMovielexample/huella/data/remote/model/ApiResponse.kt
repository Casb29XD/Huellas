package com.desarrolloMovielexample.huella.data.remote.model

import com.google.gson.annotations.SerializedName

/**
 * Envoltorio unificado para todas las respuestas de la API.
 * Usado para endpoints propios del backend de Huella.
 * @param T Tipo de dato del payload alojado en [data].
 */
data class ApiResponse<T>(
    @SerializedName("code")
    val code: Int? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)
