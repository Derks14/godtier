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
                sh './mvnw clean package -DskipTests'
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
                // must already exist in this workspace directory.
                sh 'docker compose --profile prod up -d --force-recreate'
            }
        }
    }

    post {
        success {
            echo 'godtier deployed successfully.'
        }
        failure {
            echo 'Build or deploy failed - godtier was not redeployed.'
        }
    }
}
