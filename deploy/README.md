# deploy/

Server-side files for running bds-backend with Docker and Jenkins. See [../DEPLOYMENT.md](../DEPLOYMENT.md) for the complete steps.

| File | Purpose |
|---|---|
| `setup-server.sh` | One-time, re-runnable server setup: Docker, the `bds-net` network, the Oracle Free container and generated secrets |
| `bds-backend.env.example` | Variables the container expects (Jenkins secret file credential `bds-env-file`) |
