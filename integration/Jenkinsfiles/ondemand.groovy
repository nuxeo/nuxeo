//if (currentBuild.previousBuild == null) { // first build, should configure job
    properties([
        [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
        disableConcurrentBuilds(),
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]])
//}

node('slacoin') {
    
    def sha = stage('clone') {
        checkout(
            [$class: 'GitSCM',
             branches: [[name: '*/${BRANCH_NAME}']],
             browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
             doGenerateSubmoduleConfigurations: false,
             extensions: [
                    [$class: 'CleanBeforeCheckout'],
                    [$class: 'CloneOption', noTags: true, reference: '', shallow: false]
                ],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'git@github.com:nuxeo/nuxeo.git']]
            ])
        return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }

    stage('rebase') {
        withBuildStatus('rebase', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            sh "./clone.py $BRANCH_NAME -f master --rebase"
        }
        stash('ws')
    }
    
    stage('compile') {
        withBuildStatus('compile', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                sh 'mvn -B test-compile -Pqa,addons,distrib -DskipTests'
            }
        }
    }

    stage('test') {
        withBuildStatus('test', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                sh 'mvn -B test -Pqa,addons,distrib -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
    }

    def zipfile=stage('verify') {
        withBuildStatus('verify', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                sh 'mvn -B verify -Pqa,addons,distrib,tomcat -DskipTests -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
        return sh(returnStdout: true, script: 'ls $WORKSPACE/nuxeo-distribution/nuxeo-server-tomcat/target/nuxeo-server-tomcat-*.zip')
    }

    stage('postgresql') {
        verifyProfile('pgsql', '9.6', sha, zipfile)
    }

    stage('mongo') {
        verifyProfile('mongodb', '3.4', sha, zipfile)
    }

    stage('kafka') {
        verifyProfile('kafka', '1.0', sha, zipfile)
    }
}

def verifyProfile(String profile, String version, String sha, String zipfile) {
    withEnv(["DBPROFILE=$profile", "DBVERSION=$version"]) {
        parallel(
            'cmis' : emitVerifyClosure('SLAVE', sha, zipfile, 'cmis', 'nuxeo-server-cmis-tests') {
                archive 'nuxeo-distribution/nuxeo-server-cmis-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.png, nuxeo-distribution/nuxeo-server-cmis-tests/target/*.json, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/*.log, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/log/*, nuxeo-distribution/nuxeo-server-cmis-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*'
                failOnServerError('nuxeo-distribution/nuxeo-server-cmis-tests/target/tomcat/log/server.log')
            },
            'webdriver' : emitVerifyClosure('SLAVE', sha, zipfile, 'webdriver', 'nuxeo-jsf-ui-webdriver-tests') {
                archive 'nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/failsafe-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.png, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/*.json, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/*.log, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/log/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/**/nxserver/config/distribution.properties, nuxeo-distribution/nuxeo-server-cmis-tests/target/nxtools-reports/*, nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/results/*/*'
                junit '**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/failsafe-reports/**/*.xml'
                failOnServerError('nuxeo-distribution/nuxeo-jsf-ui-webdriver-tests/target/tomcat/log/server.log')
            }
        )
    }
}

def emitVerifyClosure(String nodelabel, String sha, String zipfile, String name, String dir, Closure post) {
    return {
        node(nodelabel) {
            stage(name) {
                ws("${WORKSPACE}-${name}") {
                    unstash 'ws'
                    mvnopts = zipfile != "" ? "-Dzip.file=${WORKSPACE}/${zipfile}" : ""
                    mvncmd="mvn ${mvnopts} -B -f ${WORKSPACE}/nuxeo-distribution/${dir}/pom.xml -Pqa,tomcat,${DBPROFILE} verify"
                    echo mvncmd
                    timeout(time: 2, unit: 'HOURS') {
                        withBuildStatus("${DBPROFILE}-${DBVERSION}/ftest/${name}", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                            withDockerCompose("${JOB_NAME}-${BUILD_NUMBER}-${name}", "integration/Jenkinsfiles/docker-compose-${DBPROFILE}-${DBVERSION}.yml", mvncmd, post)
                        }
                    }
                }
            }
        }
    }
}
