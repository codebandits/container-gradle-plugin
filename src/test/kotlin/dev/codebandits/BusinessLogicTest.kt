package dev.codebandits

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BusinessLogicTest {

  private val businessLogic = BusinessLogic()

  @Test
  fun testSum() {
    assertEquals(businessLogic.sum(40, 2), 42)
  }
}
