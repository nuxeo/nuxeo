properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
    disableConcurrentBuilds(),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/'],
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    parameters([booleanParam(defaultValue: true, description: '', name: 'CLEAN_REPO')]),
    pipelineTriggers([cron('0 0 * * *')])
])


timestamps {

    timeout(time: 400, unit: 'MINUTES') {

        node ('SLAVE') {

            tool name: 'ant-1.9'
            tool name: 'maven-3'
            jdk = tool name: 'java-11-openjdk'
            env.JAVA_HOME = "${jdk}"
            def sha

            stage ('Checkout') {
                checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, userRemoteConfigs: [[credentialsId: '', url: 'https://github.com/nuxeo/nuxeo/']]])
            }
            stage ('Build') {

                withEnv(["MAVEN_OPTS=-Xms2g -Xmx4g"]) {

                    // Shell Clean Workspace step
                    sh """
                        export PATH="/opt/build/tools/maven3/bin:$PATH"
                        [ "$CLEAN_REPO" = "true" ] && rm -rf ${WORKSPACE}/.repository/org/nuxeo 2>/dev/null
                        ./clone.py master
                       """

                    sha = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()

                    withBuildStatus("mvn clean install", "https://github.com/nuxeo/nuxeo", sha, "${BUILD_URL}") {
                        withMaven( mavenLocalRepo: "$WORKSPACE/.repository") {
                            sh "mvn -B -f pom.xml -V clean install -DskipTests=true -DskipITs=true -Pdistrib "
                        }
                    }
                   parallel CheckDependencies: {

                       try {
                            withBuildStatus("mvn check dependencies", "https://github.com/nuxeo/nuxeo", sha, "${BUILD_URL}") {

                               withMaven {
                                sh "mvn -B -f pom.xml -V versions:display-dependency-updates -nsu -N "
                               }
                               withMaven {
                                sh """
                                   mvn -B -V  \
                                   dependency:tree \
                                   -Pdistrib,release \
                                   -DoutputFile="${WORKSPACE}/target/generated-sources/dependency-tree.log" \
                                   -DappendOutput=true \
                                   -nsu
                                   """
                               }
                               withMaven {
                                sh """
                                   mvn -B -V \
                                   org.owasp:dependency-check-maven:aggregate \
                                   -Pdistrib \
                                   -nsu
                                   """
                                }
                            }
                       } finally {

                            archiveArtifacts allowEmptyArchive: false, artifacts: 'target/generated-sources/license/THIRD-PARTY*,target/generated-sources/dependency-tree.log,target/dependency-check-report.html,target/outdated-dependencies.txt', caseSensitive: true, defaultExcludes: true, fingerprint: false, onlyIfSuccessful: false
                       }

                   }, checkLicenses: {

                       try {

                            withBuildStatus("mvn check licenses", "https://github.com/nuxeo/nuxeo", sha, "${BUILD_URL}") {

                               withMaven(mavenLocalRepo: "$WORKSPACE/.repository") {
                                   sh "mvn -B -f pom.xml -V license:aggregate-add-third-party -Pdistrib,release -nsu " // Licenses
                               }

                               def shellReturnStatus = sh returnStatus: true, script: """
                                ./scripts/generate-licenses.py
                                if [ -s target/generated-sources/license/THIRD-PARTY-BLACK.md ]; then
                                    echo "check black listed third parties for unknown or wrong licenses" >&2
                                    exit 1
                                fi
                                    exit 0
                                """  // Licenses
                               if (shellReturnStatus == 1) {
                                   currentBuild.result = 'UNSTABLE'
                               }
                            }
                       } finally {

                            archiveArtifacts allowEmptyArchive: false, artifacts: 'target/generated-sources/license/THIRD-PARTY*,target/generated-sources/dependency-tree.log,target/dependency-check-report.html,target/outdated-dependencies.txt', caseSensitive: true, defaultExcludes: true, fingerprint: false, onlyIfSuccessful: false
                       }
                   },
                   failFast: true
                   archiveArtifacts allowEmptyArchive: false, artifacts: 'target/generated-sources/license/THIRD-PARTY*,target/generated-sources/dependency-tree.log,target/dependency-check-report.html,target/outdated-dependencies.txt', caseSensitive: true, defaultExcludes: true, fingerprint: false, onlyIfSuccessful: false
                   warningsPublisher()
                }
            }
            stage ('PostBuild') {
              findText regexp: '.*WARNING(?!.*CHECKSUM FAILED.*)(?!.*Removing: jar from forked lifecycle.*)(?!.*org.codehaus.plexus:plexus-io:pom:1.0-alpha-3:runtime.*)(?!.*GWT plugin is configured to detect modules.*)(?!.*com.sun.jndi.ldap.LdapURL is Sun proprietary API.*)(?!.*Profile with id.*)(?!.*The requested profile \\"qa\\" could not be activated.*)(?!\\] $).*', unstableIfFound: true
              emailext body: '$DEFAULT_CONTENT', subject: '$DEFAULT_SUBJECT', to: 'ecm-qa@lists.nuxeo.com, $DEFAULT_RECIPIENTS'
              claimPublisher()
            }
        }
    }
}
