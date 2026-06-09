# ─────────────────────────────────────────
# STAGE 1 : Build avec Maven
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# Copier pom.xml et télécharger les dépendances en cache
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B

# Copier le code source et builder
COPY src ./src
RUN mvn clean package -DskipTests -Pprod

# ─────────────────────────────────────────
# STAGE 2 : Image finale légère
# ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier uniquement le JAR buildé
COPY --from=build /app/target/BabyShop-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port
EXPOSE 8081

# Lancer avec le profil prod
ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-jar", \
  "app.jar"]