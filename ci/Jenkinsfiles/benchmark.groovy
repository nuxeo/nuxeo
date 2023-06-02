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
library identifier: "platform-ci-shared-library@v0.0.13"

boolean isTriggeredByCron() {
  return currentBuild.getBuildCauses('org.jenkinsci.plugins.parameterizedscheduler.ParameterizedTimerTriggerCause')
}

boolean isTriggeredByNuxeoPromotion() {
  return "${params.NUXEO_BRANCH}".matches('^v(\\d){4}\\.(\\d)+$')
}

String resolveDockerImageVersion(String dockerImage) {
  container('maven') {
    // resolve the real nuxeo version if the given docker image has a moving tag such as 2021, 2021.x, 2021.28
    if (dockerImage.matches('^.*:(\\d){4}(\\.(x|\\d+))?$')) {
      return dockerImage.replaceAll(':.*', ':') + nxDocker.getLabel(image: dockerImage, label: 'org.nuxeo.version')
    }
    return dockerImage
  }
}

String getBenchmarkBenchSuite() {
  container('maven') {
    if (isTriggeredByCron()) {
      return sh(returnStdout: true, script: 'date +%yw%V').trim() + " CI Weekly Benchmark - Build ${params.NUXEO_DOCKER_IMAGE.replaceAll('.*:', '')}"
    } else if (isTriggeredByNuxeoPromotion()) {
      return "Nuxeo LTS ${params.NUXEO_BRANCH.replace('v', '')}";
    }
    return "${params.NUXEO_BRANCH}"
  }
}

String getBenchmarkCategory() {
  container('maven') {
    if (isTriggeredByCron()) {
      return 'continuous'
    } else if (isTriggeredByNuxeoPromotion()) {
      return 'milestone'
    }
    return 'workbench'
  }
}

void gatling(String parameters) {
  sh "mvn ${MAVEN_ARGS} test gatling:test -Durl=https://${BENCHMARK_NAMESPACE}.platform.dev.nuxeo.com/nuxeo -Dgatling.simulationClass=${parameters} -DredisHost=localhost -DredisPort=6379 -DredisDb=0"
}

