package dev.codebandits.container.gradle.image

import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

public abstract class ContainerTaskExtension(private val task: Task) {
  public fun inputLocalImage(imageReference: String) {
    task.inputLocalImage(imageReference)
  }

  public fun outputLocalImage(imageReference: String) {
    task.outputLocalImage(imageReference)
  }

  public fun inputRegistryImage(imageReference: String, autoRefresh: Boolean = false) {
    task.inputRegistryImage(imageReference, autoRefresh)
  }

  public fun outputRegistryImage(imageReference: String, autoRefresh: Boolean = false) {
    task.outputRegistryImage(imageReference, autoRefresh)
  }
}

public fun Task.inputLocalImage(
  imageReference: String,
) {
  val trackingFile = getLocalImageTrackingFile(imageReference)
  inputs.file(trackingFile.map { regularFile ->
    writeLocalImageId(imageReference, regularFile)
    regularFile
  })
}

public fun Task.outputLocalImage(
  imageReference: String,
) {
  val trackingFile = getLocalImageTrackingFile(imageReference)
  outputs.file(trackingFile.map { regularFile ->
    writeLocalImageId(imageReference, regularFile)
    regularFile
  })
  doLast { writeLocalImageId(imageReference, trackingFile.get()) }
}

public fun Task.inputRegistryImage(
  imageReference: String,
  autoRefresh: Boolean = false,
) {
  val trackingFile = getRegistryImageTrackingFile(imageReference)
  inputs.file(trackingFile.map { regularFile ->
    if (autoRefresh || !regularFile.asFile.exists()) {
      writeRegistryImageDigest(imageReference, regularFile)
    }
    regularFile
  })
}

public fun Task.outputRegistryImage(
  imageReference: String,
  autoRefresh: Boolean = false,
) {
  val trackingFile = getLocalImageTrackingFile(imageReference)
  outputs.file(trackingFile.map { regularFile ->
    if (autoRefresh || !regularFile.asFile.exists()) {
      writeRegistryImageDigest(imageReference, regularFile)
    }
    regularFile
  })
  doLast {
    val regularFile = trackingFile.get()
    if (autoRefresh || !regularFile.asFile.exists()) {
      writeRegistryImageDigest(imageReference, regularFile)
    }
  }
}

private fun Task.getLocalImageTrackingFile(
  imageReference: String,
): Provider<RegularFile> {
  val fileName = URLEncoder.encode(imageReference, StandardCharsets.UTF_8)
  return project.layout.buildDirectory.file("images/local/$fileName")
}

private fun Task.getRegistryImageTrackingFile(
  imageReference: String,
): Provider<RegularFile> {
  val fileName = URLEncoder.encode(imageReference, StandardCharsets.UTF_8)
  return project.layout.buildDirectory.file("images/registry/$fileName")
}

private fun writeLocalImageId(imageReference: String, regularFile: RegularFile) {
  val file = regularFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  val imageId = Local.getImageId(imageReference)
  file.writeText(imageId ?: "")
}

private fun writeRegistryImageDigest(imageReference: String, regularFile: RegularFile) {
  val file = regularFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  val imageDigest = Registry.getImageDigest(imageReference)
  file.writeText(imageDigest ?: "")
}
