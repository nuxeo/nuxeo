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
library identifier: "platform-ci-shared-library@v0.0.25"

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
    // set Xmx lower than pod memory limit of 3Gi, to leave some memory for javadoc command
    MAVEN_OPTS = "$MAVEN_OPTS -Xms1g -Xmx2g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
    // set Xmx/Xms to 1g for javadoc command, to avoid the pod being OOMKilled with an exit code 137
    MAVEN_ARGS = '-B -nsu -Dnuxeo.skip.enforcer=true -DadditionalJOption=-J-Xmx1g -DadditionalJOption=-J-Xms1g'
    VERSION = nxUtils.getVersion()
    // jx step helm install's --name and --namespace options require alphabetic chars to be lowercase
    PREVIEW_NAMESPACE = "nuxeo-preview-${BRANCH_NAME.toLowerCase()}"
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
            mvn ${MAVEN_ARGS} -Pdistrib,docker versions:set -DnewVersion=${VERSION} -DgenerateBackupPoms=false
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
            sh "mvn ${MAVEN_ARGS} -V -Pjavadoc -DskipTests install"
            sh "mvn ${MAVEN_ARGS} -f server/pom.xml -Pjavadoc -DskipTests install"
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
            sh "mvn ${MAVEN_ARGS} -Pjavadoc site"
          }
        }
      }
    }

    stage('Deploy Nuxeo ECM Javadoc') {
      when {
        expression { nxUtils.isPullRequest() && pullRequest.labels.contains('preview-javadoc') && !nxUtils.isDryRun() }
      }
      steps {
        container('maven') {
          nxWithGitHubStatus(context: 'javadoc/preview', message: 'Deploy Javadoc environment') {
            script {
              echo """
              ----------------------------------------
              Build Nuxeo ECM Javadoc Docker Image ${VERSION}
              ----------------------------------------
              Image tag: ${VERSION}"""
              sh "mv target/site/apidocs ci/docker/javadoc/apidocs"
              dir('ci/docker/javadoc') {
                nxDocker.build(skaffoldFile: 'skaffold.yaml')
              }

              echo """
              ----------------------------------------
              Deploy Nuxeo ECM Javadoc Environment
              ----------------------------------------
              Image tag: ${VERSION}
              Namespace: ${PREVIEW_NAMESPACE}"""
              dir('ci/helm/javadoc') {
                // first substitute environment variables in chart values
                sh """
                  mv values.yaml values.yaml.tosubst
                  envsubst < values.yaml.tosubst > values.yaml
                """
                // second create target namespace (if doesn't exist) and copy secrets to target namespace
                boolean nsExists = sh(returnStatus: true, script: "kubectl get namespace ${PREVIEW_NAMESPACE}") == 0
                if (!nsExists) {
                  sh "kubectl create namespace ${PREVIEW_NAMESPACE}"
                  nxK8s.copySecret(fromNamespace: 'platform', toNamespace: env.PREVIEW_NAMESPACE, name: 'kubernetes-docker-cfg')
                  nxK8s.copySecret(fromNamespace: 'platform', toNamespace: env.PREVIEW_NAMESPACE, name: 'platform-cluster-tls')
                }
                // third build and deploy the chart
                sh """
                  helm init --client-only --stable-repo-url=https://charts.helm.sh/stable
                  helm repo add jenkins-x https://jenkins-x-charts.github.io/v2/
                  helm dependency update .
                  APP_NAME=javadoc ORG=nuxeo jx preview \
                    --name javadoc \
                    --namespace ${PREVIEW_NAMESPACE} \
                    --source-url="${repositoryUrl}" \
                    --preview-health-timeout 15m \
                    --no-comment \
                    --verbose
                """
                host = sh(returnStdout: true, script: "kubectl get ingress --namespace=${PREVIEW_NAMESPACE} javadoc -ojsonpath='{.items[*].spec.rules[*].host}'")
                echo """
                ----------------------------------------
                Javadoc Environment available at: https://${host}
                ----------------------------------------"""
                // comment the PR if it is the first time
                if (!nsExists) {
                  pullRequest.comment("Preview Javadoc environment available [here](https://${host}).")
                }
              }
            }
          }
        }
      }
    }

  }
}
