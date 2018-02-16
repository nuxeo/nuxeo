properties([
    [$class: 'BuildDiscarderProperty', strategy: [$class: 'LogRotator', daysToKeepStr: '60', numToKeepStr: '60', artifactNumToKeepStr: '1']],
    disableConcurrentBuilds(),
    [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/nuxeo/nuxeo/'],
    [$class: 'RebuildSettings', autoRebuild: false, rebuildDisabled: false],
    [$class: 'ParametersDefinitionProperty', parameterDefinitions: [
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'BRANCH'],
        [$class: 'StringParameterDefinition', defaultValue: 'master', description: '', name: 'PARENT_BRANCH']]
        [$class: 'BooleanParameterDefinition', defaultValue: 'true', description: '', name: 'CLEAN'],
    ],
    pipelineTriggers([[$class: 'GitHubPushTrigger']])
  ])


node('SLAVE') {
    sha = stage('clone') {
        if (clean) {
            deleteDir()
        }
        checkout(
            [$class: 'GitSCM',
             branches: [[name: "*/$BRANCH"]],
             browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
             doGenerateSubmoduleConfigurations: false,
             extensions: [[$class: 'CleanBeforeCheckout']],
             submoduleCfg: [],
             userRemoteConfigs: [[url: 'git://github.com/nuxeo/nuxeo.git']]
            ])
        sh '''./clone.py --fallback $PARENT_BRANCH --rebase'''
        return sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    }
    withMaven(
        jdk: 'java-8-oracle',
        mvn: 'maven-3'
    ) {
        stage('compile') {
            withBuildStatus("$BRANCH/compile", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                checkout(
                    [$class: 'GitSCM',
                     branches: [[name: "*/$branch"]],
                     browser: [$class: 'GithubWeb', repoUrl: 'https://github.com/nuxeo/nuxeo'],
                     doGenerateSubmoduleConfigurations: false,
                     extensions: [
                            [$class: 'CloneOption', depth: 5, noTags: true, reference: '', shallow: true]
                        ],
                     submoduleCfg: [],
                     userRemoteConfigs: [[url: 'git://github.com/nuxeo/nuxeo.git']]
                    ])
                sh './clone.py'
                sh "mvn -Paddons,distrib compile -DskipTests"
            }
        }
        parallel(
            'test': {
                stage('test') {
                    node('SLAVE') {
                        ws("${WORKSPACE}-test") {
                            unstash('ws')
                            withBuildStatus("$BRANCH/test", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                                sh "mvn -Paddons,distrib test"
                            }
                        }
                    }
                }
            },
            'verify': {
                stage('verify') {
                    node('SLAVE') {
                        ws("${WORKSPACE}-verify") {
                            unstash('ws')
                            withBuildStatus("$BRANCH/verify", 'https://github.com/nuxeo/nuxeo', sha, "${BUILD_URL}") {
                                sh "mvn -Paddons,distrib -DskipTests verify"
                            }
                        }
                    }
                }
            }
        )
    }
}

sh '''
  which -a java mvn
  java -version
  mvn -V --version

  [[ &quot;\$BRANCH&quot; != &quot;\${BRANCH//[^A-Za-z0-9-_.\\/]/}&quot; ]] &amp;&amp; echo &quot;[WARNING] BRANCH name with invalid characters: \$(echo \$BRANCH|cat -v)&quot; &gt;&amp;2 || echo &quot;BRANCH name check: OK&quot;
BRANCH=\${BRANCH//[^A-Za-z0-9-_.\\/]/}
START=\$(date +%s)
unset DOCKER_HOST

if [ &quot;\$CLEAN&quot; = true ] || [ ! -e .git ]; then
  rm -rf * .??*
  git clone git@github.com:nuxeo/nuxeo.git .
fi
git checkout \$BRANCH 2&gt;/dev/null || git checkout \$PARENT_BRANCH

. scripts/gitfunctions.sh
if [ &quot;\$(type -t shr)&quot; != &quot;function&quot; ]; then
  echo ERROR: the current job is not compliant with this version of gitfunctions.sh
  exit 1
fi

if [ &quot;\$CLEAN&quot; = false ]; then
  gitfa fetch --all
  gitfa checkout \$PARENT_BRANCH
  gitfa pull --rebase
  ! gitfa -q diff --name-status --diff-filter=U | grep -B1 -e &quot;^U&quot;
fi

# switch on feature \$BRANCH if exists, else falls back on \$PARENT_BRANCH
./clone.py \$BRANCH -f \$PARENT_BRANCH
gitfa rebase origin/\$PARENT_BRANCH
! gitfa -q diff --name-status --diff-filter=U | grep -B1 -e &quot;^U&quot;
if [ &quot;\$MERGE_BEFORE_BUILD&quot; = true ]; then
  shr -a &quot;git checkout \$PARENT_BRANCH; git pull --rebase; git merge --no-ff \$BRANCH --log&quot;
  ! gitfa -q diff --name-status --diff-filter=U | grep -B1 -e &quot;^U&quot;
fi

rm -rf \$WORKSPACE/.repository/org/nuxeo/
echo &quot;Init \$((\$(date +%s) - \$START))&quot; &gt; \$WORKSPACE/.ci-metrics
echo &quot;\$(date +%s)&quot; &gt;\$WORKSPACE/.ci-metrics-mavenstart
'''

