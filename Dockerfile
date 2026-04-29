# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY PrincipiPJ-Projektna1/Projektna1_VirtualEstate/ .
RUN chmod +x gradlew && ./gradlew :composeApp:packageDistributionForCurrentOS --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/composeApp/build/compose/binaries/main/app/ ./app/
CMD ["echo", "Compose Desktop app built successfully"]
