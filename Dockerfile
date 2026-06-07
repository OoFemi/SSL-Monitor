FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
COPY config.json config.json
CMD ["java", "-jar", "app.jar"]

