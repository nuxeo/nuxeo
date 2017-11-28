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
        setBuildStatus(cause.toString().take(140), "FAILURE", context, sha)
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
            statusBackrefSource: [$class: "ManuallyEnteredBackrefSource", backref: "${BUILD_URL}"],
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

return this
