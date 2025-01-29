# Use the official OpenJDK 17 image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot .jar file into the container
COPY target/asyncapi-importer-rest.jar app.jar

# Expose port 9004
EXPOSE 9004

# Run the Spring Boot application
# Java JVM memory settings are based upon best estimates from run-time observations
# Adjust values based upon your requirements
ENTRYPOINT ["java", "-jar", "/app/app.jar", "-Xms256m", "-Xmx512m", "-XX:MetaspaceSize=128m", "-XX:MaxMetaspaceSize=256m"]
