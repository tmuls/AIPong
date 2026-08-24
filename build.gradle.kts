plugins {
    kotlin("jvm") version "2.1.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Multiplatform-friendly threading/synchronization (Dispatchers.Default,
    // Mutex) used in place of java.lang.Thread / java.util.concurrent, which
    // don't exist on Kotlin/Native.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}