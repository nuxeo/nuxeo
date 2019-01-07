pipeline {
  agent {
    label "builder-maven-nuxeo"
  }
  environment {
    ORG = 'nuxeo-sandbox'
    APP_NAME = 'nuxeo'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
  }
  stages {
    stage('Checkout nuxeo and build core ') {
      when {
        branch 'master'
      }
      steps {
        container('maven-nuxeo') {
          sh "git checkout master"
          sh "git config --global credential.helper store"
          sh "jx step git credentials"
          dir('nuxeo-runtime/nuxeo-runtime-mongodb') {
           sh " mvn clean package"
          }
          dir('nuxeo-core') {
            script {
              try {
                sh " mvn clean package"
              }
              catch(err) {}
              finally {
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'])
              }
            }
          }
        }
      }
    }

    stage('CI Build junits in feature branch against Mongo') {
      environment {
        APP_NAME = 'mongodb'
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/junits') {
            sh "make helm"
            sh "jx preview --app $APP_NAME --namespace=${BRANCH_NAME} --dir ../.."
          }
          sh "touch /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.core=mongodb > /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.mongodb.server=mongodb://preview-${APP_NAME}.${BRANCH_NAME}.svc.cluster.local >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.mongodb.dbname=vcstest >> /root/nuxeo-test-vcs.properties"  
          dir('nuxeo-core') {
            script {
              try {
                sh "mvn clean package -Pcustomdb,mongodb"
              }
              catch(err) {}
              finally {
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'])
              }
             }
            }
          sh "kubectl delete namespace ${BRANCH_NAME}"
        }
      }  
    }
    stage('CI Build junits in feature branch against Postgres') {
      environment {
        APP_NAME = "postgresql"
        PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
      }
      steps {
        container('maven-nuxeo') {
         dir('charts/junits') {
            sh "make helm"
            sh "jx preview --app $APP_NAME --namespace=${BRANCH_NAME} --dir ../.."
          }
          sh "touch /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.db=PostgreSQL > /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.server=preview-${APP_NAME}.${BRANCH_NAME}.svc.cluster.local >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.database=vctests >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.user=nuxeo >> /root/nuxeo-test-vcs.properties"
          sh "echo nuxeo.test.vcs.password=nuxeo >> /root/nuxeo-test-vcs.properties"  
          dir('nuxeo-core') {
            script {
              try {
                sh "mvn clean package -Pcustomdb,pgsql"
              }
              catch(err) {}
              finally {
                step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'])
              }
            }
          }
        }
      }  
    }
  }
  post {
        always {
          sh "kubectl delete namespace ${BRANCH_NAME}"
          cleanWs()
        }
  }
}
