# Deploy minimal CodeSync backend on AWS EC2

This is the free-tier-friendly first deployment. It runs only:

- MySQL
- Redis
- Eureka
- auth-service
- project-service
- file-service
- codesync-gateway

The full backend stack is intentionally not started.

## 1. EC2 instance

Use a free-tier eligible Ubuntu instance:

- Ubuntu 22.04 or 24.04 LTS
- `t2.micro` or `t3.micro`, only if AWS marks it free-tier eligible in your region/account
- 20 GB gp3/gp2 storage

Security group inbound rules:

- `22` from your IP only
- `8080` from anywhere for gateway
- `8081` from anywhere only if you want direct OAuth login on auth-service
- `8761` from your IP only, optional for Eureka debugging

Do not expose MySQL or Redis.

## 2. Install Docker

SSH into EC2:

```bash
ssh -i your-key.pem ubuntu@<EC2_PUBLIC_IP>
```

Install Docker:

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

## 3. Get the code onto EC2

Clone your repository:

```bash
git clone <your-repo-url> CodeSync
cd CodeSync/backend
```

Or upload the project folder and then enter `backend`.

## 4. Create env files

From `backend`:

```bash
cp env/ec2-minimal/.env.ec2-minimal.example .env.ec2-minimal
for f in env/ec2-minimal/*.env.example; do cp "$f" "${f%.example}"; done
```

Edit the shared env:

```bash
nano .env.ec2-minimal
```

Set:

```env
MYSQL_ROOT_PASSWORD=<strong-password>
```

Edit each service env and replace placeholders:

```bash
nano env/ec2-minimal/auth-service.env
nano env/ec2-minimal/project-service.env
nano env/ec2-minimal/file-service.env
```

Use the same MySQL password for:

```text
AUTH_DB_PASSWORD
PROJECT_DB_PASSWORD
FILE_DB_PASSWORD
```

In `auth-service.env`, also set:

```text
JWT_SECRET
SUPER_ADMIN_EMAIL
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET
SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GITHUB_CLIENT_SECRET
```

## 5. Build JARs

From `backend`:

```bash
docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -DskipTests clean package
```

On free-tier EC2 this may be slow. If it fails due to memory, build locally and upload the generated `target/*.jar` files, then run the compose command on EC2.

## 6. Start minimal backend

```bash
docker compose --env-file .env.ec2-minimal -f docker-compose.ec2-minimal.yml up -d --build
```

Check:

```bash
docker compose --env-file .env.ec2-minimal -f docker-compose.ec2-minimal.yml ps
docker logs -f codesync-min-gateway
```

Backend gateway URL:

```text
http://<EC2_PUBLIC_IP>:8080
```

Auth direct URL:

```text
http://<EC2_PUBLIC_IP>:8081
```

## 7. Stop minimal backend

```bash
docker compose --env-file .env.ec2-minimal -f docker-compose.ec2-minimal.yml down
```

Delete local database volume if you want a clean reset:

```bash
docker compose --env-file .env.ec2-minimal -f docker-compose.ec2-minimal.yml down -v
```
