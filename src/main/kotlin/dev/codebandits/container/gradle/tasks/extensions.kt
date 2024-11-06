package dev.codebandits.container.gradle.tasks

import org.gradle.api.Task

public val Task.inputImages: TaskImages.Input get() = extensions.getByName("inputImages") as TaskImages.Input
public val Task.outputImages: TaskImages.Output get() = extensions.getByName("outputImages") as TaskImages.Output
