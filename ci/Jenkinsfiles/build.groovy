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
library identifier: "platform-ci-shared-library@v0.0.25"

// name function differently from step to avoid 'Excessively nested closures/functions' error
def doArchiveArtifacts(artifacts, excludes = '') {
  try {
    archiveArtifacts allowEmptyArchive: true, artifacts: artifacts, excludes: excludes
  } catch (err) {
    echo hudson.Functions.printThrowable(err)
  }
}

void buildWithRedis(stage, body) {
  nxWithGitHubStatus(context: "${stage}/build", message: "Build ${stage}") {
    echo """
    ----------------------------------------
    Build ${stage}
    ----------------------------------------"""

    echo "Install Redis for unit tests"
    nxWithHelmfileDeployment(namespace: env.TEST_NAMESPACE) {
      try {
        body.call()
      } finally {
        junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        doArchiveArtifacts('**/target/*.log')
      }
    }
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-10'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: 'https://github.com/nuxeo/nuxeo')
    timeout(time: 6, unit: 'HOURS')
  }
  environment {
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
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
          script {
            nxK8s.setPodLabels()
          }
        }
      }
    }

    stage('Initialization') {
      steps {
        container('maven') {
          echo 'Clone addons'
          sh "./clone.py ${CHANGE_BRANCH} -f ${CHANGE_TARGET}"

          echo 'Work around bower root issue'
          sh "echo '{ \"allow_root\": true }' > /home/jenkins/.bowerrc"
        }
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
        container('maven') {
          nxWithGitHubStatus(context: 'distribution/build', message: 'Build distribution') {
            script {
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
              nxUtils.lookupText(regexp: ".*ERROR.*", fileSet: 'nuxeo-distribution/**/log/server.log')
            }
          }
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
      }
    }

    stage('Build addons ftests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'addons/ftests', message: 'Addons ftests') {
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
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '''
            **/target/failsafe-reports/*.xml,
            **/target/surefire-reports/*.xml
          '''
        }
      }
    }
  }
}
