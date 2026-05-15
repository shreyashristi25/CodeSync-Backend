# Deploy CodeSync backend on AWS EC2

This deployment path is isolated from local development. It uses `docker-compose.ec2.yml` and env files under `env/ec2`, so the existing local `docker-compose.yml` and local scripts can stay as they are.

## 1. Create the EC2 instance

Recommended for this full microservice stack on one machine:

- Ubuntu 22.04 or 24.04 LTS
- At least 4 vCPU / 8 GB RAM for a smoother first deployment
- 30+ GB EBS storage
- Security group inbound rules:
  - `22` from your IP only
  - `8080` from anywhere or from your frontend/user IPs
  - Optional temporary checks: `8761` from your IP only for Eureka
  - Do not expose MySQL `3306`, Redis `6379`, or RabbitMQ `5672` publicly

## 2. Install Docker on EC2

SSH into EC2 and run:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo ${UBUNTU_CODENAME:-$VERSION_CODENAME}) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker ubuntu
newgrp docker
```

## 3. Put the backend on EC2

Either clone your repository:

```bash
git clone <your-repo-url> CodeSync
cd CodeSync/backend
```

Or upload this project folder with `scp` and then enter `backend`.

## 4. Create EC2 env files

From `backend`:

```bash
cp env/ec2/.env.ec2.example .env.ec2
for f in env/ec2/*.env.example; do cp "$f" "${f%.example}"; done
```

Edit `.env.ec2` and every `env/ec2/*.env` file:

```bash
nano .env.ec2
nano env/ec2/auth-service.env
```

Minimum required replacements:

- `MYSQL_ROOT_PASSWORD`
- every service `*_DB_PASSWORD`
- `RABBITMQ_PASS` in `.env.ec2`, `execution-service.env`, and `notification-service.env`
- `JWT_SECRET`
- email/Razorpay values if those features are enabled
- OAuth secrets in `auth-service.env`

The Vercel frontend origin is already set to:

```text
https://codesync-client.vercel.app
```

## 5. Build the Spring Boot JARs

The service Dockerfiles expect `target/*.jar` to already exist. From `backend`, build all modules with a Maven Docker image:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -DskipTests clean package
```

## 6. Start the backend stack

From `backend`:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --build
```

Check containers:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps
```

Check logs:

```bash
docker logs -f codesync-gateway
docker logs -f codesync-auth
docker logs -f codesync-eureka
```

## 7. Backend URL for frontend

Your public backend base URL will be:

```text
http://<EC2_PUBLIC_IP>:8080
```

Your frontend API base should point to that gateway URL.
Important frontend note: the current Vercel client build must not point to `localhost`. Several client files still contain direct local URLs such as `http://localhost:8080`, `http://localhost:8081`, `http://localhost:8082`, and service-specific ports. Before the deployed frontend can use this EC2 backend, update the frontend API base URLs to your EC2 gateway URL, then redeploy Vercel.

## 8. OAuth callback URLs

In Google Cloud Console, add this authorized redirect URI:

```text
http://<EC2_PUBLIC_IP>:8081/login/oauth2/code/google
```

In GitHub OAuth app settings, add this callback URL:

```text
http://<EC2_PUBLIC_IP>:8081/login/oauth2/code/github
```

If you later add a domain and HTTPS, replace those with the HTTPS domain versions.

## Useful maintenance commands

Restart after env changes:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d
```

Rebuild after code changes:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --build
```

Stop everything:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml down
```

Stop and delete database/Redis/RabbitMQ volumes:

```bash
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml down -v
```