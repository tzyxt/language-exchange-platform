# Deployment Guide

This project can be deployed with Docker Compose.

## 1. Requirements

- Docker
- Docker Compose

## 2. Prepare environment variables

Create a `.env` file in the project root. You can copy from `.env.example`.

Example:

```env
MYSQL_ROOT_PASSWORD=your_strong_password
MYSQL_DATABASE=language_exchange_platform
MYSQL_PORT=3306
BACKEND_PORT=8080
FRONTEND_PORT=80
APP_CORS_ALLOWED_ORIGIN_PATTERNS=*
```

## 3. Start services

Run in the project root:

```bash
docker compose up -d --build
```

After startup:

- Frontend: `http://your-server-ip/`
- Backend API: `http://your-server-ip/api`
- Upload files: `http://your-server-ip/uploads`

## 4. Stop services

```bash
docker compose down
```

If you want to keep the database and uploaded files, do not remove volumes.

## 5. Common operations

View logs:

```bash
docker compose logs -f
```

Rebuild a single service:

```bash
docker compose up -d --build frontend
docker compose up -d --build backend
```

## 6. Server firewall

Open these ports on your server:

- `80` for the website
- `8080` only if you want direct backend access
- `3306` only if you want external database access

For safer production deployment, keep `3306` closed to the public internet.

## 7. Notes

- The frontend now supports same-origin deployment through Nginx reverse proxy.
- The backend database credentials are now read from environment variables first.
- Uploaded files are persisted in the Docker volume `uploads_data`.
