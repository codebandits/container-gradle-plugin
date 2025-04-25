import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("buildImage") {
    inputs.file("index.html")
    inputs.file("project.toml")
    container.outputs.localImage("my-image:latest")
    pullImage {
      image = "buildpacksio/pack:latest"
    }
    runContainer {
      image = "buildpacksio/pack:latest"
      cmd = listOf(
        "build", "my-image:latest",
        "--builder", "paketobuildpacks/builder-jammy-base:latest",
      )
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
