FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

RUN apt-get update && apt-get install -y maven

RUN java -version && mvn -version

COPY pom.xml /app
RUN mvn dependency:go-offline

COPY . /app
RUN mvn clean package -DskipTests

FROM eclipse-temurin:23-jdk-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]