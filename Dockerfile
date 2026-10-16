FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY src/main/java src/main/java
RUN mkdir -p build \
    && javac --release 21 -d build $(find src/main/java -name '*.java')

FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --create-home flashpoint
USER flashpoint
WORKDIR /app
COPY --from=build /app/build ./build

EXPOSE 8080
ENTRYPOINT ["java", "-cp", "build", "io.quorumforge.flashpoint.Flashpoint"]
