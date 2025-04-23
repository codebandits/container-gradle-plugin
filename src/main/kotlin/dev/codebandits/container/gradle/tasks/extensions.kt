package dev.codebandits.container.gradle.tasks

import org.gradle.api.Task

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
