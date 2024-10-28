package dev.codebandits

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BusinessLogicTest {

  private val businessLogic = BusinessLogic()

  @Test
  fun testSum() {
    expectThat(businessLogic.sum(40, 2)).isEqualTo(42)
  }
}
