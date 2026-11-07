package com.desarrolloMovielexample.huella.data.remote.network

/**
 * Catálogo de códigos de respuesta HTTP y de negocio estándar.
 */
enum class ApiStatusCode(val code: Int, val description: String) {
    OK(200, "Operación exitosa"),
    CREATED(201, "Recurso creado satisfactoriamente"),
    ACCEPTED(202, "Solicitud aceptada para procesamiento"),
    NO_CONTENT(204, "Petición procesada sin contenido de retorno"),
    BAD_REQUEST(400, "Petición inválida o parámetros incorrectos"),
    UNAUTHORIZED(401, "Credenciales inválidas o sesión expirada"),
    FORBIDDEN(403, "No tiene permisos para acceder a este recurso"),
    NOT_FOUND(404, "Recurso no encontrado"),
    CONFLICT(409, "Conflicto con el estado actual del recurso"),
    UNPROCESSABLE_ENTITY(422, "Errores de validación en los campos enviados"),
    INTERNAL_SERVER_ERROR(500, "Error interno en el servidor"),
    SERVICE_UNAVAILABLE(503, "Servicio temporalmente fuera de línea"),
    UNKNOWN(-1, "Código de respuesta no reconocido");

    companion object {
        fun fromCode(code: Int): ApiStatusCode =
            entries.find { it.code == code } ?: UNKNOWN
    }
}

/**
 * Representación del resultado de una llamada de red procesada.
 * Desacopla las respuestas HTTP de la capa de Dominio/ViewModel.
 */
sealed class NetworkResult<out T> {
    data class Success<out T>(
        val data: T?,
        val message: String? = null,
        val code: Int = 200
    ) : NetworkResult<T>()

    data class Error(
        val code: Int,
        val message: String,
        val errorBody: String? = null
    ) : NetworkResult<Nothing>()

    data class Exception(
        val throwable: Throwable
    ) : NetworkResult<Nothing>()
}
