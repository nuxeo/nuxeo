/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 *     Thomas Roger <troger@nuxeo.com>
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

def abortRunningBuilds() {
  // see https://issues.jenkins.io/browse/JENKINS-43353
  def buildNumber = BUILD_NUMBER as int
  if (buildNumber > 1) {
    milestone(buildNumber - 1)
  }
  milestone(buildNumber)
}
abortRunningBuilds()

repositoryUrl = 'https://github.com/nuxeo/nuxeo'

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: repositoryUrl],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']]
])

def getCurrentNamespace() {
  container('maven') {
    return sh(returnStdout: true, script: "kubectl get pod ${NODE_NAME} -ojsonpath='{..namespace}'")
  }
}

def setGitHubBuildStatus(String context, String message, String state) {
  if (env.DRY_RUN != "true") {
    step([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repositoryUrl],
      contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
      statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
    ])
  }
}

// name function differently from step to avoid 'Excessively nested closures/functions' error
def doArchiveArtifacts(artifacts, excludes = '') {
  try {
    archiveArtifacts allowEmptyArchive: true, artifacts: artifacts, excludes: excludes
  } catch (err) {
    echo hudson.Functions.printThrowable(err)
  }
}

void buildWithRedis(stage, body) {
  setGitHubBuildStatus("${stage}/build", "Build ${stage}", 'PENDING')

  echo "Create unit test namespace"
  sh "kubectl create namespace ${TEST_NAMESPACE}"

  try {
    echo """
    ----------------------------------------
    Build ${stage}
    ----------------------------------------"""

    echo "Install Redis for unit tests"
    helmfileSync("${TEST_NAMESPACE}")

    body.call()

    setGitHubBuildStatus("${stage}/build", "Build ${stage}", 'SUCCESS')
  } catch(err) {
    setGitHubBuildStatus("${stage}/build", "Build ${stage}", 'FAILURE')
    throw err
  } finally {
    try {
      junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
      doArchiveArtifacts('**/target/*.log')
    } finally {
      echo 'Clean up unit test namespace'
      try {
        helmfileDestroy("${TEST_NAMESPACE}")
      } finally {
        sh "kubectl delete namespace ${TEST_NAMESPACE} --ignore-not-found=true"
      }
    }
  }
}

void helmfileSync(namespace) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} deps
      ${HELMFILE_COMMAND} sync
    """
  }
}

void helmfileDestroy(namespace) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} destroy
    """
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-10'
  }
  options {
    timeout(time: 6, unit: 'HOURS')
  }
  environment {
    CURRENT_NAMESPACE = getCurrentNamespace()
    HELMFILE_COMMAND = "helmfile --file ci/helm/helmfile.yaml --helm-binary /usr/bin/helm3"
    KILL_TOMCAT = "true"
    MAVEN_OPTS = "$MAVEN_OPTS -Xms1024m -Xmx4096m -XX:MaxPermSize=2048m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    MAVEN_ARGS = '-B -V -nsu -P-nexus,nexus-private -fae -Dnuxeo.tests.random.mode=bypass'
    TEST_NAMESPACE = "$CURRENT_NAMESPACE-nuxeo-unit-tests-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
    TEST_REDIS_HOST = "redis-master.${TEST_NAMESPACE}.svc.cluster.local"
  }
  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes resource labels
          ----------------------------------------
          """
          echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}
          """
        }
      }
    }

    stage('Initialization') {
      steps {
        echo 'Clone addons'
        sh "./clone.py ${CHANGE_BRANCH} -f ${CHANGE_TARGET}"

        echo 'Work around bower root issue'
        sh "echo '{ \"allow_root\": true }' > /home/jenkins/.bowerrc"
      }
    }

    stage('Build platform') {
      steps {
        container('maven') {
          buildWithRedis('platform') {
            echo "Build platform with MAVEN_OPTS=$MAVEN_OPTS"
            sh """
              mvn ${MAVEN_ARGS} \
                -Dnuxeo.test.redis.host=${TEST_REDIS_HOST} \
                install
            """
          }
        }
      }
    }

    stage('Build addons') {
      steps {
        container('maven') {
          buildWithRedis('addons') {
            sh """
              mvn ${MAVEN_ARGS} \
                -Paddons \
                -pl addons,addons-core \
                -amd \
                -Dnuxeo.test.redis.host=${TEST_REDIS_HOST} \
                install
            """
          }
        }
      }
    }

    stage('Build distribution') {
      steps {
        setGitHubBuildStatus('distribution/build', 'Build distribution', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Build distribution
          ----------------------------------------"""
          sh """
            mvn ${MAVEN_ARGS} \
              -Pdistrib \
              -pl nuxeo-distribution \
              -amd \
              install
          """
          findText regexp: ".*ERROR.*", fileSet: 'nuxeo-distribution/**/log/server.log'
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '''
            **/target/failsafe-reports/*.xml,
            **/target/surefire-reports/*.xml
          '''
          // avoid unexplained
          // java.nio.file.AccessDeniedException: /home/jenkins/workspace/.../nuxeo-distribution/nuxeo-war-tests/target/tomcat/logs/manager.2021-09-10.log
          // by excluding nuxeo-war-tests tomcat log files
          doArchiveArtifacts('''
            **/target/failsafe-reports/*,
            **/target/*.png,
            **/target/screenshot*.html,
            **/target/*.json,
            **/target/results/result-*.html,
            **/*.log,
            **/nxserver/config/distribution.properties
          ''', 'nuxeo-distribution/nuxeo-war-tests/target/tomcat/logs/*.log')
        }
        success {
          setGitHubBuildStatus('distribution/build', 'Build distribution', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('distribution/build', 'Build distribution', 'FAILURE')
          // findText does mark the build in FAILURE but doesn't fail the stage nor stop the pipeline
          error "Errors were found!"
        }
      }
    }

    stage('Build addons ftests') {
      steps {
        setGitHubBuildStatus('addons/ftests', 'Addons ftests', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run addons ftests
          ----------------------------------------"""
          sh """
            mvn ${MAVEN_ARGS} \
              -Paddons,itest \
              -pl addons/nuxeo-platform-error-web,addons/nuxeo-platform-forms-layout-demo \
              -amd \
              install
          """
        }
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '''
            **/target/failsafe-reports/*.xml,
            **/target/surefire-reports/*.xml
          '''
        }
        success {
          setGitHubBuildStatus('addons/ftests', 'Addons ftests', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('addons/ftests', 'Addons ftests', 'FAILURE')
        }
      }
    }
  }
}
