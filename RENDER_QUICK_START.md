# 🚀 Render Deployment - Quick Start Checklist

## ✅ Pre-Deployment Checklist

- [ ] Code pushed to Git repository (GitHub/GitLab/Bitbucket)
- [ ] Render account created
- [ ] Database credentials ready

## 📝 Step-by-Step Deployment

### 1️⃣ Create Database (5 minutes)

1. Render Dashboard → **"New +"** → **"PostgreSQL"** (or MySQL)
2. Name: `pasantias-db`
3. Save connection details:
   - Host
   - Port  
   - Database name
   - Username
   - Password

### 2️⃣ Deploy Backend (10 minutes)

1. **"New +"** → **"Web Service"**
2. Connect repository → Select `Seminario_Integrador_2025`
3. Configure:
   - **Name**: `pasantias-backend`
   - **Environment**: `Java`
   - **Build Command**: `cd pasantias && mvn clean package -DskipTests`
   - **Start Command**: `cd pasantias && java -jar target/pasantias-0.0.1-SNAPSHOT.jar`
4. **Environment Variables** (copy-paste and replace placeholders):
   ```bash
   SPRING_DATASOURCE_URL=jdbc:mysql://<HOST>:<PORT>/db_pasantias?useSSL=true&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true&characterEncoding=UTF-8&useUnicode=true&connectionCollation=utf8mb4_unicode_ci
   SPRING_DATASOURCE_USERNAME=<USER>
   SPRING_DATASOURCE_PASSWORD=<PASSWORD>
   JWT_SECRET=<GENERATE_RANDOM_32_CHARS>
   JWT_EXPIRATION=3600
   CORS_ALLOWED_ORIGINS=https://pasantias-frontend.onrender.com
   MAIL_HOST=smtp.gmail.com
   MAIL_PORT=587
   MAIL_USERNAME=your-email@gmail.com
   MAIL_PASSWORD=your-app-password
   MAIL_SMTP_AUTH=true
   MAIL_SMTP_STARTTLS=true
   MAIL_FROM=your-email@gmail.com
   VERIFICATION_BASE_URL=https://pasantias-backend.onrender.com/auth/confirmar?token=
   VERIFICATION_EXPIRATION_HOURS=24
   ```
5. Click **"Create Web Service"**
6. Wait for deployment (~5-10 min)
7. **Copy backend URL** (e.g., `https://pasantias-backend.onrender.com`)

### 3️⃣ Deploy Frontend (5 minutes)

1. **"New +"** → **"Static Site"**
2. Connect same repository
3. Configure:
   - **Name**: `pasantias-frontend`
   - **Root Directory**: `frontend`
   - **Build Command**: `npm ci && npm run build`
   - **Publish Directory**: `dist`
4. **Environment Variable**:
   ```bash
   VITE_API_URL=https://pasantias-backend.onrender.com
   ```
   (Use your actual backend URL from step 2)
5. Click **"Create Static Site"**
6. Wait for deployment (~2-5 min)

### 4️⃣ Initialize Database Schema

**Option A: Using Render Shell**
1. Go to database service → **"Connect"** → **"Shell"**
2. Connect to MySQL:
   ```bash
   mysql -h <host> -u <user> -p
   ```
3. Run schema:
   ```sql
   USE db_pasantias;
   SOURCE script_bd/sql/schema.sql;
   ```

**Option B: Using MySQL Client**
1. Use external database URL from Render
2. Connect with MySQL Workbench
3. Import `script_bd/sql/schema.sql`

### 5️⃣ Update CORS (After Frontend Deploys)

1. Go to **Backend Service** → **Environment**
2. Update `CORS_ALLOWED_ORIGINS`:
   ```bash
   CORS_ALLOWED_ORIGINS=https://pasantias-frontend.onrender.com
   ```
3. **Redeploy** backend (or wait for auto-redeploy)

## 🔍 Verify Deployment

- [ ] Backend: `https://<backend-url>/swagger-ui/index.html` loads
- [ ] Frontend: `https://<frontend-url>` loads
- [ ] Database: Backend logs show successful connection
- [ ] API: Test login endpoint from Swagger UI

## 🆘 Common Issues

| Issue | Solution |
|-------|----------|
| Build fails | Check logs, verify Java 21 support |
| Database connection error | Verify credentials, check connection string |
| Frontend can't reach backend | Update `VITE_API_URL`, check CORS |
| CORS errors | Update `CORS_ALLOWED_ORIGINS` in backend |

## 📚 Full Documentation

See [RENDER_DEPLOYMENT.md](./RENDER_DEPLOYMENT.md) for detailed instructions.

---

**Estimated Total Time**: 20-30 minutes  
**Free Tier**: ✅ All services available on free tier
