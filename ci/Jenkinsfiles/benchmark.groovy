/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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

repositoryUrl = 'https://github.com/nuxeo/nuxeo-lts'

properties([
  [$class: 'GithubProjectProperty', projectUrlStr: repositoryUrl],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  if (env.DRY_RUN != "true") {
    step([
      $class: 'GitHubCommitStatusSetter',
      reposSource: [$class: 'ManuallyEnteredRepositorySource', url: repositoryUrl],
      contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
      statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
    ])
  }
}

String getCurrentNamespace() {
  container('maven') {
    return sh(returnStdout: true, script: "kubectl get pod ${NODE_NAME} -ojsonpath='{..namespace}'")
  }
}

void helmfileTemplate(namespace, environment, outputDir) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} deps
      ${HELMFILE_COMMAND} --environment ${environment} template --output-dir ${outputDir}
    """
  }
}

void helmfileSync(namespace, environment) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} deps
      ${HELMFILE_COMMAND} --environment ${environment} sync
    """
  }
}

void helmfileDestroy(namespace, environment) {
  withEnv(["NAMESPACE=${namespace}"]) {
    sh """
      ${HELMFILE_COMMAND} --environment ${environment} destroy
    """
  }
}

void copySecret(String secretName) {
  sh """
    kubectl --namespace=platform get secret ${secretName} -oyaml | \
    sed 's/namespace: platform/namespace: ${BENCHMARK_NAMESPACE}/g' | \
    kubectl --namespace=${BENCHMARK_NAMESPACE} apply -f -
  """
}

void gatling(String parameters) {
  sh "mvn ${MAVEN_ARGS} test gatling:test -Durl=https://${BENCHMARK_NAMESPACE}.platform.dev.nuxeo.com/nuxeo -Dgatling.simulationClass=${parameters} -DredisHost=localhost -DredisPort=6379 -DredisDb=0"
}

