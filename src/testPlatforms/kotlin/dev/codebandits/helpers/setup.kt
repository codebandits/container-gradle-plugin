package dev.codebandits.helpers

import dev.codebandits.GradleProjectTest
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectory

internal fun GradleProjectTest.setupPluginLibsDir() {
  val hostProjectRoot = File(System.getenv("PROJECT_ROOT"))
  val libsDirectory = projectDirectory.resolve("libs").createDirectory()
  hostProjectRoot.resolve("build/libs/container-gradle-plugin.jar")
    .copyRecursively(libsDirectory.resolve("container-gradle-plugin.jar").toFile())
}

internal fun Path.configureBuildGradlePluginFromLibsDir() {
  appendLine(
    """
    import dev.codebandits.ContainerRunTask
    buildscript {
      dependencies {
        classpath files('libs/container-gradle-plugin.jar')
      }
    }
    apply plugin: 'dev.codebandits.container'
    """.trimIndent()
  )
}

internal fun Path.configureBuildGradleKtsPluginFromLibsDir() {
  appendLine(
    """
    import dev.codebandits.ContainerRunTask
    buildscript {
      dependencies {
        classpath(files("libs/container-gradle-plugin.jar"))
      }
    }
    apply(plugin = "dev.codebandits.container")
    """.trimIndent()
  )
}
