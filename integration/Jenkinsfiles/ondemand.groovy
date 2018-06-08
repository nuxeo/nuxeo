if (currentBuild.previousBuild == null) { // first build, should configure job
    properties([
        [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
        disableConcurrentBuilds(),
        [$class: 'ParametersDefinitionProperty', parameterDefinitions:
         [[$class: 'StringParameterDefinition', name: 'BRANCH', defaultValue: BRANCH_NAME],
          [$class: 'StringParameterDefinition', name: 'PARENT_BRANCH', defaultValue: 'master'],
          [$class: 'BooleanParameterDefinition', name: 'INCREMENTAL', defaultValue: false],
          [$class: 'StringParameterDefinition', name: 'SLAVE', defaultValue: 'SLAVE']]
        ],
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
        [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false]])
}

@Library('nuxeo@feature-NXBT-2323-compose-swarm') _

node(SLAVE) {
    
    def sha = stage('clone') {
        def emitExtensions = { isCleanRequired = false ->
            def extensions = []
            if (isCleanRequired) {
                extensions.add([$class: 'CleanBeforeCheckout'])
            }
            extensions.add([$class: 'CloneOption', noTags: true, reference: '', shallow: false])
            return extensions
        }
        
        checkout(
            [$class: 'GitSCM',
             branches: [[name: '*/${BRANCH}']],
             browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
             doGenerateSubmoduleConfigurations: false,
             extensions: emitExtensions(isCleanRequired=!INCREMENTAL),
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'git@github.com:nuxeo/nuxeo.git']]
            ])
        return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }

    stage('rebase') {
        withBuildStatus('rebase', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            sh "./clone.py ${BRANCH} -f ${PARENT_BRANCH} --rebase"
        }
        stash(name: 'ws', excludes: '**/target')
    }
    
    stage('compile') {
        withBuildStatus('compile', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                sh 'mvn -nsu -B test-compile -Pqa,addons,distrib -DskipTests'
            }
        }
    }

    stage('test') {
        withBuildStatus('test', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() { 
               sh 'mvn -nsu -B test -Pqa,addons,distrib -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
    }

    def zipfile=stage('verify') {
        withBuildStatus('verify', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                sh 'mvn -nsu -B verify -Pqa,addons,distrib,tomcat -DskipTests -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
        zipfile = sh(returnStdout: true, script: 'echo -n nuxeo-distribution/nuxeo-server-tomcat/target/nuxeo-server-tomcat-*.zip')
        stash([name: 'zipfile', includes: zipfile])
        return zipfile
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
                    unstash 'zipfile'
                    zipopt = zipfile != "" ? "-Dzip.file=${WORKSPACE}/${zipfile}" : ""
                    timeout(time: 2, unit: 'HOURS') {
                        withBuildStatus("${DBPROFILE}-${DBVERSION}/ftest/${name}", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                            withSwarmCompose("${JOB_NAME}-${BUILD_NUMBER}-${name}", "integration/Jenkinsfiles/docker-compose-${DBPROFILE}-${DBVERSION}.yml", post) {
                                withMaven() {
                                    sh "mvn ${zipopt} -nsu -B -f ${WORKSPACE}/nuxeo-distribution/${dir}/pom.xml -Pqa,tomcat,${DBPROFILE} verify"
				}
                            }
                        }
                    }
                }
            }
        }
    }
}
