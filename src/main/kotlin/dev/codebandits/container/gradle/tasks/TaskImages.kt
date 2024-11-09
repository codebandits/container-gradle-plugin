package dev.codebandits.container.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import java.io.OutputStream
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
      execAction = {
        val file = imageIdentifierFileProvider.get().asFile
        file.parentFile.mkdirs()
        executable = "docker"
        args("inspect", "--format", "{{.Id}}", imageReference)
        standardOutput = imageIdentifierFileProvider.get().asFile.outputStream()
        errorOutput = OutputStream.nullOutputStream()
        isIgnoreExitValue = true
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
        task.project.runExecutionStep(step = updateStep)
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
      execAction = {
        val file = imageIdentifierFileProvider.get().asFile
        file.parentFile.mkdirs()
        executable = "docker"
        args("manifest", "inspect", imageReference)
        standardOutput = file.outputStream()
        isIgnoreExitValue = true
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
        task.project.runExecutionStep(step = updateStep)
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
      task.doLast { task -> task.project.runExecutionStep(config.updateStep) }
    }

    public fun dockerRegistry(imageReference: String, autoRefresh: Boolean = false) {
      val config = getDockerRegistryImageIdentifierFileProvider(
        imageReference = imageReference,
        autoRefresh = autoRefresh,
      )
      task.outputs.file(config.fileProvider)
      task.doLast { task -> task.project.runExecutionStep(config.updateStep) }
    }
  }
}
