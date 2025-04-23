package dev.codebandits.container.gradle.tasks

import com.github.dockerjava.api.exception.NotFoundException
import dev.codebandits.container.gradle.docker.Docker
import dev.codebandits.container.gradle.docker.Registry
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun writeLocalImageId(imageReference: String, regularFile: RegularFile) {
  val file = regularFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  val dockerClient = Docker.createClient()
  try {
    val inspectImageResponse = dockerClient.inspectImageCmd(imageReference).exec()
    val imageId = inspectImageResponse.id
    if (imageId == null) {
      file.writeText("")
    } else {
      file.writeText(imageId)
    }
  } catch (_: NotFoundException) {
    file.writeText("")
  }
}

internal fun writeRegistryImageDigest(imageReference: String, regularFile: RegularFile) {
  val digest = Registry.getDigest(imageReference)
  val file = regularFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  when (digest) {
    null -> file.writeText("")
    else -> file.writeText(digest)
  }
}

internal fun Task.getLocalImageTrackingFile(
  imageReference: String,
): Provider<RegularFile> {
  val fileName = URLEncoder.encode(imageReference, StandardCharsets.UTF_8)
  return project.layout.buildDirectory.file("images/local/$fileName")
}

internal fun Task.getRegistryImageTrackingFile(
  imageReference: String,
): Provider<RegularFile> {
  val fileName = URLEncoder.encode(imageReference, StandardCharsets.UTF_8)
  return project.layout.buildDirectory.file("images/registry/$fileName")
}
