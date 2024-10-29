package dev.codebandits

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class ContainerRunTask : ContainerExecTask() {
  public open class DockerRunSpec(objects: ObjectFactory) {
    public val image: Property<String> = objects.property(String::class.java)
    public val entrypoint: Property<String> = objects.property(String::class.java)
    public val containerArgs: Property<Array<String>> =
      objects.property(Array<String>::class.java).convention(emptyArray())
    public val workdir: DirectoryProperty = objects.directoryProperty()
    public val autoRemove: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    public val alwaysPull: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    public val daemonSocketPath: Property<String> = objects.property(String::class.java)
  }

  public fun dockerRun(configure: DockerRunSpec.() -> Unit) {
    val spec = DockerRunSpec(project.objects).apply(configure)
    val image = spec.image.get()
    val containerArgs = spec.containerArgs.get()
    val options = mutableListOf<String>()
    if (spec.autoRemove.get()) {
      options.add("--rm")
    }
    val daemonSocketPath = spec.daemonSocketPath.orNull
    val entrypoint = spec.entrypoint.orNull
    if (entrypoint != null) {
      options.addAll(listOf("--entrypoint", entrypoint))
    }
    val workdirPath = spec.workdir.orNull?.asFile?.absolutePath
    if (workdirPath != null) {
      options.addAll(listOf("--volume", "$workdirPath:/workdir", "--workdir", "/workdir"))
    }
    val dockerArgs = arrayOf("run", *options.toTypedArray(), image, *containerArgs)
    actionSteps.add(
      ExecutionStep(
        execAction = {
          executable = Commands.dockerPath
          args(*dockerArgs)
          if (daemonSocketPath != null) {
            environment("DOCKER_HOST", "unix://$daemonSocketPath")
          }
        },
      )
    )
  }
}
