package dev.codebandits.container.gradle.helpers

import dev.codebandits.container.gradle.GradleProjectTest
import java.io.File
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectory

internal fun GradleProjectTest.setupPluginLibsDir() {
  val hostProjectRoot = File(System.getenv("PROJECT_ROOT"))
  val libsDirectory = projectDirectory.resolve("libs").createDirectory()
  hostProjectRoot.resolve("build/libs/container-gradle-plugin.jar")
    .copyRecursively(libsDirectory.resolve("container-gradle-plugin.jar").toFile())
  val metadataFile = hostProjectRoot.resolve("build/pluginUnderTestMetadata/plugin-under-test-metadata.properties")
  check(metadataFile.exists()) { "metadata file not found: ${metadataFile.path}" }
  val metadata = Properties().apply { metadataFile.inputStream().use { load(it) } }
  val classpathValue = metadata.getProperty("implementation-classpath")
    ?: error("implementation-classpath not found in ${metadataFile.path}")
  val jarPaths = classpathValue.split(File.pathSeparator)
  jarPaths.forEach { jarPath ->
    val jarFile = File(jarPath)
    if (jarFile.exists() && jarFile.extension == "jar") {
      jarFile.copyRecursively(libsDirectory.resolve(jarFile.name).toFile(), overwrite = true)
    }
  }
}

internal fun Path.configureBuildGradlePluginFromLibsDir() {
  appendLine(
    """
    import dev.codebandits.container.gradle.tasks.ContainerRunTask
    buildscript {
      dependencies {
        classpath(fileTree("libs") { include("*.jar") })
      }
    }
    apply plugin: 'dev.codebandits.container'
    """.trimIndent()
  )
}

internal fun Path.configureBuildGradleKtsPluginFromLibsDir() {
  appendLine(
    """
    import dev.codebandits.container.gradle.tasks.ContainerRunTask
    buildscript {
      dependencies {
        classpath(fileTree("libs") { include("*.jar") })
      }
    }
    apply(plugin = "dev.codebandits.container")
    """.trimIndent()
  )
}
