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
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void getCurrentVersion() {
  return readMavenPom().getVersion()
}

void getReleaseVersion(version) {
  return version.replace('-SNAPSHOT', '')
}

void getLatestVersion() {
  return 'latest'
}

void dockerPull(String image) {
  sh "docker pull ${image}"
}

void dockerTag(String image, String tag) {
  sh "docker tag ${image} ${tag}"
}

void dockerPush(String image) {
  sh "docker push ${image}"
}

void promoteDockerImage(String dockerRegistry, String imageName, String buildVersion, String releaseVersion, String latestVersion) {
  String buildImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${buildVersion}"
  dockerPull(buildImage)

  String releaseImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${releaseVersion}"
  dockerTag(buildImage, releaseImage)
  dockerPush(releaseImage)

  String latestImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${latestVersion}"
  dockerTag(buildImage, latestImage)
  dockerPush(latestImage)
}

pipeline {

  agent {
    label 'jenkins-nuxeo-platform-11'
  }

  environment {
    CURRENT_VERSION = getCurrentVersion()
    RELEASE_VERSION = getReleaseVersion(CURRENT_VERSION)
    LATEST_VERSION = getLatestVersion()
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true'
    CONNECT_PROD_URL = 'https://connect.nuxeo.com/nuxeo'
    DOCKER_NAMESPACE = 'nuxeo'
    NUXEO_IMAGE_NAME = 'nuxeo'
    SLACK_CHANNEL = 'platform-notifs'
  }

  stages {

    stage('Notify promotion start on slack') {
      steps {
        script {
          if (env.DRY_RUN != 'true') {
            slackSend(channel: "${SLACK_CHANNEL}", color: '#0167FF', message: "Starting to release nuxeo/nuxeo ${RELEASE_VERSION} from build ${params.BUILD_VERSION}: ${BUILD_URL}")
          }
        }
      }
    }

    stage('Check parameters') {
      steps {
        script {
          if (params.BUILD_VERSION == '') {
            currentBuild.result = 'ABORTED';
            currentBuild.description = 'Missing required parameter BUILD_VERSION, aborting build.'
            error(currentBuild.description)
          }
          echo """
          ----------------------------------------
          Build version:   ${params.BUILD_VERSION}
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
          echo """
          ----------------------------------------
          Set Kubernetes labels
          ----------------------------------------
          """
          echo "Set label 'branch: master' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=master
          """
        }
      }
    }

    stage('Release') {
      steps {
        container('maven') {
          script {
            echo """
            -------------------------------------------------
            Release nuxeo-parent POM ${RELEASE_VERSION} from build ${params.BUILD_VERSION}
            -------------------------------------------------
            """
            sh """
              git checkout v${params.BUILD_VERSION}

              mvn ${MAVEN_ARGS} -f parent/pom.xml versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
              mvn ${MAVEN_ARGS} -f parent/pom.xml validate

              git commit -a -m "Release ${RELEASE_VERSION}"
              git tag -a v${RELEASE_VERSION} -m "Release ${RELEASE_VERSION}"
            """

            if (env.DRY_RUN != 'true') {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin v${RELEASE_VERSION}
              """
            }
          }
        }
      }
    }

    stage('Deploy nuxeo-parent POM') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
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
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
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
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          -----------------------------------------------
          Tag Docker image with version ${RELEASE_VERSION} and ${LATEST_VERSION}
          -----------------------------------------------
          """
          promoteDockerImage("${PUBLIC_DOCKER_REGISTRY}", "${NUXEO_IMAGE_NAME}", "${params.BUILD_VERSION}",
            "${RELEASE_VERSION}", "${LATEST_VERSION}")
          promoteDockerImage("${PRIVATE_DOCKER_REGISTRY}", "${NUXEO_IMAGE_NAME}", "${params.BUILD_VERSION}",
            "${RELEASE_VERSION}", "${LATEST_VERSION}")
        }
      }
    }

    stage('Bump master branch') {
      steps {
        container('maven') {
          script {
            sh 'git checkout master'
            // increment minor version
            def nextVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${CURRENT_VERSION}").trim()
            echo """
            -----------------------------------------------
            Update master version from ${CURRENT_VERSION} to ${nextVersion}
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

              git commit -a -m "Release ${RELEASE_VERSION}, update ${CURRENT_VERSION} to ${nextVersion}"
            """

            if (env.DRY_RUN != 'true') {
              sh """
                jx step git credentials
                git config credential.helper store

                git push origin master
              """
            }
          }
        }
      }
    }

  }
  post {
    success {
      script {
        if (env.DRY_RUN != 'true') {
          currentBuild.description = "Release ${RELEASE_VERSION} from build ${params.BUILD_VERSION}"
          slackSend(channel: "${SLACK_CHANNEL}", color: 'good', message: "Successfully released nuxeo/nuxeo ${RELEASE_VERSION} from build ${params.BUILD_VERSION}: ${BUILD_URL}")
        }
      }
    }
    unsuccessful {
      script {
        if (env.DRY_RUN != 'true') {
          slackSend(channel: "${SLACK_CHANNEL}", color: 'danger', message: "Failed to release nuxeo/nuxeo ${RELEASE_VERSION} from build ${params.BUILD_VERSION}: ${BUILD_URL}")
        }
      }
    }
  }
}
