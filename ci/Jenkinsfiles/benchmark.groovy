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

String getAWSCredential(String dataKey) {
  container('maven') {
    // always read AWS credentials from secret in the platform namespace, even when running in platform-staging:
    // credentials rotation is disabled in platform-staging to prevent double rotation on the same keys
    return sh(returnStdout: true, script: "kubectl get secret aws-credentials -n platform -o=jsonpath='{.data.${dataKey}}' | base64 --decode")
  }
}

def cloneRepo(name, branch, relativePath = name) {
  checkout([$class: 'GitSCM',
    branches: [[name: branch]],
    browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/' + name],
    doGenerateSubmoduleConfigurations: false,
    extensions: [
      [$class: 'RelativeTargetDirectory', relativeTargetDir: relativePath],
      [$class: 'WipeWorkspace'],
      [$class: 'CloneOption', depth: 0, noTags: false, reference: '', shallow: false, timeout: 60],
      [$class: 'CheckoutOption', timeout: 60],
      [$class: 'LocalBranch']
    ],
    submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: 'jx-pipeline-git-github-git', url: 'https://github.com/nuxeo/' + name]]
  ])
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

    BRANCH_NAME = "${params.NUXEO_BRANCH}"
    NUXEO_DOCKER_IMAGE_WITH_VERSION = "${params.NUXEO_DOCKER_IMAGE}"
    INSTALL_NEEDED_PACKAGES = "${params.INSTALL_NEEDED_PACKAGES}"
    NX_REPLICA_COUNT = "${params.NUXEO_NB_APP_NODE.toInteger()}"
    NX_WORKER_COUNT = "${params.NUXEO_NB_WORKER_NODE.toInteger()}"

    AWS_ACCESS_KEY_ID = getAWSCredential('access_key_id')
    AWS_SECRET_ACCESS_KEY = getAWSCredential('secret_access_key')
    AWS_REGION = 'eu-west-3'
    BENCHMARK_BUILD_NUMBER = "${CURRENT_NAMESPACE}-${BUILD_NUMBER}"
    BENCHMARK_CATEGORY = 'workbench'
    BENCHMARK_NAMESPACE = "${CURRENT_NAMESPACE}-benchmark"
    SERVICE_TAG = "benchmark-${BUILD_NUMBER}"
    BENCHMARK_NB_DOCS = '100000'
    HELMFILE_COMMAND = "helmfile --file ci/helm/helmfile.yaml --helm-binary /usr/bin/helm3"
    MAVEN_ARGS = '-B -nsu -P-nexus,nexus-private,bench -Dnuxeo.bench.itests=false'
    NUXEO_DOCKER_IMAGE = "${NUXEO_DOCKER_IMAGE_WITH_VERSION.replaceAll(':.*', '')}"
    DATA_URL = "https://maven-eu.nuxeo.org/nexus/service/local/repositories/vendor-releases/content/content/org/nuxeo/tools/testing/data-test-les-arbres-redis-1.1.gz/1.1/data-test-les-arbres-redis-1.1.gz-1.1.gz"
    GATLING_TESTS_PATH = "${WORKSPACE}/ftests/nuxeo-server-gatling-tests"
    REPORT_PATH = "${GATLING_TESTS_PATH}/target/reports"
    VERSION = "${NUXEO_DOCKER_IMAGE_WITH_VERSION.replaceAll('.*:', '')}"
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
        container('benchmark') {
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
              echo """
              ----------------------------------------
              Deploy Benchmark environment
                - Nuxeo Docker Image : ${NUXEO_DOCKER_IMAGE}
                - Nuxeo Docker Tag : ${VERSION}
              ----------------------------------------
              """
              sh "kubectl create namespace ${BENCHMARK_NAMESPACE}"
              echo 'Copy required secrets to deploy the stack'
              copySecret("instance-clid")
              copySecret("kubernetes-docker-cfg")
              copySecret("platform-cluster-tls")

              withEnv([
                  "BUCKET_PREFIX=benchmark-tests-${BRANCH_NAME}-BUILD-${BUILD_NUMBER}/",
              ]) {
                helmfileSync("${BENCHMARK_NAMESPACE}", "benchmark")
              }
              dir("${GATLING_TESTS_PATH}") {
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
                // archiveArtifacts doesn't support absolute path so do not use GATLING_TESTS_PATH
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ftests/nuxeo-server-gatling-tests/target/gatling/**/*'
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

    stage("Compute reports") {
      environment {
        GAT_REPORT_VERSION = '6.1'
        GAT_REPORT_URL = "https://maven-eu.nuxeo.org/nexus/service/local/repositories/vendor-releases/content/org/nuxeo/tools/gatling-report/${GAT_REPORT_VERSION}/gatling-report-${GAT_REPORT_VERSION}-capsule-fat.jar"
        GAT_REPORT_JAR = "${GATLING_TESTS_PATH}/target/gatling-report-capsule-fat.jar"

        MUSTACHE_TEMPLATE = "${GATLING_TESTS_PATH}/target/report-template.mustache"
      }
      steps {
        container('maven') {
          dir("${GATLING_TESTS_PATH}") {
            script {
              // download gatling tools
              sh "curl -o ${GAT_REPORT_JAR} ${GAT_REPORT_URL}"
              // download mustache template
              sh "curl -o ${MUSTACHE_TEMPLATE} https://raw.githubusercontent.com/nuxeo/nuxeo-bench/master/report-templates/data.mustache"
              // prepare the report
              for (def file : sh(returnStdout: true, script: 'ls -1 target/gatling').split('\n')) {
                def filePath = "target/gatling/${file}"
                if (sh(returnStatus: true, script: "ls ${filePath}/simulation.log") == 0) {
                  def destDir = filePath.replaceAll('-.*', '')
                  sh "mkdir -p ${destDir}"
                  sh "mv ${filePath} ${destDir}/detail"
                  sh "gzip ${destDir}/detail/simulation.log"
                }
              }
              sh 'mkdir -p ${REPORT_PATH}'
              sh 'mv target/gatling/* ${REPORT_PATH}'
              // build stats
              sh 'java -jar ${GAT_REPORT_JAR} -f -o ${REPORT_PATH} -n data.yml -t ${MUSTACHE_TEMPLATE} ' +
                  '-m import,bulk,mbulk,exportcsv,create,createasync,nav,search,update,updateasync,bench,crud,crudasync,reindex ' +
                  '${REPORT_PATH}/sim10massimport/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim15bulkupdatedocuments/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim25bulkupdatefolders/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim20csvexport/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim20createdocuments/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim25waitforasync/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim30navigation/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim30search/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim30updatedocuments/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim35waitforasync/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim50bench/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim50crud/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim55waitforasync/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim80reindexall/detail/simulation.log.gz'

              sh "echo >> ${REPORT_PATH}/data.yml"
              sh "echo 'build_number: ${BENCHMARK_BUILD_NUMBER}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'service: ${SERVICE_TAG}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'docker_image: ${NUXEO_DOCKER_IMAGE_WITH_VERSION}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'build_url: \"${BUILD_URL}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'job_name: \"${JOB_NAME}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'dbprofile: \"mongodb\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'bench_suite: \"${BRANCH_NAME}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo \"nuxeonodes: \$(( \${NX_REPLICA_COUNT} + \${NX_WORKER_COUNT} ))\" >> ${REPORT_PATH}/data.yml"
              sh "echo 'esnodes: 1' >> ${REPORT_PATH}/data.yml"
              sh "echo 'classifier: \"\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'distribution: \"${VERSION}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'default_category: \"${BENCHMARK_CATEGORY}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'kafka: true' >> ${REPORT_PATH}/data.yml"
              sh "echo 'import_docs: ${BENCHMARK_NB_DOCS}' >> ${REPORT_PATH}/data.yml"
              // Calculate benchmark duration between import and reindex
              sh """
                d1=\$(grep import_date ${REPORT_PATH}/data.yml| sed -e 's,^[a-z\\_]*\\:\\s,,g');
                d2=\$(grep reindex_date ${REPORT_PATH}/data.yml | sed -e 's,^[a-z\\_]*\\:\\s,,g');
                dd=\$(grep reindex_duration ${REPORT_PATH}/data.yml | sed -e 's,^[a-z\\_]*\\:\\s,,g');
                t1=\$(date -d \"\$d1\" +%s);
                t2=\$(date -d \"\$d2\" +%s);
                benchmark_duration=\$(echo \$(( \$t2 - \$t1 + \${dd%.*} )) );
                echo \"benchmark_duration: \$benchmark_duration\" >> ${REPORT_PATH}/data.yml
              """
              // Get total documents re-indexed from redis
              sh """
                total=\$(echo -e 'get reindexTotal\nquit\n' | nc localhost 6379 | grep -o '^[[:digit:]]*');
                echo \"reindex_docs: \$total\" >> ${REPORT_PATH}/data.yml
              """
              sh "echo >> ${REPORT_PATH}/data.yml"
            }
          }
        }
      }
      post {
        always {
          // archiveArtifacts doesn't support absolute path so do not use REPORT_PATH
          archiveArtifacts allowEmptyArchive: true, artifacts: "ftests/nuxeo-server-gatling-tests/target/reports/**/*"
        }
      }
    }

    stage("Publish reports") {
      environment {
        BENCH_SITE_REPO = 'nuxeo-bench-site'
      }
      when {
        not {
          environment name: 'DRY_RUN', value: 'true'
        }
      }
      steps {
        container('benchmark') {
          cloneRepo("${BENCH_SITE_REPO}", 'master');
          dir("${BENCH_SITE_REPO}") {
            script {
              // configure git credentials & master branch
              sh """
                jx step git credentials
                git config credential.helper store
                git checkout master
                git branch --set-upstream-to=origin/master master
              """
              // move reports where add_build_to_site.sh expects them to be
              sh "mkdir -p ${GATLING_TESTS_PATH}/target/deployment/archive"
              sh "mv ${REPORT_PATH} ${GATLING_TESTS_PATH}/target/deployment/archive/"
              sh "./add_build_to_site.sh -c ${BENCHMARK_CATEGORY} ${GATLING_TESTS_PATH}/target/deployment s3://nuxeo-devtools-benchmarks-reports"
            }
          }
        }
      }
      post {
        always {
          archiveArtifacts allowEmptyArchive: true, artifacts: "nuxeo-bench-site/**/${BENCHMARK_BUILD_NUMBER}.*"
        }
        success {
          script {
            currentBuild.description = "Benchmark URL: https://benchmarks.nuxeo.com/${BENCHMARK_CATEGORY}/${BENCHMARK_BUILD_NUMBER}/index.html"
          }
        }
      }
    }
  }
}
