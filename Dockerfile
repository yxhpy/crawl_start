FROM openjdk:11-jre-slim
WORKDIR /app
COPY download_url/target/*.jar app.jar
ENTRYPOINT ["java", "-Dspring.profiles.active=prod","-jar", "app.jar"]