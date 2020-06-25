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
 *     Thomas Roger <troger@nuxeo.com>
 */

dockerNamespace = 'nuxeo'
kubernetesNamespace = 'platform'
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
  if (env.DRY_RUN != "true") {
    step([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repositoryUrl],
      contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
      statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
    ])
  }
}

String getMavenArgs() {
  def args = '-B -nsu -Dnuxeo.skip.enforcer=true'
  if (!isPullRequest()) {
    args += ' -Prelease'
  }
  return args
}

def isPullRequest() {
  return BRANCH_NAME =~ /PR-.*/
}

String getVersion() {
  return isPullRequest() ? getPullRequestVersion() : getReleaseVersion()
}

String getReleaseVersion() {
  String nuxeoVersion = readMavenPom().getVersion()
  String noSnapshot = nuxeoVersion.replace("-SNAPSHOT", "")
  String version = noSnapshot + '.0' // first version ever

  // find the latest tag if any
  sh "git fetch origin 'refs/tags/v${noSnapshot}*:refs/tags/v${noSnapshot}*'"
  def tag = sh(returnStdout: true, script: "git tag --sort=taggerdate --list 'v${noSnapshot}*' | tail -1 | tr -d '\n'")
  if (tag) {
    container('maven') {
      version = sh(returnStdout: true, script: "semver bump patch ${tag} | tr -d '\n'")
    }
  }
  return version
}

String getPullRequestVersion() {
  return "${BRANCH_NAME}-" + readMavenPom().getVersion()
}

String getDockerTagFrom(String version) {
  return version.tokenize('.')[0] + '.x'
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
  String fullImageName = "${dockerNamespace}/${imageName}"
  String fixedVersionInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String latestInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${DOCKER_TAG}"
  String fixedVersionPublicImage = "${NUXEO_DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String latestPublicImage = "${NUXEO_DOCKER_REGISTRY}/${fullImageName}:${DOCKER_TAG}"

  dockerPull(fixedVersionInternalImage)
  echo "Push ${latestInternalImage}"
  dockerTag(fixedVersionInternalImage, latestInternalImage)
  dockerPush(latestInternalImage)
  echo "Push ${fixedVersionPublicImage}"
  dockerTag(fixedVersionInternalImage, fixedVersionPublicImage)
  dockerPush(fixedVersionPublicImage)
  echo "Push ${latestPublicImage}"
  dockerTag(fixedVersionInternalImage, latestPublicImage)
  dockerPush(latestPublicImage)
}

/**
 * Replaces environment variables present in the given yaml file and then runs skaffold build on it.
 * Needed environment variables are generally:
 * - BUILD_TAG
 * - DOCKER_REGISTRY
 * - GIT_COMMIT
 * - VERSION
 */
void skaffoldBuild(String yaml) {
  sh """
    envsubst < ${yaml} > ${yaml}~gen
    skaffold build -f ${yaml}~gen
  """
}

def rolloutStatus(kind, name, timeout, namespace) {
  sh """
    kubectl rollout status ${kind} ${name} \
      --timeout=${timeout} \
      --namespace=${namespace}
  """
}

def rolloutStatusRedis(namespace) {
  rolloutStatus('statefulset', "${TEST_REDIS_K8S_OBJECT}", "${TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT}", namespace)
}

def rolloutStatusMongoDB(namespace) {
  rolloutStatus('deployment', "${TEST_MONGODB_K8S_OBJECT}", "${TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT}", namespace)
}

def rolloutStatusPostgreSQL(namespace) {
  rolloutStatus('statefulset', "${TEST_POSTGRESQL_K8S_OBJECT}", "${TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT}", namespace)
}

def rolloutStatusElasticsearch(namespace) {
  rolloutStatus('statefulset', "${TEST_ELASTICSEARCH_DATA_K8S_OBJECT}", "${TEST_LONG_ROLLOUT_STATUS_TIMEOUT}", namespace)
  rolloutStatus('statefulset', "${TEST_ELASTICSEARCH_MASTER_K8S_OBJECT}", "${TEST_LONG_ROLLOUT_STATUS_TIMEOUT}", namespace)
  rolloutStatus('deployment', "${TEST_ELASTICSEARCH_CLIENT_K8S_OBJECT}", "${TEST_LONG_ROLLOUT_STATUS_TIMEOUT}", namespace)
}

def rolloutStatusKafka(namespace) {
  rolloutStatus('statefulset', "${TEST_KAFKA_K8S_OBJECT}", "${TEST_LONG_ROLLOUT_STATUS_TIMEOUT}", namespace)
}

