package dev.codebandits.container.gradle.plugin

import dev.codebandits.container.gradle.image.ContainerTaskExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

public class ContainerPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.tasks.all { task ->
      task.extensions.create("container", ContainerTaskExtension::class.java, task)
    }
  }
}
