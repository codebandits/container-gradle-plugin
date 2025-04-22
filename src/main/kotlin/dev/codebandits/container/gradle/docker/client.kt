package dev.codebandits.container.gradle.docker

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient
import com.github.dockerjava.transport.DockerHttpClient

private fun createDockerClientConfig(dockerHost: String? = null): DefaultDockerClientConfig {
  var builder = DefaultDockerClientConfig.createDefaultConfigBuilder()
  if (dockerHost != null) {
    builder = builder.withDockerHost(dockerHost)
  }
  return builder.build()
}

internal fun createDockerHttpClient(config: DefaultDockerClientConfig = createDockerClientConfig()): DockerHttpClient {
  return ApacheDockerHttpClient.Builder().dockerHost(config.dockerHost).build()
}

internal fun createDockerClient(dockerHost: String? = null): DockerClient {
  val config = createDockerClientConfig(dockerHost)
  val httpClient = createDockerHttpClient(config)
  return DockerClientImpl.getInstance(config, httpClient)
}
