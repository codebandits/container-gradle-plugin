package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import java.io.StringWriter
import kotlin.io.path.exists
import kotlin.io.path.readText

class RunContainerTest : GradleProjectTest() {

  @Test
  fun `runContainer uses the specified image`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("reportAlpineVersion") {
          pullImage { image = "alpine:3.18.9" }
          runContainer {
            image = "alpine:3.18.9"
            entrypoint = "sh"
            cmd = listOf(
              "-c",
              "printf 'ALPINE VERSION: ' && cat /etc/alpine-release",
            )
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("reportAlpineVersion")
      .build()

    expectThat(result).get { output }.contains("ALPINE VERSION: 3.18.9")
  }

  @Test
  fun `runContainer reports successful task status`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("beSuccessful") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "echo"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("beSuccessful")
      .build()

    expectThat(result).get { task(":beSuccessful") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
  }

  @Test
  fun `runContainer reports failed task status`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("alwaysFail") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "cat"
            cmd = listOf("/file-that-does-not-exist")
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("alwaysFail")
      .buildAndFail()

    expectThat(result).and {
      get { task(":alwaysFail") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.FAILED)
      get { output }.contains("cat: can't open '/file-that-does-not-exist': No such file or directory")
    }
  }

  @Test
  fun `runContainer streams stdout and stderr separately`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("outputTest") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "sh"
            cmd = listOf(
              "-c",
              "echo 'wonderful' && echo 'oh no' >&2",
            )
          }
        }
      }
      """.trimIndent()
    )

    val stdout = StringWriter()
    val stderr = StringWriter()
    GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("outputTest")
      .forwardStdOutput(stdout)
      .forwardStdError(stderr)
      .build()

    expect {
      that(stdout.toString()).and {
        contains("wonderful")
        not().contains("oh no")
      }
      that(stderr.toString()).and {
        not().contains("wonderful")
        contains("oh no")
      }
    }
  }

  @Test
  fun `runContainer uses a provided workdir`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("secretDecoder") {
          pullImage { image = "alpine:latest" }
          runContainer {
            image = "alpine:latest"
            entrypoint = "sh"
            cmd = listOf(
              "-c",
              "printf 'Be sure to drink your Ovaltine' > secret-message.txt",
            )
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
      .withArguments("secretDecoder")
      .build()

    expectThat(projectDirectory.resolve("secret-message.txt"))
      .and { get { exists() }.isTrue() }
      .and { get { readText() }.isEqualTo("Be sure to drink your Ovaltine") }
  }
}

