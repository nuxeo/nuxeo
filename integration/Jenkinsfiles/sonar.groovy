#!/usr/bin/env groovy
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
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]
  ])

node('SLAVE') {
    tool name: 'ant-1.9'
    tool name: 'maven-3'
    jdk = tool name: 'java-11-openjdk'
    env.JAVA_HOME = "${jdk}"

    timeout(time: 5, unit: 'HOURS') {
        timestamps {
            try {
                stage('clone') {
                    if (BRANCH_NAME == 'master') {
                        println """Job: $JOB_DISPLAY_URL
Build: $RUN_DISPLAY_URL"""
                        checkout scm
                        sh "./clone.py"
                    } else { // Pull Request
                        println """$BRANCH_NAME: testing branch '$CHANGE_BRANCH' with parent branch '$CHANGE_TARGET'
Title: $CHANGE_TITLE
GitHub PR: $CHANGE_URL
Changes: $RUN_CHANGES_DISPLAY_URL
Job: $JOB_DISPLAY_URL
Build: $RUN_DISPLAY_URL"""
                        currentBuild.setDescription("$BRANCH_NAME<br/>$CHANGE_BRANCH -> $CHANGE_TARGET")

                        // Check remote branch existence and effective checkouted branch
                        sh "git ls-remote --exit-code git://github.com/nuxeo/nuxeo.git $CHANGE_BRANCH"
                        // "checkout scm" step restricts to PR branch
                        git branch: CHANGE_BRANCH, url: 'git://github.com/nuxeo/nuxeo.git'

                        sh """#!/bin/bash -xe
                            ./clone.py $CHANGE_BRANCH -f $CHANGE_TARGET
                            source scripts/gitfunctions.sh
                            set +x
                            shr -aq git show -s --pretty=format:'%h%d'
                            set -x
                            test `git rev-parse HEAD` = `git rev-parse $CHANGE_BRANCH`
                        """
                    }
                    sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                }
                stage('analysis') {
                    withBuildStatus("mvn verify sonar", "https://github.com/nuxeo/nuxeo", sha, RUN_DISPLAY_URL) {
                        try {
                            withEnv(['MAVEN_OPTS=-Xms4g -Xmx8g -XX:-UseGCOverheadLimit -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=$WORKSPACE/target/gcOoO.hprof']) {
                                withCredentials([usernamePassword(credentialsId: 'c4ced779-af65-4bce-9551-4e6c0e0dcfe5', passwordVariable: 'SONARCLOUD_PWD', usernameVariable: '')]) {
                                    if (BRANCH_NAME == 'master') {
                                        TARGET_OPTION=""
                                    } else {
                                        TARGET_OPTION="-Dsonar.branch.target=${CHANGE_TARGET}"
                                    }
                                    sh """#!/bin/bash -ex
                                        mvn -B -V clean verify sonar:sonar -Dsonar.login=$SONARCLOUD_PWD -Paddons,distrib,qa,sonar -Dsonar.branch.name=$BRANCH_NAME $TARGET_OPTION \
                                          -Dit.jacoco.destFile=$WORKSPACE/target/jacoco-it.exec
                                    """
                                }
                            }
                        } finally {
                            archiveArtifacts '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
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
