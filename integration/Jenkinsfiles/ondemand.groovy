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
        withBuildStatus('ondemand/1-rebase', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            echo  "./clone.py ${BRANCH} -f ${PARENT_BRANCH} --rebase"
        }
        stash(name: 'clone', excludes: '**/target/, **/node_modules/, **/bower_components/, **/marketplace/')
    }
    
    stage('compile') {
        withBuildStatus('ondemand/2-compile', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                echo  'mvn -nsu -B test-compile -Pqa,addons,distrib -DskipTests'
            }
        }
    }

    stage('test') {
        withBuildStatus('ondemand/3-test', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() { 
                echo  'mvn -nsu -B test -Pqa,addons,distrib -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
    }

    def zipfile=stage('verify') {
        withBuildStatus('ondemand/4-verify', 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
            withMaven() {
                echo  'mvn -nsu -B verify -Pqa,addons,distrib,tomcat -DskipTests -Dmaven.test.failure.ignore=true -Dnuxeo.tests.random.mode=STRICT'
            }
        }
        return sh(returnStdout: true, script: 'echo -n ${WORKSPACE}/nuxeo-distribution/nuxeo-server-tomcat/target/nuxeo-server-tomcat-*.zip')
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
            'cmis' : emitVerifyClosure(sha, zipfile, 'cmis', 'nuxeo-server-cmis-tests') {
                archive 'target/**/failsafe-reports/**/*, target/*.png, target/*.json, target/**/*.log, target/**/log/*, target/**/nxserver/config/distribution.properties, target/nxtools-reports/*'
                failOnServerError('target/tomcat/log/server.log')
            },
            'webdriver' : emitVerifyClosure(sha, zipfile, 'webdriver', 'nuxeo-jsf-ui-webdriver-tests') {
                archive 'target/**/failsafe-reports/**/*, target/*.png, target/*.json, target/**/*.log, target/**/log/*, target/**/nxserver/config/distribution.properties, target/nxtools-reports/*, target/results/**/*'
                junit 'target/surefire-reports/*.xml, target/**/failsafe-reports/**/*.xml'
                failOnServerError('target/tomcat/log/server.log')
            }
        )
    }
}

def emitVerifyClosure(String sha, String zipfile, String name, String module, Closure post) {
    return {
        stage(name) {
            timeout(time: 2, unit: 'HOURS') {
                withBuildStatus("ondemand/4-verify/${name}/${DBPROFILE}-${DBVERSION}", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                    withSwarmCompose("${BRANCH}-${BUILD_NUMBER}-${name}", "integration/Jenkinsfiles/docker-compose-${DBPROFILE}-${DBVERSION}.yml") {
                        def tmpdir = pwd(tmp: true)
                        dir(tmpdir) {
                            dir("${DBPROFILE}-${DBVERSION}-ftest-${name}") {
                                unstash 'clone'
                                dir("nuxeo-distribution/${module}") {
                                    try {
                                        withMaven() {
                                            sh "mvn -Dzip.file=${zipfile} -nsu -B -Pqa,tomcat,${DBPROFILE} verify"
                                        }
                                    } finally {
                                        post.call()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

