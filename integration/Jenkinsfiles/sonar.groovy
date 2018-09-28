/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 * SonarQube analysis for https://sonarcloud.io/dashboard?id=org.nuxeo%3Anuxeo-ecm
 */

properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
    disableConcurrentBuilds(),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
        [$class: 'StringParameterDefinition', defaultValue: 'master', name: 'BRANCH',
            description: 'Branch to build. It must exist on the root repository.'],
        [$class: 'StringParameterDefinition', defaultValue: 'master', name: 'PARENT_BRANCH',
            description: """Fallback branch (aka parent or target branch. It is the branch to use on repositories
without BRANCH. That's the branch target for the final merge."""]
    ]],
    pipelineTriggers([[$class: 'GitHubPushTrigger']])
  ])

node('SLAVE') {
    def BRANCH = params.BRANCH
    if (env.BRANCH_NAME) {
        println "Using BRANCH_NAME parameter: $env.BRANCH_NAME"
        BRANCH = env.BRANCH_NAME
    }
    def PARENT_BRANCH = params.PARENT_BRANCH
    if (env.CHANGE_TARGET) {
        println "Using CHANGE_TARGET parameter: $env.CHANGE_TARGET"
        PARENT_BRANCH = env.CHANGE_TARGET
    }
    println "Testing branch '$BRANCH' with parent branch '$PARENT_BRANCH'"
    currentBuild.setDescription("$BRANCH -> $PARENT_BRANCH")
    tool type: 'ant', name: 'ant-1.9'
    tool type: 'hudson.model.JDK', name: 'java-8-oracle'
    tool type: 'hudson.tasks.Maven$MavenInstallation', name: 'maven-3'
    timeout(time: 5, unit: 'HOURS') {
        timestamps {
            try {
                stage('clone') {
                    // Check remote branch existence and effective checkouted branch
                    sh "git ls-remote --exit-code git://github.com/nuxeo/nuxeo.git $BRANCH"
                    git branch: "$BRANCH", url: 'git://github.com/nuxeo/nuxeo.git'
                    sh """#!/bin/bash -xe
                        ./clone.py $BRANCH -f $PARENT_BRANCH
                        source scripts/gitfunctions.sh
                        shr -a git show -s --pretty=format:'%h%d'
                        test `git rev-parse HEAD` = `git rev-parse $BRANCH`
                    """
                    sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                }
                stage('analysis') {
                    withBuildStatus("mvn verify sonar", "https://github.com/nuxeo/nuxeo", sha, BUILD_URL) {
                        try {
                            withEnv(['MAVEN_OPTS=-Xms4g -Xmx8g -XX:-UseGCOverheadLimit -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$WORKSPACE/target/gcOoO.hprof']) {
                                withCredentials([usernamePassword(credentialsId: 'c4ced779-af65-4bce-9551-4e6c0e0dcfe5', passwordVariable: 'SONARCLOUD_PWD', usernameVariable: '')]) {
                                    if (BRANCH != PARENT_BRANCH) {
                                        TARGET_OPTION="-Dsonar.branch.target=${PARENT_BRANCH}"
                                    } else {
                                        TARGET_OPTION=""
                                    }
                                    sh """#!/bin/bash -ex
                                        mvn -B -V clean verify sonar:sonar -Dsonar.login=$SONARCLOUD_PWD -Paddons,distrib,qa,sonar -Dsonar.branch.name=$BRANCH $TARGET_OPTION \
                                          -Dit.jacoco.destFile=$WORKSPACE/target/jacoco-it.exec
                                    """
                                }
                            }
                        } finally {
                            archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                            junit testDataPublishers: [[$class: 'ClaimTestDataPublisher']], testResults: '**/target/surefire-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                            warningsPublisher()
                        }
                    }
                }
            } finally {
                claimPublisher()
            }
        }
    }
}
