package dev.codebandits.helpers

import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.text.appendLine

fun Path.appendLine(value: String?) {
  appendText(buildString { appendLine(value) })
}
