FROM openjdk:11-jre-slim
WORKDIR /app
COPY download_url/target/*.jar app.jar
ENTRYPOINT ["java","-jar", "app.jar", "–Dspring.profiles.active=prod"]