package dev.codebandits.container.gradle.tasks

import org.gradle.process.ExecResult

public class ExecutionStep(
  public val action: () -> Unit,
  public val resultHandler: ((ExecResult) -> Unit)? = null,
  public val shouldRun: () -> Boolean = { true },
)
