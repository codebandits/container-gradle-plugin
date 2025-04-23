package dev.codebandits.container.gradle

import dev.codebandits.container.gradle.image.Local
import dev.codebandits.container.gradle.image.Registry
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

public abstract class ContainerTaskExtension(task: Task) {
  public val inputs: Inputs = Inputs(task)
  public val outputs: Outputs = Outputs(task)

  public class Inputs internal constructor(private val task: Task) {
    public fun localImage(imageReference: String) {
      val trackingFileProvider = task.getLocalImageTrackingFile(imageReference)
      task.inputs.file(trackingFileProvider.map { trackingFile ->
        writeLocalImageId(imageReference, trackingFile)
        trackingFile
      })
    }

    public fun registryImage(imageReference: String, autoRefresh: Boolean = false) {
      val trackingFile = task.getRegistryImageTrackingFile(imageReference)
      task.inputs.file(trackingFile.map { regularFile ->
        if (autoRefresh || !regularFile.asFile.exists()) {
          writeRegistryImageDigest(imageReference, regularFile)
        }
        regularFile
      })
    }
  }

  public class Outputs internal constructor(private val task: Task) {
    public fun localImage(imageReference: String) {
      val trackingFileProvider = task.getLocalImageTrackingFile(imageReference)
      task.outputs.file(trackingFileProvider.map { trackingFile ->
        writeLocalImageId(imageReference, trackingFile)
        trackingFile
      })
    }

    public fun captureLocalImage(imageReference: String) {
      val trackingFile = task.getLocalImageTrackingFile(imageReference).get()
      writeLocalImageId(imageReference, trackingFile)
    }

    public fun registryImage(imageReference: String, autoRefresh: Boolean = false) {
      val trackingFileProvider = task.getRegistryImageTrackingFile(imageReference)
      task.outputs.file(trackingFileProvider.map { trackingFile ->
        if (autoRefresh || !trackingFile.asFile.exists()) {
          writeRegistryImageDigest(imageReference, trackingFile)
        }
        trackingFile
      })
      task.doLast {
        val regularFile = trackingFileProvider.get()
        if (autoRefresh || !regularFile.asFile.exists()) {
          writeRegistryImageDigest(imageReference, regularFile)
        }
      }
    }

    public fun captureRegistryImage(imageReference: String) {
      val trackingFile = task.getRegistryImageTrackingFile(imageReference).get()
      writeRegistryImageDigest(imageReference, trackingFile)
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

private fun writeLocalImageId(imageReference: String, trackingFile: RegularFile) {
  val file = trackingFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  val imageId = Local.getImageId(imageReference)
  when (imageId) {
    null -> file.delete()
    else -> file.writeText(imageId)
  }
}

private fun writeRegistryImageDigest(imageReference: String, trackingFile: RegularFile) {
  val file = trackingFile.asFile
  if (!file.parentFile.exists()) {
    file.parentFile.mkdirs()
  }
  val imageDigest = Registry.getImageDigest(imageReference)
  when (imageDigest) {
    null -> file.delete()
    else -> file.writeText(imageDigest)
  }
}
