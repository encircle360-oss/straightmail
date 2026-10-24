package com.encircle360.oss.straightmail.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Mailpit test container for integration testing.
 * Provides SMTP server on port 1025 and HTTP API/UI on port 8025.
 */
public class MailpitTestContainer extends GenericContainer<MailpitTestContainer> {

    private static final DockerImageName MAILPIT_IMAGE = DockerImageName.parse("axllent/mailpit:latest");
    private static final int SMTP_PORT = 1025;
    private static final int HTTP_PORT = 8025;

    public MailpitTestContainer() {
        super(MAILPIT_IMAGE);
        addExposedPorts(SMTP_PORT, HTTP_PORT);
        waitingFor(Wait.forHttp("/api/v1/info").forPort(HTTP_PORT).withStartupTimeout(Duration.ofSeconds(60)));
    }

    /**
     * Returns the mapped SMTP port on the host.
     */
    public Integer getSmtpPort() {
        return getMappedPort(SMTP_PORT);
    }

    /**
     * Returns the mapped HTTP port on the host for the Mailpit API.
     */
    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Returns the SMTP host address.
     */
    public String getSmtpHost() {
        return getHost();
    }

    /**
     * Returns the Mailpit API base URL.
     */
    public String getApiUrl() {
        return String.format("http://%s:%d", getHost(), getHttpPort());
    }
}
