import dev.codebandits.container.gradle.tasks.ContainerRunTask
import dev.codebandits.container.gradle.tasks.outputImages

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerRunTask>("buildImage") {
    inputs.file("index.html")
    inputs.file("project.toml")
    outputImages.dockerLocal("my-image:latest")
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
