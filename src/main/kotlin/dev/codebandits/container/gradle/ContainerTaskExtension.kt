package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.image.Local
import dev.codebandits.container.gradle.image.Registry
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

public abstract class ContainerTaskExtension(private val task: Task) {
  public fun inputLocalImage(imageReference: String) {
    val trackingFile = task.getLocalImageTrackingFile(imageReference)
    task.inputs.file(trackingFile.map { regularFile ->
      writeLocalImageId(imageReference, regularFile)
      regularFile
    })
  }

  public fun outputLocalImage(imageReference: String) {
    val trackingFile = task.getLocalImageTrackingFile(imageReference)
    task.outputs.file(trackingFile.map { regularFile ->
      writeLocalImageId(imageReference, regularFile)
      regularFile
    })
    task.doLast { writeLocalImageId(imageReference, trackingFile.get()) }
  }

  public fun inputRegistryImage(imageReference: String, autoRefresh: Boolean = false) {
    val trackingFile = task.getRegistryImageTrackingFile(imageReference)
    task.inputs.file(trackingFile.map { regularFile ->
      if (autoRefresh || !regularFile.asFile.exists()) {
        writeRegistryImageDigest(imageReference, regularFile)
      }
      regularFile
    })
  }

  public fun outputRegistryImage(imageReference: String, autoRefresh: Boolean = false) {
    val trackingFile = task.getLocalImageTrackingFile(imageReference)
    task.outputs.file(trackingFile.map { regularFile ->
      if (autoRefresh || !regularFile.asFile.exists()) {
        writeRegistryImageDigest(imageReference, regularFile)
      }
      regularFile
    })
    task.doLast {
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
}
