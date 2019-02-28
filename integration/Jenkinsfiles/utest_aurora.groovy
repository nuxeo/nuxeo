/*
 *  (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      atimic
 */

currentBuild.setDescription("Branch: $BRANCH -> $PARENT_BRANCH, DB: aurora-$DBPROFILE, VERSION: $DBVERSION")

node(env.NODELABEL) {
    tool name: 'ant-1.9'
    tool name: 'maven-3'
    jdk = tool name: 'java-11-openjdk'
    env.JAVA_HOME = "${jdk}"

    def timeoutHours = params.NX_TIMEOUT_HOURS == null ? '4' : params.NX_TIMEOUT_HOURS

    timeout(time: Integer.parseInt(timeoutHours), unit: 'HOURS') {
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
                         [$class: 'CloneOption', depth: 5, noTags: true, reference: '', shallow: false]
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
                stage('tests') {
                    withBuildStatus("utest/aurora-$DBPROFILE-$DBVERSION", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                        withEnv(["NX_DB_PORT=5432", "NX_DB_ADMINNAME=nuxeoAurora"]) {
                            withCredentials([usernamePassword(credentialsId: 'AURORA_PGSQL', usernameVariable: 'NX_DB_ADMINUSER', passwordVariable: 'NX_DB_ADMINPASS')]) {
                                try {
                                    sh '''#!/bin/bash -x
                                        REGION="eu-west-1"
                                        VPC=$(aws cloudformation list-exports --query "Exports[?Name=='qa-generic-revival-VPCStack'].Value" --output text --region ${REGION})
                                        SUBNET=$(aws cloudformation list-exports --query "Exports[?Name=='qa-generic-revival-SubnetSlaveStack'].Value" --output text --region ${REGION}) 
                                        aws cloudformation create-stack --stack-name aurora-db --template-body file://$WORKSPACE/integration/Jenkinsfiles/cfn_aurora_db.yaml --parameters file://<(jq -n --arg vpc "$VPC" --arg db_adminname "$NX_DB_ADMINNAME" --arg db_adminuser "$NX_DB_ADMINUSER" --arg db_adminpass "$NX_DB_ADMINPASS" --arg subnet "$SUBNET" '[{ParameterKey: "VPC", ParameterValue: $vpc}, {ParameterKey: "NXDBADMINNAME", ParameterValue: $db_adminname}, {ParameterKey: "NXDBADMINUSER", ParameterValue: $db_adminuser}, {ParameterKey: "NXDBADMINPASS", ParameterValue: $db_adminpass}, {ParameterKey: "SUBNET", ParameterValue: $subnet}]') --capabilities CAPABILITY_NAMED_IAM --region ${REGION}
                                        aws cloudformation wait stack-create-complete --stack-name aurora-db --region ${REGION}
                                        export NX_DB_HOST=$(aws cloudformation list-exports --query "Exports[?Name=='aurora-db-DatabaseId'].Value" --output text --region ${REGION})
                                        mvn -B -f $WORKSPACE/pom.xml install -Pqa,addons,customdb,$DBPROFILE -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT
                                    '''

                                } finally {
                                    sh '''#!/bin/bash -x
                                        REGION="eu-west-1"
                                        aws cloudformation delete-stack --stack-name aurora-db --region ${REGION}
                                        aws cloudformation wait stack-delete-complete --stack-name aurora-db --region ${REGION}
                                    '''
                                    archive '**/target/failsafe-reports/*, **/target/*.png, **/target/**/*.log, **/target/**/log/*'
                                    junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                                }
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
