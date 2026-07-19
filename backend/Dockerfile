# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Extract stage ---
# Explode the Spring Boot layered jar so each dependency becomes a discrete
# file on the image filesystem (dependencies/BOOT-INF/lib/*.jar). A plain fat
# jar hides those nested jars from container scanners (Syft/Trivy/HarnessSCA),
# so application libraries never appear in the SBOM or the vuln scan — only the
# base-image OS packages do. Exploding the layers puts each library jar (e.g.
# commons-text-1.9.jar) where scanners can catalog it, and gives better Docker
# layer caching as a bonus.
FROM eclipse-temurin:17-jre-alpine AS extract
WORKDIR /app
COPY --from=build /app/target/ci-banking-backend-*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
USER spring:spring
EXPOSE 8080
# JarLauncher boots the exploded layout. JAVA_TOOL_OPTIONS (heap pin) still
# applies — the JVM reads it regardless of launch method.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
