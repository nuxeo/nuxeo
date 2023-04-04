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
library identifier: "platform-ci-shared-library@v0.0.17"

dockerNamespace = 'nuxeo'
repositoryUrl = 'https://github.com/nuxeo/nuxeo-lts'
testEnvironments = [
  'dev',
  'mongodb',
  'postgresql',
]

String getMavenArgs() {
  def args = '-B -nsu -Dnuxeo.skip.enforcer=true -P-nexus,nexus-private'
  if (!nxUtils.isPullRequest()) {
    args += ' -Prelease'
  }
  return args
}

String getMavenFailArgs() {
  return (nxUtils.isPullRequest() && pullRequest.labels.contains('failatend')) ? '--fail-at-end' : ' '
}

String getMavenJavadocArgs() {
  // set Xmx/Xms to 1g for javadoc command, to avoid the pod being OOMKilled with an exit code 137
  return nxUtils.isPullRequest() ? ' ' : '-Pjavadoc  -DadditionalJOption=-J-Xmx1g -DadditionalJOption=-J-Xms1g'
}

String getCurrentVersion() {
  return readMavenPom().getVersion();
}

void runFunctionalTests(String baseDir, String tier) {
  try {
    retry(2) {
      sh "mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} -D${tier} -f ${baseDir}/pom.xml verify"
      nxUtils.lookupText(regexp: ".*ERROR.*(?=(?:\\n.*)*\\[.*FrameworkLoader\\] Nuxeo Platform is Trying to Shut Down)",
        fileSet: "ftests/**/log/server.log")
    }
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

void dockerPushFixedVersion(String imageName) {
  String fullImageName = "${dockerNamespace}/${imageName}"
  String fixedVersionInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String latestInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${DOCKER_TAG}"

  nxDocker.copy(from: fixedVersionInternalImage, to: latestInternalImage)
}

void dockerDeploy(String dockerRegistry, String imageName) {
  String fullImageName = "${dockerNamespace}/${imageName}"
  String fixedVersionInternalImage = "${DOCKER_REGISTRY}/${fullImageName}:${VERSION}"
  String fixedVersionPublicImage = "${dockerRegistry}/${fullImageName}:${VERSION}"
  String latestPublicImage = "${dockerRegistry}/${fullImageName}:${DOCKER_TAG}"

  nxDocker.copy(from: fixedVersionInternalImage, tos: [fixedVersionPublicImage, latestPublicImage])
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
        nxWithGitHubStatus(context: "utests/${env}", message: "Unit tests - ${env} environment") {
          echo """
          ----------------------------------------
          Run ${env} unit tests
          ----------------------------------------"""
          echo "${env} unit tests: install external services"
          nxWithHelmfileDeployment(namespace: testNamespace, environment: environment) {
            script {
              try {
                echo "${env} unit tests: run Maven"
                if (isDev) {
                  // empty file required by the read-project-properties goal of the properties-maven-plugin with the
                  // customEnvironment profile
                  sh "touch ${HOME}/nuxeo-test-${env}.properties"
                } else {
                  // prepare test framework system properties
                  // prefix sample: nuxeo-lts-pr-48-3-mongodb
                  def bucketPrefix = "$GITHUB_REPO-$BRANCH_NAME-$BUILD_NUMBER-${env}".toLowerCase()
                  def testBlobProviderPrefix = "$bucketPrefix-test"
                  def otherBlobProviderPrefix = "$bucketPrefix-other"

                  sh """
                    cat ci/mvn/nuxeo-test-${env}.properties \
                      ci/mvn/nuxeo-test-elasticsearch.properties \
                      ci/mvn/nuxeo-test-s3.properties \
                      > ci/mvn/nuxeo-test-${env}.properties~gen
                    BUCKET_PREFIX=${bucketPrefix} \
                      TEST_BLOB_PROVIDER_PREFIX=${testBlobProviderPrefix} \
                      OTHER_BLOB_PROVIDER_PREFIX=${otherBlobProviderPrefix} \
                      NAMESPACE=${testNamespace} \
                      DOMAIN=${TEST_SERVICE_DOMAIN_SUFFIX} \
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
                def mvnCommand = """                 
                  mvn ${MAVEN_ARGS} ${MAVEN_FAIL_ARGS} -rf :nuxeo-core-parent \
                    -Dcustom.environment=${env} \
                    -Dcustom.environment.log.dir=target-${env} \
                    -Dnuxeo.test.core=${testCore} \
                    -Dnuxeo.test.redis.host=${redisHost} \
                    ${kafkaOptions} \
                    test
                """
                retry(2) {
                  if (isDev) {
                    sh "${mvnCommand}"
                  } else {
                    // always read AWS credentials from secret in the platform namespace, even when running in platform-staging:
                    // credentials rotation is disabled in platform-staging to prevent double rotation on the same keys
                    def awsAccessKeyId = nxK8s.getSecretData(namespace: 'platform', name: "${AWS_CREDENTIALS_SECRET}", key: 'access_key_id')
                    def awsSecretAccessKey = nxK8s.getSecretData(namespace: 'platform', name: "${AWS_CREDENTIALS_SECRET}", key: 'secret_access_key')
                    withEnv([
                      "AWS_ACCESS_KEY_ID=${awsAccessKeyId}",
                      "AWS_SECRET_ACCESS_KEY=${awsSecretAccessKey}",
                      "AWS_REGION=${AWS_REGION}",
                      "AWS_ROLE_ARN=${AWS_ROLE_ARN}"
                    ]) {
                      sh "${mvnCommand}"
                    }
                  }
                }
              } finally {
                junit allowEmptyResults: true, testResults: "**/target-${env}/surefire-reports/*.xml"
              }
            }
          }
        }
      }
    }
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-lts-2021'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: repositoryUrl)
    timeout(time: 12, unit: 'HOURS')
  }
  environment {
    // force ${HOME}=/root - for an unexplained reason, ${HOME} is resolved as /home/jenkins though sh 'env' shows HOME=/root
    HOME = '/root'
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    TEST_NAMESPACE_PREFIX = "$CURRENT_NAMESPACE-nuxeo-unit-tests-$BRANCH_NAME-$BUILD_NUMBER".toLowerCase()
    TEST_SERVICE_DOMAIN_SUFFIX = 'svc.cluster.local'
    TEST_REDIS_K8S_OBJECT = 'redis-master'
    TEST_KAFKA_K8S_OBJECT = 'kafka'
    TEST_KAFKA_PORT = '9092'
    BASE_IMAGE_NAME = 'nuxeo-base'
    NUXEO_IMAGE_NAME = 'nuxeo'
    NUXEO_BENCHMARK_IMAGE_NAME = 'nuxeo-benchmark'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms2g -Xmx3g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    MAVEN_ARGS = getMavenArgs()
    MAVEN_FAIL_ARGS = getMavenFailArgs()
    MAVEN_JAVADOC_ARGS = getMavenJavadocArgs()
    CURRENT_VERSION = getCurrentVersion()
    VERSION = nxUtils.getVersion()
    // specify VERSION because otherwise Jenkins is not able to order env var initialization inside the shared library
    DOCKER_TAG = nxUtils.getMajorMovingVersion(version: env.VERSION)
    CHANGE_BRANCH = "${env.CHANGE_BRANCH != null ? env.CHANGE_BRANCH : BRANCH_NAME}"
    CHANGE_TARGET = "${env.CHANGE_TARGET != null ? env.CHANGE_TARGET : BRANCH_NAME}"
    GITHUB_REPO = 'nuxeo-lts'
    AWS_REGION = 'eu-west-3'
    AWS_ROLE_ARN= 'arn:aws:iam::783725821734:role/nuxeo-s3directupload-role'
    AWS_CREDENTIALS_SECRET = 'aws-credentials'
    GITHUB_WORKFLOW_DOCKER_SCAN = 'docker-image-scan.yaml'
  }

  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabel()
          }
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
        expression { nxUtils.isNotPullRequestAndNotDryRun() }
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

    stage('Hotfix Protection') {
      steps {
        container('maven') {
          script {
            nxGit.cloneRepository(name: 'nuxeo-hf-protection', branch: env.CHANGE_TARGET, relativePath: 'nuxeo-patches')
          }
          dir('nuxeo-patches') {
            sh './prepare-patches'
          }
        }
      }
    }

    stage('Build') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'maven/build', message: 'Build') {
            echo """
            ----------------------------------------
            Compile
            ----------------------------------------"""
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn ${MAVEN_ARGS} ${MAVEN_JAVADOC_ARGS} -V -T4C -DskipTests install"
            sh "mvn ${MAVEN_ARGS} ${MAVEN_JAVADOC_ARGS} -f server/pom.xml -DskipTests install"
          }
        }
      }
    }

    stage('Build Docker image') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/build', message: 'Build Docker images') {
            script {
              echo """
              ----------------------------------------
              Build Docker image
              ----------------------------------------
              Image tag: ${VERSION}
              """

              dir('docker/nuxeo-base') {
                withCredentials([usernamePassword(credentialsId: 'packages.nuxeo.com-auth', usernameVariable: 'YUM_REPO_USERNAME', passwordVariable: 'YUM_REPO_PASSWORD')]) {
                  sh """
                    mkdir -p target
                    envsubst < nuxeo-private.repo > target/nuxeo-private.repo
                  """
                }
                echo "Build and push Base Docker image to internal Docker registry ${DOCKER_REGISTRY}"
                nxDocker.build(skaffoldFile: 'skaffold.yaml')
              }
              dir('docker/nuxeo') {
                echo 'Fetch locally built Nuxeo Tomcat Server with Maven'
                sh "mvn ${MAVEN_ARGS} -T4C process-resources"

                echo "Build and push Nuxeo Docker image to internal Docker registry ${DOCKER_REGISTRY}"
                nxDocker.build(skaffoldFile: 'skaffold.yaml')
              }
              echo "Build needed packages for the benchmark image"
              // build packages defined in the pom and nuxeo-packages as it is needed when processing resources
              sh """
                benchmark_packages=\$(sed -n '/<dependency>/,/<\\/dependency>/{//b;p}' docker/nuxeo-benchmark/pom.xml | grep artifactId | sed -E 's/\\s*<artifactId>(.*)<\\/artifactId>/:\\1/' |  tr '\\n' ','  | head -c -1);
                mvn ${MAVEN_ARGS} -Pdistrib -T4C -pl :nuxeo-packages,\${benchmark_packages} install
              """

              dir('docker/nuxeo-benchmark') {
                echo 'Fetch locally built Nuxeo Packages with Maven'
                sh "mvn ${MAVEN_ARGS} -T4C process-resources"

                echo "Build and push Benchmark Docker image to internal Docker registry ${DOCKER_REGISTRY}"
                nxDocker.build(skaffoldFile: 'skaffold.yaml')
              }

              if (!nxUtils.isPullRequest()) {
                dockerPushFixedVersion("${BASE_IMAGE_NAME}")
                dockerPushFixedVersion("${NUXEO_IMAGE_NAME}")
                dockerPushFixedVersion("${NUXEO_BENCHMARK_IMAGE_NAME}")
              }
            }
          }
        }
      }
    }

    stage('Test Docker image') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/test', message: 'Test Docker image') {
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
      }
    }

    stage('Scan Docker image') {
      when {
        anyOf {
          expression {
            !nxUtils.isPullRequest()
          }
          expression {
            pullRequest.labels.contains('docker-scan')
          }
          changeset "docker/**"
        }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/scan', message: 'Scan Docker image') {
            script {
              def imageName = "${dockerNamespace}/${NUXEO_IMAGE_NAME}:${VERSION}"
              echo """
              ----------------------------------------
              Scan Docker image
              ----------------------------------------
              Image full name: ${DOCKER_REGISTRY}/${imageName}
              """
              nxGitHub.runAndWatchWorkflow(
                workflowId: "${GITHUB_WORKFLOW_DOCKER_SCAN}",
                branch: "${CHANGE_BRANCH}",
                rawFields: [
                  internalRegistry: true,
                  imageName: "${imageName}",
                ],
                sha: "${GIT_COMMIT}",
                exitStatus: true
              )
            }
          }
        }
      }
    }

    stage('Trigger Benchmark tests') {
      when {
        expression { nxUtils.isPullRequest() && pullRequest.labels.contains('benchmark') }
      }
      steps {
        container('maven') {
          script {
            def parameters = [
              string(name: 'NUXEO_BRANCH', value: "${CHANGE_BRANCH}"),
              string(name: 'NUXEO_DOCKER_IMAGE', value: "${DOCKER_REGISTRY}/${dockerNamespace}/${NUXEO_BENCHMARK_IMAGE_NAME}:${VERSION}"),
              booleanParam(name: 'INSTALL_NEEDED_PACKAGES', value: false),
            ]
            echo """
            -----------------------------------------------------------
            Trigger benchmark tests with parameters: ${parameters}
            -----------------------------------------------------------
            """
            build(
                job: "nuxeo/lts/nuxeo-benchmark",
                parameters: parameters,
                wait: false
            )
          }
        }
      }
    }

    stage('Run runtime unit tests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'utests/runtime', message: 'Unit tests - runtime') {
            script {
              def testNamespace = "${TEST_NAMESPACE_PREFIX}-runtime"
              def redisHost = "${TEST_REDIS_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}"
              def kafkaHost = "${TEST_KAFKA_K8S_OBJECT}.${testNamespace}.${TEST_SERVICE_DOMAIN_SUFFIX}:${TEST_KAFKA_PORT}"
              echo """
                ----------------------------------------
                Run runtime unit tests
                ----------------------------------------"""
              echo 'runtime unit tests: install external services'
              nxWithHelmfileDeployment(namespace: testNamespace, environment: 'runtimeUnitTests') {
                try {
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
                } finally {
                  junit testResults: '**/target/surefire-reports/*.xml'
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
        container('maven') {
          nxWithGitHubStatus(context: 'utests/server', message: 'Unit tests - server') {
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
      }
    }

    stage('Build Nuxeo Packages') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'packages/build', message: 'Build Nuxeo packages') {
            echo """
            ----------------------------------------
            Package
            ----------------------------------------"""
            sh "mvn ${MAVEN_ARGS} -Dnuxeo.skip.enforcer=false -f packages/pom.xml -DskipTests install"
          }
        }
      }
    }

    stage('Run "dev" functional tests') {
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'ftests/dev', message: 'Functional tests - dev environment') {
            echo """
            ----------------------------------------
            Run "dev" functional tests
            ----------------------------------------"""
            withCredentials([string(credentialsId: 'instance-clid', variable: 'INSTANCE_CLID')]) {
              sh(script: '''#!/bin/bash +x
                echo -e "$INSTANCE_CLID" >| /tmp/instance.clid
              ''')
              withEnv(["TEST_CLID_PATH=/tmp/instance.clid"]) {
                runFunctionalTests('ftests', 'nuxeo.ftests.tier5')
                runFunctionalTests('ftests', 'nuxeo.ftests.tier6')
                runFunctionalTests('ftests', 'nuxeo.ftests.tier7')
              }
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
          if (nxUtils.isPullRequest()) {
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
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------
            Git tag and push
            ----------------------------------------
            """
            nxGit.tagPush()
          }
        }
      }
    }

    stage('Deploy Maven artifacts') {
      when {
        expression { nxUtils.isNotPullRequestAndNotDryRun() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'maven/deploy', message: 'Deploy Maven artifacts') {
            echo """
            ----------------------------------------
            Deploy Maven artifacts
            ----------------------------------------"""
            // apply Hotfix protection again since the code was unpatched and recompiled in the intermediate steps
            // otherwise, the JAR would be rebuilt from the unpatched classes in the package phase
            dir('nuxeo-patches') {
              sh './prepare-patches'
            }
            sh """
              mvn ${MAVEN_ARGS} -Pdistrib -DskipTests deploy
              mvn ${MAVEN_ARGS} -f parent/pom.xml deploy
  
              # update back nuxeo-parent version to CURRENT_VERSION version
              mvn ${MAVEN_ARGS} -f parent/pom.xml versions:set -DnewVersion=${CURRENT_VERSION} -DgenerateBackupPoms=false
              mvn ${MAVEN_ARGS} -f parent/pom.xml deploy
            """
          }
        }
      }
    }

    stage('Deploy Nuxeo Packages') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'packages/deploy', message: 'Deploy Nuxeo Packages') {
            echo """
            ----------------------------------------
            Upload Nuxeo Packages to ${CONNECT_PREPROD_SITE_URL}
            ----------------------------------------"""
            script {
              def nxPackages = findFiles(glob: 'packages/nuxeo-*-package/target/nuxeo-*-package-*.zip')
              for (nxPackage in nxPackages) {
                nxUtils.postForm(credentialsId: 'connect-preprod', url: "${CONNECT_PREPROD_SITE_URL}marketplace/upload?batch=true",
                    form: ["package=@${nxPackage.path}"])
              }
            }
          }
        }
      }
    }

    stage('Deploy Docker image') {
      when {
        expression { nxUtils.isNotPullRequestAndNotDryRun() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'docker/deploy', message: 'Deploy Docker image') {
            echo """
            ----------------------------------------
            Deploy Docker image
            ----------------------------------------
            Image tag: ${VERSION}
            """
            echo "Push Docker images to Docker registry ${PRIVATE_DOCKER_REGISTRY}"
            dockerDeploy("${PRIVATE_DOCKER_REGISTRY}", "${BASE_IMAGE_NAME}")
            dockerDeploy("${PRIVATE_DOCKER_REGISTRY}", "${NUXEO_IMAGE_NAME}")
            dockerDeploy("${PRIVATE_DOCKER_REGISTRY}", "${NUXEO_BENCHMARK_IMAGE_NAME}")
          }
        }
      }
    }

    stage('Trigger hotfix build') {
      when {
        expression { !nxUtils.isPullRequest() }
      }
      steps {
        script {
          def parameters = [
            string(name: 'NUXEO_BRANCH', value: "${BRANCH_NAME}"),
            string(name: 'NUXEO_BUILD_VERSION', value: "${VERSION}"),
          ]
          echo """
          -----------------------------------------------------------
          Trigger hotfix package build with parameters: ${parameters}
          -----------------------------------------------------------
          """
          build(
            job: "nuxeo/lts/nuxeo-hf",
            parameters: parameters,
            wait: false
          )
        }
      }
    }
  }

  post {
    always {
      script {
        nxJira.updateIssues()
      }
    }
    success {
      script {
        currentBuild.description = "Build ${VERSION}"
        if (!nxUtils.isPullRequest()
          && !hudson.model.Result.SUCCESS.toString().equals(currentBuild.getPreviousBuild()?.getResult())) {
          nxSlack.success(message: "Successfully built nuxeo/nuxeo-lts ${BRANCH_NAME} #${BUILD_NUMBER}: ${BUILD_URL}")
        }
      }
    }
    unsuccessful {
      script {
        if (!nxUtils.isPullRequest()
          && ![hudson.model.Result.ABORTED.toString(), hudson.model.Result.NOT_BUILT.toString()].contains(currentBuild.result)) {
          nxSlack.error(message: "Failed to build nuxeo/nuxeo-lts ${BRANCH_NAME} #${BUILD_NUMBER}: ${BUILD_URL}")
        }
      }
    }
  }
}
