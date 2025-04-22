package dev.codebandits.container.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.process.ExecResult

public class ExecutionStep(
  public val action: Action<Task>,
  public val resultHandler: ((ExecResult) -> Unit)? = null,
  public val shouldRun: () -> Boolean = { true },
)

internal fun Task.run(step: ExecutionStep) {
  if (step.shouldRun()) {
    apply(step.action::execute)
  }
}
