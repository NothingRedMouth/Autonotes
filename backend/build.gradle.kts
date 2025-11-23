import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.spotbugs") version "6.2.4"
    id("com.diffplug.spotless") version "6.25.0"
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
    implementation("com.giffing.bucket4j.spring.boot.starter:bucket4j-spring-boot-starter:0.13.0")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.ehcache:ehcache")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.13")
    implementation("org.mapstruct:mapstruct:1.6.3")
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:url-connection-client")
    implementation("commons-io:commons-io:2.20.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
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
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

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

tasks.named("check") {
    dependsOn("spotbugsMain", "spotbugsTest", "spotlessCheck")
}
