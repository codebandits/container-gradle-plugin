package dev.codebandits

import dev.codebandits.helpers.appendLine
import dev.codebandits.helpers.setupPluginIncludedBuild
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
    "27, 22, 8.10.2",
    "26, 21, 7.6.4",
  )
  fun `run dockerRun`(dockerVersion: String, javaVersion: String, gradleVersion: String) {
    gradleSettingsFile.appendLine("rootProject.name = \"platform-testing\"")
    setupPluginIncludedBuild()
    gradleBuildFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("helloWorld") {
          dockerRun {
            // setter syntax required for older versions of gradle
            image.set("alpine:latest")
            entrypoint.set("echo")
            args.set(arrayOf("Hello, world!"))
          }
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
      .withPrivilegedMode(true)
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
