package dev.codebandits

import dev.codebandits.helpers.appendLine
import dev.codebandits.helpers.setupPluginIncludedBuild
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo

class DaemonSocketPathTest : GradleProjectTest() {

  @Test
  fun `use a custom daemon socket path`() {
    val daemonSocketPath = "/var/run/container.sock"
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
            image = "alpine:latest"
            entrypoint = "echo"
            containerArgs = arrayOf("Hello, world!")
            daemonSocketPath = "$daemonSocketPath"
          }
        }
      }
      """.trimIndent()
    )
    setupGradleWrapper(gradleVersion = "8.10.2")

    val image = ImageFixtures.dockerTemurin(
      dockerVersion = "27",
      javaVersion = "21",
    )
    val container = GenericContainer(image)
      .withPrivilegedMode(true)
      .withFileSystemBind("/var/run/docker.sock", daemonSocketPath, BindMode.READ_ONLY)
      .withCopyFileToContainer(MountableFile.forHostPath(projectDirectory), "/project")
      .withWorkingDirectory("/project")
      .withCommand("tail", "-f", "/dev/null")

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
