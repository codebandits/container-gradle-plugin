package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class RunContainerSocketMountTest : GradleProjectTest() {

  @Test
  fun `runContainer volumes can be used to mount the docker socket`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("dockerVersion") {
          pullImage { image = "curlimages/curl:latest" }
          runContainer {
            image = "curlimages/curl:latest"
            user = "root"
            cmd = listOf("--unix-socket", "/var/run/docker.sock", "-sS", "http://./version")
            volumes = listOf(
              "/var/run/docker.sock:/var/run/docker.sock:ro",
            )
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("dockerVersion")
      .build()

    expectThat(result).and {
      get { task(":dockerVersion") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("ApiVersion")
    }
  }
}
