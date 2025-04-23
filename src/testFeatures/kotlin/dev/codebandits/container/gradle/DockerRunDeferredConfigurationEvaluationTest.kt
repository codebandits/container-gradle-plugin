package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull

class DockerRunDeferredConfigurationEvaluationTest : GradleProjectTest() {

  @Test
  fun `dockerRun configuration evaluation is deferred`() {
    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("notReady") {
          val imageProvider = project.provider<String> { TODO() }
          dockerRun {
            image = imageProvider.get()
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("tasks")
      .build()

    expectThat(result).get { task(":tasks") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
  }
}
