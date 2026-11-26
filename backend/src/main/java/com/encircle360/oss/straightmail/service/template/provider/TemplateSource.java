package com.encircle360.oss.straightmail.service.template.provider;

/**
 * Enumeration of the supported template source types used in database mode.
 *
 * <p>Each value corresponds to one {@link TemplateSourceProvider} implementation that can
 * contribute templates to the {@link CompositeTemplateProvider}.
 */
public enum TemplateSource {

    /**
     * Templates stored in the relational database as {@link com.encircle360.oss.straightmail.model.Template} entities.
     */
    DATABASE,

    /**
     * Templates loaded from a Git repository via {@link com.encircle360.oss.straightmail.service.GitSyncService}.
     */
    GIT,

    /**
     * Templates loaded from the local filesystem or classpath.
     */
    FILE
}
