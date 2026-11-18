# Deploying bds-backend

Complete steps to take a fresh Ubuntu server to a running BDS Marketplace backend, deployed by Jenkins as a Docker container next to an Oracle Database Free container.

> Once steps 1–6 are done on a server, day-to-day work starts at [step 7](#7-verify).

## Contents

0. [What you end up with](#0-what-you-end-up-with)
1. [Prerequisites](#1-prerequisites)
2. [Get the code onto the server](#2-get-the-code-onto-the-server)
3. [One-time server setup: Oracle and secrets](#3-one-time-server-setup-oracle-and-secrets)
4. [Jenkins access to the settings](#4-jenkins-access-to-the-settings)
5. [Jenkins job](#5-jenkins-job)
6. [First deployment](#6-first-deployment)
7. [Verify](#7-verify)
8. [Make it public with nginx and HTTPS](#8-make-it-public-with-nginx-and-https)
9. [Before real users: email and OTPs](#9-before-real-users-email-and-otps)
10. [Day-to-day operations](#10-day-to-day-operations)
11. [Troubleshooting](#11-troubleshooting)
12. [Frontend (bds-frontend)](#12-frontend-bds-frontend)

---

## 0. What you end up with

| Piece | Details |
|---|---|
| Code | <https://github.com/Saic98779/bds-marketplace> (`main` branch) |
| Image build | `Dockerfile`: Maven build and tests, then a non-root Java 21 runtime image |
| Pipeline | `Jenkinsfile`, Jenkins job `bds-backend` on the Jenkins running on 51.79.165.43 (port 8080), checks `main` every 5 minutes |
| Database | container `bds-oracle` (Oracle 23ai Free, database `FREEPDB1`, user `bds`), data in volume `bds-oracle-data`; host access on `127.0.0.1:1522` (1521 is often taken by another Oracle container) |
| Backend | container `bds-backend` on `127.0.0.1:8090` → `8080`, uploaded files in volume `bds-backend-data` |
| Frontend | container `bds-frontend` (nginx serving the React build) on `127.0.0.1:8091`, Jenkins job `bds-frontend`, public at <https://bds.metaversedu.in> ([step 12](#12-frontend-bds-frontend)) |
| Network | all containers on the private Docker network `bds-net`; the backend connects to `bds-oracle:1521`, the frontend proxies `/api` to `bds-backend:8080` |
| Secrets | `/etc/bds/bds-backend.env` on the server (root only); the container settings `/etc/bds/bds-backend.container.env` (readable by the `jenkins` group) |

```
                 ┌──────────────── 51.79.165.43 ─────────────────┐
  git push ──▶   │ Jenkins (8080) ── docker build ──▶ bds-backend │
  (GitHub main)  │                                   127.0.0.1:8090│
                 │                     bds-net          │         │
                 │                        └──▶ bds-oracle:1521    │
  Internet ──▶   │ nginx 443 ──▶ localhost:8090   (step 8)        │
                 └────────────────────────────────────────────────┘
```

## 1. Prerequisites

- Ubuntu server with `sudo` access, at least **4 GB free RAM** (Oracle uses about 2 GB) and 10 GB free disk.
- Jenkins installed on the same server, with these plugins: **Pipeline**, **Git**, **Timestamper**.
- Docker, with the `jenkins` user in the `docker` group:

  ```bash
  sudo usermod -aG docker jenkins
  sudo systemctl restart jenkins
  ```

## 2. Get the code onto the server

```bash
git clone https://github.com/Saic98779/bds-marketplace.git /tmp/bds-setup
```

## 3. One-time server setup: Oracle and secrets

```bash
sudo bash /tmp/bds-setup/bds-backend/deploy/setup-server.sh
```

> **Server already runs an Oracle container** (e.g. `oracle21c`, Oracle XE)? If there isn't about 3 GB of RAM free for a second database, share it instead:
>
> ```bash
> sudo EXISTING_ORACLE=oracle21c bash /tmp/bds-setup/bds-backend/deploy/setup-server.sh
> ```
>
> This creates the `bds` user in `XEPDB1` (override with `ORACLE_SERVICE=...`), connects that container to `bds-net` and sets `DB_URL=jdbc:oracle:thin:@//oracle21c:1521/XEPDB1`. Wherever this guide says `bds-oracle` / `FREEPDB1`, read `oracle21c` / `XEPDB1`. The choice is saved in `/etc/bds/bds-backend.env`, so later re-runs of the script don't need `EXISTING_ORACLE`.

The script is safe to re-run. It:

- installs Docker if it's missing, and creates the `bds-net` network
- generates the secrets into `/etc/bds/bds-backend.env`: database password, JWT, data and hash keys, and the initial admin password
- writes the container's settings to `/etc/bds/bds-backend.container.env`, readable by the `jenkins` group
- starts `bds-oracle` and waits for `DATABASE IS READY TO USE`

> ⚠️ **Back up `/etc/bds/bds-backend.env` somewhere safe.** `BDS_DATA_KEY` and `BDS_HASH_KEY` encrypt fields in the database. If you lose or change them, that data can't be read again.

The variables are listed in [`bds-backend/deploy/bds-backend.env.example`](bds-backend/deploy/bds-backend.env.example).

## 4. Jenkins access to the settings

Nothing to upload: the `Jenkinsfile` reads `/etc/bds/bds-backend.container.env` straight from the server. Check that Jenkins can read it:

```bash
sudo -u jenkins test -r /etc/bds/bds-backend.container.env && echo OK
```

If it isn't `OK` (e.g. Jenkins was installed after step 3), re-run `setup-server.sh`; it sets the file's group to `jenkins`.

> Never put these values in `application.yml` or anywhere else in the repository.

## 5. Jenkins job

1. Click **New Item**, name it `bds-backend`, choose **Pipeline**, and click **OK**.
2. Under **Pipeline**, set:
   - **Definition:** Pipeline script from SCM
   - **SCM:** Git
   - **Repository URL:** `https://github.com/Saic98779/bds-marketplace.git`
   - **Branch Specifier:** `*/main`
   - **Script Path:** `Jenkinsfile`
3. Click **Save**. The 5-minute GitHub check comes from the `Jenkinsfile` itself.

## 6. First deployment

Click **Build Now**. The pipeline then:

| Stage | What happens |
|---|---|
| Checkout | checks out the code and tags the image `<build number>-<commit>`, e.g. `bds-backend:3-81f72cb` |
| Build & Test Image | `docker build`: Maven compiles and runs the tests inside it, so a failing test stops the deploy |
| Deploy | connects `bds-oracle` to `bds-net`, remembers the running image, replaces the `bds-backend` container |
| Health Check | polls `http://localhost:8090/actuator/health` for up to 150 seconds |
| On failure | restarts the previously running image (automatic rollback) |

The first build takes about 2–3 minutes; later builds are faster because the Docker layers are cached.

## 7. Verify

On the server:

```bash
docker ps --filter name=bds-                      # bds-backend and bds-oracle both "Up"
curl http://127.0.0.1:8090/actuator/health        # {"status":"UP"}
curl -I http://127.0.0.1:8090/swagger-ui.html     # 200 or a redirect to the Swagger UI
sudo grep BDS_ADMIN_PASSWORD /etc/bds/bds-backend.env
```

Log in as `admin@bdsmarketplace.in` with that password. While `BDS_EXPOSE_OTP=true`, the login OTP is returned in the response as `devOtp`.

## 8. Make it public with nginx and HTTPS

The container only listens on `127.0.0.1`, so the recommended way in is through nginx, like the Jenkins site.

1. Create a DNS **A record**, e.g. `bds-api.metaversedu.in`, pointing to `51.79.165.43`.
2. Create `/etc/nginx/sites-available/bds-api.metaversedu.in`:

   ```nginx
   server {
       listen 80;
       server_name bds-api.metaversedu.in;
       client_max_body_size 30M;          # matches the app's upload limit

       location / {
           proxy_pass http://localhost:8090;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
           proxy_set_header X-Forwarded-Proto $scheme;
       }
   }
   ```

3. Enable the site and add a certificate:

   ```bash
   sudo ln -s /etc/nginx/sites-available/bds-api.metaversedu.in /etc/nginx/sites-enabled/
   sudo nginx -t && sudo systemctl reload nginx
   sudo certbot --nginx -d bds-api.metaversedu.in
   ```

4. The API is then at `https://bds-api.metaversedu.in`, with Swagger at `/swagger-ui.html`.

Ports 80 and 443 are already open in `ufw`, so 8090 doesn't need to be. Opening 8090 directly (`sudo ufw allow 8090/tcp`) would also need the port mapping in the `Jenkinsfile` changed from `127.0.0.1:8090` to `8090`, and it serves plain HTTP with no TLS, so it isn't recommended.

## 9. Before real users: email and OTPs

`BDS_EXPOSE_OTP=true` returns OTPs in API responses so you can log in without email. That makes the OTP check meaningless, so switch it off before going public:

1. Add or update in `/etc/bds/bds-backend.env`:

   ```
   BDS_MAIL_ENABLED=true
   MAIL_HOST=smtp.yourprovider.com
   MAIL_PORT=587
   MAIL_USERNAME=...
   MAIL_PASSWORD=...
   BDS_EXPOSE_OTP=false
   BDS_ALLOWED_ORIGINS=https://your-frontend-domain
   ```

2. Regenerate the container file: `sudo bash /tmp/bds-setup/bds-backend/deploy/setup-server.sh`
3. Click **Build Now** on `bds-backend`.

Use the same two steps whenever you change any setting.

## 10. Day-to-day operations

| Task | How |
|---|---|
| Deploy new code | `git push` to `main`; Jenkins builds within 5 minutes, or click **Build Now** |
| Follow the logs | `docker logs -f bds-backend` |
| Restart | `docker restart bds-backend` |
| See image versions | `docker images bds-backend` |
| Roll back | **Replay** or rebuild the earlier Jenkins build, or `docker run` the older tag with the same options as `runContainer` in the `Jenkinsfile` |
| Database shell | `docker exec -it bds-oracle sqlplus bds@FREEPDB1` (password: `DB_PASSWORD` in `/etc/bds/bds-backend.env`) |
| Back up the database | `docker stop bds-oracle && docker run --rm -v bds-oracle-data:/d -v $PWD:/b alpine tar czf /b/oracle-$(date +%F).tgz -C /d . && docker start bds-oracle` |
| Back up uploads | `docker run --rm -v bds-backend-data:/d -v $PWD:/b alpine tar czf /b/uploads-$(date +%F).tgz -C /d .` |
| Back up secrets | copy `/etc/bds/bds-backend.env` somewhere safe and offline |

## 11. Troubleshooting

| Symptom | Check / fix |
|---|---|
| Health check times out | `docker logs --tail 100 bds-backend`; usually the database isn't ready: `docker logs bds-oracle \| tail` |
| `UnknownHostException: oracle21c` (shared Oracle) | the Oracle container was recreated and left `bds-net`: `docker network connect bds-net oracle21c` |
| `ORA-12514` / connection refused | `docker network connect bds-net bds-oracle`; make sure `DB_URL` is `jdbc:oracle:thin:@//bds-oracle:1521/FREEPDB1` |
| `permission denied ... docker.sock` in Jenkins | add `jenkins` to the `docker` group and restart Jenkins ([step 1](#1-prerequisites)) |
| `Cannot read /etc/bds/bds-backend.container.env` | re-run `setup-server.sh` (see [step 4](#4-jenkins-access-to-the-settings)) |
| `port is already allocated` on 8090 | `sudo ss -ltnp 'sport = :8090'` to see what holds it |
| Container restarts or is killed | it's capped at 1 GB (`--memory 1g`); check `free -m` and `docker stats` |
| `apt-get update` fails on the Jenkins repo | the Jenkins apt signing key is missing; refresh it from <https://pkg.jenkins.io/debian-stable/> (`setup-server.sh` works around this) |
| Encrypted data unreadable after a redeploy | `BDS_DATA_KEY` / `BDS_HASH_KEY` changed; restore the original values from your backup |

## 12. Frontend (bds-frontend)

The React app in `bds-frontend/` is built into its own image (`bds-frontend/Dockerfile`: Node build, then nginx). nginx serves the app and forwards `/api/...` to the `bds-backend` container over `bds-net`, so the browser only talks to one site and the backend port is never exposed.

```
  Browser ──▶ https://bds.metaversedu.in ──▶ host nginx 443 ──▶ 127.0.0.1:8091 (bds-frontend)
                                                                   ├─ /        → React app
                                                                   └─ /api/... → bds-backend:8080 (bds-net)
```

| File | Purpose |
|---|---|
| `bds-frontend/Dockerfile` | `npm ci` + `vite build`, then an nginx image that serves the app |
| `bds-frontend/nginx/default.conf.template` | nginx config: SPA fallback, asset caching, `/healthz`, `/api` proxy to `bds-backend:8080` |
| `bds-frontend/Jenkinsfile` | Jenkins job `bds-frontend`: build, deploy on `127.0.0.1:8091`, health check, rollback |

Prerequisite: the backend (steps 1–7) is running, so `bds-net` and `bds-backend` exist.

### 12.1 Get the latest code and allow the frontend origin in the backend

The backend checks the browser's `Origin` header; without this, every API call from the site fails with **"Invalid CORS request"**.

```bash
rm -rf /tmp/bds-setup && git clone https://github.com/Saic98779/bds-marketplace.git /tmp/bds-setup
sudo sed -i 's#^BDS_ALLOWED_ORIGINS=.*#BDS_ALLOWED_ORIGINS=https://bds.metaversedu.in,http://localhost:5173#' /etc/bds/bds-backend.env
sudo bash /tmp/bds-setup/bds-backend/deploy/setup-server.sh     # regenerates /etc/bds/bds-backend.container.env
```

The script keeps the existing secrets and remembers a shared Oracle (`EXISTING_ORACLE`) from the first run. Then click **Build Now** on the `bds-backend` job so the container picks up the new setting (about a minute of backend downtime).

Check:

```bash
sudo grep BDS_ALLOWED_ORIGINS /etc/bds/bds-backend.container.env   # includes https://bds.metaversedu.in
```

### 12.2 Check that port 8091 is free

```bash
sudo ss -ltnp 'sport = :8091'      # should print only the header line
```

If something already uses it, pick another port and change `HOST_PORT` and `HEALTH_URL` in `bds-frontend/Jenkinsfile` and `proxy_pass` in 12.4.

### 12.3 Create the Jenkins job and deploy

1. Jenkins → **New Item** → name `bds-frontend` → **Pipeline** → **OK**.
2. Under **Pipeline**:
   - **Definition:** Pipeline script from SCM
   - **SCM:** Git
   - **Repository URL:** `https://github.com/Saic98779/bds-marketplace.git`
   - **Branch Specifier:** `*/main`
   - **Script Path:** `bds-frontend/Jenkinsfile` (not the root `Jenkinsfile`, which is the backend)
3. **Save** → **Build Now**. The first build takes 2–3 minutes (npm install); later builds use the Docker cache.

| Stage | What happens |
|---|---|
| Checkout | tags the image `<build number>-<commit>`, e.g. `bds-frontend:1-41cee78` |
| Build Image | `docker build bds-frontend`: `npm ci` + `vite build`, copied into nginx |
| Deploy | replaces the `bds-frontend` container (`127.0.0.1:8091` → `80`, on `bds-net`, 128 MB) |
| Health Check | `/healthz` and the app's `index.html`; also tries `/api/v1/public/settings` and only **warns** if the backend is down |
| On failure | restarts the previous image (automatic rollback) |

Check on the server:

```bash
docker ps --filter name=bds-frontend                                  # Up
curl http://127.0.0.1:8091/healthz                                    # ok
curl -s http://127.0.0.1:8091/api/v1/public/settings | head -c 100    # JSON from the backend
```

### 12.4 Make it public with HTTPS

`bds.metaversedu.in` already points to 51.79.165.43 (until now nginx only served its default page there). Create `/etc/nginx/sites-available/bds.metaversedu.in`:

```nginx
server {
    listen 80;
    server_name bds.metaversedu.in;
    client_max_body_size 30M;          # matches the app's upload limit

    location / {
        proxy_pass http://127.0.0.1:8091;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable it and add a certificate:

```bash
sudo ln -s /etc/nginx/sites-available/bds.metaversedu.in /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d bds.metaversedu.in
```

### 12.5 Verify

1. Open <https://bds.metaversedu.in>: the home page shows categories and services from the server.
2. Sign in as `admin@bdsmarketplace.in` (password: `BDS_ADMIN_PASSWORD` in `/etc/bds/bds-backend.env`); while `BDS_EXPOSE_OTP=true` the OTP is shown on screen.
3. Refresh a deep link such as `/admin/dashboard`: it should reload the same page, not a 404.
4. For a full functional test follow [`bds-frontend/TESTING.md`](bds-frontend/TESTING.md).

From now on every push to `main` redeploys the frontend within 5 minutes. Both jobs poll the same repository, so a push also rebuilds the backend (and vice versa), even when only the other part changed.

### 12.6 Operations and troubleshooting

| Task / symptom | How |
|---|---|
| Deploy new frontend code | `git push` to `main`; the `bds-frontend` job builds within 5 minutes |
| Logs | `docker logs -f bds-frontend` |
| Pages load but every API call fails (502) | backend down or not on the network: `docker ps --filter name=bds-backend`, `docker network inspect bds-net` |
| API calls return 403 "Invalid CORS request" | `BDS_ALLOWED_ORIGINS` doesn't include `https://bds.metaversedu.in` ([12.1](#121-get-the-latest-code-and-allow-the-frontend-origin-in-the-backend)) |
| Refreshing a page like `/buyer/rfqs` gives 404 | the host nginx site isn't proxying to 8091; check `/etc/nginx/sites-enabled/bds.metaversedu.in` |
| Upload fails with 413 | raise `client_max_body_size` in the host nginx site |
| `port is already allocated` on 8091 | see [12.2](#122-check-that-port-8091-is-free) |
| Jenkins build fails at `npm ci` | `package-lock.json` is out of sync: run `npm install` in `bds-frontend` locally and commit the lock file |
| Roll back | **Replay** an earlier `bds-frontend` build, or `docker run` an older `bds-frontend:<tag>` with the options in `runContainer` in `bds-frontend/Jenkinsfile` |
| Local development | `cd bds-frontend && npm run dev` (proxies to the production API; `VITE_GATEWAY_URL=http://localhost:8080 npm run dev` for a local backend) |
