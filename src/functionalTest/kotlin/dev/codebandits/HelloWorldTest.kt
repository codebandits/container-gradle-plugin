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
  fun `hello world`() {
    gradleBuildFile.appendText(
      //language=gradle
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

    expectThat(result).and {
      get("the :helloWorld task") { task(":helloWorld") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("Hello world!")
    }
  }

  @Test
  fun `which docker`() {
    gradleBuildFile.appendText(
      //language=gradle
      """
      tasks.register("printDockerPath") {
        doLast {
          exec {
            executable = "which"
            args("docker")
          }
        }
      }
      """.trimIndent()
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDirectory.toFile())
      .withArguments("printDockerPath")
      .build()

    expectThat(result).and {
      get { task(":printDockerPath") }.isNotNull().get { outcome }.isEqualTo(TaskOutcome.SUCCESS)
      get { output }.contains("/usr/local/bin/docker")
    }
  }
  }
}
