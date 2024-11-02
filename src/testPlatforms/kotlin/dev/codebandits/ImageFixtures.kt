package dev.codebandits

import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder

object ImageFixtures {
  private val imageCache = mutableMapOf<Any, ImageFromDockerfile>()

  enum class JavaVersion {
    TEMURIN_22,
    TEMURIN_21,
    TEMURIN_20,
    OPENJDK_17,
    OPENJDK_11,
  }

  fun dockerTemurinGradle(
    dockerVersion: String,
    javaVersion: JavaVersion,
    gradleVersion: String,
  ): ImageFromDockerfile {
    val imageIdentifier = Pair("docker-temurin-gradle", Triple(dockerVersion, javaVersion, gradleVersion))
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
