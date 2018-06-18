/**
 * run pipeline inside a docker compose stack through swarm
 */
def call(String name, String file, Closure body) {
    def config = JenkinsLocationConfiguration.get()
    def master = config.getUrl()
    def compose = {
        dir("${WORKSPACE}@tmp") {
            writeFile(file:'docker-compose-swarm.yml', text:libraryResource('docker-compose-swarm.yml'))
            dir('jenkins-slave-swarm') {
                writeFile(file:'Dockerfile', text:libraryResource('jenkins-slave-swarm/Dockerfile'))
                writeFile(file:'myinit-setup-workspace.sh', text:libraryResource('jenkins-slave-swarm/myinit-setup-workspace.sh'))
            }
        }
        return "docker-compose -f $file -f ${WORKSPACE}@tmp/docker-compose-swarm.yml"
    }.call()

    withEnv(["COMPOSE_PROJECT_NAME=$name", "SWARM=${WORKSPACE}@tmp", "JENKINS_MASTER=$master"]) {
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
