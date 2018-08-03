/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
    disableConcurrentBuilds(),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'BRANCH'],
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'PARENT_BRANCH']],
        [$class: 'BooleanParameterDefinition', defaultValue: 'true', description: '', name: 'REBASE'],
        [$class: 'BooleanParameterDefinition', defaultValue: 'true', description: '', name: 'CLEAN'],
        [$class: 'StringParameterDefinition', defaultValue: 'SLAVE', description: '', name: 'NODELABEL'],
        [$class: 'StringParameterDefinition', defaultValue: 'nuxeo-server-tomcat-*.zip', description: '', name: 'ZIPFILTER']
    ]
  ])

def nodelabel = getBinding().hasVariable("NODELABEL")?NODELABEL:'SLAVE'
def zipfilter = getBinding().hasVariable("ZIPFILTER")?ZIPFILTER:'nuxeo-server-tomcat-*.zip'

currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH, DB: $DBPROFILE, VERSION: $DBVERSION")

node(nodelabel) {
    tool name: 'ant-1.9', type: 'ant'
    tool name: 'java-8-openjdk', type: 'hudson.model.JDK'
    tool name: 'maven-3', type: 'hudson.tasks.Maven$MavenInstallation'

    timestamps {
        def sha = stage('clone') {
            if (params.CLEAN) {
                deleteDir()
            }
            checkout(
                [$class: 'GitSCM',
                 branches: [[name: '*/${BRANCH}']],
                 browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                 doGenerateSubmoduleConfigurations: false,
                 extensions: [
                        [$class: 'PathRestriction', excludedRegions: '', includedRegions: '''nuxeo-distribution/.*
integration/.*'''],
                        [$class: 'SparseCheckoutPaths', sparseCheckoutPaths: [[path: 'pom.xml'], [path: 'nuxeo-distribution'], [path: 'integration']]],
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
                'cmis' : emitVerifyClosure(nodelabel, sha, zipfile, 'cmis', 'nuxeo-server-cmis-tests') {
                    archive 'nuxeo-distribution/nuxeo-server-cmis-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.png, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.json, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/*.log, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/log/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*'
                    failOnServerError('nuxeo-distribution/nuxeo-server-cmis-tests/target/tomcat/log/server.log')
                },
                'webdriver' : emitVerifyClosure(nodelabel, sha, zipfile, 'webdriver', 'nuxeo-jsf-ui-webdriver-tests') {
                    archive 'nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.png, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.json, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/*.log, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/log/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/results/*/*'
                    junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                    failOnServerError('nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/tomcat/log/server.log')
                }
            )
        } catch (Throwable error) {
            println '---- DEBUG catched Throwable -----'
            println error
            println '----'
            throw error
        } finally {
            warningsPublisher()
            claimPublisher()
        }
    }
}

/**
 * Emit the closure which will be evaluated in the parallel step for
 * verifying.
 **/
def emitVerifyClosure(String nodelabel, String sha, String zipfile, String name, String dir, Closure post) {
    return {
        node(nodelabel) {
            stage(name) {
                ws("${WORKSPACE}-${name}") {
                    unstash 'ws'
                    def mvnopts = zipfile != "" ? "-Dzip.file=${WORKSPACE}/${zipfile}" : ""
                    timeout(time: 2, unit: 'HOURS') {
                        withBuildStatus("${DBPROFILE}-${DBVERSION}/ftest/${name}", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                            withDockerCompose("${JOB_NAME}-${BUILD_NUMBER}-${name}", "integration/Jenkinsfiles/docker-compose-${DBPROFILE}-${DBVERSION}.yml", "mvn ${mvnopts} -B -f ${WORKSPACE}/nuxeo-distribution/${dir}/pom.xml -Pqa,tomcat,${DBPROFILE} clean verify", post)
                        }
                    }
                }
            }
        }
    }
}
