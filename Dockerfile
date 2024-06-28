FROM openjdk:17-jdk-alpine

# install curl
RUN apk add --no-cache curl

#run the application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]