pipeline {
  agent {
    label 'jenkins-nuxeo-benchmark-lts-2023'
  }
  options {
    timeout(time: 12, unit: 'HOURS')
  }
  environment {
    CURRENT_NAMESPACE = nxK8s.getCurrentNamespace()
    SCM_REF = nxDocker.getLabel(image: params.NUXEO_DOCKER_IMAGE, label: 'org.nuxeo.scm-ref')

    BRANCH_NAME = "${params.NUXEO_BRANCH.replaceAll('/', '-')}"
    NUXEO_DOCKER_IMAGE_WITH_VERSION = resolveDockerImageVersion("${params.NUXEO_DOCKER_IMAGE}")
    INSTALL_NEEDED_PACKAGES = "${params.INSTALL_NEEDED_PACKAGES}"
    NX_REPLICA_COUNT = "${params.NUXEO_NB_APP_NODE.toInteger()}"
    NX_WORKER_COUNT = "${params.NUXEO_NB_WORKER_NODE.toInteger()}"

    // always read AWS credentials from secret in the platform namespace, even when running in platform-staging:
    // credentials rotation is disabled in platform-staging to prevent double rotation on the same keys
    AWS_ACCESS_KEY_ID = nxK8s.getSecretData(namespace: 'platform', name: 'aws-credentials', key: 'access_key_id')
    AWS_SECRET_ACCESS_KEY = nxK8s.getSecretData(namespace: 'platform', name: 'aws-credentials', key: 'secret_access_key')
    AWS_REGION = 'eu-west-3'
    BENCHMARK_BENCH_SUITE = getBenchmarkBenchSuite()
    BENCHMARK_BUILD_NUMBER = "${CURRENT_NAMESPACE}-${BUILD_NUMBER}"
    BENCHMARK_CATEGORY = getBenchmarkCategory()
    BENCHMARK_NAMESPACE = "${CURRENT_NAMESPACE}-benchmark"
    SERVICE_TAG = "benchmark-${BUILD_NUMBER}"
    BENCHMARK_NB_DOCS = '100000'
    HELMFILE_COMMAND = "helmfile --file ci/helm/helmfile.yaml --helm-binary /usr/bin/helm3"
    MAVEN_ARGS = '-B -nsu -P-nexus,nexus-private,bench -Dnuxeo.bench.itests=false'
    NUXEO_DOCKER_IMAGE = "${NUXEO_DOCKER_IMAGE_WITH_VERSION.replaceAll(':.*', '')}"
    DATA_ARTIFACT_GROUP = "content.org.nuxeo.tools.testing"
    DATA_ARTIFACT_ID = "data-test-les-arbres-redis-1.1.gz"
    DATA_ARTIFACT_VERSION = "1.1"
    DATA_ARTIFACT_TYPE = "gz"
    DATA_ARTIFACT = "${DATA_ARTIFACT_ID}-${DATA_ARTIFACT_VERSION}.${DATA_ARTIFACT_TYPE}"
    DATA_ARTIFACT_FULL_NAME = "${DATA_ARTIFACT_GROUP}:${DATA_ARTIFACT_ID}:${DATA_ARTIFACT_VERSION}:${DATA_ARTIFACT_TYPE}"
    GATLING_TESTS_PATH = "${WORKSPACE}/ftests/nuxeo-server-gatling-tests"
    REPORT_PATH = "${GATLING_TESTS_PATH}/target/reports"
    VERSION = "${NUXEO_DOCKER_IMAGE_WITH_VERSION.replaceAll('.*:', '')}"
  }

  stages {

    stage('Set labels') {
      steps {
        container('maven') {
          script {
            nxGitHub.setStatus(context: 'benchmark/tests', message: 'Benchmark tests', state: 'PENDING', commitSha: env.SCM_REF)
            nxK8s.setPodLabel()
          }
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
          script {
            echo "Download data..."
            nxMvn.copy(artifact: "${DATA_ARTIFACT_FULL_NAME}", outputDirectory: '/tmp')
            echo "Loading data into Redis..."
            sh "gunzip -c /tmp/${DATA_ARTIFACT} | nc -N localhost 6379 > /dev/null"
          }
        }
      }
    }

    stage("Run Gatling tests") {
      steps {
        container('maven') {
          echo """
          ----------------------------------------
          Deploy Benchmark environment
            - Nuxeo Docker Image : ${NUXEO_DOCKER_IMAGE}
            - Nuxeo Docker Tag : ${VERSION}
          ----------------------------------------
          """
          nxWithHelmfileDeployment(namespace: "${BENCHMARK_NAMESPACE}", environment: 'benchmark',
              secrets: [[name: 'platform-tls', namespace: 'platform'], [name: 'instance-clid', namespace: 'platform']],
              envVars: ["BUCKET_PREFIX=benchmark-tests-${BRANCH_NAME}-BUILD-${BUILD_NUMBER}/"]) {
            script {
              try {
                dir("${GATLING_TESTS_PATH}") {
                  gatling('org.nuxeo.cap.bench.Sim00Setup')
                  gatling("org.nuxeo.cap.bench.Sim10MassStreamImport -DnbNodes=${BENCHMARK_NB_DOCS}")
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
                  gatling("org.nuxeo.cap.bench.Sim90Cleanup")
                  gatling("org.nuxeo.cap.bench.Sim90FullGC")
                }
              } finally {
                // archiveArtifacts doesn't support absolute path so do not use GATLING_TESTS_PATH
                archiveArtifacts allowEmptyArchive: true, artifacts: 'ftests/nuxeo-server-gatling-tests/target/gatling/**/*'
                if (params.DEBUG.toBoolean()) {
                  echo """
                  ----------------------------------------
                  DEBUG mode sleeping 1h for manual intervention ....
                  ----------------------------------------
                  """
                  // Can be resumed by killing manually the sleep process on maven container
                  sh 'sleep 3600 || exit 0'
                }
              }
            }
          }
        }
      }
    }

    stage("Compute reports") {
      environment {
        GAT_REPORT_ARTIFACT_GROUP = 'org.nuxeo.tools'
        GAT_REPORT_ARTIFACT_ID = 'gatling-report'
        GAT_REPORT_ARTIFACT_VERSION = '6.1'
        GAT_REPORT_ARTIFACT_TYPE = 'jar'
        GAT_REPORT_ARTIFACT_CLASSIFIER = 'capsule-fat'
        GAT_REPORT_ARTIFACT = "${GAT_REPORT_ARTIFACT_ID}-${GAT_REPORT_ARTIFACT_VERSION}-${GAT_REPORT_ARTIFACT_CLASSIFIER}.${GAT_REPORT_ARTIFACT_TYPE}"
        GAT_REPORT_ARTIFACT_DIR = "${GATLING_TESTS_PATH}/target"
        GAT_REPORT_ARTIFACT_FULL_NAME = "${GAT_REPORT_ARTIFACT_GROUP}:${GAT_REPORT_ARTIFACT_ID}:${GAT_REPORT_ARTIFACT_VERSION}:${GAT_REPORT_ARTIFACT_TYPE}:${GAT_REPORT_ARTIFACT_CLASSIFIER}"

        JAVA_MODULES_ARGLINE = '--add-exports=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED --add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED'
        MUSTACHE_TEMPLATE = "${GATLING_TESTS_PATH}/target/report-template.mustache"
      }
      steps {
        container('maven') {
          dir("${GATLING_TESTS_PATH}") {
            script {
              // download gatling tools
              nxMvn.copy(artifact: GAT_REPORT_ARTIFACT_FULL_NAME, outputDirectory: GAT_REPORT_ARTIFACT_DIR)
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
              sh 'java ${JAVA_MODULES_ARGLINE} -jar ${GAT_REPORT_ARTIFACT_DIR}/${GAT_REPORT_ARTIFACT} -f -o ${REPORT_PATH} -n data.yml -t ${MUSTACHE_TEMPLATE} ' +
                  '-m import,bulk,mbulk,exportcsv,create,createasync,nav,search,update,updateasync,bench,crud,crudasync,reindex,cleanup,fullgc ' +
                  '${REPORT_PATH}/sim10massstreamimport/detail/simulation.log.gz ' +
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
                  '${REPORT_PATH}/sim80reindexall/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim90cleanup/detail/simulation.log.gz ' +
                  '${REPORT_PATH}/sim90fullgc/detail/simulation.log.gz'

              sh "echo >> ${REPORT_PATH}/data.yml"
              sh "echo 'build_number: ${BENCHMARK_BUILD_NUMBER}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'service: ${SERVICE_TAG}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'docker_image: ${NUXEO_DOCKER_IMAGE_WITH_VERSION}' >> ${REPORT_PATH}/data.yml"
              sh "echo 'build_url: \"${BUILD_URL}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'job_name: \"${JOB_NAME}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'dbprofile: \"mongodb\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'bench_suite: \"${BENCHMARK_BENCH_SUITE}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo \"nuxeonodes: \$(( \${NX_REPLICA_COUNT} + \${NX_WORKER_COUNT} ))\" >> ${REPORT_PATH}/data.yml"
              sh "echo 'esnodes: 1' >> ${REPORT_PATH}/data.yml"
              sh "echo 'classifier: \"\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'distribution: \"${VERSION}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'default_category: \"${BENCHMARK_CATEGORY}\"' >> ${REPORT_PATH}/data.yml"
              sh "echo 'kafka: true' >> ${REPORT_PATH}/data.yml"
              sh "echo 'import_docs: ${BENCHMARK_NB_DOCS}' >> ${REPORT_PATH}/data.yml"
              // Calculate benchmark duration between import and fullgc
              sh """
                d1=\$(grep import_date ${REPORT_PATH}/data.yml| sed -e 's,^[a-z\\_]*\\:\\s,,g');
                d2=\$(grep fullgc_date ${REPORT_PATH}/data.yml | sed -e 's,^[a-z\\_]*\\:\\s,,g');
                dd=\$(grep fullgc_duration ${REPORT_PATH}/data.yml | sed -e 's,^[a-z\\_]*\\:\\s,,g');
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
              // Get versions GC info from redis
              sh """
                versions_total=\$(echo -e 'get versionsTotal\nquit\n' | nc localhost 6379 | grep -o '^[[:digit:]]*');
                echo \"versions_total: \$versions_total\" >> ${REPORT_PATH}/data.yml
              """
              sh """
                versions_retained=\$(echo -e 'get versionsRetained\nquit\n' | nc localhost 6379 | grep -o '^[[:digit:]]*');
                echo \"versions_retained: \$versions_retained\" >> ${REPORT_PATH}/data.yml
              """
              sh "echo >> ${REPORT_PATH}/data.yml"
              // Get binary GC info from redis
              sh """
                binaries_total=\$(echo -e 'get binariesTotal\nquit\n' | nc localhost 6379 | grep -o '^[[:digit:]]*');
                echo \"binaries_total: \$binaries_total\" >> ${REPORT_PATH}/data.yml
              """
              sh """
                binaries_retained=\$(echo -e 'get binariesRetained\nquit\n' | nc localhost 6379 | grep -o '^[[:digit:]]*');
                echo \"binaries_retained: \$binaries_retained\" >> ${REPORT_PATH}/data.yml
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
        expression { !nxUtils.isDryRun() }
      }
      steps {
        container('benchmark') {
          script {
            nxGit.cloneRepository(name: "${BENCH_SITE_REPO}", branch: 'master')
            dir("${BENCH_SITE_REPO}") {
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

  post {
    success {
      script {
        nxGitHub.setStatus(context: 'benchmark/tests', message: 'Benchmark tests', state: 'SUCCESS', commitSha: env.SCM_REF)
        nxGitHub.commentPullRequest(branch: params.NUXEO_BRANCH, body: "The Benchmark tests has succeeded to run on ${SCM_REF}.\nThe results are located there: https://benchmarks.nuxeo.com/${BENCHMARK_CATEGORY}/${BENCHMARK_BUILD_NUMBER}/index.html")
      }
    }
    unsuccessful {
      script {
        nxGitHub.setStatus(context: 'benchmark/tests', message: 'Benchmark tests', state: 'FAILURE', commitSha: env.SCM_REF)
      }
    }
  }
}
