/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */

properties([
        [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
        disableConcurrentBuilds(),
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
        [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
                // branch hardcoded for now
                [$class: 'StringParameterDefinition', name: 'BRANCH', description: '', defaultValue: 'improvement-NXP-24951-run-with-java-11'],
                [$class: 'StringParameterDefinition', name: 'PARENT_BRANCH', description: '', defaultValue: 'master'],
                [$class: 'BooleanParameterDefinition', name: 'CLEAN', description: '', defaultValue: true],
        ]]])

node('SLAVE') {
    tool name: 'ant-1.9', type: 'ant'
    tool name: 'maven-3', type: 'hudson.tasks.Maven$MavenInstallation'


    currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH")

    timeout(time: 3, unit: 'HOURS') {
        timestamps {
            def sha
            stage('clone') {
                checkout(
                        [$class: 'GitSCM',
                         branches: [[name: '*/${BRANCH}']],
                         browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                         doGenerateSubmoduleConfigurations: false,
                         extensions: [
                                 [$class: 'WipeWorkspace'],
                                 [$class: 'CleanBeforeCheckout'],
                                 [$class: 'CloneOption', depth: 5, noTags: true, reference: '', shallow: true]
                         ],
                         submoduleCfg: [],
                         userRemoteConfigs: [[url: 'git@github.com:nuxeo/nuxeo.git']]
                        ])
                sh """#!/bin/bash -xe
                      ./clone.py $BRANCH -f $PARENT_BRANCH
                """
                sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
            }

            try {
                stage('build-java-8') {
                    jdk = tool name: 'java-8-openjdk', type: 'hudson.model.JDK'
                    withBuildStatus("java11-upgrade/build-java-8", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                        withEnv(["JAVA_HOME=${jdk}"]) {
                            try {
                                sh "mvn -B -f $WORKSPACE/pom.xml install -Pqa,addons -DskipTests"
                            } finally {
                                archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                            }
                        }
                    }
                }
            } finally {
                warningsPublisher()
                claimPublisher()
            }

            try {
                stage('ftest-java-11') {
                    jdk = tool name: 'java-11-openjdk', type: 'hudson.model.JDK'
                    withBuildStatus("java11-upgrade/ftest-java-11", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                        withEnv(["JAVA_HOME=${jdk}"]) {
                            try {
                                sh "mvn -B -f $WORKSPACE/nuxeo-distribution/pom.xml install -Pqa -Dmaven.test.failure.ignore=true"
                            } finally {
                                archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                                junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                            }
                        }
                    }
                }
            } finally {
                warningsPublisher()
                claimPublisher()
            }
        }
    }
}