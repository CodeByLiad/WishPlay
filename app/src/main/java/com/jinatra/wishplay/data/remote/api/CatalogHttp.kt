package com.jinatra.wishplay.data.remote.api

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun OkHttpClient.catalogJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject {
    return JSONObject(catalogText(url, "application/json", headers))
}

internal suspend fun OkHttpClient.catalogText(url: String, accept: String = "text/html", headers: Map<String, String> = emptyMap()): String {
    val call = newCall(Request.Builder().url(url).header("Accept", accept)
        .header("User-Agent", "Mozilla/5.0 (compatible; WishPlay/1.4)")
        .apply { headers.forEach { (name, value) -> header(name, value) } }.build())
    return suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                val result = runCatching {
                    response.use {
                        if (!it.isSuccessful) throw IOException("Catalog unavailable (${it.code})")
                        val body = it.body ?: throw IOException("Empty catalog response")
                        if (body.contentLength() > 6 * 1024 * 1024) throw IOException("Catalog response too large")
                        val text = body.string()
                        if (text.length > 6 * 1024 * 1024) throw IOException("Catalog response too large")
                        text
                    }
                }
                if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
            }
        })
    }
}
