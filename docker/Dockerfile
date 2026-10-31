FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install JavaFX runtime dependencies
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libfreetype6 \
    libfontconfig1 \
    libgtk-3-0 \
    libxcomposite1 \
    libxcursor1 \
    libxdamage1 \
    libxrandr2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR
COPY target/chronos-1.0.0-SNAPSHOT.jar /app/chronos.jar

# Expose Spring Boot port
EXPOSE 8080

# Data directory for JSON persistence
VOLUME ["/app/data"]

# Default config for Docker/PostgreSQL connectivity
ENV DB_URL=jdbc:postgresql://postgres:5432/chronos
ENV DB_USER=chronos
ENV DB_PASSWORD=chronos
ENV APP_DATA_DIR=/app/data

# Run the Spring Boot web application
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/chronos.jar"]
