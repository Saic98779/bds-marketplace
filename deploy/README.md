# Deploying bds-backend

The backend runs as a Docker container on the Jenkins VM, next to an Oracle Database Free container.

| Piece | Where |
|---|---|
| Image build | `Dockerfile` (Maven build + tests, then a JRE 21 runtime image) |
| Pipeline | `Jenkinsfile`, Jenkins job `bds-backend` (polls `main` every 5 minutes) |
| App container | `bds-backend`, `127.0.0.1:8090 -> 8080`, uploads in volume `bds-backend-data` |
| Database | container `bds-oracle` (Oracle 23ai Free, PDB `FREEPDB1`), data in volume `bds-oracle-data` |
| Network | both containers on `bds-net`; the app connects to `bds-oracle:1521` |

## One-time setup

1. On the server: `sudo bash deploy/setup-server.sh`
   It creates `/etc/bds/bds-backend.env` (all generated secrets, root only) and
   `/etc/bds/bds-backend.container.env` (what the container needs).
2. In Jenkins: *Manage Jenkins -> Credentials -> System -> Global -> Add credentials*,
   kind **Secret file**, ID `bds-env-file`, file `/etc/bds/bds-backend.container.env`.
   See `bds-backend.env.example` for the variables.
3. Create a *Pipeline* job `bds-backend` using *Pipeline script from SCM* on this repository, branch `main`.

Every build tags the image `<build>-<commit>`; if the health check
(`/actuator/health`) fails, the pipeline restarts the previously running image.

## Useful commands

```bash
docker logs -f bds-backend                       # application log
docker exec -it bds-oracle sqlplus bds@FREEPDB1  # database shell
sudo grep BDS_ADMIN_PASSWORD /etc/bds/bds-backend.env
```
