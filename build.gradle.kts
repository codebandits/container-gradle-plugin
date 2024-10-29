import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.invoke

plugins {
  `java-gradle-plugin`
  `jvm-test-suite`
  alias(libs.plugins.kotlinJvm)
}

kotlin {
  explicitApi()
}

sourceSets {
  create("sharedTest")
}

dependencies {
  "sharedTestApi"(libs.junit.jupiter.api)
}

testing {
  @Suppress("UnstableApiUsage")
  suites {
    register<JvmTestSuite>("functionalTest") {
      dependencies {
        implementation(project())
        implementation(sourceSets["sharedTest"].output)
      }
      targets.all {
        testTask {
          shouldRunAfter("test")
        }
      }
    }

    register<JvmTestSuite>("platformTest") {
      dependencies {
        implementation(project())
        implementation(sourceSets["sharedTest"].output)
        implementation(libs.testcontainers.testcontainers)
      }
      targets.all {
        testTask {
          environment("PROJECT_ROOT", rootDir.absolutePath)
          shouldRunAfter("test", "functionalTest")
        }
      }
    }

    withType<JvmTestSuite> {
      useJUnitJupiter(libs.versions.junit.jupiter)
      dependencies {
        implementation(libs.strikt.core)
        implementation.bundle(libs.bundles.logging.implementation)
        runtimeOnly.bundle(libs.bundles.logging.runtime)
      }
      targets.all {
        testTask {
          doFirst { fixTestKitLoggingInterference() }
          testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            exceptionFormat = TestExceptionFormat.FULL
          }
        }
      }
    }
  }
}

gradlePlugin {
  plugins {
    create("container") {
      id = "dev.codebandits.container"
      implementationClass = "dev.codebandits.ContainerPlugin"
    }
  }
  testSourceSets(
    sourceSets["functionalTest"],
    sourceSets["platformTest"],
  )
}

tasks.named("check") {
  @Suppress("UnstableApiUsage")
  dependsOn(
    testing.suites.named("functionalTest"),
    testing.suites.named("platformTest"),
  )
}

// Workaround for https://github.com/gradle/gradle/issues/1893
// Inspired by https://github.com/ratpack/ratpack/commit/0d9d2eb1d863a2a80b06c3e37f90cad1e7c4bdd1
fun Test.fixTestKitLoggingInterference() {
  val gradleTestKitFiles = (dependencies.gradleTestKit() as? FileCollectionDependency)?.files ?: emptySet()
  classpath = files(classpath.files.sortedBy { it in gradleTestKitFiles })
}
