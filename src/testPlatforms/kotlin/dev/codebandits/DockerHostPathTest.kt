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
import strikt.assertions.isNotEqualTo

class DockerHostPathTest : GradleProjectTest() {

  @Test
  fun `dockerRun uses a custom socket when dockerHost is set`() {
    setupPluginIncludedBuild()
    buildGradleKtsFile.appendLine(
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
            args = arrayOf("Hello, world!")
            dockerHost = "unix:///var/run/custom.sock"
          }
        }
      }
      """.trimIndent()
    )
    val image = ImageFixtures.dockerTemurinGradle(
      dockerVersion = "27",
      javaVersion = "21",
      gradleVersion = "8.10.2",
    )
    val container = GenericContainer(image)
      .withPrivilegedMode(true)
      .withFileSystemBind("/var/run/docker.sock", "/var/run/custom.sock", BindMode.READ_ONLY)
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

  @Test
  fun `dockerRun fails when a non-existent dockerHost is set`() {
    setupPluginIncludedBuild()
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("echo") {
          dockerRun {
            image = "alpine:latest"
            entrypoint = "echo"
            dockerHost = "unix:///wrong/path/to/docker.sock"
          }
        }
      }
      """.trimIndent()
    )

    val image = ImageFixtures.dockerTemurinGradle(
      dockerVersion = "27",
      javaVersion = "21",
      gradleVersion = "8.10.2",
    )
    val container = GenericContainer(image)
      .withPrivilegedMode(true)
      .withFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", BindMode.READ_ONLY)
      .withCopyFileToContainer(MountableFile.forHostPath(projectDirectory), "/project")
      .withWorkingDirectory("/project")
      .withCommand("tail", "-f", "/dev/null")

    val execResult = try {
      container.start()
      container.execInContainer("gradle", "echo")
    } finally {
      container.stop()
    }
    expectThat(execResult).and {
      get { stderr }.contains("docker: Cannot connect to the Docker daemon at unix:///wrong/path/to/docker.sock. Is the docker daemon running?")
      get { exitCode }.isNotEqualTo(0)
    }
  }
}
