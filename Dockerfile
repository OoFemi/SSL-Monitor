# -------------------------
# 1. BUILD STAGE
# -------------------------
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY . .

RUN chmod +x ./gradlew
RUN ./gradlew clean build -x test


# -------------------------
# 2. RUNTIME STAGE
# -------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy fat JAR
COPY --from=build /app/build/libs/*.jar app.jar

# Copy static files
COPY src/main/resources/static /app/static

# Copy config file (important!)
COPY config.properties /app/config.properties

EXPOSE 7000

ENTRYPOINT ["java", "-jar", "app.jar"]
