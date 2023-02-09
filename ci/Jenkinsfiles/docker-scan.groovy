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
 * Contributors:
 *     Antoine Taillefer <antoine.taillefer@hyland.com>
 */
library identifier: "platform-ci-shared-library@v0.0.16"

GITHUB_WORKFLOW_DOCKER_SCAN = 'docker-image-scan.yaml'
NUXEO_BRANCH = "${params.NUXEO_BRANCH}"
IMAGE_NAME = "nuxeo/nuxeo:${NUXEO_BRANCH}.x"
IMAGE_FULLNAME = "${PRIVATE_DOCKER_REGISTRY}/${IMAGE_NAME}"

def setJobNaming() {
  currentBuild.displayName = "#${currentBuild.number} (${NUXEO_BRANCH})"
}

pipeline {
  agent {
    label 'jenkins-base'
  }
  options {
    timeout(time: 1, unit: 'HOURS')
  }
  stages {
    stage('Scan Docker image') {
      steps {
        container('base') {
          script {
            setJobNaming()
            echo """
            ----------------------------------------
            Scan Docker image
            ----------------------------------------
            Image full name: ${IMAGE_FULLNAME}
            """
            nxGitHub.runAndWatchWorkflow(
              workflowId: "${GITHUB_WORKFLOW_DOCKER_SCAN}",
              branch: "${NUXEO_BRANCH}",
              rawFields: [
                internalRegistry: false,
                imageName: "${IMAGE_NAME}",
              ],
              sha: "${GIT_COMMIT}",
              exitStatus: false
            )
          }
        }
      }
    }
  }

  post {
    success {
      script {
        if (!hudson.model.Result.SUCCESS.toString().equals(currentBuild.getPreviousBuild()?.getResult())) {
          nxSlack.success(message: "Successfully scanned Nuxeo Docker image `${IMAGE_NAME}`: ${RUN_DISPLAY_URL}")
        }
      }
    }
    unsuccessful {
      script {
        if (![hudson.model.Result.ABORTED.toString(), hudson.model.Result.NOT_BUILT.toString()].contains(currentBuild.result)) {
          nxSlack.error(message: "Failed to scan Nuxeo Docker image `${IMAGE_NAME}`: ${RUN_DISPLAY_URL}")
        }
      }
    }
  }
}
