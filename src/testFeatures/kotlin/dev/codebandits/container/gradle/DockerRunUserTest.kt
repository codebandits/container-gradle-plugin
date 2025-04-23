package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class DockerRunUserTest : GradleProjectTest() {

  @Test
  fun `dockerRun user is set to guest`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("whoami") {
          dockerPull { image = "alpine:latest" }
          dockerRun {
            image = "alpine:latest"
            user = "guest"
            entrypoint = "whoami"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("whoami")
      .build()

    expectThat(result).and {
      get { task(":whoami") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("guest")
    }
  }

  @Test
  fun `dockerRun user is set to root`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("whoami") {
          dockerPull { image = "alpine:latest" }
          dockerRun {
            image = "alpine:latest"
            user = "root"
            entrypoint = "whoami"
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("whoami")
      .build()

    expectThat(result).and {
      get { task(":whoami") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("root")
    }
  }
}
