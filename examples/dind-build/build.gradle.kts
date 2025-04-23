import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("buildImage") {
    inputs.file("Dockerfile")
    container.outputs.localImage("my-image:latest")
    dockerPull {
      image = "docker:dind"
    }
    dockerRun {
      image = "docker:dind"
      entrypoint = "docker"
      args = arrayOf("build", "-t", "my-image:latest", ".")
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
  }
}
