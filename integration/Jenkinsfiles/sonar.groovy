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

 node('SLAVE') {
    tool type: 'ant', name: 'ant-1.9'
    tool type: 'hudson.model.JDK', name: 'java-8-oracle'
    tool type: 'hudson.tasks.Maven$MavenInstallation', name: 'maven-3'
    timeout(time: 5, unit: 'HOURS') {
        timestamps {
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
                shared = load("$WORKSPACE/integration/Jenkinsfiles/shared.groovy")
                sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
            }
            try {
                stage('analysis') {
                    shared.withBuildStatus("sonarqube", sha) {
                        withEnv(['MAVEN_OPTS=-Xmx6g -server']) {
                            withCredentials([usernamePassword(credentialsId: 'c4ced779-af65-4bce-9551-4e6c0e0dcfe5', passwordVariable: 'SONARCLOUD_PWD', usernameVariable: '')]) {
                                sh """#!/bin/bash -ex
                                    mvn clean install org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.login=$SONARCLOUD_PWD -Dit.jacoco.destFile=$WORKSPACE/target/jacoco-it.dump -Paddons,distrib,qa,sonar
                                """
                            }
                        }
                    }
                }
            } finally {
                archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                junit testDataPublishers: [[$class: 'ClaimTestDataPublisher']], '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                shared.warningsPublisher()
                shared.claimPublisher()
            }
        }
    }
}
