package com.chimbori.crux

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OptionalPluginsTest {
  @Test
  fun testTrackingParameterRemoverTest() {
    val trackingRemover = TrackingParameterRemover()
    assertTrue(trackingRemover.canHandle("https://example.org?utm_source".toHttpUrl()))
    assertFalse(trackingRemover.canHandle("https://example.org?not_a_tracking_parameter".toHttpUrl()))

    mapOf(
      "https://example.org/" to null,
      "https://example.org?utm_source" to "https://example.org/",
      "https://www.example.com/?utm_source=summer-mailer&utm_medium=email&utm_campaign=summer-sale"
          to "https://www.example.com/",
      "http://www.example.com/?utm_source=newsletter1&utm_medium=email&utm_campaign=summer-sale&utm_content=toplink"
          to "http://www.example.com/",
      "https://www.example.com/?utm_source=tracker&non-tracking-parameter=dont-remove"
          to "https://www.example.com/?non-tracking-parameter=dont-remove",
    ).forEach { (key, value) ->
      assertEquals(value != null, trackingRemover.canHandle(key.toHttpUrl()))
      assertEquals(
        value?.toHttpUrl() ?: key.toHttpUrl(),
        runBlocking { trackingRemover.handle(Resource(url = key.toHttpUrl())).url }
      )
    }
  }
}
