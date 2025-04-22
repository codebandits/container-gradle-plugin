package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import dev.codebandits.container.gradle.helpers.configureBuildGradlePluginFromLibsDir
import dev.codebandits.container.gradle.helpers.setupPluginLibsDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.testcontainers.containers.BindMode
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

class VersionCompatabilityTest : GradleProjectTest() {

  @ParameterizedTest(name = "run dockerRun using docker {0} java {1} gradle {2}")
  @CsvSource(
    "27, TEMURIN_21, 8.13",
    "27, TEMURIN_21, 8.10.2",
    "26, OPENJDK_17, 7.6.4",
  )
  fun `run dockerRun`(dockerVersion: String, javaVersion: JavaVersion, gradleVersion: String) {
    setupPluginLibsDir()
    buildGradleFile.configureBuildGradlePluginFromLibsDir()
    buildGradleFile.appendLine(
      """
      tasks.register('helloWorld', ContainerRunTask) {
        dockerPull {
          it.image.set('alpine:latest')
        }
        dockerRun {
          it.image.set('alpine:latest')
          it.entrypoint.set('echo')
          it.args.set(['Hello, world!'] as String[])
        }
      }
      """.trimIndent()
    )
    val container = ContainerProvider.dockerJavaGradle(
      dockerVersion = dockerVersion,
      javaVersion = javaVersion,
      gradleVersion = gradleVersion,
    )
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
