FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.lockfile settings-gradle.lockfile ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src/ ./src/
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY --from=build /app/gradle.lockfile gradle.lockfile
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
