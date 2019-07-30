/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
properties([
  [$class: 'GithubProjectProperty', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
  [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '5']],
  disableConcurrentBuilds(),
])

void setGitHubBuildStatus(String context, String message, String state) {
  step([
    $class: 'GitHubCommitStatusSetter',
    reposSource: [$class: 'ManuallyEnteredRepositorySource', url: 'https://github.com/nuxeo/nuxeo'],
    contextSource: [$class: 'ManuallyEnteredCommitContextSource', context: context],
    statusResultSource: [$class: 'ConditionalStatusResultSource', results: [[$class: 'AnyBuildResult', message: message, state: state]]],
  ])
}

pipeline {
  agent {
    label 'jenkins-nuxeo-platform-11'
  }
  environment {
    HELM_CHART_REPOSITORY_NAME = 'local-jenkins-x'
    HELM_CHART_REPOSITORY_URL = 'http://jenkins-x-chartmuseum:8080'
    HELM_CHART_NUXEO_REDIS = 'nuxeo-redis'
    HELM_RELEASE_REDIS = 'redis'
    NAMESPACE_REDIS = "nuxeo-unit-tests-redis-$BRANCH_NAME".toLowerCase()
    SERVICE_REDIS = 'redis-master'
    REDIS_HOST = "${SERVICE_REDIS}.${NAMESPACE_REDIS}.svc.cluster.local"
    SERVICE_ACCOUNT = 'jenkins'
  }
  stages {
    stage('Compile') {
      steps {
        setGitHubBuildStatus('platform/compile', 'Compile', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Compile
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -nsu -T0.8C -DskipTests install'
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/compile', 'Compile', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/compile', 'Compile', 'FAILURE')
        }
      }
    }
    stage('Run "dev" unit tests') {
      steps {
        setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Install Redis
          ----------------------------------------"""
          sh """
            # initialize Helm without installing Tiller
            helm init --client-only --service-account ${SERVICE_ACCOUNT}

            # add local chart repository
            helm repo add ${HELM_CHART_REPOSITORY_NAME} ${HELM_CHART_REPOSITORY_URL}

            # install the nuxeo-redis chart into a dedicated namespace that will be cleaned up afterwards
            # use 'jx step helm install' to avoid 'Error: incompatible versions' when running 'helm install'
            jx step helm install ${HELM_CHART_REPOSITORY_NAME}/${HELM_CHART_NUXEO_REDIS} \
              --name ${HELM_RELEASE_REDIS} \
              --namespace ${NAMESPACE_REDIS}
          """

          echo """
          ----------------------------------------
          Run "dev" unit tests
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh "mvn -B -nsu -Dnuxeo.test.redis.host=${REDIS_HOST} test"
          }
        }
      }
      post {
        always {
          junit testResults: '**/target/surefire-reports/*.xml'
          container('maven') {
            // clean up the redis namespace
            sh "kubectl delete namespace ${NAMESPACE_REDIS} --ignore-not-found=true"
          }
        }
        success {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/utests/dev', 'Unit tests - dev environment', 'FAILURE')
        }
      }
    }
    stage('Package') {
      steps {
        setGitHubBuildStatus('platform/package', 'Package', 'PENDING')
        container('maven') {
          echo """
          ----------------------------------------
          Package
          ----------------------------------------"""
          withEnv(["MAVEN_OPTS=$MAVEN_OPTS -Xms512m -Xmx3072m"]) {
            echo "MAVEN_OPTS=$MAVEN_OPTS"
            sh 'mvn -B -nsu -T0.8C -f nuxeo-distribution/pom.xml -DskipTests install'
          }
        }
      }
      post {
        success {
          setGitHubBuildStatus('platform/package', 'Package', 'SUCCESS')
        }
        failure {
          setGitHubBuildStatus('platform/package', 'Package', 'FAILURE')
        }
      }
    }
  }
  post {
    always {
      script {
        if (BRANCH_NAME == 'master') {
          // update JIRA issue
          step([$class: 'JiraIssueUpdater', issueSelector: [$class: 'DefaultIssueSelector'], scm: scm])
        }
      }
    }
  }
}
