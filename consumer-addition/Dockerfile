FROM openjdk:8-jdk-alpine

COPY target/app.jar /app.jar

ENTRYPOINT ["java","-jar","/app.jar","--spring.profiles.active=prod"]

