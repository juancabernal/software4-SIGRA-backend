# Etapa 1: compilación con Gradle (usa el wrapper del repo, no necesita Gradle instalado)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew build -x test --no-daemon

# Etapa 2: imagen final, solo el JRE y el jar ya compilado
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
