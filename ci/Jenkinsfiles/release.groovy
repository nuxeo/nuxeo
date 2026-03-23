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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

library identifier: "platform-ci-shared-library@v0.0.77"

void getCurrentVersion() {
  return readMavenPom().getVersion()
}

void promoteDockerImage(String dockerRegistry, String imageName, String buildVersion, String releaseVersion, String latestVersion) {
  String buildImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${buildVersion}"
  String releaseImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${releaseVersion}"
  String latestImage = "${dockerRegistry}/${DOCKER_NAMESPACE}/${imageName}:${latestVersion}"

  // the source image is multi-platform, so the manifest is a list of images
  // copy all of the images in the list and the list itself
  nxDocker.copy(from: buildImage, tos: [releaseImage, latestImage], options: '--all')
}

pipeline {

  agent {
    label 'jenkins-nuxeo-package-lts-2025'
  }

  environment {
    NUXEO_BRANCH = "${params.NUXEO_BRANCH}"
    NUXEO_BUILD_VERSION = "${params.NUXEO_BUILD_VERSION}"
    CURRENT_VERSION = getCurrentVersion()
    RELEASE_VERSION = nxUtils.getMajorDotMinorVersion(version: env.CURRENT_VERSION)
    LATEST_VERSION = nxUtils.getMajorVersion(version: env.RELEASE_VERSION)
    MAVEN_CLI_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -P-nexus,nexus-private'
    DOCKER_NAMESPACE = 'nuxeo'
    NUXEO_IMAGE_NAME = 'nuxeo'
  }

  stages {
    stage('Set Kubernetes labels') {
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabels()
          }
        }
      }
    }

    stage('Info') {
      steps {
        echo """
        ----------------------------------------
        Build version:   ${NUXEO_BUILD_VERSION}
        Current version: ${CURRENT_VERSION}
        Release version: ${RELEASE_VERSION}
        ----------------------------------------
        """
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

              mvn ${MAVEN_CLI_ARGS} -f parent/pom.xml versions:set -DnewVersion=${RELEASE_VERSION} -DgenerateBackupPoms=false
              mvn ${MAVEN_CLI_ARGS} -f parent/pom.xml validate
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
          sh "mvn ${MAVEN_CLI_ARGS} -f parent/pom.xml deploy"
        }
      }
    }

    stage('Upload Nuxeo Packages') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Upload Nuxeo Packages to ${CONNECT_PROD_SITE_URL}
          ----------------------------------------"""
          script {
            sh """
              # Fetch Nuxeo packages with Maven
              mvn ${MAVEN_CLI_ARGS} -f ci/release/pom.xml process-resources
            """
            def nxPackages = findFiles(glob: 'ci/release/target/packages/nuxeo-*-package-*.zip')
            for (nxPackage in nxPackages) {
              nxUtils.postForm(credentialsId: 'connect-prod', url: "${CONNECT_PROD_SITE_URL}marketplace/upload?batch=true",
                  form: ["package=@${nxPackage.path}"])
            }
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
          promoteDockerImage("${PRIVATE_DOCKER_REGISTRY}", "${NUXEO_IMAGE_NAME}", "${NUXEO_BUILD_VERSION}",
            "${RELEASE_VERSION}", "${LATEST_VERSION}")
        }
      }
    }

    stage('Bump reference branch') {
      steps {
        container('maven') {
          script {
            sh 'git checkout ${NUXEO_BRANCH}'
            // increment minor version
            def nextVersion = sh(returnStdout: true, script: "perl -pe 's/\\b(\\d+)(?=\\D*\$)/\$1+1/e' <<< ${CURRENT_VERSION}").trim()
            echo """
            -----------------------------------------------
            Update ${NUXEO_BRANCH} version from ${CURRENT_VERSION} to ${nextVersion}
            -----------------------------------------------
            """
            sh """
              # root POM
              mvn ${MAVEN_CLI_ARGS} -Pdistrib,docker,parent versions:set -DnewVersion=${nextVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${nextVersion}</nuxeo.platform.version>|' pom.xml
              perl -i -pe 's|org.nuxeo.ecm.product.version=.*|org.nuxeo.ecm.product.version=${nextVersion}|' server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults

              # nuxeo-promote-packages POM
              perl -i -pe 's|<version>.*?</version>|<version>${nextVersion}</version>|' ci/release/pom.xml
            """
            nxGit.commitPush(message: "Release ${RELEASE_VERSION}, update ${CURRENT_VERSION} to ${nextVersion}", branch: env.NUXEO_BRANCH)
          }
        }
      }
    }

    stage('Release Project') {
      environment {
        JIRA_PROJECT = 'NXP'
        JIRA_MOVING_VERSION = nxUtils.getMajorMovingVersion(version: env.RELEASE_VERSION)
        JIRA_RELEASE_VERSION = "${RELEASE_VERSION}"
        JIRA_NEXT_VERSION = nxUtils.getNextMajorDotMinorVersion(version: env.RELEASE_VERSION)
        RELEASE_VERSION_DASHED = env.RELEASE_VERSION.replaceAll('\\.', '-')
        RELEASE_MAJOR_VERSION = nxUtils.getMajorVersion(version: env.RELEASE_VERSION)
        RELEASE_MINOR_VERSION = nxUtils.getMinorVersion(version: env.RELEASE_VERSION)
      }
      steps {
        container('maven') {
          script {
            def jiraReleaseVersion = nxJira.getProjectVersion(idOrKey: 'NXP', name: env.JIRA_RELEASE_VERSION)
            def releaseDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // e.g. 2023-12-13 (Monday)
            def nextReleaseWeekCount = 3
            def nextReleaseDate = LocalDate.parse(jiraReleaseVersion.releaseDate).plusWeeks(nextReleaseWeekCount).format(DateTimeFormatter.ISO_LOCAL_DATE)
            def publishers = [
                [
                    type: 'github_release',
                ],
                [
                    type: 'jira_update_fixVersion',
                    toRemove: env.JIRA_MOVING_VERSION,
                    toAdd: env.JIRA_RELEASE_VERSION,
                ],
                [
                    type: 'jira_update_version',
                    id: jiraReleaseVersion.id,
                    jiraVersion: [
                        released: true,
                        releaseDate: releaseDate,
                    ]
                ],
                [
                    type: 'jira_new_version',
                    jiraVersion: [
                        project    : env.JIRA_PROJECT,
                        name       : env.JIRA_NEXT_VERSION,
                        description: "Nuxeo LTS ${JIRA_NEXT_VERSION}",
                        releaseDate: nextReleaseDate,
                        released   : false,
                    ]
                ]
            ]
            nxProject.release(jql: "project = NXP and (fixVersion = ${JIRA_MOVING_VERSION} or fixVersion = ${JIRA_RELEASE_VERSION})",
                publishers: publishers, version: env.RELEASE_VERSION)
          }
        }
      }
    }
  }

  post {
    always {
      script {
        currentBuild.description = "Release ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}"
      }
    }
    unsuccessful {
      script {
        nxUtils.callIfBuildRecoverOrFail({
          nxTeams.success(
            subtitle: null,
            message: "Successfully released nuxeo/nuxeo-lts ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}",
            changes: true,
          )}, {
          nxTeams.error(
            subtitle: null,
            message: "Failed to release nuxeo/nuxeo-lts ${RELEASE_VERSION} from build ${NUXEO_BUILD_VERSION}",
            changes: true,
            culprits: true,
          )}
        )
      }
    }
  }
}
