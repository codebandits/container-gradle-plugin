import dev.codebandits.container.gradle.tasks.ContainerTask

plugins {
  id("dev.codebandits.container")
}

tasks {
  register<ContainerTask>("sayHello") {
    pullImage {
      image = "alpine:latest"
    }
    runContainer {
      image = "alpine:latest"
      cmd = listOf("echo", "Hello from a container!")
    }
  }

  register<ContainerTask>("writeHello") {
    pullImage {
      image = "alpine:latest"
    }
    runContainer {
      image = "alpine:latest"
      entrypoint = "sh"
      cmd = listOf("-c", "echo Hello from a container! > message.txt")
      workdir = "/workdir"
      volumes = listOf(
        "${layout.projectDirectory}:/workdir",
      )
    }
  }
}
