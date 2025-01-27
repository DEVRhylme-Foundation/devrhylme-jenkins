pipeline {
    agent any

    environment {
        NODE_VERSION = '18'  // Change if needed
        FIREBASE_PROJECT = credentials('FIREBASE_PROJECT_ID')  // Firebase project ID from Jenkins credentials
        FIREBASE_TOKEN = credentials('FIREBASE_DEPLOY_TOKEN')  // Firebase CI token from Jenkins credentials
        REPO_URL = credentials('GIT_REPO_URL')  // GitHub repo URL stored in Jenkins credentials
        BRANCH = 'main'
    }

    stages {
        stage('Checkout Repository') {
            steps {
                git branch: "${BRANCH}", url: "${REPO_URL}"
            }
        }

        stage('Setup Node.js') {
            steps {
                script {
                    def nodeExists = sh(script: 'node -v', returnStatus: true) == 0
                    if (!nodeExists) {
                        sh "nvm install ${NODE_VERSION} && nvm use ${NODE_VERSION}"
                    }
                }
            }
        }

        stage('Install Dependencies') {
            steps {
                sh 'npm install'
                dir('functions') {
                    sh 'npm install'
                }
            }
        }

        stage('Lint & Test') {
            steps {
                sh 'npm run lint'
                sh 'npm run test -- --watch=false --browsers=ChromeHeadless'
                dir('functions') {
                    sh 'npm run lint'
                    sh 'npm test'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                sh 'npm run build -- --configuration=production'
            }
        }

        stage('Deploy to Firebase') {
            steps {
                sh 'firebase deploy --project ${FIREBASE_PROJECT} --only hosting --token "${FIREBASE_TOKEN}"'
                sh 'firebase deploy --project ${FIREBASE_PROJECT} --only functions --token "${FIREBASE_TOKEN}"'
            }
        }
    }

    post {
        success {
            echo '✅ Deployment successful!'
        }
        failure {
            echo '❌ Deployment failed. Check the logs!'
        }
    }
}
