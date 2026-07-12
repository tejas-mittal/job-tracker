# Job Application Tracker

An automated Job Application Tracker featuring a Spring Boot Monolith backend and a React frontend. It streamlines the job hunt with application tracking, Kanban boards, and AI-powered email status parsing.

---

## 🏗️ Architecture & Tech Stack

This project uses a modern, full-stack architecture optimized for easy cloud deployment (e.g., Render, Koyeb, Heroku).

**Backend (Monolith):**
- **Language**: Java 21
- **Framework**: Spring Boot 3.2
- **Auth**: Spring Security + OAuth2 Resource Server (JWT HS256)
- **Persistence**: PostgreSQL 16, Spring Data JPA
- **Event Bus**: Spring `ApplicationEventPublisher` (In-memory asynchronous events)
- **Containerization**: Docker, Docker Compose

**Frontend:**
- **Framework**: React (Vite)
- **Routing**: React Router
- **Styling**: Vanilla CSS (Premium, Glassmorphism, Modern UI)

---

## 🚀 Features

- **Job Application CRM**: Track all your applications, statuses, and interview dates.
- **Automated Email Parsing**: Integrates with Gmail to detect job statuses (Rejection, Interview, Offer) and automatically updates your tracker.
- **Kanban Board**: Drag-and-drop board for visual tracking.
- **Analytics Dashboard**: View aggregate statistics of your job hunt performance.
- **Secure Authentication**: Google OAuth2 login and secure JWT management.

---

## 🛠️ Local Setup & Development

### Prerequisites

- Docker Desktop (or Docker Engine + Compose v2)
- (Optional) Java 21 + Maven
- (Optional) Node.js 20+

### Start Everything (One-Click Setup)

The easiest way to run the entire stack (Postgres Database, Spring Boot Backend, React Frontend) is using Docker Compose.

```bash
git clone <repo-url>
cd <repo>
docker compose up -d --build
```

**Services Started:**
- **Backend API**: `http://localhost:8080`
- **Frontend App**: `http://localhost:5173`
- **PostgreSQL**: `localhost:5432`

---

## ☁️ Deployment (Cloud Hosting)

The application is designed to be easily deployed to free-tier cloud providers like Render, Koyeb, or Railway.

### 1. Provision a Database
Set up a free Postgres instance (e.g., Render PostgreSQL, Supabase, Neon.tech) and save the connection string.

### 2. Configure Environment Variables
You will need to provide the following environment variables to your deployment platform:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET` (Must be at least 32 characters)
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### 3. Deploy to Render (Example)
1. Go to your [Render Dashboard](https://dashboard.render.com/) and create a new **Web Service**.
2. Connect this GitHub repository.
3. Configuration:
   - **Root Directory:** `monolith-service`
   - **Environment:** `Docker`
   - **Instance Type:** Free (or Starter)
4. Add your Environment Variables.
5. Click **Deploy**! Render will automatically build the image using `monolith-service/Dockerfile` and host it for you.

---

## 🔐 Security Notes

- OAuth tokens (Gmail access/refresh) are encrypted at rest using AES-256.
- Secrets are managed entirely through environment variables (never hardcoded).
- Each linked Gmail account requests only the strict `gmail.readonly` scope.
