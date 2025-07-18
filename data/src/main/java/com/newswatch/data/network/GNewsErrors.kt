package com.newswatch.data.network

import com.newswatch.core.model.NewsError
import com.squareup.moshi.JsonDataException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

fun Throwable.toNewsError(): NewsError = when (this) {
    is SocketTimeoutException, is IOException -> NewsError.Network
    is HttpException -> when (code()) {
        401, 403 -> NewsError.Authentication
        429 -> NewsError.Quota
        else -> NewsError.Http(code())
    }
    is JsonDataException -> NewsError.Parse
    else -> NewsError.Unknown
}