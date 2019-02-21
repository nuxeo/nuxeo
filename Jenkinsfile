def targetTestEnvironments = [] as Set
def targetPreviewEnvironments = [] as Set
def mvnOpts = ""

def buildStepsForParallelJUnit(envs) {
  def stages = [:]
  for (env in envs) {
    stages["JUnit - ${env}"] = {
      stage("JUnit - ${env}") {
        echo "Execute junit for ${env}"
        // sh "njx nuxeo preset --name ${env} --namespace ${NAMESPACE} install"
      }
    }
  }
  return stages;
}

def buildStepsForParallelPreviews(envs) {
  def stages = [:]
  for (env in envs) {
    stages["Preview - ${env}"] = {
      stage("Preview - ${env}") {
        echo "Deploy preview for ${env}"
        sh "njx nuxeo preset --name ${env} --namespace ${NAMESPACE} preview --app ${APP_NAME}"
      }
    }
  }
  return stages;
}

pipeline {
  agent {
    label "builder-maven-nuxeo"
  }
  environment {
    ORG = 'nuxeo-sandbox'
    APP_NAME = 'nuxeo'
    CHARTMUSEUM_CREDS = credentials('jenkins-x-chartmuseum')
    NAMESPACE = "$ORG-$BRANCH_NAME-$BUILD_NUMBER"
    PREVIEW_VERSION = "0.0.0-SNAPSHOT-$BRANCH_NAME-$BUILD_NUMBER"
  }
  stages {
    stage('Fetch PR Labels') {
      when {
        branch 'PR-*'
      }
      steps {
        container('maven-nuxeo') {
          script {
            if (!(pullRequest.labels as List).isEmpty()) {
              String labels = pullRequest.labels.join(" ").trim();
              String labelsTest = sh(returnStdout: true, script: "njx pr filter-labels -m test -l ${labels}").trim();
              if (labelsTest) {
                targetTestEnvironments.addAll(labelsTest.split(","));
              }
              String labelsPreview = sh(returnStdout: true, script: "njx pr filter-labels -m preview -l ${labels}").trim();
              if (labelsPreview) {
                targetPreviewEnvironments.addAll(labelsPreview.split(","));
              }
            }
          }
        }
      }
    }
    stage('Summary') {
      when {
        branch 'PR-*'
      }
      steps {
        script {
          println("Test environments: ${(targetTestEnvironments as List).join(' ')}")
          println("Preview environments: ${(targetPreviewEnvironments as List).join(' ')}")
          println("Maven Args: ${mvnOpts}")
        }
      }
    }
    stage('Prepare tests') {
      when {
        branch 'PR-*'
      }
      steps {
        container('maven-nuxeo') {
          // Prepare vcs file for test
          sh "echo '# Custom VCS file for tests' > /root/nuxeo-test-vcs.properties"
        }
      }
    }
    stage('Core junits on H2') {
      when {
        branch 'master'
      }
      environment {
        APP_NAME = 'redis'
      }
      steps {
        container('maven-nuxeo') {
          dir('charts/junits') {
            sh "make redis"
            sh "make helm"
            sh "jx preview --app $APP_NAME --namespace=${NAMESPACE} --dir ../.."
          }
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
    stage('CI Build') {
      steps {
        container('maven-nuxeo') {
          script {
            def stages = buildStepsForParallelJUnit(targetTestEnvironments)
            parallel stages
          }
        }
      }
    }
    stage('Deploy Previews') {
      steps {
        container('maven-nuxeo') {
          script {
            def stages = buildStepsForParallelPreviews(targetPreviewEnvironments)
            parallel stages
          }
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
