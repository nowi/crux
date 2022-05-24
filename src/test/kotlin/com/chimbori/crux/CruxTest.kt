package com.chimbori.crux

import com.chimbori.crux.Fields.BANNER_IMAGE_URL
import com.chimbori.crux.Fields.CANONICAL_URL
import com.chimbori.crux.Fields.DESCRIPTION
import com.chimbori.crux.Fields.TITLE
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CruxTest {
  private lateinit var mockWebServer: MockWebServer

  @Before
  fun setUp() {
    mockWebServer = MockWebServer().apply {
      dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest) = MockResponse().setBody("${request.path}")
      }
      start()
    }
  }

  @After
  fun tearDown() {
    mockWebServer.shutdown()
  }

  @Test
  fun testResourceMetadataApiExamples() {
    val resource = Resource(
      url = "https://chimbori.com/".toHttpUrl(),
      fields = mapOf(
        TITLE to "Life, the Universe, and Everything",
        DESCRIPTION to "42"
      ), urls = mapOf(
        CANONICAL_URL to "https://chimbori.com/".toHttpUrl()
      )
    )
    assertEquals("Life, the Universe, and Everything", resource[TITLE])
    assertEquals("42", resource[DESCRIPTION])
    assertEquals("https://chimbori.com/".toHttpUrl(), resource.urls[CANONICAL_URL])
    assertNull(resource.urls[BANNER_IMAGE_URL])
  }

  @Test
  fun testPluginsAreNotAskedToHandleUrlsTheyCannotHandle() {
    val fooHandlerPlugin = object : Extractor {
      override fun canExtract(url: HttpUrl) = url.encodedPath == "/foo"
      override suspend fun extract(request: Resource) = Resource(
        url = request.url?.newBuilder()?.encodedPath("/rewritten-from-foo")?.build()
      )
    }

    val barHandlerPlugin = object : Extractor {
      override fun canExtract(url: HttpUrl) = url.encodedPath == "/bar"
      override suspend fun extract(request: Resource) = Resource(
        url = request.url?.newBuilder()?.encodedPath("/rewritten-from-bar")?.build()
      )
    }

    val cruxWithFooPlugin = Crux(plugins = listOf(fooHandlerPlugin))
    val fooMetadata = runBlocking {
      cruxWithFooPlugin.extractFrom(mockWebServer.url("/foo"))
    }
    assertEquals("/rewritten-from-foo", fooMetadata.url?.encodedPath)

    val cruxWithBarPlugin = Crux(plugins = listOf(barHandlerPlugin))
    val barMetadata = runBlocking {
      cruxWithBarPlugin.extractFrom(mockWebServer.url("/foo"))
    }
    assertEquals("/foo", barMetadata.url?.encodedPath)
  }

  @Test
  fun testDefaultPluginsCanParseTitle() {
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = MockResponse().setBody("<title>Mock Title</title>")
    }

    val crux = Crux()
    val metadata = runBlocking { crux.extractFrom(mockWebServer.url("/mock-title")) }
    assertNotNull(metadata)
    assertEquals("Mock Title", metadata[TITLE])
  }

  @Test
  fun testHttpRedirectUrlReturnedInsteadOfOriginalUrl() {
    val originalUrl = mockWebServer.url("/original")
    val redirectedUrl = mockWebServer.url("/redirected")
    mockWebServer.dispatcher = object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) = when (request.path) {
        originalUrl.encodedPath -> MockResponse().setResponseCode(301).setHeader("Location", redirectedUrl)
        redirectedUrl.encodedPath -> MockResponse().setBody("")
        else -> MockResponse().setResponseCode(404)
      }
    }

    val metadata = runBlocking { Crux().extractFrom(originalUrl) }
    assertEquals(redirectedUrl, metadata.url)
  }

  @Test
  fun testLaterPluginOperatesOnRewrittenUrlFromPreviousPlugin() {
    val rewriteFooToBarPlugin = object : Extractor {
      override fun canExtract(url: HttpUrl) = url.encodedPath == "/foo"
      override suspend fun extract(request: Resource) =
        Resource(
          url = request.url?.newBuilder()?.encodedPath("/bar")?.build(),
          fields = mapOf(TITLE to "Foo Title")
        )
    }

    val generateTitleForBarPlugin = object : Extractor {
      override fun canExtract(url: HttpUrl) = url.encodedPath == "/bar"
      override suspend fun extract(request: Resource) = Resource(fields = mapOf(TITLE to "Bar Title"))
    }

    // Test Foo before Bar.
    val fooBeforeBarCrux = Crux(listOf(rewriteFooToBarPlugin, generateTitleForBarPlugin))
    val fooBeforeBar = runBlocking {
      fooBeforeBarCrux.extractFrom(mockWebServer.url("/foo"))
    }
    assertEquals("Bar Title", fooBeforeBar[TITLE])

    // Test Bar before Foo.
    val barBeforeFooCrux = Crux(listOf(generateTitleForBarPlugin, rewriteFooToBarPlugin))
    val barBeforeFoo = runBlocking {
      barBeforeFooCrux.extractFrom(mockWebServer.url("/foo"))
    }
    assertEquals("Foo Title", barBeforeFoo[TITLE])
  }

  @Test
  fun testNoHttpRequestsAreMadeWhenCallerProvidesParsedDocument() {
  }

  @Test
  fun testLaterPluginOverridesFieldsSetByPreviousPlugin() {
  }

  @Test
  fun testLaterPluginOverridesFieldsWithNull() {
  }

  @Test
  fun testLaterPluginOverridesFieldsWithBlanks() {
  }

  @Test
  fun testPluginProvidesUpdatedParsedDocument() {
  }
}
