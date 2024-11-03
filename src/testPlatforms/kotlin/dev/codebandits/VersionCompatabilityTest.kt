package dev.codebandits

import dev.codebandits.helpers.appendLine
import dev.codebandits.helpers.configureBuildGradlePluginFromLibsDir
import dev.codebandits.helpers.setupPluginLibsDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

class VersionCompatabilityTest : GradleProjectTest() {

  @ParameterizedTest(name = "run dockerRun using docker {0} java {1} gradle {2}")
  @CsvSource(
    "27, TEMURIN_21, 8.10.2",
    "26, OPENJDK_17, 7.6.4",
  )
  fun `run dockerRun`(dockerVersion: String, javaVersion: ImageFixtures.JavaVersion, gradleVersion: String) {
    setupPluginLibsDir()
    buildGradleFile.configureBuildGradlePluginFromLibsDir()
    buildGradleFile.appendLine(
      """
      tasks.register('helloWorld', ContainerRunTask) {
        dockerRun {
          it.image.set('alpine:latest')
          it.entrypoint.set('echo')
          it.args.set(['Hello, world!'] as String[])
        }
      }
      """.trimIndent()
    )
    val image = ImageFixtures.dockerTemurinGradle(
      dockerVersion = dockerVersion,
      javaVersion = javaVersion,
      gradleVersion = gradleVersion,
    )
    val container = GenericContainer(image)
      .withStartupAttempts(3)
      .withFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", BindMode.READ_ONLY)
      .withCopyFileToContainer(MountableFile.forHostPath(projectDirectory), "/project")
      .withWorkingDirectory("/project")
      .withCommand("tail", "-f", "/dev/null")

    val execResult = try {
      container.start()
      container.execInContainer("gradle", "helloWorld")
    } finally {
      container.stop()
    }
    expectThat(execResult).and {
      get { stdout }.contains("Hello, world!")
      get { exitCode }.isEqualTo(0)
    }
  }
}
