package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.io.path.createFile

class GroovySyntaxTest : GradleProjectTest() {

  @Test
  fun `run a container and track an image output`() {
    val imageReference = generateUniqueImageReference()

    projectDirectory.resolve("index.html").createFile()
    val dockerfile = projectDirectory.resolve("Dockerfile").createFile()
    dockerfile.appendLine(
      """
      FROM scratch
      """.trimIndent()
    )

    buildGradleFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks.register("buildImage", ContainerTask) {
        container.outputs.localImage("$imageReference")
        pullImage { it.image.set("docker:dind") }
        runContainer {
          it.image.set("docker:dind")
          it.entrypoint.set("docker")
          it.cmd.set(["build", "-t", "$imageReference", "."])
          it.workdir.set("/workdir")
          it.volumes.set([
            "${'$'}{layout.projectDirectory}:/workdir",
            "/var/run/docker.sock:/var/run/docker.sock:ro",
          ])
        }
        doLast {
          container.outputs.captureLocalImage("$imageReference")
        }
      }
      """.trimIndent()
    )

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("buildImage")
        .build()

      expectThat(result).get { task(":buildImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("buildImage")
        .build()

      expectThat(result).get { task(":buildImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.UP_TO_DATE)
    }
  }
}
