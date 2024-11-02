import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.Properties

plugins {
  `java-gradle-plugin`
  `jvm-test-suite`
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.publish)
}

kotlin {
  explicitApi()
}

sourceSets {
  create("testShared")
}

dependencies {
  add(sourceSets["testShared"].apiConfigurationName, libs.junit.jupiter.api)
  testImplementation(sourceSets["testShared"].output)
}

testing {
  @Suppress("UnstableApiUsage")
  suites {
    register<JvmTestSuite>("testFeatures") {
      dependencies {
        implementation(project())
        implementation(sourceSets["testShared"].output)
      }
      targets.all {
        testTask {
          shouldRunAfter("test")
        }
      }
    }

    register<JvmTestSuite>("testPlatforms") {
      dependencies {
        implementation(project())
        implementation(sourceSets["testShared"].output)
        implementation(libs.testcontainers.testcontainers)
      }
      targets.all {
        testTask {
          environment("PROJECT_ROOT", rootDir.absolutePath)
          shouldRunAfter("test", "testFeatures")
        }
      }
    }

    register<JvmTestSuite>("testToolIntegrations") {
      dependencies {
        implementation(project())
        implementation(sourceSets["testShared"].output)
      }
      targets.all {
        testTask {
          shouldRunAfter("test", "testFeatures")
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

group = "dev.codebandits"

gradlePlugin {
  plugins {
    create("container") {
      website = "https://github.com/codebandits/gradle-container-plugin"
      vcsUrl = "https://github.com/codebandits/gradle-container-plugin"
      id = "dev.codebandits.container"
      displayName = "Container"
      description = listOf(
        "Container is a Gradle plugin that enhances build portability, reproducibility, and flexibility",
        "by integrating containers into Gradle tasks. It provides a declarative and familiar way",
        "to run task operations inside containers and declare containers as task inputs and outputs."
      ).joinToString(" ")
      tags = listOf("container", "docker", "oci", "buildpack")
      implementationClass = "dev.codebandits.ContainerPlugin"
    }
  }
  testSourceSets(
    sourceSets["testFeatures"],
    sourceSets["testPlatforms"],
    sourceSets["testToolIntegrations"],
  )
}

tasks.named("check") {
  @Suppress("UnstableApiUsage")
  dependsOn(
    testing.suites.named("testFeatures"),
    testing.suites.named("testPlatforms"),
    testing.suites.named("testToolIntegrations"),
  )
}

tasks.register("loadPublishingSecrets") {
  doLast {
    ByteArrayOutputStream()
      .use { sopsOutputStream ->
        exec {
          commandLine("sh", "-c", "sops --decrypt publishing.enc.properties")
          standardOutput = sopsOutputStream
        }
        sopsOutputStream.toString(StandardCharsets.UTF_8)
      }
      .let { output -> Properties().apply { load(StringReader(output)) } }
      .forEach { (key, value) -> project.extraProperties[key.toString()] = value }
  }
}

tasks.named("publishPlugins") {
  dependsOn("loadPublishingSecrets")
}

// Workaround for https://github.com/gradle/gradle/issues/1893
// Inspired by https://github.com/ratpack/ratpack/commit/0d9d2eb1d863a2a80b06c3e37f90cad1e7c4bdd1
fun Test.fixTestKitLoggingInterference() {
  val gradleTestKitFiles = (dependencies.gradleTestKit() as? FileCollectionDependency)?.files ?: emptySet()
  classpath = files(classpath.files.sortedBy { it in gradleTestKitFiles })
}
