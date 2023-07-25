/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 */
library identifier: "platform-ci-shared-library@v0.0.25"

boolean isNuxeoTag() {
  return NUXEO_BRANCH =~ /^v.*$/
}

def checkParameters() {
  if (!ELASTICSEARCH_IMAGE_TAG =~ /^v\d+\.\d+\.\d+/) {
    error('ELASTICSEARCH_IMAGE_TAG parameter must be a semver version such as 7.17.9')
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-lts-2021'
  }
  options {
    timeout(time: 8, unit: 'HOURS')
  }
  environment {
    // force ${HOME}=/root - for an unexplained reason, ${HOME} is resolved as /home/jenkins though sh 'env' shows HOME=/root
    HOME = '/root'
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    NUXEO_BRANCH = "${params.NUXEO_BRANCH}"
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -P-nexus,nexus-private'
    TEST_NAMESPACE = "$CURRENT_NAMESPACE-nuxeo-unit-tests-$BUILD_NUMBER-mongodb-es".toLowerCase()
    TEST_SERVICE_DOMAIN_SUFFIX = 'svc.cluster.local'
    TEST_REDIS_K8S_OBJECT = 'redis-master'
    TEST_KAFKA_K8S_OBJECT = 'kafka'
    TEST_KAFKA_PORT = '9092'
    ELASTICSEARCH_IMAGE = 'elasticsearch'
    ELASTICSEARCH_IMAGE_TAG = "${params.ELASTICSEARCH_IMAGE_TAG}"
    ELASTICSEARCH_MAJOR_DOT_MINOR_VERSION = nxUtils.getMajorDotMinorVersion(version: ELASTICSEARCH_IMAGE_TAG)
    REPOSITORY_BACKEND = 'mongodb'
  }

  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabels(branch: NUXEO_BRANCH)
          }
        }
      }
    }

    stage('Initialization') {
      steps {
        script {
          checkParameters()
          currentBuild.description = "${NUXEO_BRANCH}/${ELASTICSEARCH_MAJOR_DOT_MINOR_VERSION}"
        }
      }
    }

    stage('Compile') {
      when {
        expression { !isNuxeoTag() }
      }
      steps {
        container('maven') {
          script {
            echo "${REPOSITORY_BACKEND} unit tests: install required SNAPSHOT artifacts"
            sh "mvn ${MAVEN_ARGS} -DskipTests install"
          }
        }
      }
    }

    stage('Run mongodb unit tests') {
      steps {
        script {
          def redisHost = "${TEST_REDIS_K8S_OBJECT}.${TEST_NAMESPACE}.${TEST_SERVICE_DOMAIN_SUFFIX}"
          def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${TEST_NAMESPACE}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"

          def commitSha = env.SCM_REF
          if (isNuxeoTag()) {
            // retrieve the promoted build sha
            commitSha = nxDocker.getLabel(image: "${PRIVATE_DOCKER_REGISTRY}/nuxeo/nuxeo:${NUXEO_BRANCH}", label: 'org.nuxeo.scm-ref')
          }
          container("maven-${REPOSITORY_BACKEND}") {
            nxWithGitHubStatus(context: "utests/es-${ELASTICSEARCH_MAJOR_DOT_MINOR_VERSION}", message: "Unit tests - ES ${ELASTICSEARCH_IMAGE_TAG} environment", commitSha: commitSha) {
              echo """
              ----------------------------------------
              Run ${REPOSITORY_BACKEND} unit tests against Elasticsearch ${ELASTICSEARCH_IMAGE_TAG}
              ----------------------------------------"""
              echo "${REPOSITORY_BACKEND} unit tests: install external services"
              nxWithHelmfileDeployment(namespace: "${TEST_NAMESPACE}", environment: "mongodbUnitTests") {
                try {
                  echo "${REPOSITORY_BACKEND} unit tests: prepare env variables for Maven tests"
                  sh """
                    cat ci/mvn/nuxeo-test-${REPOSITORY_BACKEND}.properties \
                      ci/mvn/nuxeo-test-elasticsearch.properties \
                      > ci/mvn/nuxeo-test-${REPOSITORY_BACKEND}.properties~gen

                    DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
                    envsubst < ci/mvn/nuxeo-test-${REPOSITORY_BACKEND}.properties~gen > ${HOME}/nuxeo-test-${REPOSITORY_BACKEND}.properties
                  """
                  // run unit tests:
                  // - in modules/core and dependent projects only
                  // - for the repository backend (see the customEnvironment profile in pom.xml):
                  //   - in an alternative build directory
                  //   - loading some test framework system properties
                  def mvnCommand = """
                    mvn ${MAVEN_ARGS} -Prelease \
                      -rf :nuxeo-core-parent \
                      -Dcustom.environment=${REPOSITORY_BACKEND} \
                      -Dcustom.environment.log.dir=target-${REPOSITORY_BACKEND} \
                      -Dnuxeo.test.core=${REPOSITORY_BACKEND} \
                      -Dnuxeo.test.redis.host=${redisHost} \
                      -Pkafka -Dkafka.bootstrap.servers=${kafkaHost} \
                      test
                  """
                  retry(2) {
                    echo "${REPOSITORY_BACKEND} unit tests: run Maven tests"
                    sh "${mvnCommand}"
                  }
                } finally {
                  junit allowEmptyResults: true, testResults: "**/target-${REPOSITORY_BACKEND}/surefire-reports/*.xml"
                }
              }
            }
          }
        }
      }
    }
  }

  post {
    unsuccessful {
      script {
        if (isNuxeoTag()) {
          nxSlack.error(message: "Failed to test nuxeo/nuxeo-lts/${NUXEO_BRANCH} against Elasticsearch ${ELASTICSEARCH_IMAGE_TAG} #${BUILD_NUMBER}: ${BUILD_URL}")
        }
      }
    }
  }
}
