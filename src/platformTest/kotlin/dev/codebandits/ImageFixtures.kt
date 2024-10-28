package dev.codebandits

import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder

object ImageFixtures {
  private val imageCache = mutableMapOf<Any, ImageFromDockerfile>()

  fun dockerTemurin(dockerVersion: String, javaVersion: String): ImageFromDockerfile {
    val imageIdentifier = Triple("docker-temurin", dockerVersion, javaVersion)
    return imageCache.getOrPut(imageIdentifier) {
      ImageFromDockerfile()
        .withDockerfileFromBuilder { builder ->
          builder
            .from("docker:$dockerVersion-dind")
            .alpineInstallTemurin(javaVersion = javaVersion)
            .keepalive()
        }
    }
  }

  private fun DockerfileBuilder.alpineInstallTemurin(javaVersion: String): DockerfileBuilder {
    return run("wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk")
      .run("echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories")
      .run("apk add --update --no-cache --no-progress temurin-$javaVersion-jdk")
  }

  private fun DockerfileBuilder.keepalive(): DockerfileBuilder {
    return cmd("tail", "-f", "/dev/null")
  }
}
