package dev.codebandits

import dev.codebandits.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class DockerRunSocketMountTest : GradleProjectTest() {

  @Test
  fun `dockerRun volumes can be used to mount the docker socket`() {
    gradleBuildFile.appendLine(
      """
      import dev.codebandits.ContainerRunTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerRunTask>("dockerVersion") {
          dockerRun {
            image = "curlimages/curl:latest"
            user = "root"
            args = arrayOf("--unix-socket", "/var/run/docker.sock", "-sS", "http://./version")
            volumes = arrayOf(
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
