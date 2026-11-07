package com.desarrolloMovielexample.huella.data.remote.network

import com.desarrolloMovielexample.huella.data.remote.model.ApiResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Ejecuta una llamada segura a Retrofit envuelta en [ApiResponse],
 * mapeando a [NetworkResult] con evaluación de isSuccessful, código y mensaje.
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> Response<ApiResponse<T>>
): NetworkResult<T> = withContext(dispatcher) {
    try {
        val response = apiCall()
        val httpCode = response.code()

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                val businessCode = body.code ?: httpCode
                val message = body.message ?: ApiStatusCode.fromCode(businessCode).description
                NetworkResult.Success(data = body.data, message = message, code = businessCode)
            } else {
                NetworkResult.Success(data = null, message = ApiStatusCode.fromCode(httpCode).description, code = httpCode)
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            val status = ApiStatusCode.fromCode(httpCode)
            NetworkResult.Error(code = httpCode, message = errorBodyString ?: status.description, errorBody = errorBodyString)
        }
    } catch (throwable: Throwable) {
        NetworkResult.Exception(throwable)
    }
}

/**
 * Variante simplificada para llamadas directas (sin envoltorio [ApiResponse]).
 * Ideal para consumir APIs externas como PokéAPI que retornan JSON plano.
 */
suspend fun <T> safeRawApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    apiCall: suspend () -> Response<T>
): NetworkResult<T> = withContext(dispatcher) {
    try {
        val response = apiCall()
        if (response.isSuccessful) {
            NetworkResult.Success(data = response.body(), code = response.code())
        } else {
            val errorBodyString = response.errorBody()?.string()
            val status = ApiStatusCode.fromCode(response.code())
            NetworkResult.Error(code = response.code(), message = errorBodyString ?: status.description, errorBody = errorBodyString)
        }
    } catch (throwable: Throwable) {
        NetworkResult.Exception(throwable)
    }
}
