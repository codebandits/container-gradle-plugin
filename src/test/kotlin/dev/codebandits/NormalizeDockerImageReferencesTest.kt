package dev.codebandits

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class NormalizeDockerImageReferencesTest {
  @Test
  fun `normalize short image name`() {
    expectThat(normalizeDockerImageReference("alpine"))
      .isEqualTo("docker.io/library/alpine:latest")
  }

  @Test
  fun `normalize image with explicit tag`() {
    expectThat(normalizeDockerImageReference("alpine:latest"))
      .isEqualTo("docker.io/library/alpine:latest")
  }

  @Test
  fun `normalize fully qualified image reference`() {
    expectThat(normalizeDockerImageReference("docker.io/library/alpine:latest"))
      .isEqualTo("docker.io/library/alpine:latest")
  }
}
