package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.helpers.appendLine
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import kotlin.io.path.createFile

class BuildpacksIntegrationTest : GradleProjectTest() {

  @Test
  fun `build an image with Paketo NGINX buildpack`() {
    val imageReference = generateUniqueImageReference()

    projectDirectory.resolve("index.html").createFile()

    val projectTomlFile = projectDirectory.resolve("project.toml").createFile()
    projectTomlFile.appendLine(
      """
      [_]
      schema-version = "0.2"
      
      [io.buildpacks]
      builder = "paketo-buildpacks/nginx"
      include = ["index.html"]
      
      [[io.buildpacks.build.env]]
      name = "BP_WEB_SERVER"
      value = "nginx"
      """.trimIndent()
    )

    buildGradleKtsFile.appendLine(
      """
      import dev.codebandits.container.gradle.container
      import dev.codebandits.container.gradle.tasks.ContainerTask
      
      plugins {
        id("dev.codebandits.container")
      }
      
      tasks {
        register<ContainerTask>("buildImage") {
          container.outputs.localImage("$imageReference")
          pullImage {
            image = "buildpacksio/pack:latest"
          }
          runContainer {
            image = "buildpacksio/pack:latest"
            cmd = listOf(
              "build", "$imageReference",
              "--builder", "paketobuildpacks/builder-jammy-base:latest"
            )
            workdir = "/workdir"
            volumes = listOf(
              "${'$'}{layout.projectDirectory}:/workdir",
              "/var/run/docker.sock:/var/run/docker.sock:ro",
            )
          }
          doLast {
            container.outputs.captureLocalImage("$imageReference")
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("buildImage")
      .build()

    expectThat(result).and {
      get { task(":buildImage") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("Successfully built image '$imageReference'")
    }
  }
}
