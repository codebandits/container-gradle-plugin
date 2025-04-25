package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

class RunContainerAutoRemoveTest : GradleProjectTest() {
  @Test
  fun `runContainer removes containers by default`() {
    val uuid = UUID.randomUUID().toString()
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("printID") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "echo"
            cmd = listOf("$uuid")
            workdir = "/workdir"
            volumes = listOf(
              "${'$'}{layout.projectDirectory}:/workdir",
            )
          }
        }
      }
      """.trimIndent()
    )

    GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("printID")
      .build()

    expectThat(doesContainerWithCommandExist(command = "echo $uuid"))
      .describedAs("the created container still exists")
      .isFalse()
  }

  @Test
  fun `runContainer removes containers when autoRemove is true`() {
    val uuid = UUID.randomUUID().toString()
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("printID") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "echo"
            cmd = listOf("$uuid")
            workdir = "/workdir"
            volumes = listOf(
              "${'$'}{layout.projectDirectory}:/workdir",
            )
            autoRemove = true
          }
        }
      }
      """.trimIndent()
    )

    GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("printID")
      .build()

    expectThat(doesContainerWithCommandExist(command = "echo $uuid"))
      .describedAs("the created container still exists")
      .isFalse()
  }

  @Test
  fun `runContainer preserves containers when autoRemove is false`() {
    val uuid = UUID.randomUUID().toString()
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("printID") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "echo"
            cmd = listOf("$uuid")
            workdir = "/workdir"
            volumes = listOf(
              "${'$'}{layout.projectDirectory}:/workdir",
            )
            autoRemove = false
          }
        }
      }
      """.trimIndent()
    )

    GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("printID")
      .build()

    expectThat(doesContainerWithCommandExist(command = "echo $uuid"))
      .describedAs("the created container still exists")
      .isTrue()
  }

  private fun doesContainerWithCommandExist(command: String): Boolean {
    val processBuilder = ProcessBuilder("docker", "ps", "-a", "--no-trunc", "--format", "{{.Command}}")
    val process = processBuilder.start()
    val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
    process.waitFor()
    return output.lines().any { it == "\"$command\"" }
  }
}
