package dev.codebandits.container.gradle.tasks

import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.io.use
import kotlin.text.trim

internal object Commands {
  val dockerPath: String = getCommandPath("docker")

  private fun getCommandPath(command: String): String {
    val processBuilder = ProcessBuilder("which", "docker")
    val process = processBuilder.start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()
    return when (exitCode) {
      0 -> output.trim()
      else -> throw IllegalStateException("$command not found in system path.")
    }
  }
}
