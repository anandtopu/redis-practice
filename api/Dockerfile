FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY common/pom.xml common/pom.xml
COPY api/pom.xml api/pom.xml
COPY worker/pom.xml worker/pom.xml
COPY common/src common/src
COPY api/src api/src
COPY worker/src worker/src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/api/target/api-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
