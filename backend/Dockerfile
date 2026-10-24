FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY build/libs/straightmail-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
WORKDIR /app

# Pull Ubuntu security fixes the base image has not been rebuilt with yet. The base also ships
# Canonical's pebble service manager, a Go binary outside dpkg that the JVM never runs; its bundled
# Go stdlib lags behind upstream fixes and drags critical CVEs into every image scan, so it goes.
RUN apt-get update \
    && apt-get upgrade -y --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && rm -f /usr/bin/pebble

# Fixed UID/GID so the ownership of the writable directories below survives into any volume
# Docker initialises from this image.
RUN groupadd -r -g 10001 straightmail && useradd -r -u 10001 -g straightmail straightmail

COPY --from=builder /build/extracted/dependencies/ ./
COPY --from=builder /build/extracted/spring-boot-loader/ ./
COPY --from=builder /build/extracted/snapshot-dependencies/ ./
COPY --from=builder /build/extracted/application/ ./

# The app writes the SQLite database to /data and the git-sync working copies to /git, and reads
# file templates from /resources/templates. They have to exist in the image and belong to the
# runtime user: a named volume inherits this ownership on first use, while a bind mount of a
# host directory would otherwise arrive root-owned and leave the non-root process unable to write.
RUN mkdir -p /data /git /resources/templates \
    && chown -R straightmail:straightmail /app /data /git /resources/templates

USER straightmail

EXPOSE 50003 50004

ENTRYPOINT ["java", "-jar", "application.jar"]
