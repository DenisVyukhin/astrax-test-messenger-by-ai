plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "com.astrax"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

tasks.register<Copy>("installDistToDeploy") {
    dependsOn("installDist")
    from(layout.buildDirectory.dir("install/backend"))
    into(layout.projectDirectory.dir("build/deploy/backend"))
}

dependencies {
    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-cors-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("at.favre.lib:bcrypt:0.10.2")
    implementation("org.jetbrains.exposed:exposed-core:0.54.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.54.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.54.0")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
}
