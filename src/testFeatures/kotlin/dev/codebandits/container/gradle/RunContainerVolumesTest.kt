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

class RunContainerVolumesTest : GradleProjectTest() {

  @Test
  fun `runContainer mounts a provided file`() {
    val inputFile = projectDirectory.resolve("input.txt").createFile()
    inputFile.appendLine("wild horses")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("readInput") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "cat"
            cmd = listOf("/inputs/input.txt")
            volumes = listOf(
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
  fun `runContainer mounts a provided directory`() {
    val inputDirectory = projectDirectory.resolve("inputs").createDirectory()
    inputDirectory.resolve("input.txt").createFile().appendLine("wild horses")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("readInput") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "cat"
            cmd = listOf("/inputs/input.txt")
            volumes = listOf(
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
  fun `runContainer mounts multiple volumes`() {
    val input1Directory = projectDirectory.resolve("input-1").createDirectory()
    input1Directory.resolve("input.txt").createFile().appendLine("wild horses")
    val input2Directory = projectDirectory.resolve("input-2").createDirectory()
    input2Directory.resolve("input.txt").createFile().appendLine("undomesticated equines")
    val input3File = projectDirectory.resolve("input-3.txt").createFile()
    input3File.appendLine("could not keep me away")

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("inspectInputs") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "sh"
            cmd = listOf("-c", "tree /inputs && find /inputs -type f -exec cat {} +")
            volumes = listOf(
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
