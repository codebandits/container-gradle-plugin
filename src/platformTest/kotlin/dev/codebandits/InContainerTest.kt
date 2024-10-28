package dev.codebandits

import dev.codebandits.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.io.File
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.notExists

class InContainerTest {
  private val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  private val gradleBuildFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }
  private val gradleSettingsFile by lazy { projectDirectory.resolve("settings.gradle.kts").createFile() }

  @ParameterizedTest(name = "run helloWorld with docker {0} java {1} gradle {2}")
  @CsvSource(
    "27, 22, 8.10.2",
    "26, 21, 7.6.4",
  )
  fun `run helloWorld`(dockerVersion: String, javaVersion: String, gradleVersion: String) {
    gradleSettingsFile.appendLine("rootProject.name = \"platform-testing\"")
    setupPluginIncludedBuild()
    gradleBuildFile.appendLine(
      """
      plugins {
        id("dev.codebandits.container")
      }
      """.trimIndent()
    )
    setupGradleWrapper(gradleVersion = gradleVersion)

    val image = ImageFixtures.dockerTemurin(
      dockerVersion = dockerVersion,
      javaVersion = javaVersion,
    )
    val container = GenericContainer(image)
      .withCopyFileToContainer(MountableFile.forHostPath(projectDirectory), "/project")
      .withWorkingDirectory("/project")
      .withCommand("tail", "-f", "/dev/null")

    run {
      val execResult = try {
        container.start()
        container.execInContainer("./gradlew", "helloWorld")
      } finally {
        container.stop()
      }
      expectThat(execResult).and {
        get { stdout }.contains("Hello, world!")
        get { exitCode }.isEqualTo(0)
      }
    }
  }

  private fun setupGradleWrapper(gradleVersion: String) {
    if (gradleBuildFile.notExists()) {
      gradleBuildFile.createFile()
    }
    GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(projectDirectory.toFile())
      .withArguments("wrapper")
      .withPluginClasspath()
      .build()
  }

  private fun setupPluginIncludedBuild() {
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
}
