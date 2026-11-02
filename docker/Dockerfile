# ============================================================================
# Build stage — compile the JavaFX app inside Docker
# ============================================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy only the POM first to leverage Docker layer caching
COPY pom.xml .

# Download dependencies (cached layer if pom.xml unchanged)
RUN mvn -B dependency:go-offline -DskipTests

# Copy the full source and build
COPY src ./src
RUN mvn -B package -DskipTests

# Copy runtime dependencies for JavaFX module path
RUN mvn -B dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=/build/javafx-libs

# ============================================================================
# Runtime stage — run the JavaFX desktop application
# ============================================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install X11 libraries required for JavaFX GUI rendering
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxtst6 libxi6 libxrender1 libxt6 libxrandr2 libx11-6 libxext6 \
    libxfixes3 libcups2 libasound2 libgtk-3-0 libnss3 libatk1.0-0 \
    libatk-bridge2.0-0 libgtk-3-bin libgbm1 libdrm2 libxcomposite1 \
    libxdamage1 libxkbcommon0 libxkbcommon-x11-0 \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR and dependency libraries
COPY --from=builder /build/target/chronos-1.0.0-SNAPSHOT.jar /app/chronos.jar
COPY --from=builder /build/javafx-libs /app/javafx-libs

# Default config for PostgreSQL connectivity
ENV DB_URL=jdbc:postgresql://postgres:5432/chronos
ENV DB_USER=chronos
ENV DB_PASSWORD=chronos
ENV APP_DATA_DIR=/app/data
ENV DISPLAY=:99

# Run the JavaFX application with JavaFX modules on module path
ENTRYPOINT ["java", "--module-path", "/app/javafx-libs", "--add-modules", "javafx.controls,javafx.fxml,javafx.graphics,javafx.base", "-jar", "/app/chronos.jar"]