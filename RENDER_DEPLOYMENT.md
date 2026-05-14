# Render Deployment

This repo is prepared for Render with a root `render.yaml` Blueprint and `Dockerfile.render`.

## What Render Will Create

- Public web service: `codesync-gateway`
- Private services: Eureka and the backend microservices
- Render Key Value instance: `codesync-redis`

Render does not run `docker-compose.yml` directly. This Blueprint deploys each Spring Boot service as a separate Docker-backed Render service.

## Required External Services

The application currently uses MySQL and RabbitMQ. Render's managed SQL database is PostgreSQL, so use an external MySQL provider and an external RabbitMQ provider, then fill the prompted secret values during Blueprint creation.

For each backend service, set:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

Use MySQL JDBC URLs such as:

```text
jdbc:mysql://MYSQL_HOST:3306/auth_db?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

Set RabbitMQ values for services that publish or consume messages:

- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `RABBITMQ_USER`
- `RABBITMQ_PASS`
- `SPRING_RABBITMQ_HOST`
- `SPRING_RABBITMQ_PORT`
- `SPRING_RABBITMQ_USERNAME`
- `SPRING_RABBITMQ_PASSWORD`

Set application secrets:

- `JWT_SECRET`
- `SUPER_ADMIN_EMAIL`
- `FRONTEND_URL`
- `OAUTH2_REDIRECT_URI`
- `EMAIL_FROM`
- `RESEND_API_KEY`
- `RAZORPAY_KEY_ID`
- `RAZORPAY_KEY_SECRET`
- `APP_RAZORPAY_KEY_ID`
- `APP_RAZORPAY_KEY_SECRET`
- `RAZORPAY_WEBHOOK_SECRET`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_ID`
- `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET`

## Deploy Steps

1. Push this folder to a Git repository.
2. In Render, create a new Blueprint from the repository.
3. Confirm Render detects `render.yaml` at the repo root.
4. Fill all `sync: false` values Render prompts for.
5. Deploy the Blueprint.

The public API entry point will be the `codesync-gateway` Render URL.
