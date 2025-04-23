package dev.codebandits.container.gradle.image

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

internal object Local {
  internal fun getImageId(imageReference: String): String? {
    val dockerClient = Local.createClient()
    try {
      val inspectImageResponse = dockerClient.inspectImageCmd(imageReference).exec()
      return inspectImageResponse.id
    } catch (_: NotFoundException) {
      return null
    }
  }

  internal fun createClient(dockerHost: String? = null): DockerClient {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
      .let { it -> if (dockerHost == null) it else it.withDockerHost(dockerHost) }
      .build()
    val httpClient = ApacheDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
    return DockerClientImpl.getInstance(config, httpClient)
  }
}
