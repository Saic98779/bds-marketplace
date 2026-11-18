// Builds the bds-backend Docker image and (re)deploys the container on the same VM Jenkins runs on.
// One-time prerequisites on the server (see DEPLOYMENT.md):
//   - sudo bash deploy/setup-server.sh   (Oracle container on the bds-net network, generated secrets)
//   - /etc/bds/bds-backend.container.env readable by the jenkins user (DB_URL, DB_PASSWORD, BDS_JWT_SECRET, ...)

def runContainer(String image) {
    return """
        docker rm -f ${env.CONTAINER_NAME} 2>/dev/null || true
        docker run -d \\
          --name ${env.CONTAINER_NAME} \\
          --restart unless-stopped \\
          --network ${env.DOCKER_NETWORK} \\
          --env-file "\$ENV_FILE" \\
          -e PORT=${env.APP_PORT} \\
          -v ${env.DATA_VOLUME}:/app/data \\
          -p 127.0.0.1:${env.HOST_PORT}:${env.APP_PORT} \\
          --memory 1g \\
          --log-opt max-size=10m --log-opt max-file=3 \\
          ${image}
    """
}

pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        IMAGE_NAME     = 'bds-backend'
        CONTAINER_NAME = 'bds-backend'
        DB_CONTAINER   = 'bds-oracle'
        DOCKER_NETWORK = 'bds-net'
        DATA_VOLUME    = 'bds-backend-data'
        HOST_PORT      = '8090'   // Jenkins uses 8080 and other apps 8081-8084 on this VM
        APP_PORT       = '8080'
        HEALTH_URL     = 'http://localhost:8090/actuator/health'
        ENV_FILE       = '/etc/bds/bds-backend.container.env'   // written by deploy/setup-server.sh
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.BUILD_NUMBER}-${env.GIT_SHORT}"
                }
            }
        }

        stage('Build & Test Image') {
            steps {
                sh 'docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest .'
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                    [ -r "$ENV_FILE" ] || { echo "Cannot read $ENV_FILE; re-run deploy/setup-server.sh"; exit 1; }

                    # The backend reaches Oracle by container name on a private network
                    docker network inspect ${DOCKER_NETWORK} >/dev/null 2>&1 || docker network create ${DOCKER_NETWORK}
                    docker network connect ${DOCKER_NETWORK} ${DB_CONTAINER} 2>/dev/null || true

                    # Remember the image currently running so we can roll back
                    PREVIOUS_IMAGE=$(docker inspect --format '{{.Config.Image}}' ${CONTAINER_NAME} 2>/dev/null || true)
                    echo "${PREVIOUS_IMAGE}" > previous_image.txt
                '''
                sh runContainer("${env.IMAGE_NAME}:${env.IMAGE_TAG}")
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                    for i in $(seq 1 30); do
                      if curl -fs ${HEALTH_URL} | grep -q '"UP"'; then
                        echo "Application is healthy"
                        exit 0
                      fi
                      echo "Waiting for application... ($i/30)"
                      sleep 5
                    done
                    echo "Health check failed. Last logs:"
                    docker logs --tail 100 ${CONTAINER_NAME}
                    exit 1
                '''
            }
        }
    }

    post {
        failure {
            script {
                def previous = fileExists('previous_image.txt') ? readFile('previous_image.txt').trim() : ''
                if (previous && previous != "${env.IMAGE_NAME}:${env.IMAGE_TAG}") {
                    echo "Rolling back to ${previous}"
                    sh runContainer(previous)
                }
            }
        }
        always {
            // Remove dangling layers; keep tagged images for rollback
            sh 'docker image prune -f || true'
        }
    }
}
