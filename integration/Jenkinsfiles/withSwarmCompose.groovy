/**
 * run pipeline inside a docker compose stack through swarm
 */
def call(String name, String file, Closure body) {
    def config = JenkinsLocationConfiguration.get()
    def master = config.getUrl()
    def compose = "docker-compose -f $file -f ${WORKSPACE}/nuxeo/integration/Jenkinsfiles/docker-compose-swarm.yml"

    withEnv(["COMPOSE_PROJECT_NAME=$name", "JENKINS_MASTER=$master"]) {
        withCredentials([string(credentialsId: 'jenkins-api-token', variable: 'JENKINS_API_TOKEN')]) {
            try {
                sh """#!/bin/bash -ex                                                                                                                                                                                                   $compose pull                                                                                                                                                                                                       $compose build --no-cache                                                                                                                                                                                           $compose up -d --no-color --no-build                                                                                                                                                                            """
                node(name) {
                    body()
                }
            } finally {
                sh """#!/bin/bash -ex                                                                                                                                                                                                   $compose down --rmi local --volumes --remove-orphans                                                                                                                                                            """
            }
        }
    }
}

return this
