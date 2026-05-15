#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [ ! -f .env.ec2 ]; then
  echo "Missing .env.ec2. Copy env/ec2/.env.ec2.example to .env.ec2 and fill it first." >&2
  exit 1
fi

missing_env=0
for example in env/ec2/*.env.example; do
  env_file="${example%.example}"
  if [ ! -f "$env_file" ]; then
    echo "Missing $env_file. Copy $example to $env_file and fill it." >&2
    missing_env=1
  fi
done

if [ "$missing_env" -ne 0 ]; then
  exit 1
fi

docker run --rm -v "$PWD":/workspace -w /workspace maven:3.9.9-eclipse-temurin-17 mvn -DskipTests clean package
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml up -d --build
docker compose --env-file .env.ec2 -f docker-compose.ec2.yml ps