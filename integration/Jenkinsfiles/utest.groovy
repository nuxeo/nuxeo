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

currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH, DB: $DBPROFILE, VERSION: $DBVERSION")

node('SLAVE') {
    tool name: 'ant-1.9', type: 'ant'
    tool name: 'java-8-openjdk', type: 'hudson.model.JDK'
    tool name: 'maven-3', type: 'hudson.tasks.Maven$MavenInstallation'

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
                            [$class: 'CleanBeforeCheckout'],
                            [$class: 'CloneOption', depth: 5, noTags: true, reference: '', shallow: true]
                        ],
                        submoduleCfg: [],
                        userRemoteConfigs: [[url: 'git@github.com:nuxeo/nuxeo.git']]
                    ])
                sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

                sh """#!/bin/bash -xe
                ./clone.py $BRANCH -f $PARENT_BRANCH
            """
            }

            stage('tests') {
                withBuildStatus("$DBPROFILE-$DBVERSION/utest", sha) {
                    withDockerCompose("$JOB_NAME-$BUILD_NUMBER", "integration/Jenkinsfiles/docker-compose-$DBPROFILE-${DBVERSION}.yml", "mvn -B -f $WORKSPACE/pom.xml install -Pqa,addons,customdb,$DBPROFILE -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT") {
                        archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                        junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                    }
                }
            }
        }
    }
}

// should be in shared library

def warningsPublisher() {
    step([
            $class: 'WarningsPublisher',
	    consoleParsers: [
                [parserName: 'Maven'],
                [parserName: 'Java Compiler (javac)']
	    ]
	])
}

def claimPublisher() {
    step([$class: 'ClaimPublisher'])
}

def withBuildStatus(String context, String sha, Closure body) {
    currentBuild.result = 'SUCCESS'
    setBuildStatus("", "PENDING", context, sha)
    try {
        body.call()
        setBuildStatus("", "SUCCESS", context, sha)
    } catch (Throwable cause) {
        setBuildStatus(cause.toString().take(140), context, "FAILURE", sha)
        throw cause
    }
}

def setBuildStatus(String message, String state, String context, String commit) {
    // edit build description using currentBuilder.setDescription API
    step([
            $class: "GitHubCommitStatusSetter",
            reposSource: [$class: "ManuallyEnteredRepositorySource", url: 'https://github.com/nuxeo/nuxeo'],
            contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
            errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
            commitShaSource: [$class: "ManuallyEnteredShaSource", sha: commit ],
            statusBackrefSource: [$class: "ManuallyEnteredBackrefSource", backref: "${BUILD_URL}flowGraphTable/"],
            statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
        ]);
}

def withDockerCompose(String name, String file, String command, Closure post) {
    withEnv(["COMPOSE_PROJECT_NAME=$name", "TESTS_COMMAND=$command"]) {
        try {
            sh """#!/bin/bash -ex
                   docker-compose -f $file pull
                   docker-compose -f $file up --no-color --build --abort-on-container-exit tests db
               """
        } finally {
            try {
                post()
            } finally {
                sh """#!/bin/bash -ex
                   docker-compose -f $file down
                """
            }
        }
    }
}
