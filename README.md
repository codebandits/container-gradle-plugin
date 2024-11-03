# Container

Container is a Gradle plugin that enhances build portability, reproducibility, and flexibility by integrating containers into Gradle tasks. It provides a declarative and familiar way to run task operations inside containers and declare containers as task inputs and outputs.

## Status

⚠️ **Experimental**: This project is currently in its early stages (version `0.x.x`). The API and features may change as we refine and improve the plugin. We're actively seeking feedback from early adopters to help shape its development.

## Features

- Create Gradle tasks that run operations inside containers.
- Declare containers as task inputs and outputs.

## Getting Started

This plugin is published to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/dev.codebandits.container). Add the plugin to your `build.gradle.kts` file:

```kotlin
plugins {
  id("dev.codebandits.container") version "<latest-version>"
}
```

Create tasks that use containers:

```kotlin
import dev.codebandits.ContainerRunTask

tasks {
  register<ContainerRunTask>("sayHello") {
    dockerRun {
      image = "alpine:latest"
      args = arrayOf("echo", "Hello from a container!")
    }
  }

  register<ContainerRunTask>("writeHello") {
    dockerRun {
      image = "alpine:latest"
      entrypoint = "sh"
      args = arrayOf("-c", "echo 'Hello from a container!' > message.txt")
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
      )
    }
  }
}
```

Declare containers as task inputs and outputs:

```kotlin
import dev.codebandits.ContainerRunTask

tasks {
  register<ContainerRunTask>("buildContainer") {
    outputImages.dockerLocal("my-image:latest")
    dockerRun {
      image = "buildpacksio/pack:latest"
      args = arrayOf(
        "build", "my-image:latest",
        "--builder", "paketobuildpacks/builder-jammy-base:latest"
      )
      workdir = "/workdir"
      volumes = arrayOf(
        "${layout.projectDirectory}:/workdir",
        "/var/run/docker.sock:/var/run/docker.sock:ro",
      )
    }
  }

  register<ContainerRunTask>("useContainer") {
    dependsOn("buildContainer")
    inputImages.dockerLocal("my-image:latest")
    dockerRun {
      image = "my-image:latest"
      entrypoint = "echo"
      args = arrayOf("echo", "Hello from my custom container!")
    }
  }
}
```

## Contributing

We welcome contributions! Feel free to open an issue to discuss your ideas or share a pull request directly.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---

Made by Code Bandits 🦅
