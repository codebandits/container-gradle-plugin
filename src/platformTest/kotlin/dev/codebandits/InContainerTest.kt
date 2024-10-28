package dev.codebandits

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.io.File
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class InContainerTest {

  private val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  private val gradleBuildFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }

  @ParameterizedTest(name = "run hello world with docker {0} java {1}")
  @CsvSource(
    "27, 23",
    "26, 21",
  )
  fun `run hello world`(dockerVersion: String, javaVersion: String) {
    gradleBuildFile.appendText(
      """
      tasks.register("helloWorld") {
        doLast {
          println("Hello world!")
        }
      }
      """.trimIndent()
    )

    setupGradleWrapper(projectDirectory.toFile())

    val image = ImageFixtures.dockerTemurin(
      dockerVersion = dockerVersion,
      javaVersion = javaVersion,
    )
    val container = GenericContainer(image)
      .withCopyFileToContainer(MountableFile.forHostPath(projectDirectory), "/project")
      .withWorkingDirectory("/project")

    run {
      val execResult = try {
        container.start()
        container.execInContainer("./gradlew", "helloWorld")
      } finally {
        container.stop()
      }
      expectThat(execResult).and {
        get { stdout }.contains("Hello world!")
        get { exitCode }.isEqualTo(0)
      }
    }
  }

  private fun setupGradleWrapper(projectDirectory: File) {
    GradleRunner.create()
      .withProjectDir(projectDirectory)
      .withArguments("wrapper")
      .withPluginClasspath()
      .build()
  }
}
