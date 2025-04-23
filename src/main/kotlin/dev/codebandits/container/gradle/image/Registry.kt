package dev.codebandits.container.gradle.image

import org.gradle.api.GradleException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

internal object Registry {
  private val httpClient = HttpClient.newBuilder().build()

  internal fun getImageDigest(imageReference: String): String? {
    val imageReferenceParts = imageReference.toImageReferenceParts()
    val httpResponse = run {
      val httpRequest = buildRegistryManifestRequest(imageReferenceParts)
      httpClient.send(httpRequest, HttpResponse.BodyHandlers.discarding())
    }

    return when (httpResponse.statusCode()) {
      200 -> httpResponse.headers().firstValue("Docker-Content-Digest")
        .orElseThrow { GradleException("Expected Docker-Content-Digest header from registry: ${httpResponse.headers()}") }

      404 -> null

      else -> throw GradleException("Failed to fetch manifest for $imageReference: HTTP ${httpResponse.statusCode()}")
    }
  }

  private fun getPullToken(registry: String, repository: String): String {
    return fetchToken(
      registry = registry,
      scope = "repository:${repository}:pull"
    )
  }

  private fun getHost(registry: String): String = when (registry) {
    "docker.io" -> "registry-1.docker.io"
    else -> registry
  }

  private fun buildRegistryManifestRequest(parts: ImageReferenceParts): HttpRequest {
    val registryHost = getHost(parts.registry)
    val registryToken = getPullToken(parts.registry, parts.repository)
    val path = "/v2/${parts.namespace}/${parts.image}/manifests/${parts.tag}"
    val manifestUri = URI.create("https://$registryHost$path")

    return HttpRequest.newBuilder()
      .uri(manifestUri)
      .header(
        "Accept", listOf(
          "application/vnd.oci.image.index.v1+json",
          "application/vnd.docker.distribution.manifest.v2+json",
          "application/vnd.docker.distribution.manifest.list.v2+json",
        ).joinToString(", ")
      )
      .header("Authorization", "Bearer $registryToken")
      .GET()
      .build()
  }

  private fun fetchToken(registry: String, scope: String): String {
    val initialRequest = HttpRequest.newBuilder()
      .uri(URI.create("https://${getHost(registry)}/v2/"))
      .GET()
      .build()

    val httpResponse = httpClient.send(initialRequest, HttpResponse.BodyHandlers.discarding())

    if (httpResponse.statusCode() != 401) {
      throw GradleException("Expected 401 response from registry: HTTP ${httpResponse.statusCode()}.")
    }

    val authHeader = httpResponse.headers().firstValue("www-authenticate")
      .orElseThrow { GradleException("Expected www-authenticate header from registry: ${httpResponse.headers()}") }

    val tokenRealm = Regex("""realm="([^"]+)"""").find(authHeader)?.groupValues?.get(1)
      ?: throw GradleException("Missing realm in www-authenticate header")
    val tokenService = Regex("""service="([^"]+)"""").find(authHeader)?.groupValues?.get(1)
      ?: throw GradleException("Missing service in www-authenticate header")
    val tokenUri = URI.create("$tokenRealm?service=$tokenService&scope=$scope")

    val tokenRequest = HttpRequest.newBuilder()
      .uri(tokenUri)
      .GET()
      .build()

    val tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString())

    if (tokenResponse.statusCode() != 200) {
      throw GradleException("Failed to fetch token from $tokenUri: HTTP ${tokenResponse.statusCode()}")
    }

    return Regex("\"token\":\\s*\"([^\"]+)\"")
      .find(tokenResponse.body())
      ?.groupValues?.get(1)
      ?: throw RuntimeException("Could not parse token from token response")
  }
}
