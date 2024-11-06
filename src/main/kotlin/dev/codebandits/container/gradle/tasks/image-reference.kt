package dev.codebandits.container.gradle.tasks

internal fun normalizeDockerImageReference(imageReference: String): String {
  val defaultRegistry = "docker.io"
  val defaultNamespace = "library"
  val defaultTag = "latest"
  val registryAndNamespace: String
  val imageAndTag: String
  if (imageReference.contains("/")) {
    val parts = imageReference.split("/", limit = 2)
    if (parts[0].contains(".")) {
      registryAndNamespace = parts[0]
      imageAndTag = parts[1]
    } else {
      registryAndNamespace = "$defaultRegistry/${parts[0]}"
      imageAndTag = parts[1]
    }
  } else {
    registryAndNamespace = "$defaultRegistry/$defaultNamespace"
    imageAndTag = imageReference
  }

  val imageParts = imageAndTag.split(":")
  val imageName = imageParts[0]
  val imageTag = if (imageParts.size > 1) imageParts[1] else defaultTag

  return "$registryAndNamespace/$imageName:$imageTag"
}
