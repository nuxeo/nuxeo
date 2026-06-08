/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <antoine.taillefer@hyland.com>
 */
library identifier: "platform-ci-shared-library@v0.0.87"

String getPackageArtifacts(version) {
  def packagePoms = findFiles(glob: 'packages/nuxeo-*-package/pom.xml')
  return packagePoms.collect {
    "org.nuxeo.packages:${it.path.tokenize('/')[-2]}:${version}:zip"
  }.join(',')
}

pipeline {
  agent {
    label 'jenkins-nuxeo-package-lts-2025'
  }
  options {
    timeout(time: 1, unit: 'HOURS')
  }
  environment {
    NUXEO_BRANCH = "${params.NUXEO_BRANCH}" // 202x
    BUILD_VERSION = nxUtils.getLatestBuildVersion() // 202x.y.z
    RELEASE_VERSION = nxUtils.getLatestReleaseVersion() // 202x.(y-1)
    RELEASE_BUILD_VERSION = nxUtils.getLatestReleaseBuildVersion() // 202x.(y-1).z pointed by 202x.(y-1)
    BUILD_PACKAGE_ARTIFACTS = getPackageArtifacts(BUILD_VERSION)
    RELEASE_PACKAGE_ARTIFACTS = getPackageArtifacts(RELEASE_BUILD_VERSION)
    DOCKER_IMAGE_NAME = "nuxeo/nuxeo"
    DOCKER_IMAGE_FULLNAME = "${PRIVATE_DOCKER_REGISTRY}/${DOCKER_IMAGE_NAME}"
    JIRA_PROJECT = 'NXP'
    JIRA_MOVING_VERSION = '2025.x'
    JIRA_TEAM_BOARD_ID = '4661'
    JIRA_TEAM_NEXT_SPRINT = 'nxplatform next'
    JIRA_TEAM_TAG = 'nxplatform'
    SCAN_WORKFLOW_ID = 'scan.yaml'
    DOWNLOAD_DIR = 'target'
    DOWNLOAD_PATTERN = 'grype-*'
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
    stage('Scan latest release') {
      steps {
        container('maven') {
          script {
            echo """
            ---------------------------------------------------------------------
            Scan Docker image and packages for latest release: ${RELEASE_VERSION}
            Docker image: ${DOCKER_IMAGE_FULLNAME}:${RELEASE_VERSION}
            Associated build version: ${RELEASE_BUILD_VERSION}
            Package artifacts:
              ${RELEASE_PACKAGE_ARTIFACTS.split(',').join('\n              ')}
            ---------------------------------------------------------------------
            """.stripIndent()

            // Force workflow run in staging to allow processing vulnerabilities in the next stage
            withEnv(['DRY_RUN=false']) {
              env.RELEASE_WORKFLOW_RUN_URL = nxGitHub.runAndWatchWorkflow(
                workflowId: SCAN_WORKFLOW_ID,
                branch: NUXEO_BRANCH,
                rawFields: [
                  'image-name': DOCKER_IMAGE_NAME,
                  packages: RELEASE_PACKAGE_ARTIFACTS,
                  version: RELEASE_VERSION,
                ],
                exitStatus: false,
                artifactDownload: [
                  dir: DOWNLOAD_DIR,
                  pattern: DOWNLOAD_PATTERN,
                ],
              )
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts(
            allowEmptyArchive: true,
            artifacts: "${DOWNLOAD_DIR}/${DOWNLOAD_PATTERN}/${DOWNLOAD_PATTERN}-${RELEASE_VERSION}.json"
          )
        }
      }
    }
    stage('Scan latest build') {
      steps {
        container('maven') {
          script {
            echo """
            -----------------------------------------------------------------
            Scan Docker image and packages for latest build: ${BUILD_VERSION}
            Docker image: ${DOCKER_IMAGE_FULLNAME}:${BUILD_VERSION}
            Package artifacts:
            ${BUILD_PACKAGE_ARTIFACTS.split(',').join('\n              ')}
            -----------------------------------------------------------------
            """.stripIndent()

            // Force workflow run in staging to allow processing vulnerabilities in the next stage
            withEnv(['DRY_RUN=false']) {
              env.BUILD_WORKFLOW_RUN_URL = nxGitHub.runAndWatchWorkflow(
                workflowId: SCAN_WORKFLOW_ID,
                branch: NUXEO_BRANCH,
                rawFields: [
                  'image-name': DOCKER_IMAGE_NAME,
                  packages: BUILD_PACKAGE_ARTIFACTS,
                  version: BUILD_VERSION,
                ],
                exitStatus: false,
                artifactDownload: [
                  dir: DOWNLOAD_DIR,
                  pattern: DOWNLOAD_PATTERN,
                ],
              )
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts(
            allowEmptyArchive: true,
            artifacts: "${DOWNLOAD_DIR}/${DOWNLOAD_PATTERN}/${DOWNLOAD_PATTERN}-${BUILD_VERSION}.json"
          )
        }
      }
    }
    stage('Process vulnerabilities') {
      steps {
        container('maven') {
          script {
            echo """
            ----------------------------------------------------
            Process vulnerabilities
            ----------------------------------------------------
            """.stripIndent()
            def vulnerabilities = nxProject.processVulnerabilities(
              build: [
                scanFilePattern: "${DOWNLOAD_DIR}/${DOWNLOAD_PATTERN}/${DOWNLOAD_PATTERN}-${BUILD_VERSION}.json",
                workflowRunUrl: env.BUILD_WORKFLOW_RUN_URL,
              ],
              release: [
                scanFilePattern: "${DOWNLOAD_DIR}/${DOWNLOAD_PATTERN}/${DOWNLOAD_PATTERN}-${RELEASE_VERSION}.json",
                workflowRunUrl: env.RELEASE_WORKFLOW_RUN_URL,
              ],
            )
            if (vulnerabilities) {
              env.TEAMS_NOTIFICATION_MESSAGE = vulnerabilities.message
            }
          }
        }
      }
    }
  }
  post {
    always {
      script {
        currentBuild.description = "Scan ${RELEASE_VERSION} and ${BUILD_VERSION}"
      }
    }
    success {
      script {
        if (env.TEAMS_NOTIFICATION_MESSAGE) {
          nxTeams.success(
            icon: 'Warning',
            iconColor: 'warning',
            title: "Vulnerabilities found in ${nxUtils.getRepositoryName()} ${NUXEO_BRANCH}",
            subtitle: null,
            message: env.TEAMS_NOTIFICATION_MESSAGE,
            changes: true,
          )
        }
      }
    }
    unsuccessful {
      script {
        nxTeams.error(
          subtitle: null,
          message: "Failed to scan ${nxUtils.getRepositoryName()} on the latest release (${RELEASE_VERSION}) and latest build (${BUILD_VERSION})",
          changes: true,
          culprits: true,
        )
      }
    }
  }
}
