# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
# Download Maven and all dependencies from Google's mirror of Maven Central: Maven Central itself
# often rate-limits Render's shared build machines ("429 Too Many Requests").
ENV MVNW_REPOURL=https://maven-central.storage-download.googleapis.com/maven2
COPY .mvn/ .mvn/
COPY mvnw pom.xml maven-settings.xml ./
RUN chmod +x mvnw && ./mvnw -B -q -s maven-settings.xml dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q -s maven-settings.xml clean package -DskipTests

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/prep-api.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]