plugins {
  `java-gradle-plugin`
  `jvm-test-suite`
  alias(libs.plugins.kotlinJvm)
}

testing {
  @Suppress("UnstableApiUsage")
  suites {
    register<JvmTestSuite>("functionalTest") {
      dependencies {
        implementation(project())
      }
    }

    withType<JvmTestSuite> {
      useJUnitJupiter(libs.versions.junit.jupiter)
    }
  }
}

gradlePlugin {
  testSourceSets(sourceSets["functionalTest"])
}

tasks.named("check") {
  @Suppress("UnstableApiUsage")
  dependsOn(testing.suites.named("functionalTest"))
}
