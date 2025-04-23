package dev.codebandits.container.gradle

import org.gradle.api.Task

public val Task.container: ContainerTaskExtension get() = extensions.getByName("container") as ContainerTaskExtension
