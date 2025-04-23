package dev.codebandits.container.gradle.tasks

import dev.codebandits.container.gradle.docker.Docker
import dev.codebandits.container.gradle.docker.Registry
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


public abstract class TaskImages(private val task: Task) {
  protected class ImageIdentifierFileConfig(
    public val fileProvider: Provider<RegularFile?>,
    public val updateStep: ExecutionStep,
  )

  private fun getImageReferenceFileName(imageReference: String): String {
    return URLEncoder.encode(imageReference.toImageReferenceParts().normalized, StandardCharsets.UTF_8)
  }

  protected fun getDockerLocalImageIdentifierFileConfig(imageReference: String): ImageIdentifierFileConfig {
    val fileName = getImageReferenceFileName(imageReference)
    val imageIdentifierFileProvider = task.project.layout.buildDirectory.file("images/docker/local/$fileName")
    val updateStep = ExecutionStep(
      action = {
        val file = imageIdentifierFileProvider.get().asFile
        val dockerClient = Docker.createClient()
        try {
          dockerClient.pingCmd().exec()
          val inspectImageResponse = dockerClient.inspectImageCmd(imageReference).exec()
          val imageId = inspectImageResponse.id
          file.parentFile.mkdirs()
          if (imageId != null) {
            file.writeText(imageId)
          } else {
            file.delete()
          }
        } catch (exception: Exception) {
          if (exception.javaClass.name == "com.github.dockerjava.api.exception.NotFoundException") {
            file.delete()
          } else {
            throw exception
          }
        }
      },
      resultHandler = { result ->
        if (result.exitValue != 0) {
          val file = imageIdentifierFileProvider.get().asFile
          file.delete()
        }
      },
    )

    return ImageIdentifierFileConfig(
      fileProvider = imageIdentifierFileProvider.map { regularFile ->
        task.apply(updateStep.action::execute)
        regularFile
      },
      updateStep = updateStep,
    )
  }

  protected fun getDockerRegistryImageIdentifierFileProvider(
    imageReference: String,
    autoRefresh: Boolean,
  ): ImageIdentifierFileConfig {
    val fileName = getImageReferenceFileName(imageReference)
    val imageIdentifierFileProvider = task.project.layout.buildDirectory.file("images/docker/registry/$fileName")
    val updateStep = ExecutionStep(
      action = {
        val file = imageIdentifierFileProvider.get().asFile
        val digest = Registry.getDigest(imageReference)
        file.parentFile.mkdirs()
        file.writeText(digest)
      },
      resultHandler = { result ->
        if (result.exitValue != 0) {
          val file = imageIdentifierFileProvider.get().asFile
          file.delete()
        }
      },
      shouldRun = { autoRefresh || !imageIdentifierFileProvider.get().asFile.exists() },
    )

    return ImageIdentifierFileConfig(
      fileProvider = imageIdentifierFileProvider.map { regularFile ->
        task.apply(updateStep.action::execute)
        regularFile
      },
      updateStep = updateStep,
    )
  }

  public open class Input(private val task: Task) : TaskImages(task) {
    public fun dockerLocal(imageReference: String) {
      val config = getDockerLocalImageIdentifierFileConfig(
        imageReference = imageReference,
      )
      task.inputs.file(config.fileProvider)
    }

    public fun dockerRegistry(imageReference: String, autoRefresh: Boolean = false) {
      val config = getDockerRegistryImageIdentifierFileProvider(
        imageReference = imageReference,
        autoRefresh = autoRefresh,
      )
      task.inputs.file(config.fileProvider)
    }
  }

  public open class Output(private val task: Task) : TaskImages(task) {
    public fun dockerLocal(imageReference: String) {
      val config = getDockerLocalImageIdentifierFileConfig(
        imageReference = imageReference,
      )
      task.outputs.file(config.fileProvider)
      task.doLast { task -> task.apply(config.updateStep.action::execute) }
    }

    public fun dockerRegistry(imageReference: String, autoRefresh: Boolean = false) {
      val config = getDockerRegistryImageIdentifierFileProvider(
        imageReference = imageReference,
        autoRefresh = autoRefresh,
      )
      task.outputs.file(config.fileProvider)
      task.doLast { task -> task.apply(config.updateStep.action::execute) }
    }
  }
}
