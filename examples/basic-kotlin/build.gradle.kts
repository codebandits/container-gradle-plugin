import dev.codebandits.container.gradle.tasks.ContainerRunTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerRunTask>("sayHello") {
    dockerPull {
      image = "alpine:latest"
    }
    dockerRun {
      image = "alpine:latest"
      args = arrayOf("echo", "Hello from a container!")
    }
  }

  register<ContainerRunTask>("writeHello") {
    dockerPull {
      image = "alpine:latest"
    }
    dockerRun {
      image = "alpine:latest"
      entrypoint = "sh"
      args = arrayOf("-c", "echo Hello from a container! > message.txt")
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
      )
    }
  }
}
