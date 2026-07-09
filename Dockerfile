FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="shenshangshang"
LABEL description="Komga-CN with MySQL 8.4 support and page prefetch feature"

RUN apk add --no-cache ttf-dejavu wget
RUN addgroup -S komga && adduser -S komga -G komga
RUN mkdir -p /config /data && chown -R komga:komga /config /data

COPY komga/build/libs/komga-1.25.0.jar /app/komga.jar
RUN chown komga:komga /app/komga.jar

USER komga
WORKDIR /app
EXPOSE 25600

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:25600/actuator/health || exit 1

ENV SERVER_PORT=25600 \
    KOMGA_CONFIGDIR=/config \
    PREFETCH_PAGES=3

ENTRYPOINT ["java"]
CMD ["-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/komga.jar"]
