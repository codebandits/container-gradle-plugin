package dev.codebandits

import org.junit.jupiter.api.AfterEach
import kotlin.io.deleteRecursively
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

abstract class GradleProjectTest {
  val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  val gradleBuildFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }
  val gradleSettingsFile by lazy { projectDirectory.resolve("settings.gradle.kts").createFile() }

  @AfterEach
  fun cleanupProjectDirectory() {
    projectDirectory.toFile().deleteRecursively()
  }
}
