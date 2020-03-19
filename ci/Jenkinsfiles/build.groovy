/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */

dockerNamespace = 'nuxeo'
repositoryUrl = 'https://github.com/nuxeo/nuxeo'
testEnvironments= [
  'dev',
  'mongodb',
  'postgresql',
]

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: repositoryUrl],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repositoryUrl],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
  ])
}

String getVersion() {
  String nuxeoVersion = readMavenPom().getVersion()
  return BRANCH_NAME == 'master' ? nuxeoVersion : "${BRANCH_NAME}-" + nuxeoVersion
}

void runFunctionalTests(String baseDir) {
  try {
    sh "mvn ${MAVEN_ARGS} -f ${baseDir}/pom.xml verify"
  } finally {
    try {
      archiveArtifacts allowEmptyArchive: true, artifacts: "${baseDir}/**/target/failsafe-reports/*, ${baseDir}/**/target/**/*.log, ${baseDir}/**/target/*.png, ${baseDir}/**/target/**/distribution.properties, ${baseDir}/**/target/**/configuration.properties"
    } catch (err) {
      echo hudson.Functions.printThrowable(err)
    }
  }
}

void dockerPull(String image) {
  sh "docker pull ${image}"
}

void dockerRun(String image, String command, String user = null) {
  String userOption = user ? "--user=${user}" : ''
  sh "docker run --rm ${userOption} ${image} ${command}"
}

void dockerTag(String image, String tag) {
  sh "docker tag ${image} ${tag}"
}

void dockerPush(String image) {
  sh "docker push ${image}"
}

void dockerDeploy(String imageName) {
  String imageTag = "${dockerNamespace}/${imageName}:${VERSION}"
  String internalImage = "${DOCKER_REGISTRY}/${imageTag}"
  String publicImage = "${PUBLIC_DOCKER_REGISTRY}/${imageTag}"
  echo "Push ${publicImage}"
  dockerPull(internalImage)
  dockerTag(internalImage, publicImage)
  dockerPush(publicImage)
}

/**
 * Replaces environment variables present in the given yaml file and then runs skaffold build on it.
 * Needed environment variables are generally:
 * - DOCKER_REGISTRY
 * - VERSION
 */
void skaffoldBuild(String yaml) {
  sh """
    envsubst < ${yaml} > ${yaml}~gen
    skaffold build -f ${yaml}~gen
  """
}

void skaffoldBuildAll() {
  // build builder and base images
  skaffoldBuild('docker/skaffold.yaml')
  // build images depending on the builder and/or base images, waiting for dependent images support in skaffold
  skaffoldBuild('docker/slim/skaffold.yaml')
  skaffoldBuild('docker/nuxeo/skaffold.yaml')
}

