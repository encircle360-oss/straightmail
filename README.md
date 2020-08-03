## straigtmail - mail sending APIs with freemarker Templates

To run an instance of straightmail just run:

docker run -p8080:8080 \
    --env SMTP_PASSWORD=bar \
    --env SMTP_HOST=host.docker.internal \
    --env SMTP_USER=foo \
    --env SMTP_PORT=1025 \
    straightmail

Variables should be set to your correct SMTP credentials and host.

This is open source software by [encircle360](https://encircle360.com).
Use on your own risk and for personal use.
