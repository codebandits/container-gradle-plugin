package dev.codebandits.container.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

public abstract class ContainerExecTask : DefaultTask() {
  @Internal
  protected val actionSteps: MutableList<ExecutionStep> = mutableListOf<ExecutionStep>()

  @TaskAction
  public fun run() {
    actionSteps.forEach { action ->
      project.runExecutionStep(action)
    }
  }
}
