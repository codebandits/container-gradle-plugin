package dev.codebandits

import org.gradle.api.Plugin
import org.gradle.api.Project

public class ContainerPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.tasks.all {
    }
  }
}
