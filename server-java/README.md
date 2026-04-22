# Java Auth Service

Spring Boot auth service for the interview demo.

## New registration email flow

The service now supports:

- QQ mailbox verification codes over SMTP
- Redis-backed code storage
- send cooldown
- daily rate limiting by email and IP
- max retry protection
- one-time code consumption

## Required runtime configuration

Set these environment variables before starting the service:

```powershell
$env:MAIL_HOST="smtp.qq.com"
$env:MAIL_PORT="465"
$env:MAIL_PROTOCOL="smtps"
$env:MAIL_USERNAME="your_qq_mail@qq.com"
$env:MAIL_PASSWORD="your_qq_smtp_authorization_code"
$env:MAIL_FROM="your_qq_mail@qq.com"

$env:REDIS_HOST="127.0.0.1"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD=""
$env:REDIS_DATABASE="0"
```

## QQ mailbox notes

- Enable SMTP in QQ Mail settings.
- Use the SMTP authorization code from QQ Mail, not the mailbox login password.
- The current implementation only allows `@qq.com` addresses for registration verification.

## Main endpoints

- `POST /api/auth/register/email-code/send`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`

## Important note

I could not run a full Maven compile in this environment because an actual Maven executable is not available on the machine right now. The frontend build passed, and the backend changes were checked statically.
