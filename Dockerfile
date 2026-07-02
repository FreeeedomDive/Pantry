FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :bootstrap:bootJar -x test --no-daemon

FROM eclipse-temurin:24-jre AS runtime
WORKDIR /app
COPY --from=build /app/bootstrap/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
