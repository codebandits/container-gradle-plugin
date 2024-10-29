package dev.codebandits

import org.gradle.api.Plugin
import org.gradle.api.Project

public class ContainerPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.tasks.all { task ->
      task.extensions.create("inputImages", TaskImages.Input::class.java, task)
      task.extensions.create("outputImages", TaskImages.Output::class.java, task)
    }
  }
}
