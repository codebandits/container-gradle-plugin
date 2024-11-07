package dev.codebandits.container.gradle.tasks

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public abstract class ContainerRunTask : ContainerExecTask() {
  public open class DockerRunSpec(objects: ObjectFactory) {
    public val image: Property<String> = objects.property(String::class.java)
    public val volumes: Property<Array<String>> = objects.property(Array<String>::class.java).convention(emptyArray())
    public val entrypoint: Property<String> = objects.property(String::class.java)
    public val args: Property<Array<String>> = objects.property(Array<String>::class.java).convention(emptyArray())
    public val workdir: Property<String> = objects.property(String::class.java)
    public val user: Property<String> = objects.property(String::class.java)
    public val privileged: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    public val autoRemove: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    public val alwaysPull: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    public val dockerHost: Property<String> = objects.property(String::class.java)
  }

  public fun dockerRun(configure: DockerRunSpec.() -> Unit) {
    val spec = DockerRunSpec(project.objects).apply(configure)
    val image = spec.image.get()
    val options = mutableListOf<String>()
    val user = spec.user.orNull
    if (user != null) {
      options.addAll(listOf("--user", user))
    }
    if (spec.privileged.get()) {
      options.add("--privileged")
    }
    if (spec.autoRemove.get()) {
      options.add("--rm")
    }
    spec.volumes.get().forEach { volume ->
      options.addAll(listOf("--volume", volume))
    }
    val entrypoint = spec.entrypoint.orNull
    if (entrypoint != null) {
      options.addAll(listOf("--entrypoint", entrypoint))
    }
    val workdir = spec.workdir.orNull
    if (workdir != null) {
      options.addAll(listOf("--workdir", workdir))
    }
    val dockerArgs = arrayOf("run", *options.toTypedArray(), image, *spec.args.get())
    actionSteps.add(
      ExecutionStep(
        execAction = {
          executable = Commands.dockerPath
          args(*dockerArgs)
          val dockerHost = spec.dockerHost.orNull
          if (dockerHost != null) {
            environment("DOCKER_HOST", dockerHost)
          }
        },
      )
    )
  }
}
