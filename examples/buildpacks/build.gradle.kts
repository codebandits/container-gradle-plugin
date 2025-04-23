import dev.codebandits.container.gradle.tasks.ContainerTask
import dev.codebandits.container.gradle.tasks.outputLocalImage

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("buildImage") {
    inputs.file("index.html")
    inputs.file("project.toml")
    outputLocalImage("my-image:latest")
    dockerPull {
      image = "buildpacksio/pack:latest"
    }
    dockerRun {
      image = "buildpacksio/pack:latest"
      args = arrayOf(
        "build", "my-image:latest",
        "--builder", "paketobuildpacks/builder-jammy-base:latest",
      )
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
  }
}
