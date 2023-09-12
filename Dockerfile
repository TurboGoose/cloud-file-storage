FROM maven:3.9.4-amazoncorretto-20-al2023 AS build
WORKDIR /
COPY /src /src
COPY pom.xml /
RUN mvn -f /pom.xml clean package -DskipTests

FROM amazoncorretto:20.0.2-alpine3.18
WORKDIR /
COPY --from=build /target/*.jar cloud-storage.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "cloud-storage.jar"]