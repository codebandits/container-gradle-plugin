package dev.codebandits

import org.junit.jupiter.api.AfterEach
import java.util.UUID
import kotlin.io.deleteRecursively
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory

abstract class GradleProjectTest {
  val projectDirectory by lazy { createTempDirectory(prefix = "test-") }
  val buildGradleKtsFile by lazy { projectDirectory.resolve("build.gradle.kts").createFile() }
  val settingsGradleKtsFile by lazy { projectDirectory.resolve("settings.gradle.kts").createFile() }
  val buildGradleFile by lazy { projectDirectory.resolve("build.gradle").createFile() }
  val imageName = "test-gradle-container-plugin"
  fun generateUniqueImageReference() = "$imageName:test-image-${UUID.randomUUID()}"

  @AfterEach
  fun cleanupProjectDirectory() {
    projectDirectory.toFile().deleteRecursively()
  }

  @AfterEach
  fun cleanupImages() {
    removeAllImagesByName(imageName = imageName)
  }

  protected fun removeImageByReference(imageReference: String) {
    val exitCode = ProcessBuilder("docker", "rmi", imageReference)
      .inheritIO()
      .start()
      .waitFor()

    if (exitCode != 0) {
      throw Exception("Docker image removal failed with exit code $exitCode")
    }
  }

  protected fun removeAllImagesByName(imageName: String) {
    val process = ProcessBuilder("docker", "images", "--format", "{{.Repository}}:{{.Tag}}", imageName)
      .start()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      throw Exception("Failed to retrieve image references for $imageName")
    }
    val imageReferences = process.inputStream.bufferedReader().readLines()
    imageReferences.forEach { imageReference ->
      removeImageByReference(imageReference = imageReference)
    }
  }
}
