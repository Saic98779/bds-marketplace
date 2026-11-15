// Builds bds-backend and deploys it on this host (Jenkins runs on the deployment server).
// One-time prerequisite on the server: sudo bash deploy/setup-server.sh
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    environment {
        JAVA_HOME = '/usr/lib/jvm/java-21-openjdk-amd64'
        PATH = "${JAVA_HOME}/bin:${PATH}"
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    stages {
        stage('Build & test') {
            steps {
                sh 'mvn -B -ntp -pl bds-backend -am clean package'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'bds-backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'sudo /opt/bds/bin/deploy.sh "$WORKSPACE/bds-backend/target/bds-backend-1.0.0.jar"'
            }
        }
    }

    post {
        success {
            archiveArtifacts artifacts: 'bds-backend/target/bds-backend-*.jar', fingerprint: true
        }
    }
}
