package dev.codebandits.helpers

import dev.codebandits.GradleProjectTest
import java.io.File
import kotlin.io.path.createDirectory

internal fun GradleProjectTest.setupPluginIncludedBuild() {
  val hostProjectRoot = File(System.getenv("PROJECT_ROOT"))
  val includedBuildDirectory = projectDirectory.resolve("gradle-container-plugin").createDirectory()
  hostProjectRoot.resolve("build.gradle.kts")
    .copyTo(includedBuildDirectory.resolve("build.gradle.kts").toFile())
  hostProjectRoot.resolve("settings.gradle.kts")
    .copyTo(includedBuildDirectory.resolve("settings.gradle.kts").toFile())
  hostProjectRoot.resolve("gradle/libs.versions.toml")
    .copyTo(includedBuildDirectory.resolve("gradle/libs.versions.toml").toFile())
  hostProjectRoot.resolve("src/main").copyRecursively(includedBuildDirectory.resolve("src/main").toFile())
  gradleSettingsFile.appendLine("includeBuild(\"gradle-container-plugin\")")
}
