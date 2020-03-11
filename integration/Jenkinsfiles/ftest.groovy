#!/usr/bin/env groovy
/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     jcarsique
 */

def zipfilter = getBinding().hasVariable("ZIPFILTER")?ZIPFILTER:'nuxeo-server-tomcat-*.zip'

currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH, DB: $DBPROFILE, VERSION: $DBVERSION")

node('SLAVE&&STATIC') { // use a static slave in order to share the workspace between docker compose
    timestamps {
        def sha = stage('clone') {
            checkout(
                [$class: 'GitSCM',
                 branches: [[name: '*/${BRANCH}']],
                 browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                 doGenerateSubmoduleConfigurations: false,
                 extensions: [
                        [$class: 'PathRestriction', excludedRegions: '', includedRegions: '''ftests/.*
integration/.*'''],
                        [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'pom.xml'], [path: 'ftests'], [path: 'integration']]],
                        [$class: 'WipeWorkspace'],
                        [$class: 'CleanBeforeCheckout'],
                        [$class: 'CloneOption', depth: 5, noTags: true, reference: '', shallow: true]
                    ],
                 submoduleCfg: [],
                 userRemoteConfigs: [[url: 'git://github.com/nuxeo/nuxeo.git']]
                ])
            return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        }
        def zipfile = stage('zipfile') {
            dir('upstream') {
                deleteDir()
            }
            if (rawBuild.copyUpstreamArtifacts(zipfilter, 'upstream') == false) {
                return ""
            }
            return findFiles(glob:'upstream/nuxeo-server-tomcat-*.zip')[0].path
        }
        stash('ws')
        try {
            parallel (
                'cmis' : emitVerifyClosure(sha, zipfile, 'cmis', 'nuxeo-server-cmis-tests') {
                    archiveArtifacts 'ftests/nuxeo-server-cmis-tests/target/**/*.log, ftests/nuxeo-server-cmis-tests/target/**/log/*, ftests/nuxeo-server-cmis-tests/target/**/nxserver/config/distribution.properties, ftests/nuxeo-server-cmis-tests/target/nxtools-reports/*'
                    failOnServerError('ftests/nuxeo-server-cmis-tests/target/tomcat/log/server.log')
                    warningsPublisher()
                }
            )
        } finally {
            claimPublisher()
        }
    }
}

/**
 * Emit the closure which will be evaluated in the parallel step for verifying.
 * <p>
 * We don't use a new agent here because docker compose will start a new jenkins slave and this allow us to share
 * the workspace between agent controlling pipeline (and doing checkout) and the docker composes.
 */
def emitVerifyClosure(String sha, String zipfile, String name, String dir, Closure post) {
    return {
        stage(name) {
            ws("${WORKSPACE}-${name}") {
                def jdk = tool name: 'java-11-openjdk'

                def timeoutHours = params.NX_TIMEOUT_HOURS ?: '3'

                unstash 'ws'
                def mvnopts = zipfile != "" ? "-Dzip.file=${WORKSPACE}/${zipfile}" : ""
                timeout(time: Integer.parseInt(timeoutHours), unit: 'HOURS') {
                    withBuildStatus("${DBPROFILE}-${DBVERSION}/ftest/${name}", 'https://github.com/nuxeo/nuxeo', sha, RUN_DISPLAY_URL) {
                        withDockerCompose("${JOB_NAME}-${BUILD_NUMBER}-${name}", "integration/Jenkinsfiles/docker-compose-${DBPROFILE}-${DBVERSION}.yml",
                            "JAVA_HOME=${jdk} mvn ${mvnopts} -B -V -f ${WORKSPACE}/ftests/${dir}/pom.xml -Pqa,tomcat,${DBPROFILE} clean verify", post)
                    }
                }
            }
        }
    }
}
