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
  fun `run hello world`() {
    gradleBuildFile.appendText(
      """
      tasks.register("helloWorld") {
        doLast {
          println("Hello world!")
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("helloWorld")
      .build()

    expectThat(result.task(":helloWorld")).isNotNull()
      .get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
    expectThat(result.output).contains("Hello world!")
  }
}
