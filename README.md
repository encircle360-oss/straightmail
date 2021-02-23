## straightmail - mail sending APIs with template and i18n support

### Getting started
To run an instance with default tetmplates and language just run:
```
docker run -p 50003:50003 -p 50004:50004 \
    --env SMTP_PASSWORD=bar \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PORT=1025 \
    --env DEFAULT_SENDER=test@encircle360.com \
    --env SMTP_ENABLE_TLS=true \
    --env SMTP_ENABLE_SSL=false \
    --env SPRING_PROFILES_ACTIVE=development \
    registry.gitlab.com/encircle360-oss/straightmail/straightmail:latest
```
Variables should be set to your correct SMTP credentials and host. `DEFAULT_SENDER` should be set to your default email address, if no email is set in payload this one will be used.

If you want to use SSL set `SMTP_ENABLE_SSL` to `true` an `SMTP_ENABLE_TLS` to `false`. Same for TLS but in other direction

Now you can start using Straightmail for sending your email via simple rest calls.

Open `http://localhost:50003/swagger-ui/` in your browser to visit the swagger and openAPI documentation which shows the available REST endpoints you can use to send emails.

Note that you should start straightmail in `production` profile if you're in a production environment otherwise the swagger-ui will be be available and open to the world.
To do this just switch the docker env variable to `--env SPRING_PROFILES_ACTIVE=production`.

You should also note that this service is ment to be an internal service and shouldn't be exposed to the outside world since it has no security in it's APIs.

### Example REST call to send an email
Let's send an email using our straightmail instance with template as files in our templates file directory. 
Your template should be saved in two files, one for the subject and one for
the body of an email. Template id is the name of the template file without ending, the subject template always 
called templateId_subject.ftl (**important:** all HTML tags will be removed from subject while parsing).

You can also add templateId_plain.ftl for plaintext usage of the template, please avoid to use HTML in this template. 
```
curl -d '{   "recipients": ["test@encircle360.com"],   "cc": null,   "bcc": null,   "attachments": null,   "sender": "test@encircle360.com",   "model": {     "test": 200.8,     "text": "test text"   },   "locale": "de",   "subject": "test mail: ${text}",   "emailTemplateId": "default" }'  -H "Content-Type: application/json" -X POST http://localhost:50003/
```

Let's send an email using our straightmail instance with a template submitted inline.
```
curl -d '{   "recipients": ["test@localhost"],   "cc": null,   "bcc": null,   "attachments": null,   "sender": "test@localhost",   "model": {     "test": 200.8,     "text": "test text"   },   "locale": "de",   "subject": "test mail: ${text}",   "emailTemplate": "${test!\"\"}" }' -H "Content-Type: application/json" -X POST http://localhost:50003/inline
```

### Attachments
Attachments can be provided as array of attachment objects. The content of the files should be a base64 encoded utf 8 string. for example:

```
{..., "attachments": [{"filename":"picture.jpg", "mimeType": "image/jpg", "content":"IG51bGw="}]}
```

### Customization
If you want to bring your own templates and language files just use the straightmail base image and create your own straightmail image on top of that.
We suggest to use your own Dockerfile for that. You could for example put this one with your templates and language files into a git repository to have everything versioned.

#### Models in your template

You can use objects, lists and primitive types in your template (provided in model field). Numbers (doubles, integers and co.) will be mapped correctly into the template.  

### i18n

As you can see in Dockerfile we provide i18n functionality. You can also provide customized subject, just use freemarker template language in template_subject.ftl files 

In template files you can use:
```<@spring.message "message.key"/>``` to load messages from properties files. In the next section example Dockerfile, more information about templates and messages will be provided. 

### Example Dockerfile

Since straightmail will lookup for templates in `/resources/templates/` and i18n files in `/resources/i18n/` you can use the following Dockerfile as example to create your own docker image with your own templates and i18n.
You can find examples how [templates](src/main/resources/templates) or [i18n files](src/main/resources/i18n) look like [here](src/main/resources).
```
FROM registry.gitlab.com/encircle360-oss/straightmail/straightmail:latest
ADD templates /resources/templates # add your template directory containing *.ftl templates here
ADD i18n /resources/i18n # add your i18n directory containing messages.properties files here
```

If you're done with this you can build your own image using docker-cli `docker build .` or let your build pipeline do that.
E.g. we suggest to use gitlab-ci to always have your own customized straightmail docker image.

After you've build your own docker image with your own templates you can use the REST api to send emails.
The `emailTemplateId` field corresponds to the template filename. If you've added a template called `emailConfirmation.ftl` you have to use 
`"emailConfirmation"`
within your API request payload.

### Service Health

If you started with port exposing to localhost you can fetch `http://localhost:50004/actuator/health` to get health status of the service itself 

### Good to know 
Straightmail internally uses the [freemarker](https://freemarker.apache.org/) template engine which has the advantages that it's easy to copy and paste email html templates.
This is really useful if you for example use email templates bought on themeforest. Since these templates can get updates you don't have to check each html dom element while importing a template update.
Mostly you only have to focus on your content model variables and you're able to just copy the html from the update. 

Templates will be used for body and subject, too. So you can use i18n features also in your subject. 

So you have the [full feature support](https://freemarker.apache.org/docs/ref.html) of freemarker in your templates and also activated i18n support so that you can also put your resource bundles with messages.properties into your image and switch the locale for each email you're sending.

This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use. If you need support or consultancy just contact us.
