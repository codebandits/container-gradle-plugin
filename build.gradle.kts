import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.Properties

group = "dev.codebandits"

val projectDescription = listOf(
  "Container is a Gradle plugin that enhances build portability, reproducibility, and flexibility",
  "by integrating containers into Gradle tasks. It provides a declarative and familiar way",
  "to run task operations inside containers and declare containers as task inputs and outputs."
).joinToString(" ")

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.mavenPublish)
  `java-gradle-plugin`
  `jvm-test-suite`
  signing
}

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
  signAllPublications()

  pom {
    description = projectDescription
    url = "https://github.com/codebandits/container-gradle-plugin"
    licenses {
      license {
        name = "MIT License"
        url = "https://github.com/codebandits/container-gradle-plugin/blob/main/LICENSE"
        distribution = "repo"
      }
    }
    developers {
      developer {
        id.set("codebandits")
        name = "Code Bandits Team"
        organization = "Code Bandits"
        organizationUrl = "https://github.com/codebandits"
      }
    }
    scm {
      url = "https://github.com/codebandits/container-gradle-plugin"
    }
  }
}

tasks {
  val configureSigning = register("configureSigning") {
    group = "publishing"
    doLast {
      ByteArrayOutputStream()
        .use { sopsOutputStream ->
          exec {
            commandLine("sh", "-c", "sops --decrypt signing.enc.properties")
            standardOutput = sopsOutputStream
          }
          sopsOutputStream.toString(StandardCharsets.UTF_8)
        }
        .let { output -> Properties().apply { load(StringReader(output)) } }
        .forEach { key, value -> project.extraProperties.set(key.toString(), value) }
      signing.useInMemoryPgpKeys(
        project.property("signing.keyId").toString(),
        project.property("signing.key").toString(),
        project.property("signing.password").toString(),
      )
    }
  }

  withType<Sign> {
    dependsOn(configureSigning)
  }

  val loadPublishingSecrets = register("loadPublishingSecrets") {
    group = "publishing"
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
        .forEach { key, value -> project.extraProperties.set(key.toString(), value) }
    }
  }

  withType<PublishToMavenRepository> {
    if (name.endsWith("ToMavenCentralRepository")) {
      dependsOn(loadPublishingSecrets)
    }
  }
}

kotlin {
  explicitApi()
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

java {
  targetCompatibility = JavaVersion.VERSION_17
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
          dependsOn("jar")
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

gradlePlugin {
  plugins {
    create("container") {
      id = "dev.codebandits.container"
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

// Workaround for https://github.com/gradle/gradle/issues/1893
// Inspired by https://github.com/ratpack/ratpack/commit/0d9d2eb1d863a2a80b06c3e37f90cad1e7c4bdd1
fun Test.fixTestKitLoggingInterference() {
  val gradleTestKitFiles = (dependencies.gradleTestKit() as? FileCollectionDependency)?.files ?: emptySet()
  classpath = files(classpath.files.sortedBy { it in gradleTestKitFiles })
}
