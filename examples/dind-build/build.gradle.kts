import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("buildImage") {
    inputs.file("Dockerfile")
    container.outputs.localImage("my-image:latest")
    pullImage {
      image = "docker:dind"
    }
    runContainer {
      image = "docker:dind"
      entrypoint = "docker"
      cmd = listOf("build", "-t", "my-image:latest", ".")
      workdir = "/workdir"
      volumes = listOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
    doLast {
      container.outputs.captureLocalImage("my-image:latest")
    }
  }
}
