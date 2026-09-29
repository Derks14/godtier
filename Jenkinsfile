pipeline {
    agent any

        tools {
            jdk 'openjdk25'
        }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        DEPLOY_DIR = '/srv/godtier'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Remove -DskipTests to run the test suite as part of the pipeline.
                // -q suppresses Maven's build log noise, only errors/test output surface.
                sh './mvnw -q clean package -DskipTests'
            }
        }

        stage('Deploy jar') {
            steps {
                sh '''
                    mkdir -p ${DEPLOY_DIR}/target
                    cp target/godtier-*.jar ${DEPLOY_DIR}/target/godtier.jar
                '''
            }
        }

        stage('Restart container') {
            steps {
                // .env with MONGODB_URI / REDIS_PASSWORD / CORS_ALLOWED_ORIGINS
                // lives in /srv/godtier, not this workspace, so it's pointed to explicitly.
                sh 'docker compose --env-file ${DEPLOY_DIR}/.env --profile prod up -d --force-recreate'
            }
        }
    }

    post {
        success {
            echo 'godtier deployed successfully. Container logs:'
            sh 'sleep 5; docker logs --tail 200 godtier'
        }
        failure {
            echo 'Build or deploy failed - godtier was not redeployed.'
        }
    }
}
