FROM eclipse-temurin:25
COPY target/WordlTUI-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]