package dev.codebandits.container.gradle.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient

internal object Docker {
  internal fun createClient(dockerHost: String? = null): DockerClient {
    val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
      .let { it -> if (dockerHost == null) it else it.withDockerHost(dockerHost) }
      .build()
    val httpClient = ApacheDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
    return DockerClientImpl.getInstance(config, httpClient)
  }
}
