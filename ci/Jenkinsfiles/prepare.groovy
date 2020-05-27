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

String getMajorVersion(version) {
  // 11.1-SNAPSHOT -> 11
  // 12.0-SNAPSHOT -> 12
  return version.tokenize('.')[0]
}

String getMaintenanceBranch(String majorVersion) {
  return majorVersion + '.x'
}

String getNewVersion(String majorVersion) {
  def newMajorVersion = majorVersion.toInteger() + 1
  return newMajorVersion + '.0-SNAPSHOT'
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-11'
  }
  environment {
    VERSION = readMavenPom().getVersion()
    MAJOR_VERSION = getMajorVersion("${VERSION}")
  }

  stages {
    stage('Check branch') {
      steps {
        script {
          if (GIT_BRANCH.tokenize('/')[1] != 'master') {
            currentBuild.result = 'ABORTED'
            currentBuild.description = "Can only be run on the master branch, current branch: ${GIT_BRANCH}."
            error(currentBuild.description)
          }
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

    stage('Create maintenance branch') {
      steps {
        container('maven') {
          script {
            def maintenanceBranch = getMaintenanceBranch("${MAJOR_VERSION}")
            echo """
            -----------------------------------------------
            Create maintenance branch: ${maintenanceBranch}
            -----------------------------------------------
            """
            sh "git checkout -b ${maintenanceBranch}"
          }
        }
      }
    }

    stage('Update master version') {
      steps {
        container('maven') {
          script {
            def newVersion = getNewVersion("${MAJOR_VERSION}")
            echo """
            --------------------------------------------------------------
            Update master version from ${VERSION} to ${newVersion}
            --------------------------------------------------------------
            """
            sh """
              git checkout master

              mvn -B -nsu -Dnuxeo.skip.enforcer=true -Pdistrib,docker versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false
              perl -i -pe 's|<nuxeo.platform.version>.*?</nuxeo.platform.version>|<nuxeo.platform.version>${newVersion}</nuxeo.platform.version>|' pom.xml
              perl -i -pe 's|org.nuxeo.ecm.product.version=.*|org.nuxeo.ecm.product.version=${newVersion}|' server/nuxeo-nxr-server/src/main/resources/templates/nuxeo.defaults

              git add .
              git commit -m "Prepare release ${MAJOR_VERSION}, update ${VERSION} to ${newVersion}"
            """
          }
        }
      }
    }

    stage('Git push') {
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Git push
          ----------------------------------------
          """
          sh """
            jx step git credentials
            git config credential.helper store

            git push --all origin
          """
        }
      }
    }
  }

  post {
    always {
      script {
        if (env.DRY_RUN != "true") {
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
    success {
      script {
        if (env.DRY_RUN != "true") {
          currentBuild.description = "Prepare release ${MAJOR_VERSION}"
        }
      }
    }
  }
}
