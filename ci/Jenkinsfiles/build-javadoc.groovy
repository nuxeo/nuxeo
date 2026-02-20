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
 *     Thomas Roger <troger@nuxeo.com>
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
library identifier: "platform-ci-shared-library@v0.0.77"

/**
 * This pipeline is intended to be executed on Pull Requests only
 */

repositoryUrl = 'https://github.com/nuxeo/nuxeo-lts'

pipeline {
  agent {
    label 'jenkins-nuxeo-package-lts-2023'
  }
  options {
    buildDiscarder(logRotator(daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5'))
    disableConcurrentBuilds(abortPrevious: true)
    githubProjectProperty(projectUrlStr: repositoryUrl)
    timeout(time: 3, unit: 'HOURS')
  }
  environment {
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    // force ${HOME}=/root - for an unexplained reason, ${HOME} is resolved as /home/jenkins though sh 'env' shows HOME=/root
    HOME = '/root'
    MAVEN_OPTS = "$MAVEN_OPTS -Xms3g -Xmx3g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    MAVEN_CLI_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -DadditionalJOption=-J-Xmx3g -DadditionalJOption=-J-Xms3g'
    VERSION = nxUtils.getVersion()
  }

  stages {
    stage('Set labels') {
      when {
        expression { nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          script {
            nxK8s.setPodLabels()
          }
        }
      }
    }

    stage('Update version') {
      when {
        expression { nxUtils.isPullRequest() }
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
            # root POM
            mvn ${MAVEN_CLI_ARGS} -Pdistrib,docker versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
            perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${VERSION}</nuxeo.platform.version>|' pom.xml
            perl -i -pe 's|org.nuxeo.ecm.product.version=.*|org.nuxeo.ecm.product.version=${VERSION}|' server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults
          """
        }
      }
    }

    stage('Build Javadoc') {
      when {
        expression { nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'javadoc/build', message: 'Build Javadoc') {
            echo """
            ----------------------------------------
            Build Javadoc
            ----------------------------------------"""
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn ${MAVEN_CLI_ARGS} -V -Pjavadoc -DskipTests install"
            sh "mvn ${MAVEN_CLI_ARGS} -f server/pom.xml -Pjavadoc -DskipTests install"
          }
        }
      }
    }

    stage('Generate Nuxeo ECM Javadoc') {
      when {
        expression { nxUtils.isPullRequest() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'javadoc/site', message: 'Generate Javadoc site') {
            echo """
            ----------------------------------------
            Generate Nuxeo ECM Javadoc
            ----------------------------------------"""
            sh "mvn ${MAVEN_CLI_ARGS} -Pjavadoc site"
          }
        }
      }
    }
  }
}