pipeline {
  agent {
    label 'jenkins-nuxeo-benchmark-lts-2021'
  }
  options {
    timeout(time: 12, unit: 'HOURS')
  }
  environment {
    CURRENT_NAMESPACE = getCurrentNamespace()

    AWS_CREDENTIALS_SECRET = 'aws-credentials'
    AWS_REGION = 'eu-west-3'
    BENCHMARK_NAMESPACE = "${CURRENT_NAMESPACE}-benchmark"
    BENCHMARK_NB_DOCS = '100000'
    BRANCH_NAME = "${params.NUXEO_BRANCH}"
    HELMFILE_COMMAND = "helmfile --file ci/helm/helmfile.yaml --helm-binary /usr/bin/helm3"
    MAVEN_ARGS = '-B -nsu -P-nexus,nexus-private,bench -Dnuxeo.bench.itests=false'
    VERSION = "${params.NUXEO_BUILD_VERSION}"
    DATA_URL = "https://maven-eu.nuxeo.org/nexus/service/local/repositories/vendor-releases/content/content/org/nuxeo/tools/testing/data-test-les-arbres-redis-1.1.gz/1.1/data-test-les-arbres-redis-1.1.gz-1.1.gz"
    NX_REPLICA_COUNT = "${params.NUXEO_NB_APP_NODE.toInteger()}"
    NX_WORKER_COUNT = "${params.NUXEO_NB_WORKER_NODE.toInteger()}"
  }

  stages {
    stage('Set labels') {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Set Kubernetes resource labels
          ----------------------------------------
          """
          echo "Set label 'branch: ${BRANCH_NAME}' on pod ${NODE_NAME}"
          sh """
            kubectl label pods ${NODE_NAME} branch=${BRANCH_NAME}
          """
        }
      }
    }

    stage("Prepare data") {
      steps {
        container('maven') {
          echo """
            ----------------------------------------
            Load benchmark data into Redis
            ----------------------------------------
            """
          echo "Download data..."
          sh "curl -o /tmp/data.gz ${DATA_URL}"
          echo "Loading data into Redis..."
          sh "gunzip -c /tmp/data.gz | nc localhost 6379 > /dev/null"
        }
      }
    }

    stage("Run Gatling tests") {
      steps {
        container('maven') {
          script {
            try {
              // TODO correct Docker Registry when integrating with Nuxeo Build
              echo """
              ----------------------------------------
              Deploy Benchmark environment
                - Nuxeo Docker Image Version: ${VERSION}
                - Docker Registry: docker-private.packages.nuxeo.com
              ----------------------------------------
              """
              sh "kubectl create namespace ${BENCHMARK_NAMESPACE}"
              echo 'Copy required secrets to deploy the stack'
              copySecret("instance-clid")
              copySecret("kubernetes-docker-cfg")
              copySecret("platform-cluster-tls")

              def awsCredentialsNamespace = 'platform'
              def awsAccessKeyId = sh(
                  script: "kubectl get secret ${AWS_CREDENTIALS_SECRET} -n ${awsCredentialsNamespace} -o=jsonpath='{.data.access_key_id}' | base64 --decode",
                  returnStdout: true
              )
              def awsSecretAccessKey = sh(
                  script: "kubectl get secret ${AWS_CREDENTIALS_SECRET} -n ${awsCredentialsNamespace} -o=jsonpath='{.data.secret_access_key}' | base64 --decode",
                  returnStdout: true
              )
              withEnv([
                  "AWS_ACCESS_KEY_ID=${awsAccessKeyId}",
                  "AWS_SECRET_ACCESS_KEY=${awsSecretAccessKey}",
                  "BUCKET_PREFIX=benchmark-tests-${BRANCH_NAME}-BUILD-${BUILD_NUMBER}/",
                  "DOCKER_REGISTRY=docker-private.packages.nuxeo.com", // TODO to remove when integrating into Nuxeo build
              ]) {
                helmfileSync("${BENCHMARK_NAMESPACE}", "benchmark")
              }
              dir('ftests/nuxeo-server-gatling-tests') {
                gatling('org.nuxeo.cap.bench.Sim00Setup')
                gatling("org.nuxeo.cap.bench.Sim10MassImport -DnbNodes=${BENCHMARK_NB_DOCS}")
                gatling("org.nuxeo.cap.bench.Sim20CSVExport")
                gatling("org.nuxeo.cap.bench.Sim15BulkUpdateDocuments")
                gatling("org.nuxeo.cap.bench.Sim10CreateFolders")
                gatling("org.nuxeo.cap.bench.Sim20CreateDocuments -Dusers=32")
                gatling("org.nuxeo.cap.bench.Sim25WaitForAsync")
                gatling("org.nuxeo.cap.bench.Sim25BulkUpdateFolders -Dusers=32 -Dduration=180 -Dpause_ms=0")
                gatling("org.nuxeo.cap.bench.Sim30UpdateDocuments -Dusers=32 -Dduration=180")
                gatling("org.nuxeo.cap.bench.Sim35WaitForAsync")
                gatling("org.nuxeo.cap.bench.Sim30Navigation -Dusers=48 -Dduration=180")
                gatling("org.nuxeo.cap.bench.Sim30Search -Dusers=48 -Dduration=180")
                gatling("org.nuxeo.cap.bench.Sim50Bench -Dnav.users=80 -Dupd.user=15 -Dduration=180")
                gatling("org.nuxeo.cap.bench.Sim50CRUD -Dusers=32 -Dduration=120")
                gatling("org.nuxeo.cap.bench.Sim55WaitForAsync")
                gatling("org.nuxeo.cap.bench.Sim80ReindexAll")
              }
            } catch (err) {
              echo "Gatling tests error: ${err}"
              throw err
            } finally {
              try {
                archiveArtifacts allowEmptyArchive: true, artifacts: "ftests/nuxeo-server-gatling-tests/target/gatling/**/*"
                if (params.DEBUG.toBoolean()) {
                  echo """
                  ----------------------------------------
                  DEBUG mode sleeping 1h for manual intervention ....
                  ----------------------------------------
                  """
                  sleep time: 1, unit: "HOURS"
                }
              } finally {
                echo "Gatling tests: clean up benchmark namespace"
                try {
                  helmfileDestroy("${BENCHMARK_NAMESPACE}", "benchmark")
                } finally {
                  sh "kubectl delete namespace ${BENCHMARK_NAMESPACE} --ignore-not-found=true"
                }
              }
            }
          }
        }
      }
    }
  }
}
