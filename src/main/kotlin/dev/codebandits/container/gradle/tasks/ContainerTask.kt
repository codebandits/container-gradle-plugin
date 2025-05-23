package dev.codebandits.container.gradle.tasks

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.PullImageResultCallback
import com.github.dockerjava.api.command.WaitContainerResultCallback
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.HostConfig
import com.github.dockerjava.api.model.StreamType
import dev.codebandits.container.gradle.image.Local
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class ContainerTask : DefaultTask() {

  @Internal
  protected val steps: MutableList<ExecutionStep> = mutableListOf()

  @TaskAction
  public fun run() {
    steps.forEach { step ->
      if (step.shouldRun()) {
        step.action()
      }
    }
  }

  public open class PullImageSpec(objects: ObjectFactory) {
    public val image: Property<String> = objects.property(String::class.java)
    public val containerHost: Property<String> = objects.property(String::class.java)
  }

  public open class RemoveImageSpec(objects: ObjectFactory) {
    public val image: Property<String> = objects.property(String::class.java)
    public val containerHost: Property<String> = objects.property(String::class.java)
  }

  public open class RunContainerSpec(objects: ObjectFactory) {
    public val image: Property<String> = objects.property(String::class.java)
    public val volumes: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    public val entrypoint: Property<String> = objects.property(String::class.java)
    public val cmd: ListProperty<String> = objects.listProperty(String::class.java).convention(emptyList())
    public val workdir: Property<String> = objects.property(String::class.java)
    public val user: Property<String> = objects.property(String::class.java)
    public val privileged: Property<Boolean> = objects.property(Boolean::class.java).convention(false)
    public val autoRemove: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    public val containerHost: Property<String> = objects.property(String::class.java)
  }

  public fun pullImage(configure: PullImageSpec.() -> Unit) {
    steps.add(
      ExecutionStep(
        action = {
          val spec = PullImageSpec(project.objects).apply(configure)
          val containerHost = spec.containerHost.orNull
          val dockerClient = Local.createDockerClient(containerHost)

          dockerClient
            .pullImageCmd(spec.image.get())
            .exec(PullImageResultCallback())
            .awaitCompletion()
        },
      )
    )
  }

  public fun removeImage(configure: RemoveImageSpec.() -> Unit) {
    steps.add(
      ExecutionStep(
        action = {
          val spec = RemoveImageSpec(project.objects).apply(configure)
          val containerHost = spec.containerHost.orNull
          val dockerClient = Local.createDockerClient(containerHost)

          dockerClient
            .removeImageCmd(spec.image.get())
            .exec()
        },
      )
    )
  }

  public fun runContainer(configure: RunContainerSpec.() -> Unit) {
    steps.add(
      ExecutionStep(
        action = {
          val spec = RunContainerSpec(project.objects).apply(configure)
          val containerHost = spec.containerHost.orNull
          val dockerClient = Local.createDockerClient(containerHost)

          val hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(spec.autoRemove.get())
            .withPrivileged(spec.privileged.get())
            .withBinds(spec.volumes.get().map(Bind::parse))

          val createContainerCmd = dockerClient.createContainerCmd(spec.image.get())
            .withHostConfig(hostConfig)
            .withCmd(spec.cmd.get())
            .let { cmd -> spec.user.orNull?.let { user -> cmd.withUser(user) } ?: cmd }
            .let { cmd -> spec.entrypoint.orNull?.let { user -> cmd.withEntrypoint(user) } ?: cmd }
            .let { cmd -> spec.workdir.orNull?.let { user -> cmd.withWorkingDir(user) } ?: cmd }

          val container = createContainerCmd.exec()

          dockerClient.startContainerCmd(container.id).exec()

          val containerLogCallback = dockerClient
            .logContainerCmd(container.id)
            .withStdOut(true)
            .withStdErr(true)
            .withSince(0)
            .withFollowStream(true)
            .exec(object : ResultCallback.Adapter<Frame>() {
              override fun onNext(frame: Frame) {
                when (frame.streamType) {
                  StreamType.STDOUT -> System.out.write(frame.payload)
                  StreamType.STDERR -> System.err.write(frame.payload)
                  else -> {}
                }
              }
            })

          val containerStatusCodeCallback = dockerClient
            .waitContainerCmd(container.id)
            .exec(WaitContainerResultCallback())

          containerLogCallback.awaitCompletion()

          val statusCode = containerStatusCodeCallback.awaitStatusCode()

          if (statusCode != 0) {
            throw GradleException("Container exited with status code $statusCode")
          }
        },
      )
    )
  }
}
