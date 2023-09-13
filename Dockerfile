FROM eclipse-temurin:20.0.2_9-jdk AS build
WORKDIR /build

COPY mvnw .
ADD .mvn ./.mvn

COPY pom.xml .
COPY /src /src

RUN ./mvnw clean package -DskipTests


FROM eclipse-temurin:20.0.2_9-jre

WORKDIR /

COPY --from=build /build/target/*.jar cloud-storage.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "cloud-storage.jar"]