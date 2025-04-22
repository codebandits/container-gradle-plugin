package dev.codebandits.container.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class ContainerExecTask : DefaultTask() {
  @Internal
  protected val steps: MutableList<ExecutionStep> = mutableListOf()

  @TaskAction
  public fun run() {
    steps.forEach(::run)
  }
}
