package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class RunContainerPrivilegedTest : GradleProjectTest() {

  @Test
  fun `runContainer privileged is not set`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("accessMemory") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "ls"
            cmd = listOf("/dev/mem")
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("accessMemory")
      .buildAndFail()

    expectThat(result).get { task(":accessMemory") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.FAILED)
  }

  @Test
  fun `runContainer privileged is set`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("accessMemory") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "ls"
            cmd = listOf("/dev/mem")
            privileged = true
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("accessMemory")
      .build()

    expectThat(result).get { task(":accessMemory") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
  }
}
