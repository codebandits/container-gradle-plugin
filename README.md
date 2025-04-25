# Container Gradle Plugin

Container is a Gradle plugin that enhances build portability, reproducibility, and flexibility by integrating containers
into Gradle tasks. It provides a declarative and familiar way to run task operations inside containers and declare
containers as task inputs and outputs.

## Status

⚠️ **Experimental**: This project is currently in its early stages (version `0.x.x`). The API and features may change as
we refine and improve the plugin. We're actively seeking feedback from early adopters to help shape its development.

## Features

- Create Gradle tasks that run operations inside containers.
- Declare containers as task inputs and outputs.

## Getting Started

Add the plugin to your `build.gradle.kts` file:

```kotlin
plugins {
  id("dev.codebandits.container") version "<latest-version>"
}
```

Create tasks that use containers:

```kotlin
import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask

tasks {
  register<ContainerTask>("writeHello") {
    container.inputs.registryImage("alpine:latest")
    dockerPull { image = "alpine:latest" }
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
```

Create tasks that produce images:

```kotlin
import dev.codebandits.container.gradle.container
import dev.codebandits.container.gradle.tasks.ContainerTask

tasks {
  register<ContainerTask>("buildWithDocker") {
    inputs.file("Dockerfile")
    container.outputs.localImage("docker-build-image:latest")
    dockerPull {
      image = "docker:dind"
    }
    dockerRun {
      image = "docker:dind"
      entrypoint = "docker"
      args = arrayOf("build", "-t", "docker-build-image:latest", ".")
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
    doLast {
      container.outputs.captureLocalImage("docker-build-image:latest")
    }
  }

  register<ContainerTask>("buildImageWithPack") {
    inputs.file("index.html")
    inputs.file("project.toml")
    container.outputs.localImage("pack-build-image:latest")
    dockerPull {
      image = "buildpacksio/pack:latest"
    }
    dockerRun {
      image = "buildpacksio/pack:latest"
      args = arrayOf(
        "build", "pack-build-image:latest",
        "--builder", "paketobuildpacks/builder-jammy-base:latest",
      )
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
    doLast {
      container.outputs.captureLocalImage("pack-build-image:latest")
    }
  }
}
```

To see more ways to use this plugin see the [examples folder](examples/).

## Contributing

We welcome contributions! Feel free to open an issue to discuss your ideas or share a pull request directly.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Made by Code Bandits 🦅
