package dev.codebandits.container.gradle.tasks

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ImageReferencePartsTest {

  @Test
  fun `normalize short image name`() {
    expectThat("alpine".toImageReferenceParts()).and {
      get { normalized }.isEqualTo("docker.io/library/alpine:latest")
    }
  }

  @Test
  fun `normalize image with explicit tag`() {
    expectThat("alpine:latest".toImageReferenceParts()).and {
      get { normalized }.isEqualTo("docker.io/library/alpine:latest")
    }
  }

  @Test
  fun `normalize fully qualified image reference`() {
    expectThat("docker.io/library/alpine:latest".toImageReferenceParts()).and {
      get { normalized }.isEqualTo("docker.io/library/alpine:latest")
    }
  }
}
