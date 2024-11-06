package dev.codebandits.container.gradle.tasks

import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec

public class ExecutionStep(
  public val execAction: ExecSpec.() -> Unit,
  public val resultHandler: ((ExecResult) -> Unit)? = null,
  public val shouldRun: () -> Boolean = { true },
)

internal fun Project.runExecutionStep(step: ExecutionStep) {
  if (step.shouldRun()) {
    val result = exec(step.execAction)
    step.resultHandler?.invoke(result)
  }
}
