FROM eclipse-temurin:17

ENV GRADLE_CACHE ".gradle-cache/"

WORKDIR /home/build/
COPY ./gradlew .
RUN chmod +x ./gradlew

ENTRYPOINT ["./gradlew"]