def buildUnitTestStage(env) {
  def isDev = env == 'dev'
  def testNamespace = "${TEST_NAMESPACE_PREFIX}-${env}"
  def redisHost = "${TEST_REDIS_RESOURCE}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
  return {
    stage("Run ${env} unit tests") {
      container('maven') {
        script {
          setGitHubBuildStatus("platform/utests/${env}", "Unit tests - ${env} environment", 'PENDING')
          try {
            echo """
            ----------------------------------------
            Run ${env} unit tests
            ----------------------------------------"""

            echo "${env} unit tests: install external services"
            // initialize Helm without Tiller and add local repository
            sh """
              helm init --client-only
              helm repo add ${HELM_CHART_REPOSITORY_NAME} ${HELM_CHART_REPOSITORY_URL}
            """
            // prepare values to disable nuxeo and activate external services in the nuxeo Helm chart
            sh 'envsubst < ci/helm/nuxeo-test-base-values.yaml > nuxeo-test-base-values.yaml'
            def testValues = '--set-file=nuxeo-test-base-values.yaml'
            if (!isDev) {
              sh "envsubst < ci/helm/nuxeo-test-${env}-values.yaml > nuxeo-test-${env}-values.yaml"
              testValues += " --set-file=nuxeo-test-${env}-values.yaml"
            }
            // install the nuxeo Helm chart into a dedicated namespace that will be cleaned up afterwards
            sh """
              jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO} \
                --name=${TEST_HELM_CHART_RELEASE} \
                --namespace=${testNamespace} \
                --version=1.0.4-PR-6-1 \
                ${testValues}
            """
            // wait for Redis to be ready
            sh """
              kubectl rollout status statefulset ${TEST_REDIS_RESOURCE} \
                --namespace=${testNamespace} \
                --timeout=${TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT}
            """
            if (!isDev) {
              // wait for Elasticsearch to be ready
              sh """
                kubectl rollout status deployment ${TEST_ELASTICSEARCH_RESOURCE} \
                  --namespace=${testNamespace} \
                  --timeout=${TEST_ELASTICSEARCH_ROLLOUT_STATUS_TIMEOUT}
              """
              // wait for MongoDB or PostgreSQL to be ready
              def resourceType = env == 'mongodb' ? 'deployment' : 'statefulset'
              sh """
                kubectl rollout status ${resourceType} ${TEST_HELM_CHART_RELEASE}-${env} \
                  --namespace=${testNamespace} \
                  --timeout=${TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT}
              """
            }

            echo "${env} unit tests: run Maven"
            // prepare test framework system properties
            sh """
              CHART_RELEASE=${TEST_HELM_CHART_RELEASE} SERVICE=${env} NAMESPACE=${testNamespace} DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
                envsubst < ci/mvn/nuxeo-test-${env}.properties > ${HOME}/nuxeo-test-${env}.properties
            """
            // run unit tests:
            // - in nuxeo-core and dependent projects only (nuxeo-common and nuxeo-runtime are run in dedicated stages)
            // - for the given environment (see the customEnvironment profile in pom.xml):
            //   - in an alternative build directory
            //   - loading some test framework system properties
            def testCore = env == 'mongodb' ? 'mongodb' : 'vcs'
            sh """
              mvn ${MAVEN_ARGS} -rf nuxeo-core \
                -Dcustom.environment=${env} \
                -Dnuxeo.test.core=${testCore} \
                -Dnuxeo.test.redis.host=${redisHost} \
                test
            """

            setGitHubBuildStatus("platform/utests/${env}", "Unit tests - ${env} environment", 'SUCCESS')
          } catch(err) {
            setGitHubBuildStatus("platform/utests/${env}", "Unit tests - ${env} environment", 'FAILURE')
            throw err
          } finally {
            try {
              junit testResults: "**/target-${env}/surefire-reports/*.xml"
            } finally {
              echo "${env} unit tests: clean up test namespace"
              // uninstall the nuxeo Helm chart
              sh """
                jx step helm delete ${TEST_HELM_CHART_RELEASE} \
                  --namespace=${testNamespace} \
                  --purge
              """
              // clean up the test namespace
              sh "kubectl delete namespace ${testNamespace} --ignore-not-found=true"
            }
          }
        }
      }
    }
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-11'
  }
  environment {
    // force ${HOME}=/root - for an unexplained reason, ${HOME} is resolved as /home/jenkins though sh 'env' shows HOME=/root
    HOME = '/root'
    HELM_CHART_REPOSITORY_NAME = 'local-jenkins-x'
    HELM_CHART_REPOSITORY_URL = 'http://jenkins-x-chartmuseum:8080'
    HELM_CHART_NUXEO = 'nuxeo'
    TEST_HELM_CHART_RELEASE = 'test-release'
    TEST_NAMESPACE_PREFIX = "nuxeo-unit-tests-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
    TEST_SERVICE_DOMAIN_SUFFIX = 'svc.cluster.local'
    TEST_REDIS_RESOURCE = "${TEST_HELM_CHART_RELEASE}-redis-master"
    TEST_ELASTICSEARCH_RESOURCE = "${TEST_HELM_CHART_RELEASE}-elasticsearch-client"
    TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT = '1m'
     // Elasticsearch might take longer
    TEST_ELASTICSEARCH_ROLLOUT_STATUS_TIMEOUT = '3m'
    BUILDER_IMAGE_NAME = 'builder'
    BASE_IMAGE_NAME = 'base'
    NUXEO_IMAGE_NAME = 'nuxeo'
    SLIM_IMAGE_NAME = 'slim'
    // waiting for https://jira.nuxeo.com/browse/NXBT-3068 to put it in Global EnvVars
    PUBLIC_DOCKER_REGISTRY = 'docker.packages.nuxeo.com'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms512m -Xmx3072m"
    MAVEN_ARGS = '-B -nsu'
    VERSION = getVersion()
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
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
    stage('Update version') {
      when {
        branch 'PR-*'
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Update version
          ----------------------------------------
          New version: ${VERSION}
          """
          sh """
            mvn ${MAVEN_ARGS} -Pdistrib,docker versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
            perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${VERSION}</nuxeo.platform.version>|' pom.xml
          """
        }
      }
    }
    stage('Compile') {
      steps {
        setGitHubBuildStatus('platform/compile', 'Compile', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn ${MAVEN_ARGS} -V -T0.8C -DskipTests install"
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/compile', 'Compile', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/compile', 'Compile', 'FAILURE')
        }
      }
    }
    stage('Package') {
      steps {
        setGitHubBuildStatus('platform/package', 'Package', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Package
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} -f nuxeo-distribution/pom.xml -DskipTests install"
          sh "mvn ${MAVEN_ARGS} -f packages/pom.xml -DskipTests install"
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/package', 'Package', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/package', 'Package', 'FAILURE')
        }
      }
    }
    stage('Deploy Maven artifacts') {
      steps {
        setGitHubBuildStatus('platform/deploy', 'Deploy Maven artifacts', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven artifacts
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} -Pdistrib -DskipTests deploy"
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/deploy', 'Deploy Maven artifacts', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/deploy', 'Deploy Maven artifacts', 'FAILURE')
        }
      }
    }
    stage('JSF pipeline') {
      when {
        expression {
          // only trigger JSF pipeline if the target branch is master or a maintenance branch
          return CHANGE_TARGET ==~ 'master|\\d+\\.\\d+'
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Build JSF pipeline
          ----------------------------------------
          Parameters:
            NUXEO_BRANCH: ${CHANGE_BRANCH}
            NUXEO_COMMIT_SHA: ${GIT_COMMIT}
            NUXEO_VERSION: ${VERSION}
          """
          build job: "/nuxeo/nuxeo-jsf-ui-status/${CHANGE_TARGET}",
            parameters: [
              string(name: 'NUXEO_BRANCH', value: "${CHANGE_BRANCH}"),
              string(name: 'NUXEO_COMMIT_SHA', value: "${GIT_COMMIT}"),
              string(name: 'NUXEO_VERSION', value: "${VERSION}"),
            ], propagate: false, wait: false
        }
      }
    }
    stage('Build Docker images') {
      steps {
        setGitHubBuildStatus('platform/docker/build', 'Build Docker images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Build Docker images
          ----------------------------------------
          Image tag: ${VERSION}
          """
          echo "Build and push Docker images to internal Docker registry ${DOCKER_REGISTRY}"
          // Fetch Nuxeo distribution and Nuxeo Content Platform packages with Maven
          sh "mvn ${MAVEN_ARGS} -f docker/pom.xml process-resources"
          skaffoldBuildAll()
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/docker/build', 'Build Docker images', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/docker/build', 'Build Docker images', 'FAILURE')
        }
      }
    }
    stage('Test Docker images') {
      steps {
        setGitHubBuildStatus('platform/docker/test', 'Test Docker images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Test Docker images
          ----------------------------------------
          """
          script {
            // builder image
            def image = "${DOCKER_REGISTRY}/${dockerNamespace}/${BUILDER_IMAGE_NAME}:${VERSION}"
            echo "Test ${image}"
            dockerPull(image)
            dockerRun(image, 'ls -l /distrib')

            // base image
            image = "${DOCKER_REGISTRY}/${dockerNamespace}/${BASE_IMAGE_NAME}:${VERSION}"
            echo "Test ${image}"
            dockerPull(image)
            dockerRun(image, 'cat /etc/centos-release; java -version')

            // nuxeo slim image
            image = "${DOCKER_REGISTRY}/${dockerNamespace}/${SLIM_IMAGE_NAME}:${VERSION}"
            echo "Test ${image}"
            dockerPull(image)
            echo 'Run image as root (0)'
            dockerRun(image, 'nuxeoctl start')
            echo 'Run image as an arbitrary user (800)'
            dockerRun(image, 'nuxeoctl start', '800')

            // nuxeo image
            image = "${DOCKER_REGISTRY}/${dockerNamespace}/${NUXEO_IMAGE_NAME}:${VERSION}"
            echo "Test ${image}"
            dockerPull(image)
            echo 'Run image as root (0)'
            dockerRun(image, 'nuxeoctl start')
            echo 'Run image as an arbitrary user (800)'
            dockerRun(image, 'nuxeoctl start', '800')
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/docker/test', 'Test Docker images', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/docker/test', 'Test Docker images', 'FAILURE')
        }
      }
    }
    stage('Deploy Docker images') {
      when {
        branch 'master'
      }
      steps {
        setGitHubBuildStatus('platform/docker/deploy', 'Deploy Docker images', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Docker images
          ----------------------------------------
          Image tag: ${VERSION}
          """
          echo "Push Docker images to public Docker registry ${PUBLIC_DOCKER_REGISTRY}"
          dockerDeploy("${BUILDER_IMAGE_NAME}")
          dockerDeploy("${BASE_IMAGE_NAME}")
          dockerDeploy("${SLIM_IMAGE_NAME}")
          dockerDeploy("${NUXEO_IMAGE_NAME}")
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/docker/deploy', 'Deploy Docker images', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/docker/deploy', 'Deploy Docker images', 'FAILURE')
        }
      }
    }
    stage('Run common unit tests') {
      steps {
        setGitHubBuildStatus('platform/utests/common/dev', 'Unit tests - common', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run common unit tests
          ----------------------------------------"""
          dir('nuxeo-common') {
            sh "mvn ${MAVEN_ARGS} test"
          }
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('platform/utests/common/dev', 'Unit tests - common', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/utests/common/dev', 'Unit tests - common', 'FAILURE')
        }
      }
    }
    stage('Run runtime unit tests') {
      steps {
        setGitHubBuildStatus('platform/utests/runtime/dev', 'Unit tests - runtime', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run runtime unit tests
          ----------------------------------------"""
          dir('nuxeo-runtime') {
            sh "mvn ${MAVEN_ARGS} test"
          }
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('platform/utests/runtime/dev', 'Unit tests - runtime', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/utests/runtime/dev', 'Unit tests - runtime', 'FAILURE')
        }
      }
    }
    stage('Run unit tests') {
      steps {
        script {
          def stages = [:]
          for (env in testEnvironments) {
            stages["Run ${env} unit tests"] = buildUnitTestStage(env);
          }
          parallel stages
        }
      }
    }
    stage('Run "dev" functional tests') {
      steps {
        setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run "dev" functional tests
          ----------------------------------------"""
          script {
            try {
              runFunctionalTests('nuxeo-distribution/nuxeo-server-tests')
              runFunctionalTests('nuxeo-distribution/nuxeo-server-hotreload-tests')
              runFunctionalTests('nuxeo-distribution/nuxeo-server-gatling-tests')
              runFunctionalTests('ftests')
              setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
            } catch (err) {
              setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'FAILURE')
            }
          }
        }
      }
      post {
        always {
          junit testResults: '**/target/failsafe-reports/*.xml'
        }
      }
    }
  }
  post {
    always {
      script {
        if (BRANCH_NAME == 'master') {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
