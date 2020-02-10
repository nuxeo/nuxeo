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
    sh "mvn -B -nsu -f ${baseDir}/pom.xml verify"
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

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-11'
  }
  environment {
    HELM_CHART_REPOSITORY_NAME = 'local-jenkins-x'
    HELM_CHART_REPOSITORY_URL = 'http://jenkins-x-chartmuseum:8080'
    HELM_CHART_NUXEO_REDIS = 'nuxeo-redis'
    HELM_RELEASE_REDIS = 'redis'
    NAMESPACE_REDIS = "nuxeo-unit-tests-redis-$BRANCH_NAME".toLowerCase()
    SERVICE_REDIS = 'redis-master'
    REDIS_HOST = "${SERVICE_REDIS}.${NAMESPACE_REDIS}.svc.cluster.local"
    SERVICE_ACCOUNT = 'jenkins'
    BUILDER_IMAGE_NAME = 'builder'
    BASE_IMAGE_NAME = 'base'
    NUXEO_IMAGE_NAME = 'nuxeo'
    SLIM_IMAGE_NAME = 'slim'
    // waiting for https://jira.nuxeo.com/browse/NXBT-3068 to put it in Global EnvVars
    PUBLIC_DOCKER_REGISTRY = 'docker.packages.nuxeo.com'
    MAVEN_OPTS= "$MAVEN_OPTS -Xms512m -Xmx3072m"
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
            mvn -nsu -Pdocker versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
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
          sh 'mvn -V -B -nsu -T0.8C -DskipTests install'
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
          sh 'mvn -B -nsu -f nuxeo-distribution/pom.xml -DskipTests install'
          sh 'mvn -B -nsu -f packages/pom.xml -DskipTests install'
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
          sh 'mvn -B -nsu -Pdistrib -DskipTests deploy'
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
          sh "mvn -B -nsu -f docker/pom.xml process-resources"
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
    stage('Run "dev" unit tests') {
      steps {
        setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Install Redis
          ----------------------------------------"""
          sh """
            # initialize Helm without installing Tiller
            helm init --client-only --service-account ${SERVICE_ACCOUNT}

            # add local chart repository
            helm repo add ${HELM_CHART_REPOSITORY_NAME} ${HELM_CHART_REPOSITORY_URL}

            # install the nuxeo-redis chart into a dedicated namespace that will be cleaned up afterwards
            # use 'jx step helm install' to avoid 'Error: incompatible versions' when running 'helm install'
            envsubst < ci/redis-values.yaml > redis-values.yaml~gen
            jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO_REDIS} \
              --name ${HELM_RELEASE_REDIS} \
              --namespace ${NAMESPACE_REDIS} \
              --set-file=redis-values.yaml~gen
          """

          echo """
          ----------------------------------------
          Run "dev" unit tests
          ----------------------------------------"""
          sh "mvn -B -nsu -Dnuxeo.test.redis.host=${REDIS_HOST} test"
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
          container('maven') {
            // clean up the redis namespace
            sh "kubectl delete namespace ${NAMESPACE_REDIS} --ignore-not-found=true"
          }
        }
        success {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'FAILURE')
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
