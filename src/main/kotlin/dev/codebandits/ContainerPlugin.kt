package dev.codebandits

import org.gradle.api.Plugin
import org.gradle.api.Project


class ContainerPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.tasks.register("helloWorld") {
      it.group = "example"
      it.description = "Prints 'Hello, world!' to the console."
      it.doLast {
        println("Hello, world!")
      }
    }
  }
}
