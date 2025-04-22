import dev.codebandits.container.gradle.tasks.ContainerRunTask
import dev.codebandits.container.gradle.tasks.inputImages
import dev.codebandits.container.gradle.tasks.outputImages
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  id("dev.codebandits.container")
}

tasks {
  register("printImageID") {
    dependsOn("buildImage")
    inputImages.dockerLocal("my-image:latest")
    doLast {
      serviceOf<ExecOperations>().exec {
        commandLine("sh", "-c", "docker images --filter reference=my-image:latest --format {{.ID}}")
      }
    }
  }

  register<ContainerRunTask>("buildImage") {
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
