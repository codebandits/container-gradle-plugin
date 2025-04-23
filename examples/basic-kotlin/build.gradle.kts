import dev.codebandits.container.gradle.tasks.ContainerTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("sayHello") {
    dockerPull {
      image = "alpine:latest"
    }
    dockerRun {
      image = "alpine:latest"
      args = arrayOf("echo", "Hello from a container!")
    }
  }

  register<ContainerTask>("writeHello") {
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
