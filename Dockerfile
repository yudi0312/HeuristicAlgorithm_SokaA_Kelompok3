# ==============================================================================
# Dockerfile: DRALBA Real-World Cloud Task Scheduling
# Base image: Eclipse Temurin JDK/JRE 17 (Alpine Linux - Ringan & Efisien)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Salin shaded uber-jar yang telah dikompilasi
COPY target/CloudSimTaskScheduling-1.0-SNAPSHOT.jar app.jar

# Salin dataset GoCJ sebagai resource lokal tambahan di container
COPY src/main/resources /app/resources

# Buat direktori output hasil eksperimen
RUN mkdir -p /app/results

# Port default worker
EXPOSE 8080

# Default entrypoint: Worker Server
ENTRYPOINT ["java", "-cp", "app.jar", "org.example.dralba.worker.WorkerServer"]

