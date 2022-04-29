package com.chimbori.crux.common

import com.chimbori.crux.Resource
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

private const val DEFAULT_BROWSER_VERSION = "100.0.0.0"

private const val CHROME_USER_AGENT = "Mozilla/5.0 (Linux; Android 11; Build/RQ2A.210505.003) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Version/4.0 Chrome/$DEFAULT_BROWSER_VERSION Mobile Safari/537.36"

private const val GOOGLEBOT_USER_AGENT =
  "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5X Build/MMB29P) AppleWebKit/537.36 " +
      "(KHTML, like Gecko) Chrome/$DEFAULT_BROWSER_VERSION Mobile Safari/537.36 " +
      "(compatible; Googlebot/2.1; +http://www.google.com/bot.html)"

public val cruxOkHttpClient: OkHttpClient = OkHttpClient.Builder()
  .followRedirects(true)
  .followSslRedirects(true)
  .retryOnConnectionFailure(true)
  .addNetworkInterceptor { chain ->
    chain.proceed(
      chain.request().newBuilder()
        .header("User-Agent", CHROME_USER_AGENT).build()
    )
  }
  .build()

public suspend fun OkHttpClient.safeCall(request: Request): Response? = withContext(Dispatchers.IO) {
  try {
    newCall(request).execute()
  } catch (e: IOException) {
    null
  } catch (e: NullPointerException) {
    // OkHttp sometimes tries to read a cookie which is null, causing an NPE here. The root cause
    // has not been identified, but this only happens with Twitter so far.
    null
  } catch (e: IllegalArgumentException) {
    // The URL is something like "https://" (no hostname, no path, etc.) which is clearly invalid.
    null
  } catch (e: UnknownHostException) {
    // Device is offline, or this host is unreachable.
    null
  } catch (t: Throwable) {
    // Something else really bad happened, e.g. [java.net.SocketTimeoutException].
    null
  }
}

public suspend fun OkHttpClient.safeHttpGet(url: HttpUrl): Response? =
  safeCall(Request.Builder().url(url).get().build())

public suspend fun OkHttpClient.safeHttpHead(url: HttpUrl): Response? =
  safeCall(Request.Builder().url(url).head().build())

public suspend fun Resource.Companion.fromUrl(
  url: HttpUrl,
  shouldFetchContent: Boolean,
  okHttpClient: OkHttpClient = cruxOkHttpClient
): Resource = Resource(
  url = url,
  document = if (shouldFetchContent) okHttpClient.safeHttpGet(url)?.body?.let {
    Jsoup.parse(it.byteStream(), "UTF-8", url.toString())
  } else null
)
