FROM zenika/kotlin:1.3-jdk11 AS build

COPY . /swissqr-src
RUN cd /swissqr-src && \
  ./gradlew distTar && \
  mkdir swissqr-bin && \
  cd swissqr-bin && \
  tar xf ../build/distributions/swissqr-bin.tar


FROM zenika/kotlin:1.3-jdk11-slim

RUN mkdir /data
COPY config.json /data/
COPY --from=build /swissqr-src/swissqr-bin /

EXPOSE 8080

ENTRYPOINT /swissqr-bin/bin/SwissQR /data/config.json