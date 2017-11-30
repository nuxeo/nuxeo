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
 *  temporary place holder for methods candidate to integrate the
 *  nuxeo's pipeline library
 */


/**
 * see https://wiki.jenkins.io/display/JENKINS/Warnings+Plugin
 */
def warningsPublisher() {
    step([
            $class: 'WarningsPublisher',
	    consoleParsers: [
                [parserName: 'Maven'],
                [parserName: 'Java Compiler (javac)']
	    ]
	])
}

/**
 * https://wiki.jenkins.io/display/JENKINS/Claim+plugin
 */
def claimPublisher() {
    step([$class: 'ClaimPublisher'])
}

/**
 * body's execution control that propagates build statuses to github according to the result
 */
def withBuildStatus(String context, String repourl, String sha, Closure body) {
    currentBuild.result = 'SUCCESS'
    setBuildStatus("", "PENDING", context, repourl, sha)
    try {
        body.call()
        setBuildStatus("", "SUCCESS", context, repourl, sha)
    } catch (Throwable cause) {
        setBuildStatus(cause.toString().take(140), "FAILURE", context, repourl, sha)
        throw cause
    }
}


/**
 * https://wiki.jenkins.io/display/JENKINS/GitHub+Plugin
 */
def setBuildStatus(String message, String state, String context, String repourl, String sha) {
    step([
            $class: "GitHubCommitStatusSetter",
            reposSource: [$class: "ManuallyEnteredRepositorySource", url: repourl],
            contextSource: [$class: "ManuallyEnteredCommitContextSource", context: context],
            errorHandlers: [[$class: "ChangingBuildStatusErrorHandler", result: "UNSTABLE"]],
            commitShaSource: [$class: "ManuallyEnteredShaSource", sha: sha ],
            statusBackrefSource: [$class: "ManuallyEnteredBackrefSource", backref: "${BUILD_URL}"],
            statusResultSource: [$class: "ConditionalStatusResultSource", results: [[$class: "AnyBuildResult", message: message, state: state]] ]
        ]);
}

/**
 * invoke command inside a docker compose stack, executing the post body before shutting down the stack.
 */
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

return this
