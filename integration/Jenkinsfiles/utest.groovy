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

properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
    disableConcurrentBuilds(),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'BRANCH'],
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'PARENT_BRANCH'],
        [$class: 'StringParameterDefinition', defaultValue: 'injvm', description: '', name: 'DBPROFILE'],
        [$class: 'StringParameterDefinition', defaultValue: 'unknown', description: '', name: 'DBVERSION'],
        [$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'REBASE'],
        [$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'CLEAN'],
        [$class: 'StringParameterDefinition', defaultValue: 'SLAVE', description: '', name: 'NODELABEL']
    ]]])


currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH, DB: $DBPROFILE, VERSION: $DBVERSION")

node(NODELABEL) {
    tool name: 'ant-1.9', type: 'ant'
    tool name: 'java-8-openjdk', type: 'hudson.model.JDK'
    tool name: 'maven-3', type: 'hudson.tasks.Maven$MavenInstallation'
    
    
    timeout(time: 3, unit: 'HOURS') {
        timestamps {
            def sha = stage('clone') {
                if (CLEAN) {
                    deleteDir()
                }
                checkout(
                    [$class: 'GitSCM',
                     branches: [[name: '*/${BRANCH}']],
                     browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                     doGenerateSubmoduleConfigurations: false,
                     extensions: [
                            [$class: 'CleanBeforeCheckout'],
                            [$class: 'CloneOption', noTags: true, reference: '', shallow: false]
                        ],
                     submoduleCfg: [],
                     userRemoteConfigs: [[url: 'git@github.com:nuxeo/nuxeo.git']]
                    ])
                def originsha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
                sh "./clone.py $BRANCH -f $PARENT_BRANCH".concat(REBASE ? " --rebase" : "")
                return originsha
            }
            
            try {
                stage('tests') {
                    withBuildStatus("$DBPROFILE-$DBVERSION/utest", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                        withBackendProfile (
                            DBPROFILE,
                            DBVERSION,
                            "mvn -B -f $WORKSPACE/pom.xml install -Pqa,addons,customdb,$DBPROFILE -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT") {
                            archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                            junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                        }
                    }
                }
            } catch (Throwable error) {
                print error
                throw error
            } finally {
                warningsPublisher()
                claimPublisher()
            } 
        }
    }
}

def withBackendProfile(String profile, String version, String cmd, Closure post) {
    if (profile == "injvm") {
        try {
            sh cmd
        } finally {
            post()
        }
    } else {
        withDockerCompose("$JOB_NAME-$BUILD_NUMBER", "integration/Jenkinsfiles/docker-compose-$DBPROFILE-${DBVERSION}.yml", cmd, post)
    }
}
