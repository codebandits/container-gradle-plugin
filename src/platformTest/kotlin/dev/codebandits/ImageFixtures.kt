package dev.codebandits

import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder

object ImageFixtures {
  private val imageCache = mutableMapOf<Any, ImageFromDockerfile>()

  fun dockerTemurinGradle(dockerVersion: String, javaVersion: String, gradleVersion: String): ImageFromDockerfile {
    val imageIdentifier = Pair("docker-temurin-gradle", Triple(dockerVersion, javaVersion, gradleVersion))
    return imageCache.getOrPut(imageIdentifier) {
      ImageFromDockerfile()
        .withDockerfileFromBuilder { builder ->
          builder
            .from("docker:$dockerVersion-dind")
            .alpineInstallTemurin(javaVersion = javaVersion)
            .installGradle(gradleVersion = gradleVersion)
            .keepalive()
        }
    }
  }

  private fun DockerfileBuilder.alpineInstallTemurin(javaVersion: String): DockerfileBuilder {
    return run(buildString {
      append("wget -O /etc/apk/keys/adoptium.rsa.pub https://packages.adoptium.net/artifactory/api/security/keypair/public/repositories/apk")
      append(" && echo 'https://packages.adoptium.net/artifactory/apk/alpine/main' >> /etc/apk/repositories")
      append(" && apk add --update --no-cache --no-progress temurin-$javaVersion-jdk")
    })
  }

  private fun DockerfileBuilder.installGradle(gradleVersion: String): DockerfileBuilder {
    return env("GRADLE_HOME", "/opt/gradle/gradle-$gradleVersion")
      .env("PATH", "${'$'}GRADLE_HOME/bin:${'$'}PATH")
      .run(buildString {
        append("wget https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip")
        append(" && mkdir -p /opt/gradle")
        append(" && unzip -d /opt/gradle gradle-$gradleVersion-bin.zip")
        append(" && rm gradle-$gradleVersion-bin.zip")
      })
  }

  private fun DockerfileBuilder.keepalive(): DockerfileBuilder {
    return cmd("tail", "-f", "/dev/null")
  }
}
