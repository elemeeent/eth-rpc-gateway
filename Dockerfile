FROM gradle:8.9.0-jdk21

WORKDIR /app
COPY . .

RUN gradle --no-daemon clean build -x test

RUN rm -rf /app/cert.pem /app/key.pem

COPY src/main/resources/cert.pem /app/cert.pem
COPY src/main/resources/key.pem  /app/key.pem

EXPOSE 8081
CMD ["gradle","--no-daemon","bootRun"]