package dev.codebandits

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import strikt.api.*
import strikt.assertions.*
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

class HelloWorldTest {

  private val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  private val gradleBuildFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }

  @Test
  fun `helloWorld prints to console`() {
    gradleBuildFile.appendText(
      """
      plugins {
        id("dev.codebandits.container")
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("helloWorld")
      .build()

    expectThat(result).and {
      get { task(":helloWorld") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("Hello, world!")
    }
  }

  @Test
  fun `helloWorld task metadata`() {
    gradleBuildFile.appendText(
      """
      plugins {
        id("dev.codebandits.container")
      }
      tasks.register("checkHelloWorldTaskMetadata") {
        doLast {
          val helloWorldTask = tasks.getByName("helloWorld")
          println("helloWorld group: " + helloWorldTask.group)
          println("helloWorld description: " + helloWorldTask.description)
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withPluginClasspath()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("checkHelloWorldTaskMetadata")
      .build()

    expectThat(result).and {
      get { task(":checkHelloWorldTaskMetadata") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("helloWorld group: example")
      get { output }.contains("helloWorld description: Prints 'Hello, world!' to the console.")
    }
  }
}
