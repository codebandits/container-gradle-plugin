package dev.codebandits.container.gradle.image

internal data class ImageReferenceParts(
  val registry: String,
  val namespace: String,
  val image: String,
  val tag: String
) {
  val repository: String = "$namespace/$image"
  val normalized: String = "$registry/$namespace/$image:$tag"
}

internal fun String.toImageReferenceParts(): ImageReferenceParts {
  val stringValue = this
  val defaultRegistry = "docker.io"
  val defaultNamespace = "library"
  val defaultTag = "latest"
  val registry: String
  val namespace: String
  val imageAndTag: String

  if (stringValue.contains("/")) {
    val parts = stringValue.split("/", limit = 2)
    if (parts[0].contains(".")) {
      registry = parts[0]
      val namespaceAndImage = parts[1].split("/", limit = 2)
      if (namespaceAndImage.size == 2) {
        namespace = namespaceAndImage[0]
        imageAndTag = namespaceAndImage[1]
      } else {
        namespace = defaultNamespace
        imageAndTag = namespaceAndImage[0]
      }
    } else {
      registry = defaultRegistry
      namespace = parts[0]
      imageAndTag = parts[1]
    }
  } else {
    registry = defaultRegistry
    namespace = defaultNamespace
    imageAndTag = stringValue
  }

  val imageParts = imageAndTag.split(":")
  val repository = imageParts[0]
  val tag = if (imageParts.size > 1) imageParts[1] else defaultTag

  return ImageReferenceParts(
    registry = registry,
    namespace = namespace,
    image = repository,
    tag = tag
  )
}
