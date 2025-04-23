package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import java.util.*

class DockerRemoveTest : GradleProjectTest() {

  @Test
  fun `dockerRemove removes the specified image`() {
    pullImage("hello-world:latest")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("removeImage") {
          dockerRemove {
            image = "hello-world:latest"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("removeImage")
      .build()

    expectThat(result).and {
      get { task(":removeImage") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }

    expectThat(imageExists("hello-world:latest")).isFalse()
  }

  @Test
  fun `dockerRemove fails when removing an image that does not exist`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("removeImageNotExist") {
          dockerRemove {
            image = "alpine:${UUID.randomUUID()}"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("removeImageNotExist")
      .buildAndFail()

    expectThat(result).and {
      get { task(":removeImageNotExist") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.FAILED)
      get { output }.contains("No such image")
    }
  }

  private fun pullImage(imageReference: String) {
    ProcessBuilder("docker", "pull", imageReference)
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
