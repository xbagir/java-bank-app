FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests -pl transfer -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/transfer/target/transfer-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
