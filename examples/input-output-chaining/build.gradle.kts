import dev.codebandits.ContainerRunTask
import dev.codebandits.inputImages
import dev.codebandits.outputImages

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerRunTask>("printImageID") {
    dependsOn("buildImage")
    inputImages.dockerLocal("my-image:latest")
    dockerRun {
      image = "docker:dind"
      entrypoint = "docker"
      args = arrayOf("images", "--filter", "reference=my-image:latest", "--format", "{{.ID}}")
      volumes = arrayOf(
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
  }

  register<ContainerRunTask>("buildImage") {
    inputs.file("Dockerfile")
    outputImages.dockerLocal("my-image:latest")
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
