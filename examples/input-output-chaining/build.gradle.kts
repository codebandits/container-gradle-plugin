import dev.codebandits.ContainerRunTask
import dev.codebandits.inputImages
import dev.codebandits.outputImages

plugins {
  id("dev.codebandits.container")
}

tasks {
  register("printImageID") {
    dependsOn("buildImage")
    inputImages.dockerLocal("my-image:latest")
    doLast {
      exec {
        commandLine("sh", "-c", "docker images --filter reference=my-image:latest --format {{.ID}}")
      }
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
