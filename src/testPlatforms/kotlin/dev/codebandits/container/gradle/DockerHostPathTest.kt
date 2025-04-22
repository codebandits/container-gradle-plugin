package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import dev.codebandits.container.gradle.helpers.configureBuildGradleKtsPluginFromLibsDir
import dev.codebandits.container.gradle.helpers.setupPluginLibsDir
import org.junit.jupiter.api.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.utility.MountableFile
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

class DockerHostPathTest : GradleProjectTest() {

  @Test
  fun `dockerRun uses a custom socket when dockerHost is set`() {
    setupPluginLibsDir()
    buildGradleKtsFile.configureBuildGradleKtsPluginFromLibsDir()
    buildGradleKtsFile.appendLine(
      """
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
    val container = ContainerProvider.dockerJavaGradle(
      dockerVersion = "27",
      javaVersion = JavaVersion.TEMURIN_21,
      gradleVersion = "8.13",
    )
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
    setupPluginLibsDir()
    buildGradleKtsFile.configureBuildGradleKtsPluginFromLibsDir()
    buildGradleKtsFile.appendLine(
      """
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

    val container = ContainerProvider.dockerJavaGradle(
      dockerVersion = "27",
      javaVersion = JavaVersion.TEMURIN_21,
      gradleVersion = "8.13",
    )
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
      get { stderr }.contains("java.net.SocketException: No such file or directory")
      get { exitCode }.isNotEqualTo(0)
    }
  }
}
