package com.chimbori.crux.common

import mu.KotlinLogging
import org.jsoup.nodes.Node

internal object Log {
  private const val DEBUG = false

  private const val TRUNCATE = true
  private val logger = KotlinLogging.logger {}

  fun i(message: String, vararg args: Any?) {
    if (DEBUG) {
      logger.info(String.format(message, *args))
    }
  }

  fun i(reason: String, node: Node) {
    if (DEBUG) {
      val nodeToString = if (TRUNCATE) {
        node.outerHtml().take(80).replace("\n", "")
      } else {
        "\n------\n${node.outerHtml()}\n------\n"
      }
      i("%s [%s]", reason, nodeToString)
    }
  }

  fun printAndRemove(reason: String, node: Node) {
    i(reason, node)
    node.remove()
  }
}
