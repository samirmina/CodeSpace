FROM eclipse-temurin:21-jdk

WORKDIR /app

# Install JavaFX runtime dependencies and build tools
RUN apt-get update && apt-get install -y \
    maven \
    wget \
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

# Download and install JavaFX SDK 21
RUN wget -q https://download2.gluonhq.com/javafx/21.0.2/javafx-sdk-21.0.2-linux-x64.tar.gz \
    && tar -xzf javafx-sdk-21.0.2-linux-x64.tar.gz \
    && mv javafx-sdk-21.0.2 /opt/javafx-sdk \
    && rm javafx-sdk-21.0.2-linux-x64.tar.gz

ENV JAVAFX_HOME=/opt/javafx-sdk
ENV JAVAFX_MODULE_PATH=$JAVAFX_HOME/lib

# Copy Maven config and source
COPY pom.xml .
COPY src ./src

# Build the application (downloads dependencies, compiles, packages)
RUN mvn -B package -DskipTests

# Expose Spring Boot port
EXPOSE 8080

# Data directory for JSON persistence
VOLUME ["/app/data"]

# Default config for Docker/PostgreSQL connectivity
ENV DB_URL=jdbc:postgresql://postgres:5432/chronos
ENV DB_USER=chronos
ENV DB_PASSWORD=chronos
ENV APP_DATA_DIR=/app/data
ENV DISPLAY=:0

# Run the Spring Boot web application as primary interface
ENTRYPOINT ["java", "--enable-preview", "-jar", "target/chronos-1.0.0-SNAPSHOT.jar"]
