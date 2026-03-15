package com.fourdigital.marketintelligence.core.common.result

/**
 * A sealed class representing the result of an operation.
 * Provides a clean way to handle success, error, and loading states
 * throughout the application.
 */
sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val code: ErrorCode = ErrorCode.UNKNOWN
    ) : DataResult<Nothing>()

    data object Loading : DataResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun <R> map(transform: (T) -> R): DataResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    suspend fun <R> suspendMap(transform: suspend (T) -> R): DataResult<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> Loading
    }

    companion object {
        fun <T> success(data: T) = Success(data)
        fun error(message: String, cause: Throwable? = null, code: ErrorCode = ErrorCode.UNKNOWN) =
            Error(message, cause, code)

        fun loading() = Loading
    }
}

enum class ErrorCode {
    UNKNOWN,
    NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    PROVIDER_UNAVAILABLE,
    INVALID_DATA,
    AUTHENTICATION,
    NOT_FOUND,
    STALE_DATA,
    OFFLINE
}
