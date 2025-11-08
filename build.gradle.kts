plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.example"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.thymeleaf)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation("org.jetbrains.exposed:exposed-java-time:0.43.0")
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation("io.ktor:ktor-server-core-jvm:2.3.5") // версия под твою Ktor
    implementation("io.ktor:ktor-server-call-logging-jvm:2.3.5") // для CallLogging
    implementation("io.ktor:ktor-server-auth-jvm:2.3.5")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:2.3.5")
    implementation("ch.qos.logback:logback-classic:1.4.11") // для логирования
    implementation("org.mindrot:jbcrypt:0.4")
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
