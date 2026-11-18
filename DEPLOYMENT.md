# Deploying bds-backend

Complete steps to take a fresh Ubuntu server to a running BDS Marketplace backend, deployed by Jenkins as a Docker container next to an Oracle Database Free container.

> Once steps 1–6 are done on a server, day-to-day work starts at [step 7](#7-verify).

## Contents

0. [What you end up with](#0-what-you-end-up-with)
1. [Prerequisites](#1-prerequisites)
2. [Get the code onto the server](#2-get-the-code-onto-the-server)
3. [One-time server setup: Oracle and secrets](#3-one-time-server-setup-oracle-and-secrets)
4. [Jenkins credential](#4-jenkins-credential)
5. [Jenkins job](#5-jenkins-job)
6. [First deployment](#6-first-deployment)
7. [Verify](#7-verify)
8. [Make it public with nginx and HTTPS](#8-make-it-public-with-nginx-and-https)
9. [Before real users: email and OTPs](#9-before-real-users-email-and-otps)
10. [Day-to-day operations](#10-day-to-day-operations)
11. [Troubleshooting](#11-troubleshooting)

---

## 0. What you end up with

| Piece | Details |
|---|---|
| Code | <https://github.com/Saic98779/bds-marketplace> (`main` branch) |
| Image build | `Dockerfile`: Maven build and tests, then a non-root Java 21 runtime image |
| Pipeline | `Jenkinsfile`, Jenkins job `bds-backend` at <https://jenkins.metaversedu.in>, checks `main` every 5 minutes |
| Database | container `bds-oracle` (Oracle 23ai Free, database `FREEPDB1`, user `bds`), data in volume `bds-oracle-data`; host access on `127.0.0.1:1522` (1521 is often taken by another Oracle container) |
| Backend | container `bds-backend` on `127.0.0.1:8090` → `8080`, uploaded files in volume `bds-backend-data` |
| Network | both containers on the private Docker network `bds-net`; the app connects to `bds-oracle:1521` |
| Secrets | `/etc/bds/bds-backend.env` on the server (root only), plus the Jenkins credential `bds-env-file` |

```
                 ┌──────────────── 51.222.155.92 ────────────────┐
  git push ──▶   │ Jenkins (8080) ── docker build ──▶ bds-backend │
  (GitHub main)  │                                   127.0.0.1:8090│
                 │                     bds-net          │         │
                 │                        └──▶ bds-oracle:1521    │
  Internet ──▶   │ nginx 443 ──▶ localhost:8090   (step 8)        │
                 └────────────────────────────────────────────────┘
```

## 1. Prerequisites

- Ubuntu server with `sudo` access, at least **4 GB free RAM** (Oracle uses about 2 GB) and 10 GB free disk.
- Jenkins installed on the same server, with these plugins: **Pipeline**, **Git**, **Credentials**, **Plain Credentials**, **Timestamper**.
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
sudo bash /tmp/bds-setup/deploy/setup-server.sh
```

The script is safe to re-run. It:

- installs Docker if it's missing, and creates the `bds-net` network
- generates the secrets into `/etc/bds/bds-backend.env`: database password, JWT, data and hash keys, and the initial admin password
- writes the container's settings to `/etc/bds/bds-backend.container.env`
- starts `bds-oracle` and waits for `DATABASE IS READY TO USE`

> ⚠️ **Back up `/etc/bds/bds-backend.env` somewhere safe.** `BDS_DATA_KEY` and `BDS_HASH_KEY` encrypt fields in the database. If you lose or change them, that data can't be read again.

The variables are listed in [`deploy/bds-backend.env.example`](deploy/bds-backend.env.example).

## 4. Jenkins credential

1. Go to Jenkins → **Manage Jenkins** → **Credentials** → **System** → **Global credentials** → **Add Credentials**.
2. Fill in:
   - **Kind:** Secret file
   - **File:** upload `/etc/bds/bds-backend.container.env`
   - **ID:** `bds-env-file` (exactly this; the `Jenkinsfile` refers to it)
3. Click **Create**.

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

1. Create a DNS **A record**, e.g. `bds-api.metaversedu.in`, pointing to `51.222.155.92`.
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

2. Regenerate the container file: `sudo bash /tmp/bds-setup/deploy/setup-server.sh`
3. In Jenkins, open the credential `bds-env-file`, click **Update**, upload the new `/etc/bds/bds-backend.container.env`, and save.
4. Click **Build Now** on `bds-backend`.

Use the same three steps whenever you change any setting.

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
| `ORA-12514` / connection refused | `docker network connect bds-net bds-oracle`; make sure `DB_URL` is `jdbc:oracle:thin:@//bds-oracle:1521/FREEPDB1` |
| `permission denied ... docker.sock` in Jenkins | add `jenkins` to the `docker` group and restart Jenkins ([step 1](#1-prerequisites)) |
| `Could not find credentials entry with ID 'bds-env-file'` | redo [step 4](#4-jenkins-credential); the ID must match exactly |
| `port is already allocated` on 8090 | `sudo ss -ltnp 'sport = :8090'` to see what holds it |
| Container restarts or is killed | it's capped at 1 GB (`--memory 1g`); check `free -m` and `docker stats` |
| `apt-get update` fails on the Jenkins repo | the Jenkins apt signing key is missing; refresh it from <https://pkg.jenkins.io/debian-stable/> (`setup-server.sh` works around this) |
| Encrypted data unreadable after a redeploy | `BDS_DATA_KEY` / `BDS_HASH_KEY` changed; restore the original values from your backup |
