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
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
library identifier: "platform-ci-shared-library@v0.0.25"

boolean isNuxeoReleaseTag() {
  return NUXEO_BRANCH =~ /^v\d+\.\d+$/
}

def getJavadocVersion(currentVersion) {
  if (isNuxeoReleaseTag()) {
    return nxUtils.getMajorVersion(version: currentVersion)
  } else {
    return nxUtils.getMajorDotMinorVersion(version: currentVersion)
  }
}

pipeline {
  agent {
    label 'jenkins-nuxeo-package-lts-2023'
  }
  environment {
    NUXEO_BRANCH = "${params.NUXEO_BRANCH}"
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -Pjavadoc'
    VERSION = nxUtils.getVersion()
    JAVADOC_VERSION = getJavadocVersion(env.VERSION)
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
    stage('Deploy Nuxeo ECM Javadoc') {
      steps {
        container('maven') {
          script {
            def commitSha = env.SCM_REF
            if (isNuxeoReleaseTag()) {
              // retrieve the promoted build sha
              commitSha = nxDocker.getLabel(image: "${PRIVATE_DOCKER_REGISTRY}/nuxeo/nuxeo:${NUXEO_BRANCH}", label: 'org.nuxeo.scm-ref')
            }
            nxWithGitHubStatus(context: 'javadoc/site-deploy', message: 'Deploy Javadoc site', commitSha: commitSha) {
              echo """
              ----------------------------------------
              Deploy Nuxeo ECM Javadoc
              ----------------------------------------
              Javadoc Version: ${JAVADOC_VERSION}"""
              sh "mvn ${MAVEN_ARGS} site"
              if (!nxUtils.isDryRun()) {
                // just deploy the aggregated site on root pom
                // overwrite previous version by setting version to the major one
                sh "mvn ${MAVEN_ARGS} site-deploy -N -Dnuxeo.platform.javadoc.version=${JAVADOC_VERSION}"
              }
            }
          }
        }
      }
    }
  }
  post {
    always {
      script {
        currentBuild.description = "Deploy ${VERSION} Javadoc"
      }
    }
    unsuccessful {
      script {
        if (isNuxeoReleaseTag()) {
          nxSlack.error(message: "Failed to deploy Nuxeo ECM ${JAVADOC_VERSION} Javadoc: ${BUILD_URL}")
        }
      }
    }
  }
}
