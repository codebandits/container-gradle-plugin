package dev.codebandits

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import kotlin.io.deleteRecursively
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.notExists

abstract class GradleProjectTest {
  val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  val gradleBuildFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }
  val gradleSettingsFile by lazy { projectDirectory.resolve("settings.gradle.kts").createFile() }

  @AfterEach
  fun cleanupProjectDirectory() {
    projectDirectory.toFile().deleteRecursively()
  }

  protected fun setupGradleWrapper(gradleVersion: String) {
    if (gradleBuildFile.notExists()) {
      gradleBuildFile.createFile()
    }
    GradleRunner.create()
      .withGradleVersion(gradleVersion)
      .withProjectDir(projectDirectory.toFile())
      .withArguments("wrapper")
      .withPluginClasspath()
      .build()
  }
}
