FROM openjdk:11-jre-slim
WORKDIR /app
COPY download_url/target/*.jar app.jar
ENTRYPOINT ["java","-jar", "–Dspring.profiles.active=test", "app.jar"]