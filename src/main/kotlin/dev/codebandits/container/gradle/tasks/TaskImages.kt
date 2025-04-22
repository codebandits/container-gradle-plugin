package dev.codebandits.container.gradle.tasks

import com.github.dockerjava.transport.DockerHttpClient
import dev.codebandits.container.gradle.docker.createDockerClient
import dev.codebandits.container.gradle.docker.createDockerHttpClient
import org.gradle.api.GradleException
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
    return URLEncoder.encode(normalizeDockerImageReference(imageReference), StandardCharsets.UTF_8)
  }

  protected fun getDockerLocalImageIdentifierFileConfig(imageReference: String): ImageIdentifierFileConfig {
    val fileName = getImageReferenceFileName(imageReference)
    val imageIdentifierFileProvider = task.project.layout.buildDirectory.file("images/docker/local/$fileName")
    val updateStep = ExecutionStep(
      action = {
        val file = imageIdentifierFileProvider.get().asFile
        val dockerClient = createDockerClient()
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
        task.run(updateStep)
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
        val dockerHttpClient = createDockerHttpClient()

        val (repo, tag) = imageReference
          .split(":", limit = 2)
          .let { it[0] to it.getOrElse(1) { "latest" } }
        val path = "/v2/$repo/manifests/$tag"

        val request = DockerHttpClient.Request.builder()
          .method(DockerHttpClient.Request.Method.GET)
          .path(path)
          .putHeader("Accept", "application/vnd.docker.distribution.manifest.v2+json")
          .build()

        dockerHttpClient.execute(request).use { response ->
          when (response.statusCode) {
            200 -> {
              val digest = response.headers["Docker-Content-Digest"]?.firstOrNull()
              if (digest != null) {
                file.parentFile.mkdirs()
                file.writeText(digest)
              } else {
                file.delete()
              }
            }

            404 -> {
              file.delete()
            }

            else -> throw GradleException(
              "Failed to fetch manifest for $imageReference: HTTP ${response.statusCode}"
            )
          }
        }
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
        task.run(updateStep)
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
      task.doLast { task -> task.run(config.updateStep) }
    }

    public fun dockerRegistry(imageReference: String, autoRefresh: Boolean = false) {
      val config = getDockerRegistryImageIdentifierFileProvider(
        imageReference = imageReference,
        autoRefresh = autoRefresh,
      )
      task.outputs.file(config.fileProvider)
      task.doLast { task -> task.run(config.updateStep) }
    }
  }
}
