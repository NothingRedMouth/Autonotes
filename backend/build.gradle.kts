import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.spotbugs") version "6.2.4"
    id("com.diffplug.spotless") version "6.25.0"
    id("jacoco")
}

group = "ru.mtuci"
version = "0.0.1-SNAPSHOT"
description = "AutonotesBackend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    val agent by creating
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:2.36.0")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core:11.15.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.15.0")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("commons-io:commons-io:2.20.0")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.apache.tika:tika-core:2.9.1")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    compileOnly("org.projectlombok:lombok:1.18.42")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    annotationProcessor("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.2")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.2")
    testImplementation("org.testcontainers:testcontainers-minio:2.0.2")
    testImplementation("org.testcontainers:rabbitmq")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    "agent"("io.opentelemetry.javaagent:opentelemetry-javaagent:2.22.0")
}

// =============================================
// Базовая настройка тестов
// =============================================

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// =============================================
// JaCoCo
// =============================================

jacoco {
    toolVersion = "0.8.14"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    reports.html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    reports.xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacoco.xml"))
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
            excludes = listOf(
                "**/*Application*",
                "**/*Config*",
                "**/*Dto*",
                "**/*Request*",
                "**/*Response*",
            )
        }
    }
}

tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest", "spotlessCheck")
    finalizedBy(tasks.jacocoTestReport, tasks.jacocoTestCoverageVerification)
}

// =============================================
// SpotBugs
// =============================================

spotbugs {
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter = file("config/spotbugs/spotbugsExclude.xml")
}

tasks.withType<SpotBugsTask> {
    reports.create("html") {
        required = true
        outputLocation = layout.buildDirectory.dir("reports/spotbugs/main/spotbugs.html").get().asFile
    }
    reports.create("xml") {
        required = false
    }
}

// =============================================
// Spotless
// =============================================

spotless {
    format("misc") {
        target("*.gradle", "*.md", ".gitignore", "*.yml", "*.properties")
        trimTrailingWhitespace()
        indentWithSpaces(4)
        endWithNewline()
    }
    java {
        target("src/**/*.java")
        palantirJavaFormat()
        indentWithSpaces(4)
        removeUnusedImports()
        importOrder()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "settings.gradle.kts")
        ktlint("0.50.0")
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// =============================================
// OpenTelemetry
// =============================================

tasks.register<Copy>("copyOtelAgent") {
    from(configurations["agent"])
    into(layout.buildDirectory.dir("otel-agent"))
    rename { "opentelemetry-javaagent.jar" }
}

tasks.bootJar {
    dependsOn("copyOtelAgent")
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    dependsOn("copyOtelAgent")
    jvmArgs = listOf(
        "-javaagent:${layout.buildDirectory.dir("otel-agent/opentelemetry-javaagent.jar").get().asFile.absolutePath}",
    )
}
