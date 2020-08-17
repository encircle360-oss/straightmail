## straightmail - mail sending APIs with template and i18n support

### Getting started
To run an instance with default tetmplates and language just run:
```
docker run -p 50003:50003 \
    --env SMTP_PASSWORD=bar \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PORT=1025 \
    --env DEFAULT_SENDER=test@encircle360.com \
    --env SMTP_ENABLE_TLS=true \
    --env SMTP_ENABLE_SSL=false \
    --env SPRING_PROFILES_ACTIVE=development \
    registry.gitlab.com/encircle360-oss/straightmail:latest
```
Variables should be set to your correct SMTP credentials and host. `DEFAULT_SENDER` should be set to your default email address, if no email is set in payload this one will be used.

If you want to use SSL set `SMTP_ENABLE_SSL` to `true` an `SMTP_ENABLE_TLS` to `false`. Same for TLS but in other direction

Now you can start using Straightmail for sending your email via simple rest calls.

Open `http://localhost:50003/swagger-ui` in your browser to visit the swagger and openAPI documentation which shows the available REST endpoints you can use to send emails.

Note that you should start straightmail in `production` profile if you're in a production environment otherwise the swagger-ui will be be available and open to the world.
To do this just switch the docker env variable to `--env SPRING_PROFILES_ACTIVE=production`.

You should also note that this service is ment to be an internal service and shouldn't be exposed to the outside world since it has no security in it's APIs.

### Example REST call to send an email
Let's send an email using our straightmail instance.
```
curl -d "{ \"recipient\": \"test@localhost\", \"subject\": \"test mail\", \"model\": { \"testKey\": \"testValue\" }, \"emailTemplate\": { \"id\": \"default\", \"locale\": \"de\" } }"  -H "Content-Type: application/json" -X POST http://localhost:50003/
```

### Customization
If you want to bring your own templates and language files just use the straightmail base image and create your own straightmail image on top of that.
We suggest to use your own Dockerfile for that. You could for example put this one with your templates and language files into a git repository to have everything versioned.

### Example Dockerfile

Since straightmail will lookup for templates in `/resources/templates/` and i18n files in `/resources/i18n/` you can use the following Dockerfile as example to create your own docker image with your own templates and i18n.
You can find examples how [templates](src/main/resources/templates) or [i18n files](src/main/resources/i18n) look like [here](src/main/resources).
```
FROM registry.gitlab.com/encircle360-oss/straightmail:latest
ADD templates /resources/templates # add your template directory containing *.ftl templates here
ADD i18n /resources/i18n # add your i18n directory containing messages.properties files here
```

If you're done with this you can build your own image using docker-cli `docker build .` or let your build pipeline do that.
E.g. we suggest to use gitlab-ci to always have your own customized straightmail docker image.

### Good to know 
Straightmail internally uses the [freemarker](https://freemarker.apache.org/) template engine which has the advantages that it's easy to copy and paste email html templates.
This is really useful if you for example use email templates bought on themeforest. Since these templates can get updates you don't have to check each html dom element while importing a template update.
Mostly you only have to focus on your content model variables and you're able to just copy the html from the update.

So you have the [full feature support](https://freemarker.apache.org/docs/ref.html) of freemarker in your templates and also activated i18n support so that you can also put your resource bundles with messages.properties into your image and switch the locale for each email you're sending.

This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use. If you need support or consultancy just contact us.
