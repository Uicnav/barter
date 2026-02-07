FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :server:shadowJar --no-daemon

FROM eclipse-temurin:17-jre
COPY --from=build /app/server/build/libs/*-all.jar /app/server.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"]
