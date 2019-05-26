import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    id("kotlinx-serialization") version "1.3.31"
    application
}

group = "moe.nikky"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx/") {
        name = "kotlinx"
    }
//    maven("https://dl.bintray.com/orangy/maven/") {
//        name = "orangy"
//    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(group = "io.ktor", name= "ktor-client-cio", version= "1.2.0")
    implementation(group = "org.jetbrains.kotlinx", name= "kotlinx-serialization-runtime", version= "0.11.0")
//    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-cli-jvm", version = "0.1.0-dev-4")
//    implementation(group = "com.github.ajalt", name = "clikt", version = "2.0.0")
//    implementation(group = "com.xenomachina", name = "kotlin-argparser", version = "2.0.7")
    implementation(group= "com.beust", name= "jcommander", version= "1.72")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClassName = "MatterBridge"
}