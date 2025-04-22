package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile

class DockerRunWorkdirTest : GradleProjectTest() {

  @Test
  fun `dockerRun workdir is set to an existing directory`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("listMedia") {
          dockerPull { image = "alpine:latest" }
          dockerRun {
            image = "alpine:latest"
            entrypoint = "ls"
            workdir = "/media"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("listMedia")
      .build()

    expectThat(result).and {
      get { task(":listMedia") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains(
        """
        cdrom
        floppy
        usb
        """.trimIndent()
      )
    }
  }

  @Test
  fun `dockerRun workdir is set to a non-existent directory`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("listMedia") {
          dockerPull { image = "alpine:latest" }
          dockerRun {
            image = "alpine:latest"
            entrypoint = "pwd"
            workdir = "/windows"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("listMedia")
      .build()

    expectThat(result).and {
      get { task(":listMedia") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("/windows")
    }
  }

  @Test
  fun `dockerRun workdir is set to a volume provided directory`() {
    val inputDirectory = projectDirectory.resolve("inputs").createDirectory()
    inputDirectory.resolve("input.txt").createFile().appendLine("wild horses")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("listMedia") {
          dockerPull { image = "alpine:latest" }
          dockerRun {
            image = "alpine:latest"
            entrypoint = "cat"
            args = arrayOf("input.txt")
            workdir = "/inputs"
            volumes = arrayOf(
              "${inputDirectory.absolutePathString()}:/inputs",
            )
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("listMedia")
      .build()

    expectThat(result).and {
      get { task(":listMedia") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("wild horses")
    }
  }
}
