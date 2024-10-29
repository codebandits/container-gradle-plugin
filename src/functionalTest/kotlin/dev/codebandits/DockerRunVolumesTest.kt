package dev.codebandits

import dev.codebandits.helpers.appendLine
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

class DockerRunVolumesTest : GradleProjectTest() {

  @Test
  fun `dockerRun mounts a provided file`() {
    val inputFile = projectDirectory.resolve("input.txt").createFile()
    inputFile.appendLine("wild horses")

    gradleBuildFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("readInput") {
          dockerRun {
            image = "alpine:latest"
            entrypoint = "cat"
            args = arrayOf("/inputs/input.txt")
            volumes = arrayOf(
              "${inputFile.absolutePathString()}:/inputs/input.txt",
            )
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("readInput")
      .build()

    expectThat(result).and {
      get { task(":readInput") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("wild horses")
    }
  }

  @Test
  fun `dockerRun mounts a provided directory`() {
    val inputDirectory = projectDirectory.resolve("inputs").createDirectory()
    inputDirectory.resolve("input.txt").createFile().appendLine("wild horses")

    gradleBuildFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("readInput") {
          dockerRun {
            image = "alpine:latest"
            entrypoint = "cat"
            args = arrayOf("/inputs/input.txt")
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
      .withArguments("readInput")
      .build()

    expectThat(result).and {
      get { task(":readInput") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("wild horses")
    }
  }

  @Test
  fun `dockerRun mounts multiple volumes`() {
    val input1Directory = projectDirectory.resolve("input-1").createDirectory()
    input1Directory.resolve("input.txt").createFile().appendLine("wild horses")
    val input2Directory = projectDirectory.resolve("input-2").createDirectory()
    input2Directory.resolve("input.txt").createFile().appendLine("undomesticated equines")
    val input3File = projectDirectory.resolve("input-3.txt").createFile()
    input3File.appendLine("could not keep me away")

    gradleBuildFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("inspectInputs") {
          dockerRun {
            image = "alpine:latest"
            entrypoint = "sh"
            args = arrayOf("-c", "tree /inputs && find /inputs -type f -exec cat {} +")
            volumes = arrayOf(
              "${input1Directory.absolutePathString()}:/inputs/input-1",
              "${input2Directory.absolutePathString()}:/inputs/input-2",
              "${input3File.absolutePathString()}:/inputs/input-3.txt",
            )
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("inspectInputs")
      .build()

    expectThat(result).and {
      get { task(":inspectInputs") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }
        .contains(
          """
          /inputs
          ├── input-1
          │   └── input.txt
          ├── input-2
          │   └── input.txt
          └── input-3.txt
          """.trimIndent()
        )
        .contains("wild horses")
        .contains("undomesticated equines")
        .contains("could not keep me away")
    }
  }
}
