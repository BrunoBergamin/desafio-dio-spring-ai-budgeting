# ---------- Etapa 1: compila a aplicação ----------
# Usa o Maven de dentro da imagem, então quem for testar não precisa de Java nem Maven instalados.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copia só o pom primeiro para aproveitar o cache de dependências do Docker
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Etapa 2: imagem final, só com o necessário para rodar ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
