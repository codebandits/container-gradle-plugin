import dev.codebandits.container.gradle.tasks.ContainerTask
import dev.codebandits.container.gradle.tasks.outputImages

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("buildImage") {
    inputs.file("Dockerfile")
    outputImages.dockerLocal("my-image:latest")
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
