## straightmail - mail sending APIs with freemarker Templates

To run an instance of straightmail just run:
```
docker run -p 50003:50003 \
    --env SMTP_PASSWORD=bar \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PORT=1025 \
    --env DEFAULT_SENDER=test@encircle360.com \
    --env SMTP_ENABLE_TLS=true \
    --env SMTP_ENABLE_SSL=false \
    --env SPRING_PROFILES_ACTIVE=prod \
    registry.gitlab.com/encircle360-oss/straightmail:latest
```

Variables should be set to your correct SMTP credentials and host. DEFAULT_SENDER should be set to your default email address, if no email is set in payload this one will be used.

If you want to use SSL set SMTP_ENABLE_SSL to true and SMTP_ENABLE_TLS to false. Same for TLS but in other direction

Now you can start using Straightmail for sending your email via simple rest calls

To add custom templates and i18n simply change the following line in Dockerfile to copy resources into docker container:

```
ADD src/main/resources /resources
```

Lets give your straightmail a call:
```
curl -d "{ \"recipient\": \"test@localhost\", \"subject\": \"test mail\", \"model\": { \"testKey\": \"testValue\" }, \"emailTemplate\": { \"id\": \"default\", \"locale\": \"de\" } }"  -H "Content-Type: application/json" -X POST http://localhost:50003/
```

If you use dev profile visit:

```
http://straightmail-address:50003/swagger-ui/index.html?url=/v3/api-docs#/
```
On this page you can use swagger ui to test the RESTful API for E-Mail sending 

This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use.
