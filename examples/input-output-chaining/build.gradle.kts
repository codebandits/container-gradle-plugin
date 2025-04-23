import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask
import org.gradle.kotlin.dsl.support.serviceOf

plugins {
  id("dev.codebandits.container")
}

tasks {
  register("printImageID") {
    dependsOn("buildImage")
    container.inputs.localImage("my-image:latest")
    doLast {
      serviceOf<ExecOperations>().exec {
        commandLine("sh", "-c", "docker images --filter reference=my-image:latest --format {{.ID}}")
      }
    }
  }

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
    doLast {
      container.outputs.captureLocalImage("my-image:latest")
    }
  }
}
