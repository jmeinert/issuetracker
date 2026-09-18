FROM eclipse-temurin:21-jdk-noble AS build

WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -ntp package -DskipTests


FROM eclipse-temurin:21-jre-noble

WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

COPY --from=build /build/target/issuetracker-*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
