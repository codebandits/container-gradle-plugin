package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.util.UUID

class DockerPullTest : GradleProjectTest() {

  @Test
  fun `dockerPull pulls the specified image`() {
    removeImage("alpine:3.18.9")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("pullAlpineImage") {
          dockerPull {
            image = "alpine:3.18.9"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("pullAlpineImage")
      .build()

    expectThat(result).and {
      get { task(":pullAlpineImage") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }

    expectThat(imageExists("alpine:3.18.9")).isTrue()
  }

  @Test
  fun `dockerPull fails when pulling an image that does not exist`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("pullImageNotExist") {
          dockerPull {
            image = "alpine:${UUID.randomUUID()}"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("pullImageNotExist")
      .buildAndFail()

    expectThat(result).and {
      get { task(":pullImageNotExist") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.FAILED)
      get { output }.contains("manifest unknown")
    }
  }

  private fun removeImage(imageReference: String) {
    ProcessBuilder("docker", "rmi", "-f", imageReference)
      .inheritIO()
      .start()
      .waitFor()
  }

  private fun imageExists(imageReference: String): Boolean {
    val process = ProcessBuilder("docker", "image", "inspect", imageReference)
      .redirectErrorStream(true)
      .start()
    val exitCode = process.waitFor()
    return exitCode == 0
  }
}

