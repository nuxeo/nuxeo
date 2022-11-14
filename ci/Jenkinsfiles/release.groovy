/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
library identifier: "platform-ci-shared-library@v0.0.3"

void getCurrentVersion() {
  return readMavenPom().getVersion()
}

void getReleaseVersion(version) {
  return version.replace('-SNAPSHOT', '')
}

void getLatestVersion(version) {
  return version.split('\\.')[0];
}

void promoteDockerImage(String dockerRegistry, String imageName, String buildVersion, String releaseVersion, String latestVersion) {
  String buildImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${buildVersion}"
  String releaseImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${releaseVersion}"
  String latestImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${latestVersion}"

  nxDocker.copy(from: buildImage, tos: [releaseImage, latestImage])
}

pipeline {

  agent {
    label 'jenkins-nuxeo-platform-lts-2021'
  }

  environment {
    NUXEO_BUILD_VERSION = "${params.NUXEO_BUILD_VERSION}"
    CURRENT_VERSION = getCurrentVersion()
    RELEASE_VERSION = getReleaseVersion("${CURRENT_VERSION}")
    LATEST_VERSION = getLatestVersion("${RELEASE_VERSION}")
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -P-nexus,nexus-private'
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
    DOCKER_NAMESPACE = 'nuxeo'
    BASE_IMAGE_NAME = 'nuxeo-base'
    NUXEO_IMAGE_NAME = 'nuxeo'
    NUXEO_BENCHMARK_IMAGE_NAME = 'nuxeo-benchmark'
    SLACK_CHANNEL = 'platform-notifs'
    REFERENCE_BRANCH = '2021'
  }

  stages {

    stage('Notify promotion start on slack') {
      steps {
        script {
          nxSlack.send(color: '#0167FF', message: "Starting to release nuxeo/nuxeo-lts ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}: ${BUILD_URL}")
        }
      }
    }

    stage('Check parameters') {
      steps {
        script {
          if (NUXEO_BUILD_VERSION == '') {
            currentBuild.result = 'ABORTED';
            currentBuild.description = 'Missing required parameter BUILD_VERSION, aborting build.'
            error(currentBuild.description)
          }
          echo """
          ----------------------------------------
          Build version:   ${NUXEO_BUILD_VERSION}
          Current version: ${CURRENT_VERSION}
          Release version: ${RELEASE_VERSION}
          ----------------------------------------
          """
        }
      }
    }

    stage('Set Kubernetes labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabel()
          }
        }
      }
    }

    stage('Release') {
      steps {
        container('maven') {
          script {
            echo """
            -------------------------------------------------
            Release nuxeo-parent POM ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}
            -------------------------------------------------
            """
            sh """
              git checkout v${NUXEO_BUILD_VERSION}

              mvn ${MAVEN_ARGS} -f parent/pom.xml versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
              mvn ${MAVEN_ARGS} -f parent/pom.xml validate
            """
            nxGit.commitTagPush(version: env.RELEASE_VERSION)
          }
        }
      }
    }

    stage('Deploy nuxeo-parent POM') {
      when {
        expression { !nxUtils.isDryRun() }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy nuxeo-parent POM
          ----------------------------------------"""
          sh "mvn ${MAVEN_ARGS} -f parent/pom.xml deploy"
        }
      }
    }

    stage('Upload Nuxeo Packages') {
      when {
        expression { !nxUtils.isDryRun() }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PROD_URL}
          ----------------------------------------"""
          withCredentials([usernameColonPassword(credentialsId: 'connect-prod', variable: 'CONNECT_PASS')]) {
            sh """
              # Fetch Nuxeo packages with Maven
              mvn ${MAVEN_ARGS} -f ci/release/pom.xml process-resources

              # Upload Nuxeo packages
              PACKAGES_TO_UPLOAD="ci/release/target/packages/nuxeo-*-package-*.zip"
              for file in \$PACKAGES_TO_UPLOAD ; do
                curl --fail -i -u "$CONNECT_PASS" -F package=@\$(ls \$file) "$CONNECT_PROD_URL"/site/marketplace/upload?batch=true ;
              done
            """
          }
        }
      }
    }

    stage('Promote Docker image') {
      when {
        expression { !nxUtils.isDryRun() }
      }
      steps {
        container('maven') {
          echo """
          -----------------------------------------------
          Tag Docker images with version ${RELEASE_VERSION} and ${LATEST_VERSION}
          -----------------------------------------------
          """
          promoteDockerImage("${PRIVATE_DOCKER_REGISTRY}", "${BASE_IMAGE_NAME}", "${NUXEO_BUILD_VERSION}",
                  "${RELEASE_VERSION}", "${LATEST_VERSION}")
          promoteDockerImage("${PRIVATE_DOCKER_REGISTRY}", "${NUXEO_IMAGE_NAME}", "${NUXEO_BUILD_VERSION}",
            "${RELEASE_VERSION}", "${LATEST_VERSION}")
        }
      }
    }

    stage('Bump reference branch') {
      steps {
        container('maven') {
          script {
            sh 'git checkout ${REFERENCE_BRANCH}'
            // increment minor version
            def nextVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${CURRENT_VERSION}").trim()
            echo """
            -----------------------------------------------
            Update ${REFERENCE_BRANCH} version from ${CURRENT_VERSION} to ${nextVersion}
            -----------------------------------------------
            """
            sh """
              # root POM
              mvn ${MAVEN_ARGS} -Pdistrib,docker versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${nextVersion}</nuxeo.platform.version>|' pom.xml
              perl -i -pe 's|org.nuxeo.ecm.product.version=.*|org.nuxeo.ecm.product.version=${nextVersion}|' server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults

              # nuxeo-parent POM
              perl -i -pe 's|<version>.*?</version>|<version>${nextVersion}</version>|' parent/pom.xml

              # nuxeo-promote-packages POM
              perl -i -pe 's|<version>.*?</version>|<version>${nextVersion}</version>|' ci/release/pom.xml
            """
            nxGit.commitPush(message: "Release ${RELEASE_VERSION}, update ${CURRENT_VERSION} to ${nextVersion}", branch: env.REFERENCE_BRANCH)
          }
        }
      }
    }

    stage('Trigger downstream jobs') {
      when {
        expression { !nxUtils.isDryRun() }
      }
      parallel {
        stage('Trigger JSF UI release') {
          steps {
            script {
              def parameters = [
                string(name: 'NUXEO_BUILD_VERSION', value: "${NUXEO_BUILD_VERSION}"),
              ]
              echo """
              -----------------------------------------------------
              Trigger JSF UI release with parameters: ${parameters}
              -----------------------------------------------------
              """
              build(
                job: "nuxeo/lts/release-nuxeo-jsf-ui-2021",
                parameters: parameters,
                wait: false
              )
            }
          }
        }

        stage('Trigger Benchmark tests') {
          steps {
            script {
              def parameters = [
                string(name: 'NUXEO_BRANCH', value: "v${RELEASE_VERSION}"),
                string(name: 'NUXEO_DOCKER_IMAGE', value: "${PRIVATE_DOCKER_REGISTRY}/${DOCKER_NAMESPACE}/${NUXEO_BENCHMARK_IMAGE_NAME}:${NUXEO_BUILD_VERSION}"),
                booleanParam(name: 'INSTALL_NEEDED_PACKAGES', value: false),
              ]
              echo """
              -----------------------------------------------------
              Trigger benchmark tests with parameters: ${parameters}
              -----------------------------------------------------
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
    }

  }
  post {
    success {
      script {
        currentBuild.description = "Release ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}"
        nxSlack.success(message: "Successfully released nuxeo/nuxeo-lts ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}: ${BUILD_URL}")
      }
    }
    unsuccessful {
      script {
        nxSlack.error(message: "Failed to release nuxeo/nuxeo-lts ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}: ${BUILD_URL}")
      }
    }
  }
}
