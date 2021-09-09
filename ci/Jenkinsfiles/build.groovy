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

// Always abort build if triggered by https://jenkins.platform.dev.nuxeo.com/job/nuxeo/job/10.10/job/nuxeo/,
// configured to build all pull requests in nuxeo/nuxeo (cannot filter on the ones targeting the 10.10 branch only).
def abort() {
  currentBuild.result = 'ABORTED'
  error('Not building pull requests against the master branch.')
}
abort()

dockerNamespace = 'nuxeo'
repositoryUrl = 'https://github.com/nuxeo/nuxeo'
testEnvironments = [
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

String getCurrentNamespace() {
  container('maven') {
    return sh(returnStdout: true, script: "kubectl get pod ${NODE_NAME} -ojsonpath='{..namespace}'")
  }
}

String getMavenArgs() {
  def args = '-B -nsu -Dnuxeo.skip.enforcer=true'
  if (!isPullRequest()) {
    args += ' -Prelease'
  }
  return args
}

String getMavenFailArgs() {
  return (isPullRequest() && pullRequest.labels.contains('failatend')) ? '--fail-at-end' : ' '
}

String getMavenJavadocArgs() {
  // set Xmx/Xms to 1g for javadoc command, to avoid the pod being OOMKilled with an exit code 137
  return isPullRequest() ? ' ' : '-Pjavadoc  -DadditionalJOption=-J-Xmx1g -DadditionalJOption=-J-Xms1g'
}

def isPullRequest() {
  return BRANCH_NAME =~ /PR-.*/
}

String getCurrentVersion() {
  return readMavenPom().getVersion();
}

String getVersion() {
  return isPullRequest() ? getPullRequestVersion() : getReleaseVersion()
}

String getReleaseVersion() {
  String nuxeoVersion = getCurrentVersion()
  String noSnapshot = nuxeoVersion.replace('-SNAPSHOT', '')
  String version = noSnapshot + '.1' // first version ever

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
  return "${BRANCH_NAME}-" + getCurrentVersion()
}

String getDockerTagFrom(String version) {
  return version.tokenize('.')[0] + '.x'
}

void runFunctionalTests(String baseDir, String tier) {
  try {
    retry(2) {
      sh "mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} -D${tier} -f ${baseDir}/pom.xml verify"
    }
    findText regexp: ".*ERROR.*", fileSet: "ftests/**/log/server.log"
  } catch(err) {
    echo "${baseDir} functional tests error: ${err}"
    throw err
  } finally {
    try {
      archiveArtifacts allowEmptyArchive: true, artifacts: "${baseDir}/**/target/failsafe-reports/*, ${baseDir}/**/target/**/*.log, ${baseDir}/**/target/*.png, ${baseDir}/**/target/*.html, ${baseDir}/**/target/**/distribution.properties, ${baseDir}/**/target/**/configuration.properties"
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

void dockerPushFixedVersion() {
  String fullImageName = "${dockerNamespace}/${NUXEO_IMAGE_NAME}"
  String fixedVersionInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String latestInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${DOCKER_TAG}"
  dockerPull(fixedVersionInternalImage)
  echo "Push ${latestInternalImage}"
  dockerTag(fixedVersionInternalImage, latestInternalImage)
  dockerPush(latestInternalImage)
}

void dockerDeploy(String dockerRegistry) {
  String fullImageName = "${dockerNamespace}/${NUXEO_IMAGE_NAME}"
  String fixedVersionInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String fixedVersionPublicImage = "${dockerRegistry}/${fullImageName}:${VERSION}"
  String latestPublicImage = "${dockerRegistry}/${fullImageName}:${DOCKER_TAG}"

  dockerPull(fixedVersionInternalImage)
  echo "Push ${fixedVersionPublicImage}"
  dockerTag(fixedVersionInternalImage, fixedVersionPublicImage)
  dockerPush(fixedVersionPublicImage)
  echo "Push ${latestPublicImage}"
  dockerTag(fixedVersionInternalImage, latestPublicImage)
  dockerPush(latestPublicImage)
}

void helmfileTemplate(namespace, environment, outputDir) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} deps
      ${HELMFILE_COMMAND} --environment ${environment} template --output-dir ${outputDir}
    """
  }
}

void helmfileSync(namespace, environment) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} deps
      ${HELMFILE_COMMAND} --environment ${environment} sync
    """
  }
}

void helmfileDestroy(namespace, environment) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} --environment ${environment} destroy
    """
  }
}

def buildUnitTestStage(env) {
  def isDev = env == 'dev'
  def testNamespace = "${TEST_NAMESPACE_PREFIX}-${env}"
  // Helmfile environment
  def environment = "${env}UnitTests"
  def redisHost = "${TEST_REDIS_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
  def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"
  def containerName = isDev ? 'maven' : "maven-${env}"
  return {
    stage("Run ${env} unit tests") {
      container("${containerName}") {
        // TODO NXP-29512: on a PR, make the build continue even if there is a test error
        // on other environments than the dev one
        // to remove when all test environments will be mandatory
        catchError(buildResult: isPullRequest() && "dev" != env ? 'SUCCESS' : 'FAILURE', stageResult: 'FAILURE', catchInterruptions: false) {
          script {
            setGitHubBuildStatus("utests/${env}", "Unit tests - ${env} environment", 'PENDING')
            sh "kubectl create namespace ${testNamespace}"
            try {
              echo """
              ----------------------------------------
              Run ${env} unit tests
              ----------------------------------------"""

              echo "${env} unit tests: install external services"
              helmfileSync("${testNamespace}", "${environment}")

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
                  NAMESPACE=${testNamespace} DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
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

              retry(2) {
                sh """
                  mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} -rf :nuxeo-core-parent \
                    -Dcustom.environment=${env} \
                    -Dcustom.environment.log.dir=target-${env} \
                    -Dnuxeo.test.core=${testCore} \
                    -Dnuxeo.test.redis.host=${redisHost} \
                    ${kafkaOptions} \
                    test
                """
              }

              setGitHubBuildStatus("utests/${env}", "Unit tests - ${env} environment", 'SUCCESS')
            } catch(err) {
              echo "${env} unit tests error: ${err}"
              setGitHubBuildStatus("utests/${env}", "Unit tests - ${env} environment", 'FAILURE')
              throw err
            } finally {
              try {
                junit allowEmptyResults: true, testResults: "**/target-${env}/surefire-reports/*.xml"
                if (!isDev) {
                  archiveKafkaLogs(testNamespace, "${env}-kafka.log")
                }
              } finally {
                echo "${env} unit tests: clean up test namespace"
                try {
                  helmfileDestroy("${testNamespace}", "${environment}")
                } finally {
                  // clean up test namespace
                  sh "kubectl delete namespace ${testNamespace} --ignore-not-found=true"
                }
              }
            }
          }
        }
      }
    }
  }
}

void archiveKafkaLogs(namespace, logFile) {
  // don't fail if pod doesn't exist
  sh "kubectl logs ${TEST_KAFKA_POD_NAME} --namespace=${namespace} > ${logFile} || true"
  archiveArtifacts allowEmptyArchive: true, artifacts: "${logFile}"
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-11'
  }
  options {
    timeout(time: 12, unit: 'HOURS')
  }
  environment {
    // force ${HOME}=/root - for an unexplained reason, ${HOME} is resolved as /home/jenkins though sh 'env' shows HOME=/root
    HOME = '/root'
    HELMFILE_COMMAND = "helmfile --file ci/helm/helmfile.yaml --helm-binary /usr/bin/helm3"
    CURRENT_NAMESPACE = getCurrentNamespace()
    TEST_NAMESPACE_PREFIX = "$CURRENT_NAMESPACE-nuxeo-unit-tests-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
    TEST_SERVICE_DOMAIN_SUFFIX = 'svc.cluster.local'
    TEST_REDIS_K8S_OBJECT = 'redis-master'
    TEST_KAFKA_K8S_OBJECT = 'kafka'
    TEST_KAFKA_PORT = '9092'
    TEST_KAFKA_POD_NAME = "${TEST_KAFKA_K8S_OBJECT}-0"
    NUXEO_IMAGE_NAME = 'nuxeo'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms2g -Xmx3g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    MAVEN_ARGS = getMavenArgs()
    MAVEN_FAIL_ARGS = getMavenFailArgs()
    MAVEN_JAVADOC_ARGS = getMavenJavadocArgs()
    CURRENT_VERSION = getCurrentVersion()
    VERSION = getVersion()
    DOCKER_TAG = getDockerTagFrom("${VERSION}")
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    CONNECT_PREPROD_URL = 'https://nos-preprod-connect.nuxeocloud.com/nuxeo'
    SLACK_CHANNEL = 'platform-notifs'
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
            perl -i -pe 's|<version>.*?</version>|<version>${VERSION}</version>|' parent/pom.xml

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

    stage('Build') {
      steps {
        setGitHubBuildStatus('maven/build', 'Build', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          echo "MAVEN_OPTS=$MAVEN_OPTS"
          sh "mvn ${MAVEN_ARGS} ${MAVEN_JAVADOC_ARGS} -V -T4C -DskipTests install"
          sh "mvn ${MAVEN_ARGS} ${MAVEN_JAVADOC_ARGS} -f server/pom.xml -DskipTests install"
        }
      }
      post {
        success {
          setGitHubBuildStatus('maven/build', 'Build', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('maven/build', 'Build', 'FAILURE')
        }
      }
    }

    stage('Run runtime unit tests') {
      steps {
        container('maven') {
          script {
            setGitHubBuildStatus('utests/runtime', 'Unit tests - runtime', 'PENDING')
            def testNamespace = "${TEST_NAMESPACE_PREFIX}-runtime"
            // Helmfile environment
            def environment = 'runtimeUnitTests'
            def redisHost = "${TEST_REDIS_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
            def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"
            sh "kubectl create namespace ${testNamespace}"
            try {
              echo """
              ----------------------------------------
              Run runtime unit tests
              ----------------------------------------"""

              echo 'runtime unit tests: install external services'
              helmfileSync("${testNamespace}", "${environment}")

              echo 'runtime unit tests: run Maven'
              dir('modules/runtime') {
                retry(2) {
                  sh """
                    mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} \
                      -Dnuxeo.test.redis.host=${redisHost} \
                      -Pkafka -Dkafka.bootstrap.servers=${kafkaHost} \
                      test
                  """
                }
              }

              setGitHubBuildStatus('utests/runtime', 'Unit tests - runtime', 'SUCCESS')
            } catch(err) {
              echo "runtime unit tests error: ${err}"
              setGitHubBuildStatus('utests/runtime', 'Unit tests - runtime', 'FAILURE')
              throw err
            } finally {
              try {
                junit testResults: '**/target/surefire-reports/*.xml'
                archiveKafkaLogs(testNamespace, 'runtime-kafka.log')
              } finally {
                echo 'runtime unit tests: clean up test namespace'
                try {
                  helmfileDestroy("${testNamespace}", "${environment}")
                } finally {
                  // clean up test namespace
                  sh "kubectl delete namespace ${testNamespace} --ignore-not-found=true"
                }
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

    stage('Run server unit tests') {
      steps {
        setGitHubBuildStatus('utests/server', 'Unit tests - server', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run server unit tests
          ----------------------------------------"""
          // run server tests
          dir('server') {
            sh "mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} test"
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('utests/server', 'Unit tests - server', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('utests/server', 'Unit tests - server', 'FAILURE')
        }
      }
    }

    stage('Build Nuxeo Packages') {
      steps {
        setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Package
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} -Dnuxeo.skip.enforcer=false -f packages/pom.xml -DskipTests install"
        }
      }
      post {
        success {
          setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('packages/build', 'Build Nuxeo packages', 'FAILURE')
        }
      }
    }

    stage('Run "dev" functional tests') {
      steps {
        setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Run "dev" functional tests
          ----------------------------------------"""
          runFunctionalTests('ftests', 'nuxeo.ftests.tier5')
          runFunctionalTests('ftests', 'nuxeo.ftests.tier6')
          runFunctionalTests('ftests', 'nuxeo.ftests.tier7')
        }
      }
      post {
        always {
          junit testResults: '**/target/failsafe-reports/*.xml'
        }
        success {
          setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('ftests/dev', 'Functional tests - dev environment', 'FAILURE')
          // findText does mark the build in FAILURE but doesn't stop the pipeline, error does
          error "Errors were found!"
        }
      }
    }

    stage('Build Docker image') {
      steps {
        setGitHubBuildStatus('docker/build', 'Build Docker image', 'PENDING')
        container('maven') {
          dir('docker') {
            echo """
            ----------------------------------------
            Build Docker image
            ----------------------------------------
            Image tag: ${VERSION}
            """
            echo 'Fetch locally built Nuxeo Tomcat Server with Maven'
            sh "mvn ${MAVEN_ARGS} -T4C process-resources"

            echo "Build and push Docker image to internal Docker registry ${DOCKER_REGISTRY}"
            sh """
              envsubst < skaffold.yaml > skaffold.yaml~gen
              skaffold build -f skaffold.yaml~gen
            """
            script {
              if (!isPullRequest()) {
                dockerPushFixedVersion()
              }
            }
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('docker/build', 'Build Docker image', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('docker/build', 'Build Docker image', 'FAILURE')
        }
      }
    }

    stage('Test Docker image') {
      steps {
        setGitHubBuildStatus('docker/test', 'Test Docker image', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Test Docker image
          ----------------------------------------
          """
          script {
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
          setGitHubBuildStatus('docker/test', 'Test Docker image', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('docker/test', 'Test Docker image', 'FAILURE')
        }
      }
    }

    stage('Trigger REST API tests') {
      steps {
        echo """
        ----------------------------------------
        Trigger REST API tests
        ----------------------------------------
        """
        script {
          def parameters = [
            string(name: 'NUXEO_VERSION', value: "${VERSION}"),
          ]
          if (isPullRequest()) {
            parameters.add(string(name: 'NUXEO_REPOSITORY', value: "${repositoryUrl}"))
            parameters.add(string(name: 'NUXEO_SHA', value: "${GIT_COMMIT}"))
          }
          build(
            job: 'nuxeo/rest-api-compatibility-tests/master',
            parameters: parameters,
            wait: false
          )
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
        setGitHubBuildStatus('maven/deploy', 'Deploy Maven artifacts', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Maven artifacts
          ----------------------------------------"""
          sh """
            mvn ${MAVEN_ARGS} -Pdistrib -DskipTests deploy
            mvn ${MAVEN_ARGS} -f parent/pom.xml deploy

            # update back nuxeo-parent version to CURRENT_VERSION version
            mvn ${MAVEN_ARGS} -f parent/pom.xml versions:set -DnewVersion=${CURRENT_VERSION} -DgenerateBackupPoms=false
            mvn ${MAVEN_ARGS} -f parent/pom.xml deploy
          """
        }
      }
      post {
        success {
          setGitHubBuildStatus('maven/deploy', 'Deploy Maven artifacts', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('maven/deploy', 'Deploy Maven artifacts', 'FAILURE')
        }
      }
    }

    stage('Deploy Nuxeo Packages') {
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
        setGitHubBuildStatus('packages/deploy', 'Deploy Nuxeo Packages', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PREPROD_URL}
          ----------------------------------------"""
          withCredentials([usernameColonPassword(credentialsId: 'connect-preprod', variable: 'CONNECT_PASS')]) {
            sh '''
              PACKAGES_TO_UPLOAD="packages/nuxeo-*-package/target/nuxeo-*-package-*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl --fail -i -u $CONNECT_PASS -F package=@\$(ls \$file) $CONNECT_PREPROD_URL/site/marketplace/upload?batch=true ;
              done
            '''
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('packages/deploy', 'Deploy Nuxeo Packages', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('packages/deploy', 'Deploy Nuxeo Packages', 'FAILURE')
        }
      }
    }

    stage('Deploy Docker image') {
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
        setGitHubBuildStatus('docker/deploy', 'Deploy Docker image', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Docker image
          ----------------------------------------
          Image tag: ${VERSION}
          """
          echo "Push Docker image to Docker registry ${PUBLIC_DOCKER_REGISTRY}"
          dockerDeploy("${PUBLIC_DOCKER_REGISTRY}")
          echo "Push Docker image to Docker registry ${PRIVATE_DOCKER_REGISTRY}"
          dockerDeploy("${PRIVATE_DOCKER_REGISTRY}")
        }
      }
      post {
        success {
          setGitHubBuildStatus('docker/deploy', 'Deploy Docker image', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('docker/deploy', 'Deploy Docker image', 'FAILURE')
        }
      }
    }

    stage('Deploy Server Preview') {
      when {
        not {
          branch 'PR-*'
        }
      }
      steps {
        setGitHubBuildStatus('server/preview', 'Deploy server preview', 'PENDING')
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Deploy Preview environment
            ----------------------------------------"""
            // Kubernetes namespace, requires lower case alphanumeric characters
            def previewNamespace = "${CURRENT_NAMESPACE}-nuxeo-preview-${BRANCH_NAME.toLowerCase()}"
            def previewEnvironment = 'default'
            def previewHelmRelease = 'nuxeo'
            boolean nsExists = sh(returnStatus: true, script: "kubectl get namespace ${previewNamespace}") == 0
            if (!nsExists) {
              echo 'Create preview namespace'
              sh "kubectl create namespace ${previewNamespace}"

              echo 'Deploy TLS secret replicator to preview namespace'
              sh """
                kubectl apply -f ci/helm/templates/empty-tls-secret-for-kubernetes-replicator.yaml \
                  --namespace=${previewNamespace}
              """

              echo 'Copy image pull secret to preview namespace'
              sh "kubectl --namespace=platform get secret kubernetes-docker-cfg -ojsonpath='{.data.\\.dockerconfigjson}' | base64 --decode > /tmp/config.json"
              sh """kubectl create secret generic kubernetes-docker-cfg \
                  --namespace=${previewNamespace} \
                  --from-file=.dockerconfigjson=/tmp/config.json \
                  --type=kubernetes.io/dockerconfigjson --dry-run -o yaml | kubectl apply -f -"""
            }

            if (nsExists) {
              // scale down nuxeo preview deployment, otherwise K8s won't be able to mount the binaries volume for the pods
              // TODO: rely on a statefulset in the nuxeo Helm chart, as in mongodb
              echo 'Scale down nuxeo preview deployment before release upgrade'
              sh """
                kubectl scale deployment ${previewHelmRelease} \
                  --namespace=${previewNamespace} \
                  --replicas=0
              """
            }

            echo 'Upgrade external service and nuxeo preview releases'
            helmfileTemplate("${previewNamespace}", "${previewEnvironment}", 'target')
            try {
              helmfileSync("${previewNamespace}", "${previewEnvironment}")
            } catch (e) {
              sh """
                kubectl --namespace=${previewNamespace} get event --sort-by .lastTimestamp
                kubectl --namespace=${previewNamespace} get all,configmaps,secrets
                kubectl --namespace=${previewNamespace} describe pod --selector=app=${previewHelmRelease}
                kubectl --namespace=${previewNamespace} logs --selector=app=${previewHelmRelease} --tail=1000
              """
              throw e
            }

            host = sh(returnStdout: true, script: """
              kubectl get ingress ${previewHelmRelease} \
                --namespace=${previewNamespace} \
                -ojsonpath='{.spec.rules[*].host}'
            """)
            echo """
              -----------------------------------------------
              Preview available at: https://${host}
              -----------------------------------------------"""
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/**/*.yaml'
        }
        success {
          setGitHubBuildStatus('server/preview', 'Deploy server preview', 'SUCCESS')
        }
        unsuccessful {
          setGitHubBuildStatus('server/preview', 'Deploy server preview', 'FAILURE')
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
        if (!isPullRequest() && env.DRY_RUN != 'true') {
          currentBuild.description = "Build ${VERSION}"
          if (!hudson.model.Result.SUCCESS.toString().equals(currentBuild.getPreviousBuild()?.getResult())) {
            slackSend(channel: "${SLACK_CHANNEL}", color: 'good', message: "Successfully built nuxeo/nuxeo ${BRANCH_NAME} #${BUILD_NUMBER}: ${BUILD_URL}")
          }
        }
      }
    }
    unsuccessful {
      script {
        if (!isPullRequest() && env.DRY_RUN != 'true') {
          slackSend(channel: "${SLACK_CHANNEL}", color: 'danger', message: "Failed to build nuxeo/nuxeo ${BRANCH_NAME} #${BUILD_NUMBER}: ${BUILD_URL}")
        }
      }
    }
  }
}
