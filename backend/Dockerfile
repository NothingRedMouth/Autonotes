FROM eclipse-temurin:24-jdk-noble as builder

WORKDIR /app

COPY build.gradle* settings.gradle* ./
COPY gradlew ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:24-jre-noble

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