def buildUnitTestStage(env) {
  def isDev = env == 'dev'
  def testNamespace = "${TEST_NAMESPACE_PREFIX}-${env}"
  def redisHost = "${TEST_REDIS_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
  def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"
  return {
    stage("Run ${env} unit tests") {
      container("maven-${env}") {
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
            def testValues = '--set-file=ci/helm/nuxeo-test-base-values.yaml~gen'
            if (!isDev) {
              testValues += " --set-file=ci/helm/nuxeo-test-${env}-values.yaml~gen"
              testValues += " --set-file=ci/helm/nuxeo-test-elasticsearch-values.yaml~gen"
              testValues += " --set-file=ci/helm/nuxeo-test-kafka-values.yaml~gen"
            }
            // install the nuxeo Helm chart into a dedicated namespace that will be cleaned up afterwards
            sh """
              jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO} \
                --name=${TEST_HELM_CHART_RELEASE} \
                --namespace=${testNamespace} \
                ${testValues}
            """
            // wait for external services to be ready
            rolloutStatusRedis(testNamespace)
            if (!isDev) {
              if (env == 'mongodb') {
                rolloutStatusMongoDB(testNamespace)
              } else {
                rolloutStatusPostgreSQL(testNamespace)
              }
              rolloutStatusElasticsearch(testNamespace)
              rolloutStatusKafka(testNamespace)
            }

            echo "${env} unit tests: run Maven"
            if (isDev) {
              // empty file required by the read-project-properties goal of the properties-maven-plugin with the
              // customEnvironment profile
              sh "touch ${HOME}/nuxeo-test-${env}.properties"
            } else {
              // prepare test framework system properties
              sh """
                cat ci/mvn/nuxeo-test-${env}.properties \
                  ci/mvn/nuxeo-test-elasticsearch.properties \
                  > ci/mvn/nuxeo-test-${env}.properties~gen
                CHART_RELEASE=${TEST_HELM_CHART_RELEASE} NAMESPACE=${testNamespace} DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
                  envsubst < ci/mvn/nuxeo-test-${env}.properties~gen > ${HOME}/nuxeo-test-${env}.properties
              """
            }
            // run unit tests:
            // - in modules/core and dependent projects only (modules/runtime is run in a dedicated stage)
            // - for the given environment (see the customEnvironment profile in pom.xml):
            //   - in an alternative build directory
            //   - loading some test framework system properties
            def testCore = env == 'mongodb' ? 'mongodb' : 'vcs'
            def kafkaOptions = isDev ? '' : "-Pkafka -Dkafka.bootstrap.servers=${kafkaHost}"
            sh """
              mvn ${MAVEN_ARGS} -rf :nuxeo-core-parent \
                -Dcustom.environment=${env} \
                -Dcustom.environment.log.dir=target-${env} \
                -Dnuxeo.test.core=${testCore} \
                -Dnuxeo.test.redis.host=${redisHost} \
                ${kafkaOptions} \
                test
            """

            setGitHubBuildStatus("platform/utests/${env}", "Unit tests - ${env} environment", 'SUCCESS')
          } catch(err) {
            setGitHubBuildStatus("platform/utests/${env}", "Unit tests - ${env} environment", 'FAILURE')
            throw err
          } finally {
            try {
              junit testResults: "**/target-${env}/surefire-reports/*.xml"
              if (!isDev) {
                archiveKafkaLogs(testNamespace, "${env}-kafka.log")
              }
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

void archiveKafkaLogs(namespace, logFile) {
  sh "kubectl logs ${TEST_KAFKA_POD_NAME} --namespace=${namespace} > ${logFile}"
  archiveArtifacts allowEmptyArchive: true, artifacts: "${logFile}"
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
    TEST_REDIS_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-redis-master"
    TEST_MONGODB_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-mongodb"
    TEST_POSTGRESQL_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-postgresql"
    TEST_ELASTICSEARCH_DATA_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-elasticsearch-data"
    TEST_ELASTICSEARCH_MASTER_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-elasticsearch-master"
    TEST_ELASTICSEARCH_CLIENT_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-elasticsearch-client"
    TEST_KAFKA_K8S_OBJECT = "${TEST_HELM_CHART_RELEASE}-kafka"
    TEST_KAFKA_PORT = '9092'
    TEST_KAFKA_POD_NAME = "${TEST_KAFKA_K8S_OBJECT}-0"
    TEST_DEFAULT_ROLLOUT_STATUS_TIMEOUT = '3m'
    // Elasticsearch and Kafka might take longer
    TEST_LONG_ROLLOUT_STATUS_TIMEOUT = '5m'
    NUXEO_IMAGE_NAME = 'nuxeo'
    SLIM_IMAGE_NAME = 'slim'
    // waiting for https://jira.nuxeo.com/browse/NXBT-3068 to put it in Global EnvVars
    NUXEO_DOCKER_REGISTRY = 'docker-private.packages.nuxeo.com'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms2g -Xmx3g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    MAVEN_ARGS = getMavenArgs()
    VERSION = getVersion()
    DOCKER_TAG = getDockerTagFrom("${VERSION}")
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    // jx step helm install's --name and --namespace options require alphabetic chars to be lowercase
    PREVIEW_NAMESPACE = "nuxeo-preview-${BRANCH_NAME.toLowerCase()}"
    PERSISTENCE = "${!isPullRequest()}"
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
          // set branch name in Helm chart values used for the unit tests
          sh """
            for valuesFile in ci/helm/*.yaml; do
              envsubst < \$valuesFile > \$valuesFile~gen
            done
          """
          // output pod description
          echo "Describe pod ${NODE_NAME}"
          sh """
            kubectl describe pod ${NODE_NAME}
          """
        }
      }
    }

    stage('Update version') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Update version
          ----------------------------------------
          New version: ${VERSION}
          """
          sh """
            # root POM
            mvn ${MAVEN_ARGS} -Pdistrib,docker versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
            perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${VERSION}</nuxeo.platform.version>|' pom.xml
            perl -i -pe 's|org.nuxeo.ecm.product.version=.*|org.nuxeo.ecm.product.version=${VERSION}|' server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults

            # nuxeo-parent POM
            # only replace the first <version> occurence
            perl -i -pe '!\$x && s|<version>.*?</version>|<version>${VERSION}</version>| && (\$x=1)' parent/pom.xml

            # nuxeo-promote-packages POM
            # only replace the first <version> occurence
            perl -i -pe '!\$x && s|<version>.*?</version>|<version>${VERSION}</version>| && (\$x=1)' ci/release/pom.xml
          """
        }
      }
    }

    stage('Git commit') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Git commit
          ----------------------------------------
          """
          sh """
            git commit -a -m "Release ${VERSION}"
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
          sh "mvn ${MAVEN_ARGS} -V -T4C -DskipTests install"
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

    stage('Run runtime unit tests') {
      steps {
        container('maven') {
          script {
            setGitHubBuildStatus('platform/utests/runtime', 'Unit tests - runtime', 'PENDING')
            def testNamespace = "${TEST_NAMESPACE_PREFIX}-runtime"
            def redisHost = "${TEST_REDIS_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
            def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"
            try {
              echo """
              ----------------------------------------
              Run runtime unit tests
              ----------------------------------------"""

              echo "runtime unit tests: install external services"
              // initialize Helm without Tiller and add local repository
              sh """
                helm init --client-only
                helm repo add ${HELM_CHART_REPOSITORY_NAME} ${HELM_CHART_REPOSITORY_URL}
              """
              // install the nuxeo Helm chart into a dedicated namespace that will be cleaned up afterwards
              sh """
                jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO} \
                  --name=${TEST_HELM_CHART_RELEASE} \
                  --namespace=${testNamespace} \
                  --set-file=ci/helm/nuxeo-test-base-values.yaml~gen \
                  --set-file=ci/helm/nuxeo-test-kafka-values.yaml~gen
              """
              // wait for external services to be ready
              rolloutStatusRedis(testNamespace)
              rolloutStatusKafka(testNamespace)

              echo "runtime unit tests: run Maven"
              // run unit tests
              dir('modules/runtime') {
                sh """
                  mvn ${MAVEN_ARGS} \
                    -Dnuxeo.test.redis.host=${redisHost} \
                    -Pkafka -Dkafka.bootstrap.servers=${kafkaHost} \
                    test
                """
              }

              setGitHubBuildStatus('platform/utests/runtime', 'Unit tests - runtime', 'SUCCESS')
            } catch(err) {
              setGitHubBuildStatus('platform/utests/runtime', 'Unit tests - runtime', 'FAILURE')
              throw err
            } finally {
              try {
                junit testResults: "**/target/surefire-reports/*.xml"
                archiveKafkaLogs(testNamespace, 'runtime-kafka.log')
              } finally {
                echo "runtime unit tests: clean up test namespace"
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

    stage('Package') {
      steps {
        setGitHubBuildStatus('platform/package', 'Package', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Package
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} -Dnuxeo.skip.enforcer=false -f server/pom.xml -DskipTests install"
          sh "mvn ${MAVEN_ARGS} -Dnuxeo.skip.enforcer=false -f packages/pom.xml -DskipTests install"
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

    stage('Run "dev" functional tests') {
      steps {
        setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run "dev" functional tests
          ----------------------------------------"""
          runFunctionalTests('ftests')
        }
        findText regexp: ".*ERROR.*", fileSet: "ftests/**/log/server.log", unstableIfFound: true
      }
      post {
        always {
          junit testResults: '**/target/failsafe-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/ftests/dev', 'Functional tests - dev environment', 'FAILURE')
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
          // Fetch Nuxeo Tomcat Server and Nuxeo Content Platform packages with Maven
          sh "mvn ${MAVEN_ARGS} -T4C -f docker/pom.xml process-resources"
          skaffoldBuild('docker/skaffold.yaml')
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
            // nuxeo slim image
            def image = "${DOCKER_REGISTRY}/${dockerNamespace}/${SLIM_IMAGE_NAME}:${VERSION}"
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

    stage('Git tag and push') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Git tag and push
          ----------------------------------------
          """
          sh """
            #!/usr/bin/env bash -xe
            # create the Git credentials
            jx step git credentials
            git config credential.helper store

            # Git tag
            git tag -a v${VERSION} -m "Release ${VERSION}"
            git push origin v${VERSION}
          """
        }
      }
    }

    stage('Deploy Maven artifacts') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      steps {
        setGitHubBuildStatus('platform/deploy', 'Deploy Maven artifacts', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven artifacts
          ----------------------------------------"""
          sh """
            mvn ${MAVEN_ARGS} -Pdistrib -DskipTests deploy
            mvn ${MAVEN_ARGS} -f parent/pom.xml deploy
          """
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

    stage('Upload Nuxeo Packages') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
      }
      steps {
        setGitHubBuildStatus('platform/upload/packages', 'Upload Nuxeo Packages', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PREPROD_URL}
          ----------------------------------------"""
          withCredentials([usernameColonPassword(credentialsId: 'connect-preprod', variable: 'CONNECT_PASS')]) {
            sh """
              PACKAGES_TO_UPLOAD="packages/nuxeo-*-package/target/nuxeo-*-package-*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl --fail -i -u "$CONNECT_PASS" -F package=@\$(ls \$file) "$CONNECT_PREPROD_URL"/site/marketplace/upload?batch=true ;
              done
            """
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/upload/packages', 'Upload Nuxeo Packages', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/upload/packages', 'Upload Nuxeo Packages', 'FAILURE')
        }
      }
    }

    stage('Deploy Docker images') {
      when {
        allOf {
          not {
            branch 'PR-*'
          }
          not {
            environment name: 'DRY_RUN', value: 'true'
          }
        }
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
          echo "Push Docker images to Docker registry ${NUXEO_DOCKER_REGISTRY}"
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

    stage('Deploy Preview') {
      when {
        not {
          branch 'PR-*'
        }
      }
      steps {
        setGitHubBuildStatus('nuxeo/preview', 'Deploy nuxeo preview', 'PENDING')
        container('maven') {
          dir('ci/helm/preview') {
            echo """
            ----------------------------------------
            Deploy Preview environment
            ----------------------------------------"""
            // first substitute environment variables in chart values
            sh """
              mv values.yaml values.yaml.tosubst
              envsubst < values.yaml.tosubst > values.yaml
            """
            script {
              boolean nsExists = sh(returnStatus: true, script: "kubectl get namespace ${PREVIEW_NAMESPACE}") == 0
              if (nsExists) {
                // Previous preview deployment needs to be scaled to 0 to be replaced correctly
                sh "kubectl -n ${PREVIEW_NAMESPACE} scale deployment nuxeo-preview --replicas=0"
              }
              sh "kubectl --namespace platform get secret kubernetes-docker-cfg -ojsonpath='{.data.\\.dockerconfigjson}' | base64 --decode > /tmp/config.json"
              sh """kubectl create secret generic kubernetes-docker-cfg \
                  --namespace=${PREVIEW_NAMESPACE} \
                  --from-file=.dockerconfigjson=/tmp/config.json \
                  --type=kubernetes.io/dockerconfigjson --dry-run -o yaml | kubectl apply -f -"""
              // build and deploy the chart
              // To avoid jx gc cron job, reference branch previews are deployed by calling jx step helm install instead of jx preview
              sh """
                jx step helm build
                mkdir target && helm template . --output-dir target
                jx step helm install --namespace ${PREVIEW_NAMESPACE} --name ${PREVIEW_NAMESPACE} .
              """
              // We need to expose the nuxeo url by hand
              url = sh(returnStdout: true, script: "jx get urls -n ${PREVIEW_NAMESPACE} | grep -oP https://.* | tr -d '\\n'")
              echo """
                ----------------------------------------
                Preview available at: ${url}
                ----------------------------------------"""
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/requirements.lock, **/target/**/*.yaml'
        }
        success {
          setGitHubBuildStatus('nuxeo/preview', 'Deploy nuxeo preview', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('nuxeo/preview', 'Deploy nuxeo preview', 'FAILURE')
        }
      }
    }
  }

  post {
    always {
      script {
        if (!isPullRequest()) {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
    success {
      script {
        if (!isPullRequest() && env.DRY_RUN != "true") {
          currentBuild.description = "Build ${VERSION}"
        }
      }
    }
  }
}
