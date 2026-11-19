FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -B -DskipTests -pl accounts -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/accounts/target/accounts-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
