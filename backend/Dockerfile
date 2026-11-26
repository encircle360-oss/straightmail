# Base images are pinned by digest so a build is reproducible and a vulnerable JDK cannot slip in
# through a stale tag cache. Both digests are Temurin 25.0.5.1+2 (fix for CVE-2026-114938 and the
# other November 2026 OpenJDK CVEs). Renovate bumps the digest whenever the 25 tags are rebuilt.
FROM eclipse-temurin:25-jdk@sha256:ce2ef9d2d67a68de5e196d4de9d743327c44a10b57fe02ec39f572c652cacfd3 AS builder
WORKDIR /build
COPY build/libs/straightmail-*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre@sha256:4ed132f6832e9804b123e89a849a7b4502756ae64a4ee08ebab939d9d5d0a9fa
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
# file templates from /resources/templates and message bundles from /resources/i18n. They have to
# exist in the image and belong to the runtime user: a named volume inherits this ownership on
# first use, while a bind mount of a host directory would otherwise arrive root-owned and leave the
# non-root process unable to write.
RUN mkdir -p /data /git /resources/templates /resources/i18n \
    && chown -R straightmail:straightmail /app /data /git /resources/templates /resources/i18n

USER straightmail

EXPOSE 50003 50004

ENTRYPOINT ["java", "-jar", "application.jar"]
