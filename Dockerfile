FROM alpine:latest

RUN apk add --no-cache openjdk11-jre

COPY game.jar /app/game.jar
COPY game.config.properties /app/game.config.properties
COPY scripts/ /app/scripts/

WORKDIR /app

CMD ["java", "-jar", "game.jar"]