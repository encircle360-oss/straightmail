## straightmail - mail sending APIs with freemarker Templates

To run an instance of straightmail just run:
```
docker run -p8080:8080 \
    --env SMTP_PASSWORD=bar \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PORT=1025 \
    straightmail
```

Variables should be set to your correct SMTP credentials and host.

Now you can start using Straightmail for sending your email via simple rest calls

To add custom templates and i18n simply change the following line in Dockerfile to copy resources into docker container:

```
ADD src/main/resources /resources
```


This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use.
