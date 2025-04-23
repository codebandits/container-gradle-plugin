package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.util.UUID
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class TaskImagesTest : GradleProjectTest() {

  @Test
  fun `inputImages dockerLocal checks external image state for up-to-date determination`() {
    val imageReference = generateUniqueImageReference()

    updateImage(imageReference = imageReference)

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.inputImages
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register("useImage") {
          inputImages.dockerLocal("$imageReference")
          outputs.upToDateWhen { true }
          doLast { }
        }
      }
      """.trimIndent()
    )

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("useImage")
        .build()

      expectThat(result).get { task(":useImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }

    updateImage(imageReference = imageReference)

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("useImage")
        .build()

      expectThat(result).get { task(":useImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("useImage")
        .build()

      expectThat(result).get { task(":useImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.UP_TO_DATE)
    }
  }

  @Test
  fun `outputImages dockerLocal checks external image state for up-to-date determination`() {
    val imageReference = generateUniqueImageReference()

    projectDirectory.resolve("index.html").createFile()
    val dockerfile = projectDirectory.resolve("Dockerfile").createFile()
    dockerfile.appendLine(
      """
      FROM scratch
      """.trimIndent()
    )

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.tasks.ContainerTask
      import dev.codebandits.container.gradle.tasks.outputImages
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("buildImage") {
          outputImages.dockerLocal("$imageReference")
          dockerPull { image = "docker:dind" }
          dockerRun {
            image = "docker:dind"
            entrypoint = "docker"
            args = arrayOf("build", "-t", "$imageReference", "." )
            workdir = "/workdir"
            volumes = arrayOf(
              "${'$'}{layout.projectDirectory}:/workdir",
              "/var/run/docker.sock:/var/run/docker.sock:ro",
            )
          }
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

    removeImageByReference(imageReference = imageReference)

    run {
      val result = GradleRunner.create()
        .withPluginClasspath()
        .withProjectDir(projectDirectory.toFile())
        .withArguments("buildImage")
        .build()

      expectThat(result).get { task(":buildImage") }.isNotNull()
        .get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    }
  }

  private fun updateImage(imageReference: String) {
    val directory = createTempDirectory(prefix = "test-")
    try {
      val uuidFile = directory.resolve("uuid.txt").createFile()
      uuidFile.appendLine(UUID.randomUUID().toString())
      val dockerFile = directory.resolve("Dockerfile").createFile()
      dockerFile.appendLine("FROM scratch")
      dockerFile.appendLine("COPY uuid.txt /uuid.txt")
      val exitCode = ProcessBuilder("docker", "build", "-t", imageReference, directory.absolutePathString())
        .inheritIO()
        .start()
        .waitFor()

      if (exitCode != 0) {
        throw Exception("Docker build failed with exit code $exitCode")
      }
    } finally {
      directory.toFile().deleteRecursively()
    }
  }
}
