FROM eclipse-temurin:24-jdk-noble as builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY gradlew ./
COPY gradle ./gradle

COPY src ./src

RUN chmod +x ./gradlew

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre-noble

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
