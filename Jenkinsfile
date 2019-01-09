pipeline {
  agent {
    label "builder-maven-nuxeo"
  }
  environment {
    ORG = 'nuxeo-sandbox'
    APP_NAME = 'nuxeo'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    NAMESPACE = "$ORG-$BRANCH_NAME-$BUILD_NUMBER"
  }
  stages {
    stage('Core junits on H2') {
      when {
        branch 'master'
      }
      steps {
        container('maven-nuxeo') {
          sh "git checkout master"
          sh "git config --global credential.helper store"
          sh "jx step git credentials"
          dir('nuxeo-core') {
            script {
              try {
                 sh "mvn clean package -fae -Dmaven.test.failure.ignore=true"
              }
              catch(err) {
                if (currentBuild.result == 'FAILURE'){
                  throw err
                }
              }
              finally {
                junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
              }
            }
          }
        }
      }
    }

    stage('Core junits on Mongo') {
      environment {
        APP_NAME = 'mongodb'
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/junits') {
            sh "make mongodb"
            sh "make helm"
            sh "jx preview --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
          sh "touch /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.core=mongodb > /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.mongodb.server=mongodb://preview-${APP_NAME}.${NAMESPACE}.svc.cluster.local >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.mongodb.dbname=vcstest >> /root/nuxeo-test-vcs.properties"  
          dir('nuxeo-core') {
            script {
              try {
                sh "mvn clean package -fae -Pcustomdb,mongodb  -Dmaven.test.failure.ignore=true"
              }
              catch(err) {
                if (currentBuild.result == 'FAILURE'){
                  throw err
                }
              }
              finally {
                junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
              }
             }
            }
        sh "kubectl delete namespace ${NAMESPACE}"    
        }
      }  
    }
    stage('Core junits on Postgres') {
      environment {
        APP_NAME = "postgresql"
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
      }
      steps {
        container('maven-nuxeo') {
         dir('charts/junits') {
            sh "make postgresql"
            sh "make helm"
            sh "jx preview --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
          sh "touch /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.db=PostgreSQL > /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.server=preview-${APP_NAME}.${NAMESPACE}.svc.cluster.local >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.database=vctests >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.user=nuxeo >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.password=nuxeo >> /root/nuxeo-test-vcs.properties"  
          dir('nuxeo-core') {
            script {
              try {
                sh "mvn clean package -fae -Pcustomdb,pgsql  -Dmaven.test.failure.ignore=true"
              }
              catch(err) {
                if (currentBuild.result == 'FAILURE'){
                  throw err
                }
              }
              finally {
                junit testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
              }
            }
          }
        sh "kubectl delete namespace ${NAMESPACE}" 
        }
      }  
    }
  }
  post {
        always {
          cleanWs()
        }
  }
}
