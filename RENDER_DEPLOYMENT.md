# 🚀 Render Deployment Guide

This guide will help you deploy the entire project (Backend, Frontend, and Database) on Render.

## 📋 Prerequisites

1. A [Render account](https://render.com) (free tier available)
2. Your code pushed to a Git repository (GitHub, GitLab, or Bitbucket)
3. MySQL database credentials (Render provides MySQL databases)

## 🗄️ Step 1: Create MySQL Database

1. Log in to [Render Dashboard](https://dashboard.render.com)
2. Click **"New +"** → **"PostgreSQL"** (or MySQL if available)
3. **Note**: Render primarily supports PostgreSQL. If MySQL is not available, you have two options:
   - Use an external MySQL service (like [PlanetScale](https://planetscale.com), [Aiven](https://aiven.io), or [Railway](https://railway.app))
   - Migrate to PostgreSQL (see migration notes below)
4. Configure:
   - **Name**: `pasantias-db`
   - **Database**: `db_pasantias`
   - **User**: `pasantias_user`
   - **Plan**: Free (or paid for production)
5. Click **"Create Database"**
6. **Save the connection details** - you'll need:
   - **Internal Database URL** (for backend service)
   - **External Database URL** (for local access if needed)
   - **Host**
   - **Port**
   - **Database Name**
   - **Username**
   - **Password**

## 🔧 Step 2: Deploy Backend (Spring Boot)

1. In Render Dashboard, click **"New +"** → **"Web Service"**
2. Connect your repository:
   - Select your Git provider (GitHub/GitLab/Bitbucket)
   - Choose the repository: `Seminario_Integrador_2025`
   - Select branch: `develop` (or `feature/render-deployment` if you want to use the new branch)
3. Configure the service:
   - **Name**: `pasantias-backend`
   - **Environment**: `Docker` ⚠️ **Select Docker, not Java**
   - **Region**: Choose closest to your users
   - **Branch**: `develop` (or your deployment branch)
   - **Root Directory**: `pasantias` (important: this tells Render where the Dockerfile is)
   - **Dockerfile Path**: `Dockerfile` (relative to root directory, so it will be `pasantias/Dockerfile`)
   - **Docker Context**: `pasantias` (same as root directory)
   - **Instance Type**: Free (or upgrade for production)
   
   **Important Docker Configuration**:
   - The Dockerfile is located at `pasantias/Dockerfile`
   - Render will automatically build the Docker image
   - The app will listen on the port provided by Render's `PORT` environment variable (configured in `application.properties`)
   - No need to set `PORT` manually - Spring Boot will use it automatically

4. **Environment Variables** - Add these:
   ```
   SPRING_DATASOURCE_URL=jdbc:mysql://<DB_HOST>:<DB_PORT>/db_pasantias?useSSL=true&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true&connectionCollation=utf8mb4_unicode_ci
   SPRING_DATASOURCE_USERNAME=<DB_USER>
   SPRING_DATASOURCE_PASSWORD=<DB_PASSWORD>
   JWT_SECRET=<GENERATE_A_LONG_RANDOM_SECRET_KEY_HERE>
   JWT_EXPIRATION=3600
   CORS_ALLOWED_ORIGINS=https://<YOUR_FRONTEND_URL>
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=<YOUR_EMAIL>
   MAIL_PASSWORD=<YOUR_APP_PASSWORD>
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS=true
   MAIL_FROM=<YOUR_EMAIL>
   VERIFICATION_BASE_URL=https://<YOUR_BACKEND_URL>/auth/confirmar?token=
   VERIFICATION_EXPIRATION_HOURS=24
   ```
   
   **Note**: For `CORS_ALLOWED_ORIGINS`, you can add multiple URLs separated by commas:
   ```
   CORS_ALLOWED_ORIGINS=https://pasantias-frontend.onrender.com,https://www.yourdomain.com
   ```

   **Important**: Replace placeholders:
   - `<DB_HOST>`, `<DB_PORT>`, `<DB_USER>`, `<DB_PASSWORD>` with your database credentials
   - `<YOUR_BACKEND_URL>` with your backend Render URL (e.g., `pasantias-backend.onrender.com`)
   - Generate a secure `JWT_SECRET` (at least 32 characters)

5. Click **"Create Web Service"**
6. Wait for deployment to complete (5-10 minutes)

## 🎨 Step 3: Deploy Frontend (React + Vite)

1. In Render Dashboard, click **"New +"** → **"Static Site"**
2. Connect your repository:
   - Select the same repository
   - Branch: `develop` (or `main`)
3. Configure:
   - **Name**: `pasantias-frontend`
   - **Root Directory**: `frontend`
   - **Build Command**: 
     ```bash
     npm ci && npm run build
     ```
   - **Publish Directory**: `dist`
   - **Environment**: `Node`

4. **Environment Variables**:
   ```
   VITE_API_URL=https://<YOUR_BACKEND_URL>
   ```
   Replace `<YOUR_BACKEND_URL>` with your backend Render URL (e.g., `https://pasantias-backend.onrender.com`)

5. Click **"Create Static Site"**
6. Wait for deployment (2-5 minutes)

## 📊 Step 4: Initialize Database Schema

After the database is created, you need to run the schema:

1. **Option A: Using Render Shell** (Recommended)
   - Go to your database service in Render
   - Click **"Connect"** → **"Shell"**
   - Run:
     ```bash
     mysql -h <host> -u <user> -p <database> < /path/to/schema.sql
     ```
   - Or copy-paste the contents of `script_bd/sql/schema.sql`

2. **Option B: Using MySQL Client Locally**
   - Use the external database URL from Render
   - Connect with MySQL Workbench or command line:
     ```bash
     mysql -h <external-host> -P <port> -u <user> -p
     ```
   - Run:
     ```sql
     USE db_pasantias;
     SOURCE script_bd/sql/schema.sql;
     ```

3. **Option C: Using a Migration Script**
   - Create a one-time web service that runs the schema
   - Or use Render's database initialization feature

## 🔗 Step 5: Update Frontend API URL

After backend deployment, update the frontend environment variable:

1. Go to **Frontend Service** → **Environment**
2. Update `VITE_API_URL` to your backend URL:
   ```
   VITE_API_URL=https://pasantias-backend.onrender.com
   ```
3. **Redeploy** the frontend (Render will auto-redeploy on next commit, or manually trigger)

## ✅ Step 6: Verify Deployment

1. **Backend Health Check**:
   - Visit: `https://<backend-url>/swagger-ui/index.html`
   - Should show Swagger UI

2. **Frontend**:
   - Visit your frontend URL
   - Should load the React app

3. **Database Connection**:
   - Check backend logs for database connection success
   - Test an API endpoint

## 🔄 Alternative: Using render.yaml (Infrastructure as Code)

If you prefer to use the `render.yaml` file:

1. Ensure `render.yaml` is in your repository root
2. In Render Dashboard, click **"New +"** → **"Blueprint"**
3. Connect your repository
4. Render will automatically detect and create services from `render.yaml`
5. You'll still need to:
   - Set environment variables manually
   - Initialize the database schema

## 🐛 Troubleshooting

### Backend won't start
- Check logs: **Service** → **Logs**
- Verify database connection string
- Ensure Docker build completed successfully (check build logs)
- Verify **Root Directory** is set to `pasantias`
- Verify **Dockerfile Path** is `Dockerfile` (relative to root directory)
- The app automatically uses Render's `PORT` env var via `server.port=${PORT:8080}` in `application.properties`
- Check Docker build logs for Maven build errors

### Database connection errors
- Verify database credentials
- Check if database is accessible from backend service
- Ensure database is in the same region
- Verify connection string format

### Frontend can't connect to backend
- Check `VITE_API_URL` environment variable
- Verify CORS is configured in backend
- Check browser console for errors
- Ensure backend URL includes `https://`

### Build failures
- Check build logs for specific errors
- Verify all dependencies are in `pom.xml` / `package.json`
- Ensure build commands are correct

## 📝 Environment Variables Reference

### Backend Required Variables:
```bash
SPRING_DATASOURCE_URL          # MySQL connection string
SPRING_DATASOURCE_USERNAME     # Database username
SPRING_DATASOURCE_PASSWORD     # Database password
JWT_SECRET                     # Secret key for JWT tokens
JWT_EXPIRATION                # Token expiration in seconds
VERIFICATION_BASE_URL         # Base URL for email verification links
```

### Frontend Required Variables:
```bash
VITE_API_URL                  # Backend API URL
```

## 🔐 Security Notes

1. **Never commit** `.env` files or secrets
2. Use Render's environment variables for all secrets
3. Generate strong `JWT_SECRET` (use: `openssl rand -base64 32`)
4. Use HTTPS (Render provides SSL automatically)
5. Update `VERIFICATION_BASE_URL` to production URL
6. Configure CORS properly in backend for production domain

## 📚 Additional Resources

- [Render Documentation](https://render.com/docs)
- [Spring Boot on Render](https://render.com/docs/deploy-spring-boot)
- [Static Sites on Render](https://render.com/docs/static-sites)
- [MySQL on Render](https://render.com/docs/databases)

## 🆘 Support

If you encounter issues:
1. Check Render service logs
2. Verify environment variables
3. Test database connection separately
4. Check Render status page for outages

---

**Last Updated**: January 2026
**Render Plan**: Free tier (upgrade for production)
