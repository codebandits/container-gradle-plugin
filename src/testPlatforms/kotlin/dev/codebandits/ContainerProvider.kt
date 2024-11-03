package dev.codebandits

import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder

object ContainerProvider {
  fun dockerJavaGradle(
    dockerVersion: String,
    javaVersion: JavaVersion,
    gradleVersion: String,
  ): GenericContainer<*> {
    val image = ImageFixtures.dockerJavaGradle(
      dockerVersion = dockerVersion,
      javaVersion = javaVersion,
      gradleVersion = gradleVersion,
    )
    val container: GenericContainer<*> = GenericContainer(image)
    if (System.getenv("CI") == "true") {
      container.withStartupAttempts(3)
    }
    return container
  }
}

private object ImageFixtures {
  private val imageCache = mutableMapOf<List<Any>, ImageFromDockerfile>()

  fun dockerJavaGradle(
    dockerVersion: String,
    javaVersion: JavaVersion,
    gradleVersion: String,
  ): ImageFromDockerfile {
    val imageIdentifier = listOf("docker-java-gradle", dockerVersion, javaVersion, gradleVersion)
    return imageCache.getOrPut(imageIdentifier) {
      ImageFromDockerfile()
        .withDockerfileFromBuilder { builder ->
          builder.from("docker:$dockerVersion-dind")
          when (javaVersion) {
            JavaVersion.TEMURIN_22 -> builder.alpineInstallTemurin("22")
            JavaVersion.TEMURIN_21 -> builder.alpineInstallTemurin("21")
            JavaVersion.TEMURIN_20 -> builder.alpineInstallTemurin("20")
            JavaVersion.OPENJDK_17 -> builder.alpineInstallOpenjdk("17")
            JavaVersion.OPENJDK_11 -> builder.alpineInstallOpenjdk("11")
          }
          builder.installGradle(gradleVersion = gradleVersion)
          builder.keepalive()
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

  private fun DockerfileBuilder.alpineInstallOpenjdk(javaVersion: String): DockerfileBuilder {
    return run("apk add --update --no-cache openjdk$javaVersion")
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
