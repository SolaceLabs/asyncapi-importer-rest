# Use the official OpenJDK 17 image as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot .jar file into the container
COPY target/asyncapi-importer-rest.jar app.jar

# Expose port 9004
EXPOSE 9004

# Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "/app/app.jar", "-Xms256m", "-Xmx1g", "-XX:MetaspaceSize=128m", "-XX:MaxMetaspaceSize=256m"]
