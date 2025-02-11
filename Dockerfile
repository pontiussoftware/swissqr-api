FROM gradle:jdk21 AS build
COPY --chown=gradle:gradle . /swissqr-api
WORKDIR /swissqr-api
RUN gradle --no-daemon distTar
WORKDIR /swissqr-api/build/distributions
RUN tar -xf ./swissqr-bin.tar

FROM eclipse-temurin:21-jre

RUN mkdir /data
COPY config.json /data/
COPY --from=build /swissqr-api/build/distributions/swissqr-bin /swissqr-bin

EXPOSE 8081

ENTRYPOINT /swissqr-bin/bin/SwissQRService /data/config.json