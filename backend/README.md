[![Backend Build](../../../actions/workflows/backend.yml/badge.svg)](../../../actions/workflows/backend.yml)

## straightmail - mail sending APIs with template and i18n support

### Getting started

To run an instance with the built-in default template and messages, without authentication and
without a database:

```
docker run -p 50003:50003 -p 50004:50004 \
    --env AUTH_MODE=none \
    --env ENCRYPTION_KEY="$(openssl rand -base64 32)" \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_PORT=1025 \
    --env SMTP_USER=foo \
    --env SMTP_PASSWORD=bar \
    --env DEFAULT_SENDER=test@encircle360.com \
    --env SMTP_ENABLE_TLS=true \
    --env SMTP_ENABLE_SSL=false \
    ghcr.io/encircle360-oss/straightmail:0.5.1
```

The image carries the API only. From 0.5.1 on it is published as
`ghcr.io/encircle360-oss/straightmail/backend` and, with the identical digest, as
`ghcr.io/encircle360-oss/straightmail`; releases up to 0.5.0 exist only under the second name. The
image with the admin UI embedded is `ghcr.io/encircle360-oss/straightmail/aio` — see
[Images](../README.md#images) in the root README.

Variables should be set to your correct SMTP credentials and host. `DEFAULT_SENDER` is used when a
request carries no `sender`. `SMTP_PORT` defaults to `587`.

If you want to use SSL set `SMTP_ENABLE_SSL` to `true` and `SMTP_ENABLE_TLS` to `false`. Same for TLS
but in the other direction.

`AUTH_MODE` defaults to `oidc`, which needs a reachable OIDC issuer at startup; `ENCRYPTION_KEY` must
be the Base64 encoding of exactly 32 bytes, otherwise the application does not start. See the
[root README](../README.md#authentication) for the authentication modes.

All REST endpoints live under `API_PREFIX` (default `/api`). The Swagger UI is served at
`http://localhost:50003/swagger-ui/index.html`, the OpenAPI document at
`http://localhost:50003/api/v3/api-docs`.

Start straightmail with the `production` profile in a production environment
(`--env SPRING_PROFILES_ACTIVE=production`); it disables Swagger UI and the API docs. With
`AUTH_MODE=none` the API has no protection of its own, so keep such an instance on an internal
network.

### Example REST call to send an email

Let's send an email using a template from the templates directory. A template consists of the body
`<templateId>.ftl` and the subject `<templateId>_subject.ftl` (**important:** all HTML tags are
removed from the subject). `<templateId>_plain.ftl` is optional and holds a plain-text body; without
it the plain text is derived from the HTML body. The template ID is the file name without ending.

```
curl -d '{ "recipients": ["test@encircle360.com"], "sender": "test@encircle360.com", "senderName": "straightmail", "model": { "test": 200.8, "text": "test text" }, "locale": "de", "emailTemplateId": "default" }' \
    -H "Content-Type: application/json" -X POST http://localhost:50003/api/v1/email
```

The optional `senderName` renders the `From` header as `Display Name <address>`.

Let's send an email with a template submitted inline.

```
curl -d '{ "recipients": ["test@localhost"], "sender": "test@localhost", "model": { "test": 200.8, "text": "test text" }, "locale": "de", "subject": "test mail: ${text}", "emailTemplate": "${test!\"\"}" }' \
    -H "Content-Type: application/json" -X POST http://localhost:50003/api/v1/email/inline
```

`POST /api/v1/render` renders a template without sending it.

### Attachments

Attachments can be provided as an array of attachment objects. The content of the files should be a
base64 encoded string. For example:

```
{..., "attachments": [{"filename":"picture.jpg", "mimeType": "image/jpg", "content":"IG51bGw="}]}
```

### Customization

Bring your own templates and message bundles by mounting them into the container or by building
your own image on top of the straightmail image:

```
FROM ghcr.io/encircle360-oss/straightmail:0.5.1
ADD templates /resources/templates
ADD i18n /resources/i18n
```

Templates under `/resources/templates/<templateId>.ftl` are found by `POST /api/v1/email`. Render and
template listing read the per-tenant layout `TEMPLATE_FILE_PATH/<tenant>/<templateId>.ftl` instead —
the [root README](../README.md#templates-and-translations) describes both layouts. You can find
examples how [templates](src/main/resources/templates) and [i18n files](src/main/resources/i18n) look
like in [src/main/resources](src/main/resources).

#### Models in your template

You can use objects, lists and primitive types in your template (provided in the `model` field).
Numbers (doubles, integers and co.) will be mapped correctly into the template.

FreeMarker runs sandboxed: `?new` and `?api` are rejected, and the Spring form-binding macros are not
available.

### i18n

In template files you can use `<@spring.message "message.key"/>` to load messages from
`messages*.properties` bundles; the `locale` of the request selects the bundle. Bundles in
`I18N_PATH` (default `/resources/i18n`) are read first, the built-in bundles answer every other key.
Subjects are templates too, so you can use messages there as well.

### Database Support (optional)

The `database` profile stores templates and tenants in a database and enables template CRUD and
tenant management. SQLite is the default; the schema is managed by Liquibase on startup.

Example docker run command:

```
docker run -p 50003:50003 -p 50004:50004 \
    --env SPRING_PROFILES_ACTIVE=database \
    --env AUTH_MODE=none \
    --env ENCRYPTION_KEY="<openssl rand -base64 32>" \
    --env DB_URL="jdbc:sqlite:/data/straightmail.db?journal_mode=WAL" \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_PORT=1025 \
    --env DEFAULT_SENDER=test@encircle360.com \
    --volume straightmail-data:/data \
    ghcr.io/encircle360-oss/straightmail:0.5.1
```

Environment variables for database configuration:

| Variable       | Description              | Default                                                    |
|----------------|--------------------------|------------------------------------------------------------|
| `DB_URL`       | JDBC URL                 | `jdbc:sqlite:docker/data/sqlite/straightmail.db?journal_mode=WAL` |
| `DB_DRIVER`    | JDBC driver class        | `org.sqlite.JDBC`                                          |
| `DB_DIALECT`   | Hibernate dialect        | `org.hibernate.community.dialect.SQLiteDialect`            |
| `DB_USERNAME`  | Database username        | —                                                          |
| `DB_PASSWORD`  | Database password        | —                                                          |
| `DB_POOL_SIZE` | Maximum connection pool  | `1`                                                        |

The default `DB_URL` is relative to the working directory, which in the image is `/app`; set it to a
path on a volume such as `/data`. For PostgreSQL set `DB_URL=jdbc:postgresql://<host>:5432/<db>`,
`DB_DRIVER=org.postgresql.Driver` and `DB_DIALECT=org.hibernate.dialect.PostgreSQLDialect` — see
[`docker/oidc-postgres.yml`](../docker/oidc-postgres.yml).

### Service Health

If the management port is mapped, `http://localhost:50004/actuator/health` returns the health status
of the service, `/actuator/health/liveness` and `/actuator/health/readiness` the probe groups.

### Good to know

straightmail internally uses the [freemarker](https://freemarker.apache.org/) template engine which has the advantages
that it's easy to copy and paste email html templates.
This is really useful if you for example use email templates bought on themeforest. Since these templates can get
updates you don't have to check each html dom element while importing a template update.
Mostly you only have to focus on your content model variables and you're able to just copy the html from the update.

This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use. If you need support or consultancy just contact us.
